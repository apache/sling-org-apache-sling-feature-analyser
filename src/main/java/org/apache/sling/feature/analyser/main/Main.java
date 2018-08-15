/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.feature.analyser.main;

import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.analyser.Analyser;
import org.apache.sling.feature.io.ArtifactManagerConfig;
import org.apache.sling.feature.io.json.FeatureJSONReader;
import org.apache.sling.feature.scanner.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static String featureFile;
    private static String pluginClass;

    private static void parseArgs(final String[] args) {
        Option fileOption = new Option("f", true, "Set feature file");
        Option pluginOption = new Option("p", true, "Explicitly specify plugin class to run, "
                + "if ommitted the default plugins are used");

        Options options = new Options();
        options.addOption(fileOption);
        options.addOption(pluginOption);

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cl = parser.parse(options, args);

            featureFile = cl.getOptionValue(fileOption.getOpt());
            if (cl.hasOption(pluginOption.getOpt())) {
                pluginClass = cl.getOptionValue(pluginOption.getOpt());
            }
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("", options);
            System.exit(1);
        }
    }

    public static void main(final String[] args) {
        // setup logging
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
        System.setProperty("org.slf4j.simpleLogger.showThreadName", "false");
        System.setProperty("org.slf4j.simpleLogger.levelInBrackets", "true");
        System.setProperty("org.slf4j.simpleLogger.showLogName", "false");

        final Logger logger = LoggerFactory.getLogger("analyser");
        logger.info("Apache Sling Application Analyser");
        logger.info("");

        parseArgs(args);

        if (featureFile == null) {
            logger.error("Required argument missing: feature file");
            System.exit(1);
        }

        Feature feature = null;
        try ( final FileReader r = new FileReader(featureFile)) {
            feature = FeatureJSONReader.read(r, featureFile);
        } catch ( final IOException ioe) {
            logger.error("Unable to read application: {}", featureFile, ioe);
            System.exit(1);
        }

        try {
            final Scanner scanner = new Scanner(new ArtifactManagerConfig());
            final Analyser analyser;
            if (pluginClass != null) {
                analyser = new Analyser(scanner, pluginClass);
            } else {
                analyser = new Analyser(scanner);
            }
            analyser.analyse(feature);
        } catch ( final Exception e) {
            logger.error("Unable to analyse feature: {}", featureFile, e);
            System.exit(1);
        }
    }
}
