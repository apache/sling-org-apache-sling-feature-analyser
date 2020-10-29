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

import org.apache.sling.feature.*;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.jar.Manifest;

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
}
