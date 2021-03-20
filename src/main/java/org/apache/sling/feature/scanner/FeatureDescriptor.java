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
package org.apache.sling.feature.scanner;

import org.apache.sling.feature.Feature;

/**
 * Information about a feature.
 *
 * Note that this implementation is not synchronized. If multiple threads access
 * a descriptor concurrently, and at least one of the threads modifies the
 * descriptor structurally, it must be synchronized externally. However, once a
 * descriptor is locked, it is safe to access it concurrently.
 */
public abstract class FeatureDescriptor extends ContainerDescriptor {

    private final Feature feature;

    /**
     * Constructor for a feature descriptor
     * @param f The feature
     */
    protected FeatureDescriptor(final Feature f) {
        super(f.getId().toMvnId());
        feature = f;
        analyze(f);
    }

    private void analyze(final Feature f) {
        getCapabilities().addAll(f.getCapabilities());
        getRequirements().addAll(f.getRequirements());
    }

    /**
     * Return the feature
     * @return The feature
     */
    public Feature getFeature() {
        return feature;
    }
}