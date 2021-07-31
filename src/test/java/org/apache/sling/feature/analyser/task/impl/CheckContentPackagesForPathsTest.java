/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.feature.analyser.task.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.analyser.task.impl.CheckContentPackagesForPaths.Rules;
import org.apache.sling.feature.scanner.impl.ContentPackageDescriptor;
import org.junit.Test;

public class CheckContentPackagesForPathsTest {
    
    @Test public void testNoRulesConfiguration() {
        final CheckContentPackagesForPaths analyser = new CheckContentPackagesForPaths();
        final AnalyserTaskContext ctx = new AnalyserTaskContextImpl();
        
        assertNull(analyser.getRules(ctx));
    }

    @Test public void testIncludesRulesConfiguration() {
        final CheckContentPackagesForPaths analyser = new CheckContentPackagesForPaths();
        final AnalyserTaskContext ctx = new AnalyserTaskContextImpl();
        ctx.getConfiguration().put("includes", "/a, /b");

        final Rules r = analyser.getRules(ctx);
        assertNull(r.excludes);
        assertEquals(2, r.includes.length);
        assertEquals("/a", r.includes[0]);
        assertEquals("/b", r.includes[1]);
    }

    @Test public void testExcludesRulesConfiguration() {
        final CheckContentPackagesForPaths analyser = new CheckContentPackagesForPaths();
        final AnalyserTaskContext ctx = new AnalyserTaskContextImpl();
        ctx.getConfiguration().put("excludes", "/a, /b");

        final Rules r = analyser.getRules(ctx);
        assertNull(r.includes);
        assertEquals(2, r.excludes.length);
        assertEquals("/a", r.excludes[0]);
        assertEquals("/b", r.excludes[1]);
    }

    @Test public void testPathsCheck() throws IOException {
        final CheckContentPackagesForPaths analyser = new CheckContentPackagesForPaths();
        final AnalyserTaskContextImpl ctx = new AnalyserTaskContextImpl();
        ctx.getConfiguration().put("excludes", "/a");
        ctx.getConfiguration().put("includes", "/ab,/b");

        final Rules r = analyser.getRules(ctx);
        final ContentPackageDescriptor desc = new ContentPackageDescriptor("name", new Artifact(ArtifactId.parse("g:a:1")), 
            new URL("https://sling.apache.org"));
        desc.paths.add("/b/foo");
        desc.paths.add("/a");
        desc.paths.add("/a/foo");
        desc.paths.add("/ab");
        desc.paths.add("/b");
        desc.paths.add("/c");

        analyser.checkPackage(ctx, desc, r);
        
        assertEquals(3, ctx.getErrors().size());
        assertTrue(ctx.getErrors().get(0), ctx.getErrors().get(0).endsWith(" /a"));
        assertTrue(ctx.getErrors().get(1), ctx.getErrors().get(1).endsWith(" /a/foo"));
        assertTrue(ctx.getErrors().get(2), ctx.getErrors().get(2).endsWith(" /c"));
    }
}
