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

import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.junit.Before;
import org.junit.Test;

public class CheckFeatureIdTest {

    private AnalyserTaskContextImpl ctx;
    private AnalyserTask task;

    @Before
    public void setUp() {
        ctx = new AnalyserTaskContextImpl("myGroupId:myArtifactId:jar:myClassifier");
        task = new CheckFeatureId();
    }
 
    @Test
    public void testValidFeatureId() throws Exception {
        ctx.getConfiguration().put(CheckFeatureId.CONFIG_KEY_ACCEPTED_FEATURE_IDS, "myGroupId:*:jar:myClassifier");
        task.execute(ctx);
        assertTrue(ctx.getErrors().isEmpty());
    }

    @Test
    public void testInValidFeatureId() throws Exception {
        ctx.getConfiguration().put(CheckFeatureId.CONFIG_KEY_ACCEPTED_FEATURE_IDS, "myGroupId:*:jar:myOtherClassifier");
        task.execute(ctx);
        assertEquals(1, ctx.getErrors().size());
    }
}
