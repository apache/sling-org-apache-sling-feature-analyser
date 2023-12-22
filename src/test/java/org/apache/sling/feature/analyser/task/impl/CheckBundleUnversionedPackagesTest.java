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

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.FeatureDescriptor;
import org.apache.sling.feature.scanner.impl.BundleDescriptorImpl;
import org.apache.sling.feature.scanner.impl.FeatureDescriptorImpl;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

public class CheckBundleUnversionedPackagesTest {
    private static File resourceRoot;

    @BeforeClass
    public static void setupClass() {
        resourceRoot =
                new File(CheckBundleExportsImportsTest.class.
                        getResource("/test-content.zip").getFile()).getParentFile();
    }

    @Test
    public void testId() {
        assertEquals("bundle-unversioned-packages", new CheckBundleUnversionedPackages().getId());
    }

    @Test
    public void testBundleUnversionedPackage() throws Exception {
        CheckBundleUnversionedPackages t = new CheckBundleUnversionedPackages();

        Feature f = new Feature(ArtifactId.fromMvnId("f:f:1"));
        f.setComplete(true);
        FeatureDescriptor fd = new FeatureDescriptorImpl(f);

        fdAddBundle(fd, "g:b1:1", "test-bundle-unversioned.jar");

        AnalyserTaskContext ctx = Mockito.mock(AnalyserTaskContext.class);
        Mockito.when(ctx.getFeature()).thenReturn(f);
        Mockito.when(ctx.getFeatureDescriptor()).thenReturn(fd);
        t.execute(ctx);

        Mockito.verify(ctx, Mockito.times(2)).reportArtifactWarning(Mockito.any(), Mockito.anyString());

        Mockito.verify(ctx).reportArtifactWarning(
                Mockito.eq(ArtifactId.fromMvnId("g:b1:1")),
                Mockito.contains("org.foo.i"));
        Mockito.verify(ctx).reportArtifactWarning(
                Mockito.eq(ArtifactId.fromMvnId("g:b1:1")),
                Mockito.contains("org.foo.e"));
    }


    private void fdAddBundle(FeatureDescriptor fd, String id, String file, ArtifactId... origins) throws URISyntaxException, IOException {
        Artifact artifact = new Artifact(ArtifactId.fromMvnId(id));
        artifact.setFeatureOrigins(origins);
        BundleDescriptor bd1 = new BundleDescriptorImpl(
                artifact, new File(resourceRoot, file).toURI().toURL());
        fd.getBundleDescriptors().add(bd1);
    }
}
