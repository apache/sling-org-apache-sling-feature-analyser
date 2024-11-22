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

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.scanner.ContentPackageDescriptor;

/**
 * This analyser checks for content paths in packages
 */
public class CheckContentPackagesForPaths implements AnalyserTask {

    private static final String PROP_INCLUDES = "includes";

    private static final String PROP_EXCLUDES = "excludes";

    @Override
    public String getName() {
        return "Content Packages Path Check";
    }

    @Override
    public String getId() {
        return "content-packages-paths";
    }

    @Override
    public void execute(final AnalyserTaskContext ctx) throws Exception {
        final Rules rules = new Rules(ctx);
        if (!rules.isConfigured()) {
            ctx.reportError("Configuration for task " + getId() + " is missing.");
            return;
        }

        for (final ContentPackageDescriptor d :
                ctx.getFeatureDescriptor().getDescriptors(ContentPackageDescriptor.class)) {
            checkPackage(ctx, d, rules);
        }
    }

    void checkPackage(final AnalyserTaskContext ctx, final ContentPackageDescriptor desc, final Rules rules) {
        desc.getContentPaths().stream()
                .filter(rules::isDisAllowed)
                .forEach(path -> ctx.reportArtifactError(desc.getArtifact().getId(), "Content not allowed: " + path));
    }

    static final class Rules {
        final String[] includes;
        final String[] excludes;

        Rules(final AnalyserTaskContext ctx) {
            final String inc = ctx.getConfiguration().get(PROP_INCLUDES);
            final String exc = ctx.getConfiguration().get(PROP_EXCLUDES);
            includes = splitAndTrim(inc);
            excludes = splitAndTrim(exc);
        }

        private String[] splitAndTrim(String property) {
            if (property == null) {
                return new String[] {};
            }

            return Stream.of(property.split(","))
                    .map(String::trim)
                    .collect(Collectors.toList())
                    .toArray(new String[] {});
        }

        boolean isConfigured() {
            return includes.length > 0 || excludes.length > 0;
        }

        boolean isDisAllowed(String path) {
            return !isAllowed(path);
        }

        boolean isAllowed(String path) {
            boolean isAllowed = includes.length == 0;
            int matchLength = 0;
            if (!isAllowed) {
                for (final String i : includes) {
                    if (path.equals(i) || path.startsWith(i.concat("/"))) {
                        isAllowed = true;
                        matchLength = i.length();
                        break;
                    }
                }
            }
            if (isAllowed && excludes.length > 0) {
                for (final String i : excludes) {
                    if (path.equals(i) || path.startsWith(i.concat("/")) && i.length() > matchLength) {
                        isAllowed = false;
                        break;
                    }
                }
            }
            return isAllowed;
        }
    }
}
