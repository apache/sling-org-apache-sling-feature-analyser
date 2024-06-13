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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.jar.Manifest;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.analyser.extensions.AnalyserMetaDataExtension.SystemBundle;
import org.apache.sling.feature.io.json.FeatureJSONReader;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class AnalyserMetaDataExtensionTest {
    @Test
    public void testAnalyserMetaDataExtension() throws Exception {
        Extension extension = new Extension(ExtensionType.JSON, AnalyserMetaDataExtension.EXTENSION_NAME, ExtensionState.REQUIRED);
        extension.setJSON("{}");
        AnalyserMetaDataExtension analyserMetaDataExtension = AnalyserMetaDataExtension.getAnalyserMetaDataExtension(extension);

        Assert.assertNotNull(analyserMetaDataExtension);
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("foo", "bar");
        BundleDescriptor descriptor = Mockito.mock(BundleDescriptor.class);
        Artifact artifact = new Artifact(ArtifactId.fromMvnId("org:foo:0.1"));
        Mockito.when(descriptor.getManifest()).thenReturn(manifest);
        Mockito.when(descriptor.getArtifact()).thenReturn(artifact);
        analyserMetaDataExtension.add(descriptor);
        analyserMetaDataExtension.setReportErrors(artifact.getId(), false);

        analyserMetaDataExtension.toExtension(extension);

        Extension testExtension = new Extension(ExtensionType.JSON, AnalyserMetaDataExtension.EXTENSION_NAME, ExtensionState.REQUIRED);
        testExtension.setJSON(extension.getJSON());

        Assert.assertTrue(AnalyserMetaDataExtension.isAnalyserMetaDataExtension(testExtension));

        AnalyserMetaDataExtension testAnalyserMetaDataExtension = AnalyserMetaDataExtension.getAnalyserMetaDataExtension(testExtension);

        Assert.assertNotNull(testAnalyserMetaDataExtension.getManifest(artifact.getId()));
        Assert.assertEquals("bar", testAnalyserMetaDataExtension.getManifest(artifact.getId()).get("foo"));

        Assert.assertFalse(testAnalyserMetaDataExtension.reportError(artifact.getId()));

    }
    
    @Test
    public void readSystemBundleInformation() throws IOException {
        InputStream featureStream = Objects.requireNonNull(getClass().getResourceAsStream("/analyse-metadata/feature-output-system-bundle.json"), "Unable to locate feature file");
        Feature feature = FeatureJSONReader.read(new InputStreamReader(featureStream), null);
        
        Extension extension = Objects.requireNonNull(feature.getExtensions().getByName(AnalyserMetaDataExtension.EXTENSION_NAME), "Failed to load analyser-metadata extension");

        AnalyserMetaDataExtension analyserMetaDataExtension = AnalyserMetaDataExtension.getAnalyserMetaDataExtension(extension);
        assertThat(analyserMetaDataExtension).as("metadata extension").isNotNull();
        
        SystemBundle systemBundle = analyserMetaDataExtension.getSystemBundle();
        assertThat(systemBundle).as("system bundle")
            .isNotNull()
            .extracting(SystemBundle::getArtifactId)
            .isEqualTo(ArtifactId.fromMvnId("org.apache.felix:org.apache.felix.framework:7.0.5"));

        assertThat(systemBundle.getScannerCacheKey()).as("system bundle scanner cache key")
            .isNotBlank();
        
        assertThat(systemBundle.getManifest()).as("system bundle manifest")
            .containsKeys("Export-Package", "Provide-Capability");
    }
}
