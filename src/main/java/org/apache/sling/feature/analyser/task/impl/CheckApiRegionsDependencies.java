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

import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.FeatureDescriptor;
import org.apache.sling.feature.scanner.PackageInfo;
import org.osgi.framework.Constants;

import java.util.List;
import java.util.Set;

public class CheckApiRegionsDependencies extends AbstractApiRegionsAnalyserTask {

    @Override
    public String getId() {
        return API_REGIONS_KEY + "-dependencies";
    }

    @Override
    public String getName() {
        return "Api Regions dependecies analyser task";
    }

    @Override
    protected void execute(ApiRegions apiRegions, AnalyserTaskContext ctx) throws Exception {
        List<String> regions = apiRegions.getRegions();

        for (int i=0; i < regions.size(); i++) {
            for (int j=i+1; j < regions.size(); j++) {
                execute(ctx, apiRegions, regions.get(i), regions.get(j));
            }
        }
    }

    private void execute(AnalyserTaskContext ctx, ApiRegions apiRegions, String exportingApisName, String hidingApisName) {
        Set<String> exportingApis = apiRegions.getApis(exportingApisName);
        Set<String> hidingApis = apiRegions.getApis(hidingApisName);

        FeatureDescriptor featureDescriptor = ctx.getFeatureDescriptor();
        for (BundleDescriptor bundleDescriptor : featureDescriptor.getBundleDescriptors()) {
            for (PackageInfo packageInfo : bundleDescriptor.getExportedPackages()) {
                String exportedPackage = packageInfo.getName();

                if (exportingApis.contains(exportedPackage)) {
                    if (hidingApis.contains(exportedPackage)) {
                        String errorMessage = String.format(
                                "Bundle '%s' (defined in feature '%s') declares '%s' in the '%s' header that is enlisted in both exporting '%s' and hiding '%s' APIs regions, please adjust Feature settings",
                                bundleDescriptor.getArtifact().getId(),
                                ctx.getFeature().getId(),
                                exportedPackage,
                                Constants.EXPORT_PACKAGE,
                                exportingApisName,
                                hidingApisName);
                        ctx.reportError(errorMessage);
                    } else {
                        for (String uses : packageInfo.getUses()) {
                            if (hidingApis.contains(uses)) {
                                String errorMessage = String.format(
                                        "Bundle '%s' (defined in feature '%s') declares '%s' in the '%s' header, enlisted in the '%s' region, which uses '%s' package that is in the '%s' region",
                                        bundleDescriptor.getArtifact().getId(),
                                        ctx.getFeature().getId(),
                                        exportedPackage,
                                        Constants.EXPORT_PACKAGE,
                                        exportingApisName,
                                        uses,
                                        hidingApisName);
                                ctx.reportError(errorMessage);
                            }
                        }
                    }
                }
            }
        }
    }

}
