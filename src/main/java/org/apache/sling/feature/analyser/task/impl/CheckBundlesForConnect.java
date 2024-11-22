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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.osgi.framework.Constants;

public class CheckBundlesForConnect implements AnalyserTask {

    @Override
    public String getName() {
        return "Bundle Check For Connect";
    }

    @Override
    public String getId() {
        return "bundle-connect";
    }

    @Override
    public void execute(final AnalyserTaskContext ctx) {
        final Map<String, List<Artifact>> packageMap = new HashMap<>();
        for (final BundleDescriptor bd : ctx.getFeatureDescriptor().getBundleDescriptors()) {
            if (bd.getManifest() != null) {
                final String cp = bd.getManifest().getMainAttributes().getValue(Constants.BUNDLE_CLASSPATH);
                String[] jars = null;
                if (cp != null) {
                    jars = cp.split(",");
                    ctx.reportArtifactWarning(bd.getArtifact().getId(), "Found bundle classpath in : " + cp);
                }

                final Set<String> packages = new HashSet<>();
                try (final JarInputStream jis =
                        new JarInputStream(bd.getArtifactFile().openStream())) {
                    JarEntry entry;
                    while ((entry = jis.getNextJarEntry()) != null) {
                        if (entry.getName().endsWith(".class")) {
                            final int lastPos = entry.getName().lastIndexOf('/');
                            if (lastPos == -1) {
                                ctx.reportArtifactError(
                                        bd.getArtifact().getId(), "Bundle contains classes in the default package");
                            } else {
                                packages.add(entry.getName().substring(0, lastPos));
                            }
                        } else if (!entry.isDirectory() && jars != null) {
                            for (final String jar : jars) {
                                if (jar.equals(entry.getName())) {
                                    final JarInputStream is = new JarInputStream(jis);
                                    JarEntry inner;
                                    while ((inner = is.getNextJarEntry()) != null) {
                                        if (inner.getName().endsWith(".class")) {
                                            final int lastPos = inner.getName().lastIndexOf('/');
                                            if (lastPos == -1) {
                                                ctx.reportArtifactError(
                                                        bd.getArtifact().getId(),
                                                        "Bundle contains (embedded) classes in the default package");
                                            } else {
                                                packages.add(inner.getName().substring(0, lastPos));
                                            }
                                        }
                                        is.closeEntry();
                                    }
                                }
                            }
                        }
                        jis.closeEntry();
                    }
                } catch (final IOException ioe) {
                    ctx.reportArtifactError(bd.getArtifact().getId(), "Unable to scan bundle: " + ioe.getMessage());
                }
                for (final String p : packages) {
                    List<Artifact> list = packageMap.get(p);
                    if (list == null) {
                        list = new ArrayList<>();
                        packageMap.put(p, list);
                    }
                    list.add(bd.getArtifact());
                }
            }
        }

        for (final Map.Entry<String, List<Artifact>> entry : packageMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                ctx.reportWarning("Duplicate package " + entry.getKey() + " in " + entry.getValue());
            }
        }
    }
}
