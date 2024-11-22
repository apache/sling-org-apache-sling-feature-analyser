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
package org.apache.sling.feature.analyser;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.FeatureDescriptor;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The result returned by the {@code Analyser}.
 * A result of an analyser run might contain warnings and or errors.
 */
@ProviderType
public interface AnalyserResult {

    /**
     * Base class for a warning or an error.
     */
    public class Report<T> {
        private final T key;
        private final String value;
        private final String taskId;

        Report(T key, String value, String taskId) {
            this.key = key;
            this.value = value;
            this.taskId = taskId;
        }

        /**
         * The key for the message
         * @return The key. Might be {@code null}
         */
        public T getKey() {
            return key;
        }

        /**
         * The message
         * @return The message
         */
        public String getValue() {
            return value;
        }

        /**
         * Return the task id of the analyser task issuing this report
         * @return The task id
         * @since 1.5.0
         */
        public String getTaskId() {
            return this.taskId;
        }

        @Override
        public String toString() {
            return "["
                    .concat(this.getTaskId())
                    .concat("] ")
                    .concat(this.getKey().toString())
                    .concat(": ")
                    .concat(this.getValue());
        }
    }

    /**
     * Report about a configuration
     * @since 1.4.0
     */
    public class ConfigurationReport extends Report<Configuration> {
        ConfigurationReport(Configuration key, String value, String taskId) {
            super(key, value, taskId);
        }

        @Override
        public String toString() {
            return "["
                    .concat(this.getTaskId())
                    .concat("] Configuration ")
                    .concat(this.getKey().getPid())
                    .concat(": ")
                    .concat(this.getValue());
        }
    }

    /**
     * Report about an artifact, for example a bundle.
     */
    public class ArtifactReport extends Report<ArtifactId> {
        ArtifactReport(ArtifactId key, String value, String taskId) {
            super(key, value, taskId);
        }
    }

    /**
     * Report about an extension
     */
    public class ExtensionReport extends Report<String> {
        ExtensionReport(String key, String value, String taskId) {
            super(key, value, taskId);
        }
    }

    /**
     * Report about the feature in general
     */
    public class GlobalReport extends Report<Void> {

        GlobalReport(String value, String taskId) {
            super(null, value, taskId);
        }

        @Override
        public String toString() {
            return "[".concat(this.getTaskId()).concat("] ").concat(this.getValue());
        }
    }

    /**
     * List of warnings. Warnings can be used to improve the feature.
     * This method returns all warnings, if more detailed information about the warnings is desired,
     * use {@link #getGlobalWarnings()}, {@link #getArtifactWarnings()}, {@link #getExtensionWarnings()},
     * and {@link #getConfigurationWarnings()} instead.
     * @return A list of warnings, might be empty.
     */
    default List<String> getWarnings() {
        return Stream.of(
                        getGlobalWarnings().stream().map(GlobalReport::toString),
                        getArtifactWarnings().stream().map(Report::toString),
                        getExtensionWarnings().stream().map(Report::toString),
                        getConfigurationWarnings().stream().map(Report::toString))
                .flatMap(Function.identity())
                .collect(Collectors.toList());
    }

    /**
     * List of global warnings. Warnings can be used to improve the feature.
     * @return A list of warnings, might be empty.
     */
    List<GlobalReport> getGlobalWarnings();

    /**
     * List of warnings for artifact ids. Warnings can be used to improve the feature.
     * @return A list of warnings, might be empty.
     */
    List<ArtifactReport> getArtifactWarnings();

    /**
     * List of warnings for extension names. Warnings can be used to improve the feature.
     * @return A list of warnings, might be empty.
     */
    List<ExtensionReport> getExtensionWarnings();

    /**
     * List of warnings for configurations. Warnings can be used to improve the feature.
     * @return A list of warnings, might be empty.
     * @since 1.4.0
     */
    List<ConfigurationReport> getConfigurationWarnings();

    /**
     * List of errors. Errors should be fixed in the feature
     * This method returns all errors, if more detailed information about the errors is desired,
     * use {@link #getGlobalErrors()}, {@link #getArtifactErrors()}, {@link #getExtensionErrors ()},
     * and {@link #getConfigurationErrors()} instead.
     * @return A list of errors, might be empty.
     */
    default List<String> getErrors() {
        return Stream.of(
                        getGlobalErrors().stream().map(Report::toString),
                        getArtifactErrors().stream().map(Report::toString),
                        getExtensionErrors().stream().map(Report::toString),
                        getConfigurationErrors().stream().map(Report::toString))
                .flatMap(Function.identity())
                .collect(Collectors.toList());
    }

    /**
     * List of global errors. Errors should be fixed in the feature
     * @return A list of error,s might be empty
     */
    List<GlobalReport> getGlobalErrors();

    /**
     * List of errors for artifact ids. Errors should be fixed in the feature.
     * @return A list of errors, might be empty
     */
    List<ArtifactReport> getArtifactErrors();

    /**
     * List of errors for extension names. Errors should be fixed in the feature
     * @return A list of errors, might be empty
     */
    List<ExtensionReport> getExtensionErrors();

    /**
     * List of errors for configurations. Errors should be fixed in the feature
     * @return A list of errors, might be empty
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
