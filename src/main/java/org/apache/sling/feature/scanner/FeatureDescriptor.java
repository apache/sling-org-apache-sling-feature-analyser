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
 */
public abstract class FeatureDescriptor extends ContainerDescriptor {
    private final Feature feature;

    protected FeatureDescriptor(Feature f) {
        super(f.getId().toMvnId());
        feature = f;
        analyze(f);
    }

    private void analyze(Feature f) {
        getCapabilities().addAll(f.getCapabilities());
        getRequirements().addAll(f.getRequirements());
    }

    public Feature getFeature() {
        return feature;
    }
}