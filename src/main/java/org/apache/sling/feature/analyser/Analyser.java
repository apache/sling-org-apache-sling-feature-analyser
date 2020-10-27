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

import static org.apache.sling.feature.analyser.task.AnalyzerTaskProvider.getTasks;
import static org.apache.sling.feature.analyser.task.AnalyzerTaskProvider.getTasksByClassName;
import static org.apache.sling.feature.analyser.task.AnalyzerTaskProvider.getTasksByIds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.ExecutionEnvironmentExtension;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.analyser.extensions.AnalyserMetaDataExtension;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.builder.FeatureProvider;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.FeatureDescriptor;
import org.apache.sling.feature.scanner.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Analyser {
    /**
     * Configurration key for configuration that applies to all tasks.
     */
    static final String ALL_TASKS_KEY = "all";

    private final AnalyserTask[] tasks;

    private final Scanner scanner;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Map<String, Map<String, String>> configurations;

    /**
     * Create new analyser with a provided scanner and the tasks to run
     * 
     * @param scanner The scanner
     * @param tasks   The tasks to run
     * @throws IOException If setting up the analyser fails
     */
    public Analyser(final Scanner scanner, final AnalyserTask... tasks) throws IOException {
        this(scanner, Collections.emptyMap(), tasks);
    }

    /**
     * Create a new analyser with a provided scanner, tasks and configurations
     * 
     * @param scanner        The scanner
     * @param configurations The configurations for the tasks
     * @param tasks          The tasks
     * @throws IOException If setting up the analyser fails
     */
    public Analyser(final Scanner scanner, final Map<String, Map<String, String>> configurations,
            final AnalyserTask... tasks) throws IOException {
        this.tasks = tasks;
        this.configurations = configurations;
        this.scanner = scanner;
    }

    /**
     * Create a new analyser with the provided scanner and task class names
     * 
     * @param scanner        The scanner
     * @param taskClassNames The task class names
     * @throws IOException If setting up the analyser fails
     */
    public Analyser(final Scanner scanner, final String... taskClassNames) throws IOException {
        this(scanner, Collections.emptyMap(), taskClassNames);
    }

    /**
     * Create a new analyser with a provided scanner, task class names and
     * configurations
     * 
     * @param scanner        The scanner
     * @param configurations The configurations for the tasks
     * @param taskClassNames The task class names
     * @throws IOException If setting up the analyser fails
     */
    public Analyser(final Scanner scanner, final Map<String, Map<String, String>> configurations,
            final String... taskClassNames) throws IOException {
        this(scanner, configurations, getTasksByClassName(taskClassNames));
        if (this.tasks.length != taskClassNames.length) {
            throw new IOException("Couldn't find all tasks " + Arrays.toString(taskClassNames));
        }
    }

    /**
     * Create a new analyser with a provided scanner and includes/excludes for the
     * task ids
     * 
     * @param scanner  The scanner
     * @param includes The includes for the task ids - can be {@code null}
     * @param excludes The excludes for the task ids - can be {@code null}
     * @throws IOException If setting up the analyser fails
     */
    public Analyser(final Scanner scanner, final Set<String> includes, final Set<String> excludes) throws IOException {
        this(scanner, Collections.emptyMap(), includes, excludes);
    }

    /**
     * Create a new analyser with a provided scanner and includes/excludes for the
     * task ids and configuration
     * 
     * @param scanner        The scanner
     * @param configurations The configurations for the tasks
     * @param includes       The includes for the task ids - can be {@code null}
     * @param excludes       The excludes for the task ids - can be {@code null}
     * @throws IOException If setting up the analyser fails
     */
    public Analyser(final Scanner scanner, final Map<String, Map<String, String>> configurations,
            final Set<String> includes, final Set<String> excludes) throws IOException {
        this(scanner, configurations, getTasksByIds(includes, excludes));
    }

    /**
     * Create a new analyser with the provided scanner and use all available tasks
     * 
     * @param scanner The scanner
     * @throws IOException If setting up the analyser fails
     */
    public Analyser(final Scanner scanner) throws IOException {
        this(scanner, getTasks());
    }

    /**
     * Analyse the feature
     * 
     * @param feature The feature to analyse
     * @return The analyser result
     * @throws Exception If analysing fails
     */
    public AnalyserResult analyse(final Feature feature) throws Exception {
        return this.analyse(feature, null);
    }

    /**
     * Analyse the feature using the provided framework artifact
     * 
     * @param feature The feature to analyse
     * @param fwk     The OSGi framework artifact
     * @return The analyser result
     * @throws Exception If analysing fails
     */
    public AnalyserResult analyse(final Feature feature, final ArtifactId fwk) throws Exception {
        return analyse(feature, fwk, null);
    }

    /**
     * Analyse the feature using the provided framework artifact
     * 
     * @param feature         The feature to analyse
     * @param fwk             The OSGi framework artifact
     * @param featureProvider Optional provider to resolve features (if required)
     * @return The analyser result
     * @throws Exception If analysing fails
     */
    public AnalyserResult analyse(final Feature feature, final ArtifactId fwk, final FeatureProvider featureProvider)
            throws Exception {
        logger.info("Starting analyzing feature '{}'...", feature.getId());

        final FeatureDescriptor featureDesc = scanner.scan(feature);
        BundleDescriptor bd = null;
        ArtifactId framework = fwk;
        if (framework == null) {
            final ExecutionEnvironmentExtension ext = ExecutionEnvironmentExtension
                    .getExecutionEnvironmentExtension(feature);
            if (ext != null && ext.getFramework() != null) {
                framework = ext.getFramework().getId();
            }
        }
        if (framework != null) {
            bd = scanner.scan(framework, feature.getFrameworkProperties());
        }
        final BundleDescriptor fwkDesc = bd;

        final List<AnalyserResult.GlobalReport> globalWarnings = new ArrayList<>();
        final List<AnalyserResult.ArtifactReport> artifactWarnings = new ArrayList<>();
        final List<AnalyserResult.ExtensionReport> extensionWarnings = new ArrayList<>();

        final List<AnalyserResult.GlobalReport> globalErrors = new ArrayList<>();
        final List<AnalyserResult.ArtifactReport> artifactErrors = new ArrayList<>();
        final List<AnalyserResult.ExtensionReport> extensionErrors = new ArrayList<>();

        AnalyserMetaDataExtension analyserMetaDataExtension = AnalyserMetaDataExtension.getAnalyserMetaDataExtension(feature);

        // execute analyser tasks
        for (final AnalyserTask task : tasks) {
            logger.info("- Executing {} [{}]...", task.getName(), task.getId());

            final Map<String, String> taskConfiguration = getConfiguration(task.getId());

            task.execute(new AnalyserTaskContext() {

                @Override
                public Feature getFeature() {
                    return feature;
                }

                @Override
                public FeatureDescriptor getFeatureDescriptor() {
                    return featureDesc;
                }

                @Override
                public FeatureProvider getFeatureProvider() {
                    return featureProvider;
                }

                @Override
                public BundleDescriptor getFrameworkDescriptor() {
                    return fwkDesc;
                }

                @Override
                public Map<String, String> getConfiguration() {
                    return taskConfiguration;
                }

                @Override
                public void reportWarning(final String message) {
                    if (analyserMetaDataExtension != null && analyserMetaDataExtension.reportWarning(feature.getId())) {
                        globalWarnings.add(new AnalyserResult.GlobalReport(message));
                    }
                }

                @Override
                public void reportArtifactWarning(ArtifactId artifactId, String message) {
                    if (analyserMetaDataExtension != null && analyserMetaDataExtension.reportWarning(artifactId) && analyserMetaDataExtension.reportWarning(feature.getId())) {
                        artifactWarnings.add(new AnalyserResult.ArtifactReport(artifactId, message));
                    }
                }

                @Override
                public void reportArtifactError(ArtifactId artifactId, String message) {
                    if (analyserMetaDataExtension != null && analyserMetaDataExtension.reportError(artifactId)&& analyserMetaDataExtension.reportError(feature.getId())) {
                        artifactErrors.add(new AnalyserResult.ArtifactReport(artifactId, message));
                    }
                }

                @Override
                public void reportExtensionWarning(String extension, String message) {
                    if (analyserMetaDataExtension != null && analyserMetaDataExtension.reportWarning(feature.getId())) {
                        extensionWarnings.add(new AnalyserResult.ExtensionReport(extension, message));
                    }
                }

                @Override
                public void reportExtensionError(String extension, String message) {
                    if (analyserMetaDataExtension != null && analyserMetaDataExtension.reportError(feature.getId())) {
                        extensionErrors.add(new AnalyserResult.ExtensionReport(extension, message));
                    }
                }

                @Override
                public void reportError(final String message) {
                    if (analyserMetaDataExtension != null && analyserMetaDataExtension.reportError(feature.getId())) {
                        globalErrors.add(new AnalyserResult.GlobalReport(message));
                    }
                }
            });
        }

        logger.info("Analyzing feature '" + feature.getId() + "' finished : " + globalWarnings.size() + artifactWarnings.size() + extensionWarnings.size()  + " warnings, "
                + globalErrors.size() + artifactErrors.size() + extensionErrors.size() + " errors.");

        return new AnalyserResult() {
            @Override
            public List<GlobalReport> getGlobalWarnings() {
                return globalWarnings;
            }

            @Override
            public List<ArtifactReport> getArtifactWarnings() {
                return artifactWarnings;
            }

            @Override
            public List<ExtensionReport> getExtensionWarnings() {
                return extensionWarnings;
            }

            @Override
            public List<GlobalReport> getGlobalErrors() {
                return globalErrors;
            }

            @Override
            public List<ArtifactReport> getArtifactErrors() {
                return artifactErrors;
            }

            @Override
            public List<ExtensionReport> getExtensionErrors() {
                return extensionErrors;
            }

            @Override
            public FeatureDescriptor getFeatureDescriptor() {
                return featureDesc;
            }

            @Override
            public BundleDescriptor getFrameworkDescriptor() {
                return fwkDesc;
            }
        };
    }

    Map<String,String> getConfiguration(final String id) {
        final Map<String,String> result = new HashMap<>();

        Map<String, String> globalCfg = this.configurations.get(ALL_TASKS_KEY);
        if (globalCfg != null)
            result.putAll(globalCfg);


        Map<String, String> specificCfg = this.configurations.get(id);
        if (specificCfg != null)
            result.putAll(specificCfg);

        return result;
    }
}
