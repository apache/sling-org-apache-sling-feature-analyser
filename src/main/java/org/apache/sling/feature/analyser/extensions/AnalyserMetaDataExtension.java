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
package org.apache.sling.feature.analyser.extensions;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.scanner.BundleDescriptor;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public class AnalyserMetaDataExtension {
    public static final String EXTENSION_NAME = "analyser-metadata";

    private final Map<ArtifactId, Map<String, String>> manifests = new HashMap<>();
    private final Map<ArtifactId, Boolean> reportWarnings = new HashMap<>();
    private final Map<ArtifactId, Boolean> reportErrors = new HashMap<>();

    public static AnalyserMetaDataExtension getAnalyserMetaDataExtension(Feature feature) {
        Extension ext = feature == null ? null : feature.getExtensions().getByName(EXTENSION_NAME);
        return getAnalyserMetaDataExtension(ext);
    }

    public static AnalyserMetaDataExtension getAnalyserMetaDataExtension(Extension ext) {
        if (ext == null) {
            return null;
        } else if (ext.getType() != ExtensionType.JSON) {
            throw new IllegalArgumentException("Extension " + ext.getName() + " must have JSON type");
        } else {
            return new AnalyserMetaDataExtension(ext.getJSONStructure().asJsonObject());
        }
    }

    private AnalyserMetaDataExtension(JsonObject json) {
        for (Map.Entry<String, JsonValue> entry : json.entrySet()) {
            ArtifactId id = ArtifactId.fromMvnId(entry.getKey());
            JsonObject headers = entry.getValue().asJsonObject();
            if (headers.containsKey("manifest")) {
                Map<String, String> manifest = new LinkedHashMap<>();
                JsonObject manifestHeaders = headers.getJsonObject("manifest");
                for (String name : manifestHeaders.keySet()) {
                    manifest.put(name, manifestHeaders.getString(name));
                }
                this.manifests.put(id, manifest);
            }
            if (headers.containsKey("report")) {
                JsonObject report = headers.getJsonObject("report");
                if (report.containsKey("warning")) {
                    reportWarnings.put(id, report.getBoolean("warning"));
                }
                if (report.containsKey("error")) {
                    reportErrors.put(id, report.getBoolean("error"));
                }
            }
        }
    }

    public static boolean isAnalyserMetaDataExtension(Extension ext) {
        return ext != null && ext.getName().equals(EXTENSION_NAME) && ext.getType() == ExtensionType.JSON;
    }

    public Map<String, String> getManifest(ArtifactId artifactId) {
        return this.manifests.get(artifactId);
    }

    public boolean reportWarning(ArtifactId artifactId) {
        return !this.reportWarnings.containsKey(artifactId) || this.reportWarnings.get(artifactId);
    }

    public boolean reportError(ArtifactId artifactId) {
        return !this.reportErrors.containsKey(artifactId) || this.reportErrors.get(artifactId);
    }

    public Extension toExtension(Extension extension) {
        if (isAnalyserMetaDataExtension(extension)) {
            JsonObjectBuilder builder = Json.createObjectBuilder(extension.getJSONStructure().asJsonObject());
            Stream.concat(Stream.concat(manifests.keySet().stream(), reportErrors.keySet().stream()), reportWarnings.keySet().stream()).distinct().forEachOrdered(
                    id -> {
                        JsonObjectBuilder metadata = Json.createObjectBuilder();
                        if (manifests.containsKey(id)) {
                            JsonObjectBuilder manifest = Json.createObjectBuilder();
                            manifests.get(id).forEach(manifest::add);
                            metadata.add("manifest", manifest);
                        }
                        if (reportErrors.containsKey(id) || reportWarnings.containsKey(id)) {
                            JsonObjectBuilder report = Json.createObjectBuilder();
                            if (reportErrors.containsKey(id)) {
                                report.add("error", reportErrors.get(id));
                            }
                            if (reportWarnings.containsKey(id)) {
                                report.add("warning", reportWarnings.get(id));
                            }
                            metadata.add("report", report);
                        }
                        builder.add(id.toMvnId(), metadata);
                    }
            );
            extension.setJSONStructure(builder.build());
        }
        return extension;
    }

    public void add(BundleDescriptor... bundleDescriptors) {
        for (BundleDescriptor descriptor : bundleDescriptors) {
            Map<String, String> manifest = new LinkedHashMap<>();
            descriptor.getManifest().getMainAttributes().entrySet().stream()
                    .forEachOrdered(entry -> manifest.put(entry.getKey().toString(), (String) entry.getValue()));

            manifests.put(descriptor.getArtifact().getId(), manifest);
        }
    }

    public void setReportWarnings(ArtifactId id, boolean enabled) {
        reportWarnings.put(id, enabled);
    }

    public void setReportErrors(ArtifactId id, boolean enabled) {
        reportErrors.put(id, enabled);
    }
}
