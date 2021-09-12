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
import org.apache.sling.feature.scanner.ContentPackageDescriptor;

/**
 * This analyser checks for bundles and configurations in packages
 */
public class CheckContentPackageForInstallables implements AnalyserTask {

    static final String CFG_CHECK_PACKAGES = "embedded-packages";

    @Override
    public String getName() {
        return "Content Packages Installable Check";
    }

    @Override
    public String getId() {
        return "content-packages-installables";
    }

    @Override
    public void execute(final AnalyserTaskContext ctx)
            throws Exception {
        final boolean checkPcks = Boolean.parseBoolean(ctx.getConfiguration().getOrDefault(CFG_CHECK_PACKAGES, "false"));

        for (final ContentPackageDescriptor cp : ctx.getFeatureDescriptor().getDescriptors(ContentPackageDescriptor.class)) {
            if (cp.getArtifactFile() ==  null) {
                ctx.reportArtifactError(cp.getArtifact().getId(), "Content package " + cp.getName() + " is not resolved and can not be checked.");
                continue;
            }
            if ( checkPcks && cp.isEmbeddedInContentPackage() ) {
                ctx.reportArtifactError(cp.getParentContentPackage().getId(), "Content package " + cp.getParentContentPackage().getId() +
                        " embedds content package " + cp.getName());
            }
            if (!cp.hasEmbeddedArtifacts() || cp.isEmbeddedInContentPackage()) {
                continue;
            }

            ctx.reportArtifactError(cp.getArtifact().getId(), "Content package " + cp.getName() +
                    " contains " + String.valueOf(cp.getBundles().size()) + " bundles and "
                    + String.valueOf(cp.getConfigurations().size()) + " configurations.");

        }
    }
}
