/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.feature.analyser.task.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.felix.utils.resource.CapabilitySet;
import org.apache.felix.utils.resource.RequirementImpl;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.scanner.ArtifactDescriptor;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.Descriptor;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Requirement;

public class CheckRequirementsCapabilities implements AnalyserTask {
    private final String format = "Artifact %s requires %s in start level %d but %s";

    @Override
    public String getId() {
        return "requirements-capabilities";
    }

    @Override
    public String getName() {
        return "Requirements Capabilities check";
    }

    @Override
    public void execute(AnalyserTaskContext ctx) throws Exception {
        final SortedMap<Integer, List<Descriptor>> artifactsMap = new TreeMap<>();
        for (final BundleDescriptor bi : ctx.getFeatureDescriptor().getBundleDescriptors()) {
            List<Descriptor> list = getDescriptorList(bi.getArtifact().getStartOrder(), artifactsMap);
            list.add(bi);
        }

        if (!ctx.getFeatureDescriptor().getArtifactDescriptors().isEmpty()) {
            artifactsMap.put(
                    (artifactsMap.isEmpty() ? 0 : artifactsMap.lastKey()) + 1,
                    new ArrayList<>(ctx.getFeatureDescriptor().getArtifactDescriptors()));
        }

        String featureMavenID = ctx.getFeature().getId().toMvnId();

        // Add descriptor for feature capabilties. These are added at start level 0
        Descriptor featureCaps = new ReqCapDescriptor(featureMavenID);
        featureCaps.getCapabilities().addAll(ctx.getFeature().getCapabilities());
        getDescriptorList(0, artifactsMap).add(featureCaps);

        // Add descriptor for feature requirements. These are added at the highest start level found
        Descriptor featureReqs = new ReqCapDescriptor(featureMavenID);
        featureReqs.getRequirements().addAll(ctx.getFeature().getRequirements());
        Integer highestStartLevel = artifactsMap.lastKey();
        getDescriptorList(highestStartLevel, artifactsMap).add(featureReqs);

        // add system artifact
        final List<Descriptor> artifacts = new ArrayList<>();
        if (ctx.getFrameworkDescriptor() != null) {
            artifacts.add(ctx.getFrameworkDescriptor());
        }

        boolean errorReported = false;
        for (final Map.Entry<Integer, List<Descriptor>> entry : artifactsMap.entrySet()) {
            // first add all providing artifacts
            for (final Descriptor info : entry.getValue()) {
                if (info.getCapabilities() != null) {
                    artifacts.add(info);
                }
            }
            // check requiring artifacts
            for (final Descriptor info : entry.getValue()) {
                if (info.getRequirements() != null) {
                    for (Requirement requirement : info.getRequirements()) {
                        String ns = requirement.getNamespace();

                        // Package namespace is handled by the CheckBundleExportsImports analyzer.
                        // Service namespace is special - we don't provide errors or warnings in this case
                        if (!BundleRevision.PACKAGE_NAMESPACE.equals(ns)
                                && !ServiceNamespace.SERVICE_NAMESPACE.equals(ns)) {
                            List<Descriptor> candidates = getCandidates(artifacts, requirement);

                            if (candidates.isEmpty()) {
                                if (!RequirementImpl.isOptional(requirement)) {
                                    String message = String.format(
                                            format,
                                            info.getName(),
                                            requirement.toString(),
                                            entry.getKey(),
                                            "no artifact is providing a matching capability in this start level.");
                                    if (info instanceof ArtifactDescriptor) {
                                        ctx.reportArtifactError(
                                                ((ArtifactDescriptor) info)
                                                        .getArtifact()
                                                        .getId(),
                                                message);
                                    } else {
                                        ctx.reportError(message);
                                    }
                                    errorReported = true;
                                } else {
                                    String message = String.format(
                                            format,
                                            info.getName(),
                                            requirement.toString(),
                                            entry.getKey(),
                                            "while the requirement is optional no artifact is providing a matching capability in this start level.");
                                    if (info instanceof ArtifactDescriptor) {
                                        ctx.reportArtifactWarning(
                                                ((ArtifactDescriptor) info)
                                                        .getArtifact()
                                                        .getId(),
                                                message);
                                    } else {
                                        ctx.reportWarning(message);
                                    }
                                }
                            } else if (candidates.size() > 1) {
                                String message = String.format(
                                        format,
                                        info.getName(),
                                        requirement.toString(),
                                        entry.getKey(),
                                        "there is more than one matching capability in this start level: "
                                                + candidates);
                                if (info instanceof ArtifactDescriptor) {
                                    ctx.reportArtifactWarning(
                                            ((ArtifactDescriptor) info)
                                                    .getArtifact()
                                                    .getId(),
                                            message);
                                } else {
                                    ctx.reportWarning(message);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (errorReported && ctx.getFeature().isComplete()) {
            ctx.reportError(
                    ctx.getFeature().getId().toMvnId() + " is marked as 'complete' but has unsatisfied requirements.");
        }
    }

    private List<Descriptor> getDescriptorList(int sl, SortedMap<Integer, List<Descriptor>> artifactsMap) {
        List<Descriptor> list = artifactsMap.get(sl);
        if (list == null) {
            list = new ArrayList<>();
            artifactsMap.put(sl, list);
        }
        return list;
    }

    private List<Descriptor> getCandidates(List<Descriptor> artifactDescriptors, Requirement requirement) {
        return artifactDescriptors.stream()
                .filter(artifactDescriptor -> artifactDescriptor.getCapabilities() != null)
                .filter(artifactDescriptor -> artifactDescriptor.getCapabilities().stream()
                        .anyMatch(capability -> CapabilitySet.matches(capability, requirement)))
                .collect(Collectors.toList());
    }

    static class ReqCapDescriptor extends Descriptor {
        protected ReqCapDescriptor(String name) {
            super(name);
        }
    }
}
