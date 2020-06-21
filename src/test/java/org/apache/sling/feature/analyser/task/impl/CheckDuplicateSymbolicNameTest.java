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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;

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


public class CheckDuplicateSymbolicNameTest  {

    private List<String> errors = new LinkedList<>();

    private AnalyserTaskContext ctx;

    @Before public void setup() {
        ctx = mock(AnalyserTaskContext.class);

        final Feature feature = new Feature(ArtifactId.parse("g:a:1"));
        FeatureDescriptor featureDescriptor = new FeatureDescriptorImpl(feature);

        when(ctx.getFeatureDescriptor()).thenReturn(featureDescriptor);

        doAnswer(invocation -> {
            String error = invocation.getArgument(0);
            errors.add(error);
            return null;
        }).when(ctx).reportError(anyString());
    }

    private BundleDescriptor newDescriptor(final ArtifactId id, final String symbolicName) {
        final Artifact artifact = new Artifact(id);
        final BundleDescriptor desc = mock(BundleDescriptor.class);
        when(desc.getBundleSymbolicName()).thenReturn(symbolicName);
        when(desc.getArtifact()).thenReturn(artifact);
        return desc;
    }

    @Test public void testNoDuplicates() throws Exception {
        final AnalyserTask task = new CheckDuplicateSymbolicName();

        ctx.getFeatureDescriptor().getBundleDescriptors().add(newDescriptor(ArtifactId.parse("g:b1:1"), "b1"));
        ctx.getFeatureDescriptor().getBundleDescriptors().add(newDescriptor(ArtifactId.parse("g:b2:1"), "b2"));
        ctx.getFeatureDescriptor().getBundleDescriptors().add(newDescriptor(ArtifactId.parse("g:b3:1"), "b3"));
        task.execute(ctx);
        assertTrue(errors.isEmpty());
    }

    @Test public void testDuplicates() throws Exception {
        final AnalyserTask task = new CheckDuplicateSymbolicName();

        ctx.getFeatureDescriptor().getBundleDescriptors().add(newDescriptor(ArtifactId.parse("g:b1:1"), "b1"));
        ctx.getFeatureDescriptor().getBundleDescriptors().add(newDescriptor(ArtifactId.parse("g:b2:1"), "b1"));
        ctx.getFeatureDescriptor().getBundleDescriptors().add(newDescriptor(ArtifactId.parse("g:b3:1"), "b2"));
        ctx.getFeatureDescriptor().getBundleDescriptors().add(newDescriptor(ArtifactId.parse("g:b4:1"), "b3"));
        ctx.getFeatureDescriptor().getBundleDescriptors().add(newDescriptor(ArtifactId.parse("g:b5:1"), "b3"));
        task.execute(ctx);
        assertEquals(2, errors.size());
    }

}
