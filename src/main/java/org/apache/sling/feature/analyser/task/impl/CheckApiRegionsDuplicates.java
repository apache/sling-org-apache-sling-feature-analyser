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

import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.sling.feature.analyser.task.AnalyserTaskContext;

public class CheckApiRegionsDuplicates extends AbstractApiRegionsAnalyserTask {

    @Override
    public String getId() {
        return API_REGIONS_KEY + "-duplicates";
    }

    @Override
    public String getName() {
        return "Api Regions duplicates analyser task";
    }

    @Override
    protected void execute(ApiRegions apiRegions, AnalyserTaskContext ctx) throws Exception {
        List<String> sourceRegions = new ArrayList<>(apiRegions.getRegions());

        for (int i = 0; i < sourceRegions.size(); i++) {
            String sourceRegion = sourceRegions.get(i);
            List<String> targetRegions = sourceRegions.subList(i + 1, sourceRegions.size());

            for (String targetRegion : targetRegions) {
                if (sourceRegion.equals(targetRegion)) {
                    continue;
                }

                Set<String> intersection = calculateIntersection(apiRegions.getApis(sourceRegion), apiRegions.getApis(targetRegion));
                if (!intersection.isEmpty()) {
                    Formatter formatter = new Formatter();
                    formatter.format("Regions '%s' and '%s' defined in feature '%s' declare both %s package(s):%n",
                                     sourceRegion,
                                     targetRegion,
                                     ctx.getFeature().getId(),
                                     intersection.size());
                    intersection.forEach(api -> formatter.format(" * %s%n", api));

                    ctx.reportError(formatter.toString());

                    formatter.close();
                }
            }
        }
    }

    private static Set<String> calculateIntersection(Set<String> source, Set<String> target) {
        final Set<String> intersection = new HashSet<>();

        for (String packageName : source) {
            if (target.contains(packageName)) {
                intersection.add(packageName);
            }
        }

        return intersection;
    }

}
