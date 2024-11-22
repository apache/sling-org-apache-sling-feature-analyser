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
import java.util.Arrays;
import java.util.List;

import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.PackageInfo;
import org.osgi.framework.Version;

public class CheckBundleUnversionedPackages implements AnalyserTask {

    /** Ignore JDK packages */
    private static final List<String> IGNORED_IMPORT_PREFIXES =
            Arrays.asList("java.", "javax.", "org.w3c.", "org.xml.");

    @Override
    public String getName() {
        return "Bundle Unversioned Packages Check";
    }

    @Override
    public String getId() {
        return "bundle-unversioned-packages";
    }

    private boolean ignoreImportPackage(final String name) {
        for (final String prefix : IGNORED_IMPORT_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void execute(final AnalyserTaskContext ctx) throws Exception {
        for (final BundleDescriptor info : ctx.getFeatureDescriptor().getBundleDescriptors()) {

            final List<PackageInfo> exportWithoutVersion = new ArrayList<>();
            for (final PackageInfo i : info.getExportedPackages()) {
                if (i.getPackageVersion().compareTo(Version.emptyVersion) == 0) {
                    exportWithoutVersion.add(i);
                }
            }
            final List<PackageInfo> importWithoutVersion = new ArrayList<>();
            for (final PackageInfo i : info.getImportedPackages()) {
                if (i.getVersion() == null && !ignoreImportPackage(i.getName())) {
                    importWithoutVersion.add(i);
                }
            }

            final String key = "Bundle "
                    .concat(info.getArtifact().getId().getArtifactId())
                    .concat(":")
                    .concat(info.getArtifact().getId().getVersion());

            if (!importWithoutVersion.isEmpty()) {
                ctx.reportArtifactWarning(
                        info.getArtifact().getId(),
                        key.concat(" is importing ")
                                .concat(getPackageInfo(importWithoutVersion))
                                .concat(" without specifying a version range."));
            }
            if (!exportWithoutVersion.isEmpty()) {
                ctx.reportArtifactWarning(
                        info.getArtifact().getId(),
                        key.concat(" is exporting ")
                                .concat(getPackageInfo(exportWithoutVersion))
                                .concat(" without a version."));
            }
        }
    }

    private String getPackageInfo(final List<PackageInfo> pcks) {
        if (pcks.size() == 1) {
            return "package ".concat(pcks.get(0).getName());
        }
        final StringBuilder sb = new StringBuilder("packages ");
        boolean first = true;
        sb.append('[');
        for (final PackageInfo info : pcks) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(info.getName());
        }
        sb.append(']');
        return sb.toString();
    }
}
