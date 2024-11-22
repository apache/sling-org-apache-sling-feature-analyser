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
import java.net.URL;
import java.util.Properties;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.analyser.task.impl.CheckContentPackagesForPaths.Rules;
import org.apache.sling.feature.scanner.impl.ContentPackageDescriptorImpl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CheckContentPackagesForPathsTest {

    @Test
    public void testPathsCheck() throws IOException {
        final CheckContentPackagesForPaths analyser = new CheckContentPackagesForPaths();
        final AnalyserTaskContextImpl ctx = new AnalyserTaskContextImpl();
        ctx.getConfiguration().put("excludes", "/a");
        ctx.getConfiguration().put("includes", "/ab,/b");

        final Rules r = new Rules(ctx);
        final ContentPackageDescriptorImpl desc = new ContentPackageDescriptorImpl(
                "name",
                new Artifact(ArtifactId.parse("g:a:1")),
                new URL("https://sling.apache.org"),
                null,
                null,
                null,
                null,
                new Properties());
        desc.getContentPaths().add("/b/foo");
        desc.getContentPaths().add("/a");
        desc.getContentPaths().add("/a/foo");
        desc.getContentPaths().add("/ab");
        desc.getContentPaths().add("/b");
        desc.getContentPaths().add("/c");

        analyser.checkPackage(ctx, desc, r);

        assertEquals(3, ctx.getErrors().size());
        assertTrue(ctx.getErrors().get(0), ctx.getErrors().get(0).endsWith(" /a"));
        assertTrue(ctx.getErrors().get(1), ctx.getErrors().get(1).endsWith(" /a/foo"));
        assertTrue(ctx.getErrors().get(2), ctx.getErrors().get(2).endsWith(" /c"));
    }

    @Test
    public void testPathsCheckLongestMatching() throws IOException {
        final CheckContentPackagesForPaths analyser = new CheckContentPackagesForPaths();
        final AnalyserTaskContextImpl ctx = new AnalyserTaskContextImpl();
        ctx.getConfiguration().put("excludes", "/a");
        ctx.getConfiguration().put("includes", "/a/foo");

        final Rules r = new Rules(ctx);
        final ContentPackageDescriptorImpl desc = new ContentPackageDescriptorImpl(
                "name",
                new Artifact(ArtifactId.parse("g:a:1")),
                new URL("https://sling.apache.org"),
                null,
                null,
                null,
                null,
                new Properties());
        desc.getContentPaths().add("/a/foo");
        desc.getContentPaths().add("/a/bar");

        analyser.checkPackage(ctx, desc, r);

        assertEquals(1, ctx.getErrors().size());
        assertTrue(ctx.getErrors().get(0), ctx.getErrors().get(0).endsWith(" /a/bar"));
    }
}
