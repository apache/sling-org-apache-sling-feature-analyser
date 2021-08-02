/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.feature.analyser.task.impl;

import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.scanner.ArtifactDescriptor;
import org.apache.sling.feature.scanner.impl.ContentPackageDescriptor;

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
    public void execute(final AnalyserTaskContext ctx)
    throws Exception {
        final Rules rules = getRules(ctx);

        if (rules != null ) {
            for (final ArtifactDescriptor d : ctx.getFeatureDescriptor().getArtifactDescriptors()) {
                if (d instanceof ContentPackageDescriptor) {
                    checkPackage(ctx, (ContentPackageDescriptor) d, rules);
                }
            }
        } else {
            ctx.reportError("Configuration for task " + getId() + " is missing.");
        }
    }

    static final class Rules {
        public String[] includes;
        public String[] excludes;
    }

    Rules getRules(final AnalyserTaskContext ctx) {
        final String inc = ctx.getConfiguration().get(PROP_INCLUDES);
        final String exc = ctx.getConfiguration().get(PROP_EXCLUDES);

        if ( inc != null || exc != null ) {
            final Rules r = new Rules();
            r.includes = inc == null ? null : inc.split(",");
            clean(r.includes);
            r.excludes = exc == null ? null : exc.split(",");
            clean(r.excludes);
            return r;
        }
        return null;
    }
    private static void clean(final String[] array) {
        if ( array != null ) {
            for(int i=0;i<array.length;i++) {
                array[i] = array[i].trim();
            }
        }
    }

    void checkPackage(final AnalyserTaskContext ctx, final ContentPackageDescriptor desc, final Rules rules) {
        for(final String path : desc.paths) {
            boolean isAllowed = rules.includes == null;
            int matchLength = 0;
            if ( !isAllowed ) {
                for(final String i : rules.includes) {
                    if ( path.equals(i) || path.startsWith(i.concat("/")) ) {
                        isAllowed = true;
                        matchLength = i.length();
                        break;
                    }
                }
            }
            if ( isAllowed && rules.excludes != null ) {
                for(final String i : rules.excludes) {
                    if ( path.equals(i) || path.startsWith(i.concat("/")) && i.length() > matchLength ) {
                        isAllowed = false;
                        break;
                    }
                }
            }
            if ( !isAllowed ) {
                ctx.reportArtifactError(desc.getArtifact().getId(), "Content not allowed: ".concat(path));
            }
        }
    }
}
