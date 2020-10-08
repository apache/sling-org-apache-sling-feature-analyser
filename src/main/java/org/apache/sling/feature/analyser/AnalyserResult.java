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
package org.apache.sling.feature.analyser;

import java.util.List;

import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.FeatureDescriptor;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The result returned by the analyser
 */
@ProviderType
public interface AnalyserResult {

    /**
     * List of warnings. Warnings can be used to improve the feature.
     * @return A list of warnings might be empty.
     */
    List<String> getWarnings();

    /**
     * List of errors. Errors should be fixed in the feature
     * @return A list of errors might be empty
     */
    List<String> getErrors();

    /**
     * Return the feature descriptor created during scanning
     * @return The feature descriptor
     * @since 1.2.0
     */
    FeatureDescriptor getFeatureDescriptor();

    /**
     * Return the framework descriptor created during scanning if available
     * @return The framework descriptor or {@code null}.
     * @since 1.2.0
     */
    BundleDescriptor getFrameworkDescriptor();
}
