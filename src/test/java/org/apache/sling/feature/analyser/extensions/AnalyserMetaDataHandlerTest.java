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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.builder.ArtifactProvider;
import org.apache.sling.feature.builder.HandlerContext;
import org.apache.sling.feature.io.json.FeatureJSONReader;
import org.apache.sling.feature.io.json.FeatureJSONWriter;
import org.junit.Test;
import org.mockito.Mockito;

import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

public class AnalyserMetaDataHandlerTest {

    @Test
    public void testMetaDataHandler() throws Exception {
        URL url = getClass().getResource("/analyse-metadata/feature-input.json");

        Feature feature;
        try (Reader r = new InputStreamReader(url.openStream())) {
            feature = FeatureJSONReader.read(r, url.toString());
        }

        assertThat(feature).isNotNull();

        HandlerContext ctx = Mockito.mock(HandlerContext.class);
        ArtifactProvider provider = artifactId -> {
            try {
                return new URL("https://repo1.maven.org/maven2/" + artifactId.toMvnPath());
            } catch (MalformedURLException e) {
                throw new UncheckedIOException(e);
            }
        };

        Mockito.when(ctx.getArtifactProvider()).thenReturn(provider);

        new AnalyserMetaDataHandler().
            postProcess(ctx, feature, feature.getExtensions().getByName("analyser-metadata"));

        URL expectedURL = getClass().getResource("/analyse-metadata/feature-expected-output.json");

        Feature expected;
        try (Reader r = new InputStreamReader(expectedURL.openStream())) {
            expected = FeatureJSONReader.read(r, expectedURL.toString());
        }

        StringWriter featureWriter = new StringWriter();
        StringWriter expectedWriter = new StringWriter();

        FeatureJSONWriter.write(featureWriter, feature);
        FeatureJSONWriter.write(expectedWriter, expected);

        assertThat(expectedWriter.toString()).isEqualTo(featureWriter.toString());
    }
    
    @Test
    public void testMetadataHandlerSystemBundle() throws IOException {
        URL url = getClass().getResource("/analyse-metadata/feature-input-system-bundle.json");

        Feature feature;
        try (Reader r = new InputStreamReader(url.openStream())) {
            feature = FeatureJSONReader.read(r, url.toString());
        }

        assertThat(feature).isNotNull();

        HandlerContext ctx = Mockito.mock(HandlerContext.class);
        ArtifactProvider provider = artifactId -> {
            try {
                return new URL("https://repo1.maven.org/maven2/" + artifactId.toMvnPath());
            } catch (MalformedURLException e) {
                throw new UncheckedIOException(e);
            }
        };

        Mockito.when(ctx.getArtifactProvider()).thenReturn(provider);

        new AnalyserMetaDataHandler().postProcess(ctx, feature, feature.getExtensions().getByName("analyser-metadata"));
        
        JsonObject metadata = feature.getExtensions().getByName("analyser-metadata").getJSONStructure().asJsonObject();
        assertThat(metadata).as("analyser-metadata extension").isNotNull();
        
        JsonValue systemBundle = metadata.get("system.bundle");
        assertThat(systemBundle).as("system.bundle property of the metadata extension").isNotNull();
        
        // ensure the artifactId is recorded and correctly formed
        JsonValue artifactId = systemBundle.asJsonObject().get("artifactId");
        assertThat(artifactId).as("artifactId of the system bundle").isNotNull();
        ArtifactId.fromMvnId(artifactId.toString());
        
        JsonValue manifest = systemBundle.asJsonObject().get("manifest");
        assertThat(manifest).as("manifest of the system bundle").isNotNull();

        JsonString systemBundleCapabilities = manifest.asJsonObject().getJsonString("Provide-Capability");
        assertThat(systemBundleCapabilities).as("capabilities for the system bundle").isNotNull();
        String capabilities = systemBundleCapabilities.getString();
        
        // validate that the capabilities header is correctly formed
        Clause[] clauses = Parser.parseHeader(capabilities);
        
        // validate that we have exactly one osgi.ee clause with the value "JavaSE" 
        List<Clause> javaSeEeClauses = Arrays.stream(clauses)
            .filter( c -> c.getName().equals("osgi.ee") )
            .filter( c -> c.getAttribute("osgi.ee").equals("JavaSE") )
            .collect(Collectors.toList());
        
        assertThat(javaSeEeClauses).as("osgi.ee=JavaSE capabilities")
            .hasSize(1)
            .element(0).extracting( c -> c.getAttribute("version:List<Version>") )
            .asString().contains("1.8");
        
        JsonString systemBundleExports = manifest.asJsonObject().getJsonString("Export-Package");
        assertThat(systemBundleExports).as("exports for the system bundle").isNotNull();
        String exports = systemBundleExports.getString();
        
        // validate that the exports header is correctly formed
        Clause[] exportClauses = Parser.parseHeader(exports);
        List<Clause> javaUtilExports = Arrays.stream(exportClauses)
            .filter( c -> c.getName().equals("java.util") )
            .collect(Collectors.toList());

        // validate that the java.util.function export is present
        assertThat(javaUtilExports).as("java.util package exports")
            .hasSize(1)
            .element(0).extracting( c -> c.getAttribute("version") )
            .isNotNull();
    }

}
