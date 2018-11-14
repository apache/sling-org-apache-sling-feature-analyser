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
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.FeatureDescriptor;
import org.apache.sling.feature.scanner.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.sling.feature.analyser.task.AnalyzerTaskProvider.getTasks;
import static org.apache.sling.feature.analyser.task.AnalyzerTaskProvider.getTasksByClassName;
import static org.apache.sling.feature.analyser.task.AnalyzerTaskProvider.getTasksByIds;

public class Analyser {

    private final AnalyserTask[] tasks;

    private final Scanner scanner;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Map<String, Map<String,String>> configurations;

    public Analyser(final Scanner scanner,
            final AnalyserTask...tasks) throws IOException {
        this(scanner, Collections.emptyMap(), tasks);
    }

    public Analyser(final Scanner scanner,
            final Map<String, Map<String,String>> configurations,
            final AnalyserTask...tasks) throws IOException {
        this.tasks = tasks;
        this.configurations = configurations;
        this.scanner = scanner;
    }

    public Analyser(final Scanner scanner,
            final String... taskClassNames)
    throws IOException {
        this(scanner, Collections.emptyMap(), taskClassNames);
    }

    public Analyser(final Scanner scanner,
            final Map<String, Map<String,String>> configurations,
            final String... taskClassNames)
    throws IOException {
        this(scanner, configurations, getTasksByClassName(taskClassNames));
        if ( this.tasks.length != taskClassNames.length ) {
            throw new IOException("Couldn't find all tasks " + Arrays.toString(taskClassNames));
        }
    }

    public Analyser(final Scanner scanner,
                    final Set<String> includes,
                    final Set<String> excludes) throws IOException {
        this(scanner, Collections.emptyMap(), includes, excludes);
    }

    public Analyser(final Scanner scanner,
            final Map<String, Map<String,String>> configurations,
                    final Set<String> includes,
                    final Set<String> excludes) throws IOException {
        this(scanner, configurations, getTasksByIds(includes, excludes));
    }

    public Analyser(final Scanner scanner) throws IOException {
        this(scanner, getTasks());
    }

    public AnalyserResult analyse(final Feature feature) throws Exception {
        return this.analyse(feature, null);
    }

    public AnalyserResult analyse(final Feature feature, final ArtifactId fwk) throws Exception {
        logger.info("Starting analyzing feature '{}'...", feature.getId());

        final FeatureDescriptor featureDesc = scanner.scan(feature);
        BundleDescriptor bd = null;
        if ( fwk != null ) {
            bd = scanner.scan(fwk, feature.getFrameworkProperties());
        }
        final BundleDescriptor fwkDesc = bd;

        final List<String> warnings = new ArrayList<>();
        final List<String> errors = new ArrayList<>();

        // execute analyser tasks
        for(final AnalyserTask task : tasks) {
            logger.info("- Executing {} [{}]...", task.getName(), task.getId());

            final Map<String,String> taskConfiguration = getConfiguration(task.getId());

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
                public BundleDescriptor getFrameworkDescriptor() {
                    return fwkDesc;
                }

                @Override
                public Map<String,String> getConfiguration() {
                    return taskConfiguration;
                }

                @Override
                public void reportWarning(final String message) {
                    warnings.add(message);
                }

                @Override
                public void reportError(final String message) {
                    errors.add(message);
                }
            });
        }

        logger.info("Feature '"
                    + feature.getId()
                + "' provisioning model analyzer finished : " + warnings.size() + " warnings, " + errors.size()
                + " errors.");

        return new AnalyserResult() {

            @Override
            public List<String> getWarnings() {
                return warnings;
            }

            @Override
            public List<String> getErrors() {
                return errors;
            }
        };
    }

    private Map<String,String> getConfiguration(final String id) {
        final Map<String,String> result = new HashMap<>();

        Map<String, String> globalCfg = this.configurations.get("*");
        if (globalCfg != null)
            result.putAll(globalCfg);


        Map<String, String> specificCfg = this.configurations.get(id);
        if (specificCfg != null)
            result.putAll(specificCfg);

        return result;
    }
}
