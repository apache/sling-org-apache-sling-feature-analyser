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

import java.io.IOException;
import java.util.jar.Manifest;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.FeatureDescriptor;
import org.apache.sling.feature.scanner.impl.FeatureDescriptorImpl;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Constants;

public class CheckBundleNativeCodeTest {

    @Test
    public void testNoNativeCodeHeader() throws Exception {
        final CheckBundleNativeCode task = new CheckBundleNativeCode();

        final Feature f = new Feature(ArtifactId.fromMvnId("f:f:2"));
        final FeatureDescriptor fd = new FeatureDescriptorImpl(f);

        AnalyserTaskContext ctx = Mockito.mock(AnalyserTaskContext.class);
        Mockito.when(ctx.getFeature()).thenReturn(f);
        Mockito.when(ctx.getFeatureDescriptor()).thenReturn(fd);

        // bundle without native code - this should pass
        fdAddBundle(fd, "g:b1:1", null);
        task.execute(ctx);

        Mockito.verify(ctx, Mockito.never()).reportError(Mockito.anyString());
        Mockito.verify(ctx, Mockito.never()).reportWarning(Mockito.anyString());
    }

    @Test
    public void testNativeCodeHeader() throws Exception {
        final CheckBundleNativeCode task = new CheckBundleNativeCode();

        final Feature f = new Feature(ArtifactId.fromMvnId("f:f:2"));
        final FeatureDescriptor fd = new FeatureDescriptorImpl(f);

        AnalyserTaskContext ctx = Mockito.mock(AnalyserTaskContext.class);
        Mockito.when(ctx.getFeature()).thenReturn(f);
        Mockito.when(ctx.getFeatureDescriptor()).thenReturn(fd);

        // bundle with native code - this should fail
        fdAddBundle(fd, "g:b1:1", "something");
        task.execute(ctx);

        Mockito.verify(ctx, Mockito.times(1)).reportArtifactError(Mockito.any(), Mockito.anyString());
        Mockito.verify(ctx, Mockito.never()).reportWarning(Mockito.anyString());
    }

    private void fdAddBundle(final FeatureDescriptor fd, final String id, final String header) throws IOException {
        final BundleDescriptor bd = Mockito.mock(BundleDescriptor.class);
        final ArtifactId aId = ArtifactId.parse(id);
        final Artifact a = new Artifact(aId);
        Mockito.when(bd.getArtifact()).thenReturn(a);
        final Manifest mf = new Manifest();
        if ( header != null ) {
            mf.getMainAttributes().putValue(Constants.BUNDLE_NATIVECODE, header);
        }
        Mockito.when(bd.getManifest()).thenReturn(mf);
        fd.getBundleDescriptors().add(bd);
    }
}
