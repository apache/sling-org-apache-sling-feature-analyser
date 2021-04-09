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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.builder.FeatureProvider;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.FeatureDescriptor;

public class AnalyserTaskContextImpl implements AnalyserTaskContext {

    private final Feature f;
    private final Map<String, String> configuration = new HashMap<>();

    private final List<String> errors = new ArrayList<>();

    public AnalyserTaskContextImpl() {
        this("g:a:1");
    }
    
    public AnalyserTaskContextImpl(String artifactId) {
        f = new Feature(ArtifactId.parse(artifactId));
    }

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
        return configuration;
    }

    public void putConfigurationValue(String key, String value) {
        configuration.put(key, value);
    }

    @Override
    public FeatureProvider getFeatureProvider() {
        return null;
    }

    @Override
    public void reportWarning(String message) {
    }

    @Override
    public void reportArtifactWarning(ArtifactId artifactId, String message) {

    }

    @Override
    public void reportArtifactError(ArtifactId artifactId, String message) {
        errors.add(message);
    }

    @Override
    public void reportExtensionWarning(String extension, String message) {

    }

    @Override
    public void reportExtensionError(String extension, String message) {
        errors.add(message);
    }

    @Override
    public void reportError(String message) {
        errors.add(message);
    }

    public List<String> getErrors() {
        return this.errors;
    }
}