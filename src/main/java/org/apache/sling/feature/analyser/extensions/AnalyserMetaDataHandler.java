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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.jar.JarFile;

import org.apache.commons.lang3.SystemUtils;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.ExecutionEnvironmentExtension;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.builder.HandlerContext;
import org.apache.sling.feature.builder.PostProcessHandler;
import org.apache.sling.feature.io.IOUtils;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.spi.FrameworkScanner;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

public class AnalyserMetaDataHandler implements PostProcessHandler {
    private static final Logger LOG = LoggerFactory.getLogger(AnalyserMetaDataHandler.class);
    
    private static final String MANIFEST_KEY = "manifest";

    @Override
    public void postProcess(HandlerContext handlerContext, Feature feature, Extension extension) {

        if (AnalyserMetaDataExtension.EXTENSION_NAME.equals(extension.getName())) {
            LOG.debug("Handling analyser-metadata extension {}", extension);
            JsonObject extensionJSONStructure = extension.getJSONStructure().asJsonObject();
            JsonObjectBuilder result = Json.createObjectBuilder();
            Map<String, JsonValue> directEntries = new HashMap<>();
            Map<String, JsonValue> wildcardEntries = new LinkedHashMap<>();
            JsonObject[] frameworkDefinitionHolder = new JsonObject[1];
            extensionJSONStructure.entrySet().forEach(
                    entry -> {
                        if (entry.getKey().contains("*")) {
                            wildcardEntries.put(entry.getKey(), entry.getValue());
                        } else if (entry.getKey().equals(Constants.SYSTEM_BUNDLE_SYMBOLICNAME)) {
                            frameworkDefinitionHolder[0] = entry.getValue().asJsonObject();
                        } else {
                            directEntries.put(entry.getKey(), entry.getValue());
                        }
                    }
            );

            feature.getBundles().stream().forEach(
                    bundle ->
                        findFirst(directEntries, wildcardEntries, bundle.getId()).ifPresent(
                             json -> {
                                 if (nullManifest(json)) {
                                     JsonObjectBuilder wrapper = Json.createObjectBuilder(json);
                                     wrapper.remove(MANIFEST_KEY);
                                     result.add(bundle.getId().toMvnId(), wrapper);
                                 } else if (noManifest(json)) {
                                     JsonObjectBuilder wrapper = Json.createObjectBuilder(json);
                                     getManifest(handlerContext, bundle.getId()).ifPresent(manifest ->
                                            wrapper.add(MANIFEST_KEY, manifest));
                                     result.add(bundle.getId().toMvnId(), wrapper);
                                 } else {
                                     result.add(bundle.getId().toMvnId(), json);
                                 }
                             }
                        )
                    );
            
            if (frameworkDefinitionHolder[0] != null) {
                JsonObject v = frameworkDefinitionHolder[0];
                // TODO - add test
                // TODO - finalise contract
                // TODO - make use of the information in the Analyser, if present
                FrameworkScanner scanner = ServiceLoader.load(FrameworkScanner.class).iterator().next();
                try {
                    ExecutionEnvironmentExtension executionEnv = ExecutionEnvironmentExtension.getExecutionEnvironmentExtension(feature);
                    if ( executionEnv != null ) {
                        ArtifactId frameworkId = executionEnv.getFramework().getId();
                        if ( executionEnv.getJavaVersion() == null ) {
                            LOG.warn("No java version set in execution environment extension, skipping version validation");
                        } else {
                            Version requiredJavaVersion = executionEnv.getJavaVersion();
                            Version currentJavaVersion = new Version(SystemUtils.JAVA_VERSION);
                            
                            if ( requiredJavaVersion.getMajor() != currentJavaVersion.getMajor() ) 
                                throw new IllegalStateException("Execution environment requires Java " + requiredJavaVersion.getMajor() + ", but running on " + currentJavaVersion.getMajor() + ". Aborting.");
                            
                        }
                        BundleDescriptor fw = scanner.scan(frameworkId, feature.getFrameworkProperties(), handlerContext.getArtifactProvider());
                        JsonObjectBuilder wrapper = Json.createObjectBuilder(v);
                        wrapper.add(Constants.PROVIDE_CAPABILITY, fw.getCapabilities().toString());
                        wrapper.add(Constants.EXPORT_PACKAGE, fw.getExportedPackages().toString());
                        result.add(Constants.SYSTEM_BUNDLE_SYMBOLICNAME, wrapper);
                    } else {
                        LOG.warn("No execution environment found, not creating framework capabilities");
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }


            feature.getExtensions().remove(extension);

            // Mark the extension as optional now that we've processed it.
            Extension newEx = new Extension(ExtensionType.JSON, extension.getName(), ExtensionState.OPTIONAL);
            newEx.setJSONStructure(result.build());
            feature.getExtensions().add(newEx);
        }
    }

    private boolean noManifest(JsonObject object) {
        return manifest(object, null) && !object.getBoolean("no-manifest", false);
    }

    private boolean nullManifest(JsonObject object) {
        return manifest(object, JsonValue.NULL);
    }

    private boolean manifest(JsonObject object, Object match) {
        return object.get(MANIFEST_KEY) == match;
    }

    private Optional<JsonObject> findFirst(Map<String, JsonValue> directValues, Map<String, JsonValue> wildcardValues, ArtifactId bundle) {
        JsonValue direct = directValues.get(bundle.toMvnId());
        if (direct != null && direct != JsonValue.NULL) {
            return Optional.of(direct.asJsonObject());
        }
        return wildcardValues.entrySet().stream()
                .filter(entry -> bundle.toMvnId().matches(entry.getKey()))
                .filter(entry -> entry.getValue() != JsonValue.NULL)
                .map(entry -> entry.getValue().asJsonObject())
                .findFirst();
    }

    private Optional<JsonObject> getManifest(HandlerContext handlerContext, ArtifactId bundle) {
        URL url = handlerContext.getArtifactProvider().provide(bundle);
        try (JarFile jarFile = IOUtils.getJarFileFromURL(url, false, null)) {
            return Optional.ofNullable(jarFile.getManifest())
                    .map(manifest -> {
                        JsonObjectBuilder manifestBuilder = Json.createObjectBuilder();
                        manifest.getMainAttributes().entrySet().stream()
                                .forEachOrdered(entry -> manifestBuilder.add(entry.getKey().toString(), (String) entry.getValue()));

                        return manifestBuilder.build();
                    });
        } catch (IOException ex) {
            LOG.error("Unable to parse manifest of: " + bundle, ex);
            throw new UncheckedIOException(ex);
        }
    }
}