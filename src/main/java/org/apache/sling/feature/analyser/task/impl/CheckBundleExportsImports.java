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
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.PackageInfo;

public class CheckBundleExportsImports implements AnalyserTask {

    @Override
    public String getName() {
        return "Bundle Import/Export Check";
    }

    @Override
    public String getId() {
        return "bundle-packages";
    }

    public static final class Report {

        public List<PackageInfo> exportMatchingSeveral = new ArrayList<>();

        public List<PackageInfo> missingExports = new ArrayList<>();

        public List<PackageInfo> missingExportsWithVersion = new ArrayList<>();

        public List<PackageInfo> missingExportsForOptional = new ArrayList<>();
    }

    private Report getReport(final Map<BundleDescriptor, Report> reports, final BundleDescriptor info) {
        return reports.computeIfAbsent(info, key -> new Report());
    }

    @Override
    public void execute(final AnalyserTaskContext ctx) throws IOException {
        // basic checks
        final Map<BundleDescriptor, Report> reports = new HashMap<>();

        final SortedMap<Integer, List<BundleDescriptor>> bundlesMap = new TreeMap<>();
        for(final BundleDescriptor bi : ctx.getFeatureDescriptor().getBundleDescriptors()) {
            bundlesMap.computeIfAbsent(bi.getArtifact().getStartOrder(), key -> new ArrayList<>()).add(bi);
        }

        // add all system packages
        final List<BundleDescriptor> exportingBundles = new ArrayList<>();
        if ( ctx.getFrameworkDescriptor() != null ) {
            exportingBundles.add(ctx.getFrameworkDescriptor());
        }

        for(final Map.Entry<Integer, List<BundleDescriptor>> entry : bundlesMap.entrySet()) {
            // first add all exporting bundles
            for(final BundleDescriptor info : entry.getValue()) {
                if ( !info.getExportedPackages().isEmpty() ) {
                    exportingBundles.add(info);
                }
            }
            // check importing bundles
            for(final BundleDescriptor info : entry.getValue()) {
                for(final PackageInfo pck : info.getImportedPackages() ) {
                    final List<BundleDescriptor> candidates = getCandidates(exportingBundles, pck);
                    if ( candidates.isEmpty() ) {
                        if ( pck.isOptional() ) {
                            getReport(reports, info).missingExportsForOptional.add(pck);
                        } else {
                            getReport(reports, info).missingExports.add(pck);
                        }
                    } else {
                        final List<BundleDescriptor> matchingCandidates = new ArrayList<>();
                        for (final BundleDescriptor i : candidates) {
                            if (i.isExportingPackage(pck)) {
                                matchingCandidates.add(i);
                            }
                        }
                        if ( matchingCandidates.isEmpty() ) {
                            if ( pck.isOptional() ) {
                                getReport(reports, info).missingExportsForOptional.add(pck);
                            } else {
                                getReport(reports, info).missingExportsWithVersion.add(pck);
                            }
                        } else if ( matchingCandidates.size() > 1 ) {
                            getReport(reports, info).exportMatchingSeveral.add(pck);
                        }
                    }
                }
            }
        }

        boolean errorReported = false;
        for(final Map.Entry<BundleDescriptor, Report> entry : reports.entrySet()) {
            if ( !entry.getValue().missingExports.isEmpty() ) {
                ctx.reportArtifactError(entry.getKey().getArtifact().getId(), "Bundle is importing " + getPackageInfo(entry.getValue().missingExports, false) + " with start order " +
                        String.valueOf(entry.getKey().getArtifact().getStartOrder())
                        + " but no bundle is exporting these for that start order.");
                errorReported = true;
            }
            if ( !entry.getValue().missingExportsWithVersion.isEmpty() ) {
                ctx.reportArtifactError(entry.getKey().getArtifact().getId(), "Bundle is importing "
                        + getPackageInfo(entry.getValue().missingExportsWithVersion, true) + " with start order "
                        + String.valueOf(entry.getKey().getArtifact().getStartOrder())
                        + " but no bundle is exporting these for that start order in the required version range.");
                errorReported = true;
            }
        }
        if (errorReported && ctx.getFeature().isComplete()) {
            ctx.reportError(ctx.getFeature().getId().toMvnId() + " is marked as 'complete' but has missing imports.");
        }
    }

    private String getPackageInfo(final List<PackageInfo> pcks, final boolean includeVersion) {
        if ( pcks.size() == 1 ) {
            final StringBuilder sb = new StringBuilder("package ");
            sb.append(pcks.get(0).getName());
            if (includeVersion) {
                sb.append(";version=");
                sb.append(pcks.get(0).getVersion());
            }
            return sb.toString();
        }
        final StringBuilder sb = new StringBuilder("packages ");
        boolean first = true;
        sb.append('[');
        for(final PackageInfo info : pcks) {
            if ( first ) {
                first = false;
            } else {
                sb.append(", ");
            }
            if (includeVersion) {
                sb.append(info.toString());
            } else {
                sb.append(info.getName());
            }
        }
        sb.append(']');
        return sb.toString();
    }

    private List<BundleDescriptor> getCandidates(final List<BundleDescriptor> exportingBundles, final PackageInfo pck) {
        final List<BundleDescriptor> candidates = new ArrayList<>();
        for(final BundleDescriptor info : exportingBundles) {
            if ( info.isExportingPackage(pck.getName()) ) {
                candidates.add(info);
            }
        }
        return candidates;
    }
}