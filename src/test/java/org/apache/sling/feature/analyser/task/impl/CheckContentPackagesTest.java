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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import java.util.Properties;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.scanner.FeatureDescriptor;
import org.apache.sling.feature.scanner.impl.ContentPackageDescriptorImpl;
import org.apache.sling.feature.scanner.impl.FeatureDescriptorImpl;
import org.junit.Test;

public class CheckContentPackagesTest {
    
    private final CheckContentPackages analyser;
    private final AnalyserTaskContextImpl ctx;
    private final FeatureDescriptor fd;

    public CheckContentPackagesTest() {
        analyser = new CheckContentPackages();
        ctx = new AnalyserTaskContextImpl();
        fd = new FeatureDescriptorImpl(ctx.getFeature());
        ctx.setFeatureDescriptor(fd);
    }
    
    @Test 
    public void testContentPackageWithInvalidXMLOnLowReportLevelShouldNotBeReported() throws Exception {
        ctx.getConfiguration().put(CheckContentPackages.MAX_REPORT_LEVEL, "WARN");
        final ContentPackageDescriptorImpl cpd = new ContentPackageDescriptorImpl("content", new Artifact(ArtifactId.parse("g:c:1")), 
                getClass().getClassLoader().getResource("test-invalid-xml.zip").toURI().toURL(),
                null, null, null, null, new Properties());
        fd.getArtifactDescriptors().add(cpd);

        analyser.execute(ctx);
        List<String> errors = ctx.getErrors();
        assertThat(errors.size(), equalTo(0));
    }

    @Test 
    public void testContentPackageWithInvalidXMLShouldBeReported() throws Exception {
        ctx.getConfiguration().put(CheckContentPackages.DISABLED_VALIDATORS, "jackrabbit-filter");
        ctx.getConfiguration().put(CheckContentPackages.MAX_REPORT_LEVEL, "ERROR");
        final ContentPackageDescriptorImpl cpd = new ContentPackageDescriptorImpl("content", new Artifact(ArtifactId.parse("g:c:1")), 
                getClass().getClassLoader().getResource("test-invalid-xml.zip").toURI().toURL(),
                null, null, null, null, new Properties());
        fd.getArtifactDescriptors().add(cpd);

        analyser.execute(ctx);
        List<String> errors = ctx.getErrors();
        assertThat(errors.size(), equalTo(1));
        assertThat(errors.get(0), equalTo("ValidationViolation: \"jackrabbit-docviewparser: Invalid XML found: The reference to entity \"se\" must end with the ';' delimiter.\", filePath=jcr_root/apps/cschneidervalidation/configs/com.adobe.test.Invalid.xml, nodePath=/apps/cschneidervalidation/configs/com.adobe.test.Invalid"));
    }
    
    @Test
    public void testInfo() throws Exception {
        assertThat(analyser.getId(), equalTo("content-packages-validation"));
        assertThat(analyser.getName(), equalTo("Content Package validation"));
    }
    
    @Test
    public void testNoContentPackageURLIsReported() throws Exception {
        final ContentPackageDescriptorImpl cpd = new ContentPackageDescriptorImpl("content", new Artifact(ArtifactId.parse("g:c:1")), 
                null,
                null, null, null, null, new Properties());
        fd.getArtifactDescriptors().add(cpd);
        analyser.execute(ctx);
        List<String> errors = ctx.getErrors();
        assertThat(errors.size(), equalTo(1));
        assertThat(errors.get(0), equalTo("Content package content is not resolved and can not be checked."));
    }
}
