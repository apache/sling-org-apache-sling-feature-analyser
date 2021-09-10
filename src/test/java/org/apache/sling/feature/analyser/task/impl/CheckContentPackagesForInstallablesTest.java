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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.scanner.FeatureDescriptor;
import org.apache.sling.feature.scanner.impl.ContentPackageDescriptor;
import org.apache.sling.feature.scanner.impl.FeatureDescriptorImpl;
import org.junit.Test;

public class CheckContentPackagesForInstallablesTest {

    @Test public void testEmptyDescriptor() throws Exception {
        final CheckContentPackageForInstallables analyser = new CheckContentPackageForInstallables();
        final AnalyserTaskContextImpl ctx = new AnalyserTaskContextImpl();

        final FeatureDescriptor fd = new FeatureDescriptorImpl(ctx.getFeature());
        ctx.setFeatureDescriptor(fd);

        analyser.execute(ctx);
        assertTrue(ctx.getErrors().isEmpty());
    }

    @Test public void testEmptyContentPackage() throws Exception {
        final CheckContentPackageForInstallables analyser = new CheckContentPackageForInstallables();
        final AnalyserTaskContextImpl ctx = new AnalyserTaskContextImpl();

        final FeatureDescriptor fd = new FeatureDescriptorImpl(ctx.getFeature());
        ctx.setFeatureDescriptor(fd);

        final ContentPackageDescriptor cpd = new ContentPackageDescriptor("content", new Artifact(ArtifactId.parse("g:c:1")), new URL("file:/foo"));
        fd.getArtifactDescriptors().add(cpd);

        analyser.execute(ctx);
        assertTrue(ctx.getErrors().toString(), ctx.getErrors().isEmpty());

        ctx.putConfigurationValue(CheckContentPackageForInstallables.CFG_CHECK_PACKAGES, "true");
        analyser.execute(ctx);
        assertTrue(ctx.getErrors().toString(), ctx.getErrors().isEmpty());
    }

    @Test public void testEmbeddedContentPackage() throws Exception {
        final CheckContentPackageForInstallables analyser = new CheckContentPackageForInstallables();
        final AnalyserTaskContextImpl ctx = new AnalyserTaskContextImpl();

        final FeatureDescriptor fd = new FeatureDescriptorImpl(ctx.getFeature());
        ctx.setFeatureDescriptor(fd);

        final ContentPackageDescriptor cpd = new ContentPackageDescriptor("content", new Artifact(ArtifactId.parse("g:c:1")), new URL("file:/foo"));
        fd.getArtifactDescriptors().add(cpd);

        final ContentPackageDescriptor embedded = new ContentPackageDescriptor("embedded", new Artifact(ArtifactId.parse("g:e:1")), new URL("file:/foo"));
        embedded.setContentPackageInfo(cpd.getArtifact(), "/path");
        fd.getArtifactDescriptors().add(embedded);

        analyser.execute(ctx);
        assertTrue(ctx.getErrors().toString(), ctx.getErrors().isEmpty());

        ctx.putConfigurationValue(CheckContentPackageForInstallables.CFG_CHECK_PACKAGES, "true");
        analyser.execute(ctx);
        assertFalse(ctx.getErrors().toString(), ctx.getErrors().isEmpty());
    }
}
