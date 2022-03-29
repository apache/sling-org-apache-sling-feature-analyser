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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.builder.FeatureProvider;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.FeatureDescriptor;
import org.junit.Test;

public class CheckApisJarsPropertiesTest {

    private static class SpyAnalyserTaskContext implements AnalyserTaskContext {
        private final Feature f;
        private final List<String> warnings = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();

        private SpyAnalyserTaskContext(Feature f) {
            this.f = f;
        }

        @Override
        public void reportWarning(String message) {
            System.out.println("[WARN] " + message);
            warnings.add(message);
        }

        @Override
        public void reportArtifactWarning(ArtifactId artifactId, String message) {
            System.out.println("[WARN] " + message);
            warnings.add(message);
        }

        @Override
        public void reportArtifactError(ArtifactId artifactId, String message) {
            System.out.println("[ERROR] " + message);
            errors.add(message);
        }

        @Override
        public void reportExtensionWarning(String extension, String message) {
            System.out.println("[WARN] " + message);
            warnings.add(message);
        }

        @Override
        public void reportExtensionError(String extension, String message) {
            System.out.println("[ERROR] " + message);
            errors.add(message);
        }

        @Override
        public void reportError(String message) {
            System.out.println("[ERROR] " + message);
            errors.add(message);
        }

        @Override
        public BundleDescriptor getFrameworkDescriptor() {
            return null;
        }

        @Override
        public FeatureDescriptor getFeatureDescriptor() {
            throw new UnsupportedOperationException("stub");
        }

        @Override
        public Feature getFeature() {
            return f;
        }

        @Override
        public Map<String, String> getConfiguration() {
            return Collections.emptyMap();
        }
        
        @Override
        public FeatureProvider getFeatureProvider() {
            return null;
        }

        public List<String> getErrors() {
            return errors;
        }
        
        public List<String> getWarnings() {
            return warnings;
        }

        @Override
        public void reportConfigurationError(Configuration cfg, String message) {
            System.out.println("[WARN] " + message);
            errors.add(message);
        }

        @Override
        public void reportConfigurationWarning(Configuration cfg, String message) {
            System.out.println("[ERROR] " + message);
            warnings.add(message);
        }
    }
    
    static class FeatureStub extends Feature {

        public FeatureStub() {
            super(ArtifactId.fromMvnId("org.apache.sling:org.apache.sling.feature.analyser.test:slingosgifeature:validSourceIds:1.0"));
            
            setComplete(true);
        }
        
        public void addArtifactWithSourceId(String artifactId, String sourceId) {
            Artifact artifact = new Artifact(ArtifactId.parse(artifactId));
            if ( sourceId != null )
                artifact.getMetadata().put("source-ids", sourceId);
            
            getBundles().add(artifact);
        }
        
        public void addArtifact(String artifactId) {
            addArtifactWithSourceId(artifactId, null);
        }
        
    }

    @Test
    public void validSourceIds() throws Exception {
        CheckApisJarsProperties check = new CheckApisJarsProperties();
        
        FeatureStub f = new FeatureStub();
        // 1 entry with CSV sourceId
        f.addArtifactWithSourceId("org.apache.aries.transaction:org.apache.aries.transaction.manager:1.2.0", 
                "org.apache.aries.transaction:org.apache.aries.transaction.manager:jar:sources:1.2.0,javax.transaction:javax.transaction-api:jar:sources:1.2,org.apache.geronimo.components:geronimo-transaction:jar:sources:3.1.2");

        // 1 entry with simple sourceId
        f.addArtifactWithSourceId("org.apache.sling:org.apache.sling.scripting.jsp-api:1.0.0", 
                "org.apache.tomcat:tomcat-jsp-api:jar:sources:7.0.96");
        
        // 1 entry with no sourceId
        f.addArtifact("org.apache.sling:org.apache.sling.engine:2.6.20");

        SpyAnalyserTaskContext context = new SpyAnalyserTaskContext(f);
        check.execute(context);
        assertEquals("errors.size", 0, context.getErrors().size());
        assertEquals("warnings.size", 0, context.getWarnings().size());
    }
    
    @Test
    public void invalidSourceId() throws Exception {
        CheckApisJarsProperties check = new CheckApisJarsProperties();
        
        FeatureStub f = new FeatureStub();
        // 1 entry with invalid sourceId
        f.addArtifactWithSourceId("org.apache.sling:org.apache.sling.scripting.jsp-api:1.0.0", 
                "invalid-source-id");

        SpyAnalyserTaskContext context = new SpyAnalyserTaskContext(f);
        check.execute(context);
        assertEquals("errors.size", 1, context.getErrors().size());
        assertEquals("warnings.size", 0, context.getWarnings().size());  
    }
}
