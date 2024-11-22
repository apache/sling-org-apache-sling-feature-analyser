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

import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Constants;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CheckConfigurationsTest {

    private AnalyserTaskContextImpl ctx;
    private AnalyserTask task;

    @Before
    public void setUp() {
        this.ctx = new AnalyserTaskContextImpl("myGroupId:myArtifactId:jar:myClassifier:1.0.0");
        this.task = new CheckConfigurations();
    }

    @Test
    public void testServiceRankingOk() throws Exception {
        Configuration cfg = new Configuration("myPid");
        cfg.getProperties().put(Constants.SERVICE_RANKING, 1);
        this.ctx.getFeature().getConfigurations().add(cfg);
        this.task.execute(this.ctx);
        assertTrue(this.ctx.getErrors().isEmpty());
    }

    @Test
    public void testServiceRankingLong() throws Exception {
        Configuration cfg = new Configuration("myPid");
        cfg.getProperties().put(Constants.SERVICE_RANKING, 1L);
        this.ctx.getFeature().getConfigurations().add(cfg);
        this.task.execute(this.ctx);
        assertEquals(1, this.ctx.getErrors().size());
    }

    @Test
    public void testServiceRankingString() throws Exception {
        Configuration cfg = new Configuration("myPid");
        cfg.getProperties().put(Constants.SERVICE_RANKING, "100");
        this.ctx.getFeature().getConfigurations().add(cfg);
        this.task.execute(this.ctx);
        assertEquals(1, this.ctx.getErrors().size());
    }
}
