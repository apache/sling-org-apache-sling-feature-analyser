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

import java.util.Map;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Artifacts;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.builder.FeatureProvider;

public class CheckCompareFeatures implements AnalyserTask {

    @Override
    public String getId() {
        return "compare-features";
    }

    @Override
    public String getName() {
        return "Comparing Features";
    }

    @Override
    public void execute(AnalyserTaskContext ctx) throws Exception {
        Map<String, String> cfg = ctx.getConfiguration();
        String aid = cfg.get("compare-with");
        if (aid == null) {
            throw new Exception("Missing 'compare-with' configuration for compare-features analyser.");
        }

        String ext = cfg.get("compare-extension");
        String mode = cfg.getOrDefault("compare-mode", "SAME");
        String type = cfg.getOrDefault("compare-type", "ARTIFACTS");
        if (!"ARTIFACTS".equals(type)) {
            throw new Exception("The only supported value for 'compare-type' right now is ARTIFACTS");
        }

        boolean strictMetadata = !cfg.getOrDefault("compare-metadata", "false").equalsIgnoreCase("false");

        FeatureProvider featureProvider = ctx.getFeatureProvider();
        if (featureProvider == null) {
            throw new Exception("This analyser requires a Feature Provider to be set in the Analyser Task Context.");
        }

        Feature feat = featureProvider.provide(ArtifactId.fromMvnId(aid));
        if (feat == null) throw new Exception("Feature not found: " + aid);

        Artifacts mainArts = getArtifactsToCompare(feat, ext);
        Artifacts compArts = getArtifactsToCompare(ctx.getFeature(), ext);

        String violationMessage = null;
        switch (mode) {
            case "SAME":
                violationMessage = assertArtifactsSame(mainArts, compArts, strictMetadata);
                break;
            case "DIFFERENT":
                violationMessage = assertArtifactsSame(mainArts, compArts, strictMetadata);
                if (violationMessage == null) {
                    violationMessage = "Artifacts are not different";
                } else {
                    violationMessage = null;
                }
                break;
            default:
                throw new Exception("Unknown comparison mode: " + mode);
        }

        if (violationMessage != null) {
            String origin;
            if (ext == null) {
                origin = "bundles";
            } else {
                origin = "extension " + ext;
            }

            ctx.reportError("Compare " + origin + " in feature " + feat.getId() + " and "
                    + ctx.getFeature().getId() + " failed: " + violationMessage);
        }
    }

    static String assertArtifactsSame(Artifacts mainArts, Artifacts compArts, boolean strictMetadata) {
        if (mainArts.size() != compArts.size()) {
            return "Compared artifacts are of different sizes";
        }

        for (Artifact a : mainArts) {
            Artifact a2 = findArtifact(compArts, a.getId());
            if (a2 == null) {
                return "Artifact " + a.getId() + " not found.";
            }

            if (strictMetadata) {
                Map<String, String> md1 = a.getMetadata();
                Map<String, String> md2 = a2.getMetadata();
                if (!md1.equals(md2)) {
                    return "Metadata of " + a.getId() + " is different: " + md1 + " vs " + md2;
                }
            }
        }
        return null;
    }

    private static Artifact findArtifact(Artifacts list, ArtifactId artifactId) {
        for (Artifact a : list) {
            if (a.getId().equals(artifactId)) return a;
        }
        return null;
    }

    static Artifacts getArtifactsToCompare(Feature feat, String ext) throws Exception {
        Artifacts artifacts;
        if (ext == null) {
            // compare bundles
            artifacts = feat.getBundles();
        } else {
            // compare extensions
            Extension extension = feat.getExtensions().getByName(ext);
            if (extension == null) {
                throw new Exception("Extension " + ext + " not found in feature " + feat.getId());
            }
            if (ExtensionType.ARTIFACTS != extension.getType()) {
                throw new Exception("Extension " + extension + " is not of type ARTIFACTS.");
            }

            artifacts = extension.getArtifacts();
        }
        return artifacts;
    }
}
