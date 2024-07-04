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
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.FeatureDescriptor;
import org.apache.sling.feature.scanner.impl.FeatureDescriptorImpl;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CheckIncompatibleBundlesTest {
    private final List<ArtifactId> warnings = new LinkedList<>();

    private AnalyserTaskContext ctx;

    @Before
    public void setup() {
        ctx = mock(AnalyserTaskContext.class);

        final Feature feature = new Feature(ArtifactId.parse("g:a:1"));
        FeatureDescriptor featureDescriptor = new FeatureDescriptorImpl(feature);

        when(ctx.getFeatureDescriptor()).thenReturn(featureDescriptor);

        doAnswer(invocation -> {
            ArtifactId warning = invocation.getArgument(0);
            warnings.add(warning);
            return null;
        }).when(ctx).reportArtifactWarning(any(ArtifactId.class), anyString());
    }

    private BundleDescriptor newDescriptor(final ArtifactId id) {
        final Artifact artifact = new Artifact(id);
        final BundleDescriptor desc = mock(BundleDescriptor.class);
        when(desc.getArtifact()).thenReturn(artifact);
        return desc;
    }

    @Test
    public void testId() {
        assertEquals("check-incompatible-bundles", new CheckIncompatibleBundles().getId());
    }

    @Test
    public void testOneUnsupportedBundle() throws Exception {
        final AnalyserTask task = new CheckIncompatibleBundles();
        ctx.getFeatureDescriptor().getBundleDescriptors().add(newDescriptor(ArtifactId.parse("org.cid15.aem.groovy.console:aem-groovy-console-bundle:17.0.0")));

        task.execute(ctx);

        assertEquals(1, warnings.size());
        assertEquals("org.cid15.aem.groovy.console:aem-groovy-console-bundle:17.0.0", warnings.get(0).toString());
    }

    @Test
    public void testNoUnsupportedBundles() throws Exception {
        final AnalyserTask task = new CheckIncompatibleBundles();
        ctx.getFeatureDescriptor().getBundleDescriptors().add(newDescriptor(ArtifactId.parse("org.cid15.aem.groovy.console:aem-groovy-console-bundle:17.0.2")));

        task.execute(ctx);

        assertTrue(warnings.isEmpty());
    }
}
