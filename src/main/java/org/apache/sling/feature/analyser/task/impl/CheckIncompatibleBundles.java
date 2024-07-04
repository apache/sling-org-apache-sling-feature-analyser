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

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.stream.JsonParser;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.scanner.BundleDescriptor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class CheckIncompatibleBundles implements AnalyserTask {

    private static final String UNSUPPORTED_BUNDLES_FILE_NAME = "unsupported_bundles.json";

    @Override
    public String getName() {
        return "Incompatible Bundle Check";
    }

    @Override
    public String getId() {
        return "check-incompatible-bundles";
    }

    @Override
    public void execute(final AnalyserTaskContext ctx) throws IOException {
        JsonObject jsonObject;
        try (JsonReader jsonReader = Json.createReader(new StringReader(getJsonString()))) {
            jsonObject = jsonReader.readObject();
        }

        JsonArray unsupported = jsonObject.getJsonArray("unsupported");
        if (unsupported.size() != 1) {
            throw new RuntimeException("problem with unsupported bundles file: " + UNSUPPORTED_BUNDLES_FILE_NAME);
        }

        String message = unsupported.getJsonObject(0).getString("reason");
        JsonArray bundles = unsupported.getJsonObject(0).getJsonArray("bundles");

        Collection<ArtifactId> unsupportedBundles = getUnsupportedBundles(bundles);
        for (final BundleDescriptor info : ctx.getFeatureDescriptor().getBundleDescriptors()) {
            if (matchesAnyOf(info.getArtifact().getId(), unsupportedBundles)) {
                ctx.reportArtifactWarning(info.getArtifact().getId(), message);
            }
        }
    }

    private static String getJsonString() {
        try (InputStream is = JsonParser.class.getClassLoader().getResourceAsStream(UNSUPPORTED_BUNDLES_FILE_NAME);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Collection<ArtifactId> getUnsupportedBundles(JsonArray bundles) {
        return IntStream.range(0, bundles.size())
                .mapToObj(bundles::getString)
                .map(ArtifactId::fromMvnId)
                .collect(Collectors.toList());
    }

    private static boolean matchesAnyOf(ArtifactId artifactId, Collection<ArtifactId> unsupportedBundles) {
        return unsupportedBundles.stream()
                .anyMatch(artifactId::equals);
    }
}