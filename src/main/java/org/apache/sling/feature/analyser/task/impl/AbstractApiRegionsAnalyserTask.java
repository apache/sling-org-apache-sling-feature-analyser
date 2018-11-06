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

import javax.json.stream.JsonParsingException;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Extensions;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;

public abstract class AbstractApiRegionsAnalyserTask implements AnalyserTask {

    protected static final String API_REGIONS_KEY = "api-regions";

    @Override
    public final void execute(AnalyserTaskContext ctx) throws Exception {
        Feature feature = ctx.getFeature();

        // extract and check the api-regions

        Extensions extensions = feature.getExtensions();
        Extension apiRegionsExtension = extensions.getByName(API_REGIONS_KEY);
        if (apiRegionsExtension == null) {
            // no need to be analyzed
            return;
        }

        String jsonRepresentation = apiRegionsExtension.getJSON();
        if (jsonRepresentation == null || jsonRepresentation.isEmpty()) {
            // no need to be analyzed
            return;
        }

        // read the api-regions and create a Sieve data structure for checks

        ApiRegions apiRegions;
        try {
            apiRegions = ApiRegions.fromJson(jsonRepresentation);
        } catch (JsonParsingException e) {
            ctx.reportError("API Regions '"
                    + jsonRepresentation
                    + "' does not represent a valid JSON 'api-regions': "
                    + e.getMessage());
            return;
        }

        execute(apiRegions, ctx);
    }

    protected abstract void execute(ApiRegions apiRegions, AnalyserTaskContext ctx) throws Exception;

}
