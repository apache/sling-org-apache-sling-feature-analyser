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

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.FeatureDescriptor;
import org.osgi.annotation.versioning.ProviderType;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The result returned by the analyser
 */
@ProviderType
public interface AnalyserResult {

    public class Report<T> {
        private final T key;
        private final String value;
        
        Report(T key, String value) {
            this.key = key;
            this.value = value;
        }
        public T getKey() {
            return key;
        }
        
        public String getValue() {
            return value;
        }
        
        public String toString() {
            return this.getKey().toString().concat(": ").concat(this.getValue());
        }
    }

    /**
     * Report about a configuration
     * @since 1.4.0
     */
    public class ConfigurationReport extends Report<Configuration> {
        ConfigurationReport(Configuration key, String value) {
            super(key, value);
        }

        public String toString() {
            return "Configuration ".concat(this.getKey().getPid()).concat(": ").concat(this.getValue());
        }
    }

    public class ArtifactReport extends Report<ArtifactId> {
        ArtifactReport(ArtifactId key, String value) {
            super(key, value);
        }
    }

    public class ExtensionReport extends Report<String> {
        ExtensionReport(String key, String value) {
            super(key, value);
        }
    }

    public class GlobalReport extends Report<Void> {

        GlobalReport(String value) {
            super(null, value);
        }

        public String toString() {
            return this.getValue();
        }
    }

    /**
     * List of warnings. Warnings can be used to improve the feature.
     * @return A list of warnings might be empty.
     * @deprecated - use {@link #getGlobalWarnings()}, {@link #getArtifactWarnings()}, {@link #getExtensionWarnings()}, and {@link #getConfigurationWarnings()} instead.
     */
    default List<String> getWarnings() {
        return Stream.of(getGlobalWarnings().stream().map(GlobalReport::toString),
                getArtifactWarnings().stream().map(Report::toString),
                getExtensionWarnings().stream().map(Report::toString),
                getConfigurationWarnings().stream().map(Report::toString))
                .flatMap(Function.identity())
                .collect(Collectors.toList());
    }

    /**
     * List of global warnings. Warnings can be used to improve the feature.
     * @return A list of warnings might be empty.
     */
    List<GlobalReport> getGlobalWarnings();

    /**
     * List of warnings for artifact ids. Warnings can be used to improve the feature.
     * @return A list of warnings might be empty.
     */
    List<ArtifactReport> getArtifactWarnings();

    /**
     * List of warnings for extension names. Warnings can be used to improve the feature.
     * @return A list of warnings might be empty.
     */
    List<ExtensionReport> getExtensionWarnings();

    /**
     * List of warnings for configurations. Warnings can be used to improve the feature.
     * @return A list of warnings might be empty.
     * @since 1.4.0
     */
    List<ConfigurationReport> getConfigurationWarnings();

    /**
     * List of errors. Errors should be fixed in the feature
     * @return A list of errors might be empty
     * @deprecated - use {@link #getGlobalErrors()}, {@link #getArtifactErrors()}, {@link #getExtensionErrors()}, and {@link #getConfigurationErrors()} instead.
     */
    default List<String> getErrors() {
        return Stream.of(getGlobalErrors().stream().map(Report::toString),
                getArtifactErrors().stream().map(Report::toString),
                getExtensionErrors().stream().map(Report::toString),
                getConfigurationErrors().stream().map(Report::toString))
                .flatMap(Function.identity())
                .collect(Collectors.toList());
    }

    /**
     * List of global errors. Errors should be fixed in the feature
     * @return A list of errors might be empty
     */
    List<GlobalReport> getGlobalErrors();

    /**
     * List of errors for artifact ids. Errors should be fixed in the feature
     * @return A list of errors might be empty
     */
    List<ArtifactReport> getArtifactErrors();

    /**
     * List of errors for extension names. Errors should be fixed in the feature
     * @return A list of errors might be empty
     */
    List<ExtensionReport> getExtensionErrors();

    /**
     * List of errors for configurations. Errors should be fixed in the feature
     * @return A list of errors might be empty
     * @since 1.4.0
     */
    List<ConfigurationReport> getConfigurationErrors();

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
