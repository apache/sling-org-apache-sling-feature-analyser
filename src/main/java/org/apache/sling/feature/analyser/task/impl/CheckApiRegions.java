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

import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
import javax.json.stream.JsonParsingException;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Extensions;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.FeatureDescriptor;
import org.apache.sling.feature.scanner.PackageInfo;

public class CheckApiRegions implements AnalyserTask {

    private static final String API_REGIONS_KEY = "api-regions";

    private static final String NAME_KEY = "name";

    private static final String EXPORTS_KEY = "exports";

    @Override
    public String getId() {
        return API_REGIONS_KEY;
    }

    @Override
    public String getName() {
        return "Api Regions analyser task";
    }

    @Override
    public void execute(AnalyserTaskContext ctx) throws Exception {
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
            apiRegions = fromJson(jsonRepresentation);
        } catch (JsonParsingException e) {
            ctx.reportError("API Regions '"
                    + jsonRepresentation
                    + "' does not represent a valid JSON 'api-regions': "
                    + e.getMessage());
            return;
        }

        // then, for each bundle, get the Export-Package and process the packages

        FeatureDescriptor featureDescriptor = ctx.getFeatureDescriptor();
        for (BundleDescriptor bundleDescriptor : featureDescriptor.getBundleDescriptors()) {
            for (PackageInfo packageInfo : bundleDescriptor.getExportedPackages()) {
                String exportedPackage = packageInfo.getName();
                // use the Sieve technique: remove bundle exported packages from the api-regions
                apiRegions.remove(exportedPackage);
            }
        }

        // final evaluation: if the Sieve is not empty, not all declared packages are exported by bundles of the same feature
        if (!apiRegions.isEmpty()) {
            // track a single error for each region
            for (String region : apiRegions.getRegions()) {
                Set<String> apis = apiRegions.getApis(region);
                if (!apis.isEmpty()) {
                    Formatter formatter = new Formatter();
                    formatter.format("Region '%s' defined in feature '%s' declares %s package%s which %s not exported by any bundle:%n",
                                     region,
                                     feature.getId(),
                                     apis.size(),
                                     getExtension(apis, "", "s"),
                                     getExtension(apis, "is", "are"));
                    apis.forEach(api -> formatter.format(" * %s%n", api));

                    ctx.reportError(formatter.toString());

                    formatter.close();
                }
            }
        }
    }

    // utility methods

    private static <T> String getExtension(Collection<T> collection, String singular, String plural) {
        return collection.size() > 1 ? plural : singular;
    }

    private static ApiRegions fromJson(String jsonRepresentation) {
        ApiRegions apiRegions = new ApiRegions();

        // pointers
        Event event;
        String region = null;
        Collection<String> apis = null;

        JsonParser parser = Json.createParser(new StringReader(jsonRepresentation));
        while (parser.hasNext()) {
            event = parser.next();
            if (Event.KEY_NAME == event) {
                switch (parser.getString()) {
                    case NAME_KEY:
                        parser.next();
                        region = parser.getString();
                        break;

                    case EXPORTS_KEY:
                        apis = new LinkedList<>();

                        // start array
                        parser.next();

                        while (parser.hasNext() && Event.VALUE_STRING == parser.next()) {
                            String api = parser.getString();
                            // skip comments
                            if ('#' != api.charAt(0)) {
                                apis.add(api);
                            }
                        }

                        break;

                    default:
                        break;
                }
            } else if (Event.END_OBJECT == event) {
                if (region != null && apis != null) {
                    apiRegions.add(region, apis);
                }

                region = null;
                apis = null;
            }
        }

        return apiRegions;
    }

    // the Sieve data structure to check exported packages

    private static final class ApiRegions {

        // class members

        private final Map<String, Set<String>> apis = new TreeMap<>();

        // ctor

        protected ApiRegions() {
            // it should not be directly instantiated outside this package
        }

        // methods

        public void add(String region, Collection<String> exportedApis) {
            apis.computeIfAbsent(region, k -> new TreeSet<>()).addAll(exportedApis);
        }

        public Iterable<String> getRegions() {
            return apis.keySet();
        }

        public Set<String> getApis(String region) {
            return apis.computeIfAbsent(region, k -> Collections.emptySet());
        }

        public void remove(String packageName) {
            apis.values().forEach(apis -> apis.remove(packageName));
        }

        public boolean isEmpty() {
            for (Set<String> packages : apis.values()) {
                if (!packages.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return apis.toString().replace(',', '\n');
        }

    }


}
