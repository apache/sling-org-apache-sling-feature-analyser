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

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import org.apache.jackrabbit.vault.packaging.Dependency;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageManager;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.apache.jackrabbit.vault.packaging.impl.PackageManagerImpl;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.io.IOUtils;
import org.apache.sling.feature.scanner.ArtifactDescriptor;
import org.apache.sling.feature.scanner.ContentPackageDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckContentPackagesDependencies implements AnalyserTask {

    private final PackageManager packageManager = new PackageManagerImpl();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public String getId() {
        return "content-packages-dependencies";
    }

    @Override
    public String getName() {
        return "Content packages dependencies checker";
    }

    @Override
    public void execute(AnalyserTaskContext ctx) throws Exception {
        Map<PackageId, Dependency[]> dependenciesMap = new HashMap<>();

        for (ArtifactDescriptor descriptor :
                ctx.getFeatureDescriptor().getDescriptors(ContentPackageDescriptor.class)) {
            onDescriptor(ctx, descriptor, dependenciesMap);
        }

        for (PackageId contentPackageId : dependenciesMap.keySet()) {
            verifyDependenciesTree(ctx, contentPackageId, dependenciesMap);
        }
    }

    private void onDescriptor(
            AnalyserTaskContext ctx, ArtifactDescriptor descriptor, Map<PackageId, Dependency[]> dependenciesMap)
            throws Exception {
        URL resourceUrl = descriptor.getArtifactFile();
        if (resourceUrl != null) {
            File artifactFile = IOUtils.getFileFromURL(resourceUrl, true, null);
            if (!artifactFile.exists() || !artifactFile.isFile()) {
                ctx.reportArtifactError(
                        descriptor.getArtifact().getId(),
                        "Artifact file " + artifactFile + " does not exist or it is not a file");
                return;
            }

            try (VaultPackage vaultPackage = packageManager.open(artifactFile, true)) {
                PackageId packageId = vaultPackage.getId();

                logger.debug("Collecting " + packageId + " dependencies...");

                dependenciesMap.put(packageId, vaultPackage.getDependencies());

                logger.debug(packageId + " dependencies collected.");
            }
        } else {
            ctx.reportArtifactError(
                    descriptor.getArtifact().getId(),
                    "Ignoring " + descriptor.getName() + " as file could not be found");
        }
    }

    private void verifyDependenciesTree(
            AnalyserTaskContext ctx, PackageId root, Map<PackageId, Dependency[]> dependenciesMap) {
        logger.debug("Verifying " + root + " transitive dependencies...");

        Queue<Dependency> toBeVisited = new LinkedList<>();
        enqueue(toBeVisited, dependenciesMap.get(root));

        Set<Dependency> alreadyVisited = new HashSet<>();

        while (!toBeVisited.isEmpty()) {
            Dependency current = toBeVisited.poll();

            if (alreadyVisited.add(current)) {
                boolean found = false;

                for (Entry<PackageId, Dependency[]> entry : dependenciesMap.entrySet()) {
                    if (current.matches(entry.getKey())) {
                        enqueue(toBeVisited, entry.getValue());
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    ctx.reportError("Missing " + current + " dependency for " + root);
                }
            }
        }
    }

    private static void enqueue(Queue<Dependency> target, Dependency... dependencies) {
        if (dependencies == null) {
            return;
        }

        for (Dependency dependency : dependencies) {
            target.offer(dependency);
        }
    }
}
