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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.scanner.ContentPackageDescriptor;
import org.apache.sling.feature.scanner.FeatureDescriptor;
import org.apache.sling.feature.scanner.impl.FeatureDescriptorImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CheckContentPackagesDependenciesTest {

    private AnalyserTask task;

    @Before
    public void setUp() {
        task = new CheckContentPackagesDependencies();
    }

    @After
    public void tearDown() {
        task = null;
    }

    @Test
    public void dependenciesNotResolvedThrowsError() throws Exception {
        List<String> errors = execute("test_a-1.0.zip");

        assertFalse(errors.isEmpty());

        Iterator<String> errorsIterator = errors.iterator();
        assertEquals("Missing my_packages:test_b dependency for my_packages:test_a:1.0", errorsIterator.next());
        assertEquals(
                "Missing my_packages:test_c:[1.0,2.0) dependency for my_packages:test_a:1.0", errorsIterator.next());
    }

    @Test
    public void dependenciesResolved() throws Exception {
        List<String> errors =
                execute("test_a-1.0.zip", "test_b-1.0.zip", "test_c-1.0.zip", "test_d-1.0.zip", "test_e-1.0.zip");
        assertTrue(errors.isEmpty());
    }

    private List<String> execute(String... resources) throws Exception {
        Feature feature = mock(Feature.class);
        when(feature.getId())
                .thenReturn(new ArtifactId(
                        "org.apache.sling.testing", "org.apache.sling.testing.contentpackages", "1.0.0", null, null));
        FeatureDescriptor featureDescriptor = new FeatureDescriptorImpl(feature);

        for (String resource : resources) {
            ArtifactId id = mock(ArtifactId.class);
            Artifact artifact = mock(Artifact.class);
            when(artifact.getId()).thenReturn(id);

            ContentPackageDescriptor descriptor = mock(ContentPackageDescriptor.class);
            when(descriptor.getArtifact()).thenReturn(artifact);
            when(descriptor.getArtifactFile())
                    .thenReturn(getClass().getClassLoader().getResource(resource));

            featureDescriptor.getArtifactDescriptors().add(descriptor);
        }

        AnalyserTaskContext ctx = mock(AnalyserTaskContext.class);

        when(ctx.getFeatureDescriptor()).thenReturn(featureDescriptor);

        List<String> errors = new LinkedList<>();

        doAnswer(invocation -> {
                    String error = invocation.getArgument(0);
                    errors.add(error);
                    return null;
                })
                .when(ctx)
                .reportError(anyString());

        task.execute(ctx);

        return errors;
    }
}
