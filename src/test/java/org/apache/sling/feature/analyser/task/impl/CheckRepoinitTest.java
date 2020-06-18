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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.builder.FeatureProvider;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.FeatureDescriptor;
import org.junit.Test;

public class CheckRepoinitTest {

    @Test public void testValidExtension() throws Exception {
        final AnalyserTaskContextImpl ctx = new AnalyserTaskContextImpl();
        final AnalyserTask task = new CheckRepoinit();

        // no extension
        task.execute(ctx);
        assertTrue(ctx.getErrors().isEmpty());

        // create valid extension
        final Extension ext = new Extension(ExtensionType.TEXT, Extension.EXTENSION_NAME_REPOINIT, ExtensionState.OPTIONAL);
        ctx.getFeature().getExtensions().add(ext);
        ext.setText("create path /acltest/A/B");

        task.execute(ctx);
        assertTrue(ctx.getErrors().isEmpty());
    }

    @Test public void testInvalidExtension() throws Exception {
        final AnalyserTaskContextImpl ctx = new AnalyserTaskContextImpl();
        final AnalyserTask task = new CheckRepoinit();

        // wrong type
        final Extension ext = new Extension(ExtensionType.JSON, Extension.EXTENSION_NAME_REPOINIT, ExtensionState.OPTIONAL);
        ctx.getFeature().getExtensions().add(ext);

        task.execute(ctx);
        assertEquals(1, ctx.getErrors().size());
    }

    @Test public void testInvalidExtensionRepoinit() throws Exception {
        final AnalyserTaskContextImpl ctx = new AnalyserTaskContextImpl();
        final AnalyserTask task = new CheckRepoinit();

        // invalid repoinit
        final Extension ext = new Extension(ExtensionType.TEXT, Extension.EXTENSION_NAME_REPOINIT, ExtensionState.OPTIONAL);
        ext.setText("create spaceship");
        ctx.getFeature().getExtensions().add(ext);

        task.execute(ctx);
        assertEquals(1, ctx.getErrors().size());
    }

    @Test public void testValidFactoryConfig() throws Exception {
        final AnalyserTaskContextImpl ctx = new AnalyserTaskContextImpl();
        final AnalyserTask task = new CheckRepoinit();

        final Configuration cfg = new Configuration(CheckRepoinit.FACTORY_PID.concat("~name"));
        cfg.getProperties().put("scripts", "create path /acltest/A/B");
        ctx.getFeature().getConfigurations().add(cfg);

        task.execute(ctx);
        assertTrue(ctx.getErrors().isEmpty());
    }

    @Test public void testInvalidFactoryConfig() throws Exception {
        final AnalyserTaskContextImpl ctx = new AnalyserTaskContextImpl();
        final AnalyserTask task = new CheckRepoinit();

        final Configuration cfg = new Configuration(CheckRepoinit.FACTORY_PID.concat("~name"));
        cfg.getProperties().put("scripts", "create spaceship");
        ctx.getFeature().getConfigurations().add(cfg);

        task.execute(ctx);
        assertEquals(1, ctx.getErrors().size());
    }

    public static class AnalyserTaskContextImpl implements AnalyserTaskContext {

        private final Feature f = new Feature(ArtifactId.parse("g:a:1"));

        private final List<String> errors = new ArrayList<>();

        @Override
        public Feature getFeature() {
            return f;
        }

        @Override
        public FeatureDescriptor getFeatureDescriptor() {
            return null;
        }

        @Override
        public BundleDescriptor getFrameworkDescriptor() {
            return null;
        }

        @Override
        public Map<String, String> getConfiguration() {
            return null;
        }

        @Override
        public FeatureProvider getFeatureProvider() {
            return null;
        }

        @Override
        public void reportWarning(String message) {
        }

        @Override
        public void reportError(String message) {
            errors.add(message);
        }

        public List<String> getErrors() {
            return this.errors;
        }
    }
}
