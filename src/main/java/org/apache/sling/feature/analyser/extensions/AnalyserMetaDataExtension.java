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
package org.apache.sling.feature.analyser.extensions;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.osgi.framework.Constants;

public class AnalyserMetaDataExtension {

    public static final String EXTENSION_NAME = "analyser-metadata";

    // use ArtifactId.fromMvnId to ensure the string form can be parsed back to an ArtifactId
    static final String SYSTEM_BUNDLE_KEY = ArtifactId.fromMvnId(
                    "extra-metadata:" + Constants.SYSTEM_BUNDLE_SYMBOLICNAME + ":0")
            .toString();
    static final String MANIFEST_KEY = "manifest";
    static final String REPORT_KEY = "report";
    static final String WARNING_KEY = "warning";
    static final String ERROR_KEY = "error";
    static final String ARTIFACT_ID_KEY = "artifactId";
    static final String SCANNER_CACHE_KEY = "scannerCacheKey";

    private final Map<ArtifactId, Map<String, String>> manifests = new HashMap<>();
    private final Map<ArtifactId, Boolean> reportWarnings = new HashMap<>();
    private final Map<ArtifactId, Boolean> reportErrors = new HashMap<>();
    private SystemBundle systemBundle;

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

            // handle system bundle separately
            if (entry.getKey().equals(SYSTEM_BUNDLE_KEY)) {
                JsonObject systemBundleConfig = entry.getValue().asJsonObject();
                JsonObject manifestObj = systemBundleConfig.getJsonObject(MANIFEST_KEY);
                String artifactId =
                        systemBundleConfig.getJsonString(ARTIFACT_ID_KEY).getString();
                String scannerCacheKey =
                        systemBundleConfig.getJsonString(SCANNER_CACHE_KEY).getString();

                Map<String, String> manifest = new HashMap<>();
                for (String key : manifestObj.keySet()) {
                    manifest.put(key, manifestObj.getString(key));
                }
                systemBundle = new SystemBundle(manifest, ArtifactId.fromMvnId(artifactId), scannerCacheKey);

                continue;
            }

            ArtifactId id = ArtifactId.fromMvnId(entry.getKey());
            JsonObject headers = entry.getValue().asJsonObject();
            if (headers.containsKey(MANIFEST_KEY)) {
                Map<String, String> manifest = new LinkedHashMap<>();
                JsonObject manifestHeaders = headers.getJsonObject(MANIFEST_KEY);
                for (String name : manifestHeaders.keySet()) {
                    manifest.put(name, manifestHeaders.getString(name));
                }
                this.manifests.put(id, manifest);
            }
            if (headers.containsKey(REPORT_KEY)) {
                JsonObject report = headers.getJsonObject(REPORT_KEY);
                if (report.containsKey(WARNING_KEY)) {
                    reportWarnings.put(id, report.getBoolean(WARNING_KEY));
                }
                if (report.containsKey(ERROR_KEY)) {
                    reportErrors.put(id, report.getBoolean(ERROR_KEY));
                }
            }
        }
    }

    public static boolean isAnalyserMetaDataExtension(Extension ext) {
        return ext != null && ext.getName().equals(EXTENSION_NAME) && ext.getType() == ExtensionType.JSON;
    }

    public Map<String, String> getManifest(final ArtifactId artifactId) {
        return this.manifests.get(artifactId);
    }

    public SystemBundle getSystemBundle() {
        return systemBundle;
    }

    public boolean reportWarning(ArtifactId artifactId) {
        return !this.reportWarnings.containsKey(artifactId) || this.reportWarnings.get(artifactId);
    }

    public boolean reportError(ArtifactId artifactId) {
        return !this.reportErrors.containsKey(artifactId) || this.reportErrors.get(artifactId);
    }

    public Extension toExtension(Extension extension) {
        if (isAnalyserMetaDataExtension(extension)) {
            JsonObjectBuilder builder =
                    Json.createObjectBuilder(extension.getJSONStructure().asJsonObject());
            Stream.concat(
                            Stream.concat(manifests.keySet().stream(), reportErrors.keySet().stream()),
                            reportWarnings.keySet().stream())
                    .distinct()
                    .forEachOrdered(id -> {
                        JsonObjectBuilder metadata = Json.createObjectBuilder();
                        if (manifests.containsKey(id)) {
                            JsonObjectBuilder manifest = Json.createObjectBuilder();
                            manifests.get(id).forEach(manifest::add);
                            metadata.add(MANIFEST_KEY, manifest);
                        }
                        if (reportErrors.containsKey(id) || reportWarnings.containsKey(id)) {
                            JsonObjectBuilder report = Json.createObjectBuilder();
                            if (reportErrors.containsKey(id)) {
                                report.add(ERROR_KEY, reportErrors.get(id));
                            }
                            if (reportWarnings.containsKey(id)) {
                                report.add(WARNING_KEY, reportWarnings.get(id));
                            }
                            metadata.add(REPORT_KEY, report);
                        }
                        builder.add(id.toMvnId(), metadata);
                    });
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

    public static class SystemBundle {

        private Map<String, String> manifest = new HashMap<>();
        private ArtifactId artifactId;
        private String scannerCacheKey;

        public SystemBundle(Map<String, String> manifest, ArtifactId artifactId, String scannerCacheKey) {
            this.manifest = manifest;
            this.artifactId = artifactId;
            this.scannerCacheKey = scannerCacheKey;
        }

        public ArtifactId getArtifactId() {
            return artifactId;
        }

        public Map<String, String> getManifest() {
            return manifest;
        }

        public String getScannerCacheKey() {
            return scannerCacheKey;
        }
    }
}
