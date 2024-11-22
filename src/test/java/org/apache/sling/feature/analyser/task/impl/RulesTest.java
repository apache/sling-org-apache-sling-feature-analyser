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

import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.analyser.task.impl.CheckContentPackagesForPaths.Rules;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class RulesTest {
    @Test
    public void testNoRulesConfiguration() {
        final AnalyserTaskContext ctx = new AnalyserTaskContextImpl();
        assertThat(new Rules(ctx).isConfigured(), equalTo(false));
    }

    @Test
    public void testIncludesRulesConfiguration() {
        final AnalyserTaskContext ctx = new AnalyserTaskContextImpl();
        ctx.getConfiguration().put("includes", "/a, /b");

        final Rules r = new Rules(ctx);
        assertEquals(0, r.excludes.length);
        assertEquals(2, r.includes.length);
        assertEquals("/a", r.includes[0]);
        assertEquals("/b", r.includes[1]);
    }

    @Test
    public void testExcludesRulesConfiguration() {
        final AnalyserTaskContext ctx = new AnalyserTaskContextImpl();
        ctx.getConfiguration().put("excludes", "/a, /b");

        final Rules r = new Rules(ctx);
        assertEquals(0, r.includes.length);
        assertEquals(2, r.excludes.length);
        assertEquals("/a", r.excludes[0]);
        assertEquals("/b", r.excludes[1]);
    }
}
