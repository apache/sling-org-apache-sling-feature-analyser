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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.junit.Before;
import org.junit.Test;

public class CheckFeatureIdTest {

    private AnalyserTaskContextImpl ctx;
    private AnalyserTask task;

    @Before
    public void setUp() {
        ctx = new AnalyserTaskContextImpl("myGroupId:myArtifactId:jar:myClassifier:1.0.0");
        task = new CheckFeatureId();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMissingConfiguration() throws Exception {
        task.execute(ctx);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConfiguration() throws Exception {
        ctx.getConfiguration().put(CheckFeatureId.CONFIG_KEY_ACCEPTED_FEATURE_IDS, "myGroupId");
        task.execute(ctx);
    }

    @Test
    public void testValidFeatureId() throws Exception {
        ctx.getConfiguration().put(CheckFeatureId.CONFIG_KEY_ACCEPTED_FEATURE_IDS, "myGroupId:*:jar:myFirstClassifier:1.0.0,myGroupId:*:jar:myClassifier:1.0.0");
        task.execute(ctx);
        assertTrue(ctx.getErrors().isEmpty());
    }

    @Test
    public void testInValidFeatureId() throws Exception {
        ctx.getConfiguration().put(CheckFeatureId.CONFIG_KEY_ACCEPTED_FEATURE_IDS, "myGroupId:*:jar:myOtherClassifier:1.0.0");
        task.execute(ctx);
        assertEquals(1, ctx.getErrors().size());
    }

    @Test
    public void checkMatches() {
        assertTrue(CheckFeatureId.matches(ArtifactId.parse("myGroupId:myArtifactId:jar:1.0.0"), ArtifactId.parse("*:myArtifactId:1.0.0")));
        assertTrue(CheckFeatureId.matches(ArtifactId.parse("myGroupId:myArtifactId:jar:1.0.0"), ArtifactId.parse("myGroupId:*:1.0.0")));
        assertTrue(CheckFeatureId.matches(ArtifactId.parse("myGroupId:myArtifactId:jar:1.0.0"), ArtifactId.parse("myGroupId:myArtifactId:*")));
        assertTrue(CheckFeatureId.matches(ArtifactId.parse("myGroupId:myArtifactId:jar:1.0.0"), ArtifactId.parse("myGroupId:myArtifactId:jar:1.0.0")));
        assertTrue(CheckFeatureId.matches(ArtifactId.parse("myGroupId:myArtifactId:jar:1.0.0"), ArtifactId.parse("myGroupId:myArtifactId:jar:*")));
        assertTrue(CheckFeatureId.matches(ArtifactId.parse("myGroupId:myArtifactId:jar:1.0.0"), ArtifactId.parse("myGroupId:myArtifactId:*:1.0.0")));
        assertTrue(CheckFeatureId.matches(ArtifactId.parse("myGroupId:myArtifactId:jar:1.0.0"), ArtifactId.parse("myGroupId:myArtifactId:jar:*:1.0.0")));
        
        assertFalse(CheckFeatureId.matches(ArtifactId.parse("myGroupId:myArtifactId:jar:1.0.0"), ArtifactId.parse("*:myOtherArtifactId:1.0.0")));
        assertFalse(CheckFeatureId.matches(ArtifactId.parse("myGroupId:myArtifactId:jar:1.0.0"), ArtifactId.parse("myOtherGroupId:*:1.0.0")));
        assertFalse(CheckFeatureId.matches(ArtifactId.parse("myGroupId:myArtifactId:jar:1.0.0"), ArtifactId.parse("myGroupId:myOtherArtifactId:*")));
        assertFalse(CheckFeatureId.matches(ArtifactId.parse("myGroupId:myArtifactId:jar:1.0.0"), ArtifactId.parse("myGroupId:myArtifactId:someothertype:*")));
        assertFalse(CheckFeatureId.matches(ArtifactId.parse("myGroupId:myArtifactId:jar:1.0.0"), ArtifactId.parse("myGroupId:myArtifactId:jar:*:1.1.0")));
        assertFalse(CheckFeatureId.matches(ArtifactId.parse("myGroupId:myArtifactId:jar:1.0.0"), ArtifactId.parse("myGroupId:myArtifactId:*:*:1.1.0")));
        
        // enforce no classifier
        assertTrue(CheckFeatureId.matches(ArtifactId.parse("myGroupId:myArtifactId:jar::1.0.0"), ArtifactId.parse("myGroupId:myArtifactId:jar::1.0.0")));
        assertFalse(CheckFeatureId.matches(ArtifactId.parse("myGroupId:myArtifactId:jar:invalidClassifier:1.0.0"), ArtifactId.parse("myGroupId:myArtifactId:jar::1.0.0")));
        
        // enforce default type
        assertTrue(CheckFeatureId.matches(ArtifactId.parse("myGroupId:myArtifactId:1.0.0"), ArtifactId.parse("myGroupId:myArtifactId:1.0.0")));
        assertFalse(CheckFeatureId.matches(ArtifactId.parse("myGroupId:myArtifactId:othertype:1.0.0"), ArtifactId.parse("myGroupId:myArtifactId:1.0.0")));
    }
}
