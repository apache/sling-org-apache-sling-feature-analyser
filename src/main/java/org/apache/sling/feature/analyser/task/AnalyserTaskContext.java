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

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.builder.FeatureProvider;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.FeatureDescriptor;
import org.osgi.annotation.versioning.ProviderType;

import java.util.Map;

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
    FeatureDescriptor getFeatureDescriptor();

    /**
     * Returns a Feature Provider, if present.
     *
     * @return the feature provider to use, or {@code null} if not present.
     */
    FeatureProvider getFeatureProvider();

    /**
     * The framework descriptor
     * @return the descriptor
     */
    BundleDescriptor getFrameworkDescriptor();

    /**
     * Returns the configuration.
     *
     * @return The configuration map for the analyser task
     */
    Map<String,String> getConfiguration();

    /**
     * This method is invoked by a {@link AnalyserTask} to report
     * a global warning.
     * @param message The message.
     */
    void reportWarning(String message);

    /**
     * This method is invoked by a {@link AnalyserTask} to report
     * an artifact warning.
     * @param artifactId the artifactid
     * @param message The message.
     */
    void reportArtifactWarning(ArtifactId artifactId, String message);

    /**
     * This method is invoked by a {@link AnalyserTask} to report
     * an artifact error.
     * @param artifactId the artifactid
     * @param message The message.
     */
    void reportArtifactError(ArtifactId artifactId, String message);

    /**
     * This method is invoked by a {@link AnalyserTask} to report
     * an extension warning.
     * @param extension the extension.
     * @param message The message.
     */
    void reportExtensionWarning(String extension, String message);

    /**
     * This method is invoked by a {@link AnalyserTask} to report
     * an extension error.
     * @param extension the extension.
     * @param message The message.
     */
    void reportExtensionError(String extension, String message);

    /**
     * This method is invoked by a {@link AnalyserTask} to report
     * a configuration warning.
     * @param cfg the configuration.
     * @param message The message.
     * @since 1.3.0
     */
    void reportConfigurationWarning(Configuration cfg, String message);

    /**
     * This method is invoked by a {@link AnalyserTask} to report
     * a configuration error.
     * @param cfg the configuration.
     * @param message The message.
     * @since 1.3.0
     */
    void reportConfigurationError(Configuration cfg, String message);

    /**
     * This method is invoked by a {@link AnalyserTask} to report
     * a global error.
     * @param message The message.
     */
    void reportError(String message);


}

