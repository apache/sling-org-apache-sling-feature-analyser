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
package org.apache.sling.feature.analyser.task;

import org.apache.sling.feature.Feature;
import org.apache.sling.feature.scanner.FeatureDescriptor;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface AnalyserTaskContext {

    /**
     * The assembled feature.
     * @return The feature.
     */
    Feature getFeature();

    /**
     * The feature descriptor.
     * @return the descriptor.
     */
    FeatureDescriptor getDescriptor();

    /**
     * This method is invoked by a {@link AnalyserTask} to report
     * a warning.
     * @param message The message.
     */
    void reportWarning(String message);

    /**
     * This method is invoked by a {@link AnalyserTask} to report
     * an error.
     * @param message The message.
     */
    void reportError(String message);
}

