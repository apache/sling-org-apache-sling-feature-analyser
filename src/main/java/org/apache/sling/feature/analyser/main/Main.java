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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.TimeZone;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.analyser.Analyser;
import org.apache.sling.feature.io.file.ArtifactManager;
import org.apache.sling.feature.io.file.ArtifactManagerConfig;
import org.apache.sling.feature.io.json.FeatureJSONReader;
import org.apache.sling.feature.scanner.Scanner;
import org.apache.sling.feature.scanner.spi.ArtifactProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "sfa",
    description = "Apache Sling Application Analyser",
    footer = "Copyright(c) 2018 The Apache Software Foundation."
)
public class Main implements Runnable {

    @Option(names = { "-h", "--help" }, usageHelp = true, description = "Display the usage message.")
    private boolean helpRequested;

    @Option(names = { "-X", "--verbose" }, description = "Produce execution debug output.")
    private boolean debug;

    @Option(names = { "-q", "--quiet" }, description = "Log errors only.")
    private boolean quiet;

    @Option(names = { "-v", "--version" }, description = "Display version information.")
    private boolean printVersion;

    @Option(names = { "-f", "--feature-file" }, description = "Set feature file.", required = true)
    private File featureFile;

    @Option(names = { "-p", "--plugin-class" }, description = "Explicitly specify plugin class to run, "
            + "if ommitted the default plugins are used")
    private String pluginClass;

    public void run() {
        // setup logging
        if (quiet) {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "error");
        } else if (debug) {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
        } else {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
        }
        System.setProperty("org.slf4j.simpleLogger.showThreadName", "false");
        System.setProperty("org.slf4j.simpleLogger.levelInBrackets", "true");
        System.setProperty("org.slf4j.simpleLogger.showLogName", "false");

        final Logger logger = LoggerFactory.getLogger("analyser");

        // Add the Shutdown Hook to the Java virtual machine
        // in order to destroy all the allocated resources
        Runtime.getRuntime().addShutdownHook(new ShutDownHook(logger));

        if (printVersion) {
            printVersion(logger);
        }

        String appName = getClass().getAnnotation(Command.class).description()[0];

        logger.info(appName);
        logger.info("");

        if (!featureFile.exists() || !featureFile.isFile()) {
            logger.error("Feature file does not exist or is not a valid file");
            System.exit(1);
        }

        Feature feature = null;
        try ( final FileReader r = new FileReader(featureFile)) {
            feature = FeatureJSONReader.read(r, featureFile.getAbsolutePath());
        } catch ( final IOException ioe) {
            logger.error("Unable to read application: {}", featureFile, ioe);
            System.exit(1);
        }

        try {
            final ArtifactManager am = ArtifactManager.getArtifactManager(new ArtifactManagerConfig());
            final Scanner scanner = new Scanner(new ArtifactProvider() {

                @Override
                public File provide(ArtifactId id) {
                    try {
                        return am.getArtifactHandler(id.toMvnUrl()).getFile();
                    } catch (final IOException e) {
                        return null;
                    }
                }
            });
            final Analyser analyser;
            if (pluginClass != null) {
                analyser = new Analyser(scanner, pluginClass);
            } else {
                analyser = new Analyser(scanner);
            }
            analyser.analyse(feature);

            logger.info( "+-----------------------------------------------------+" );
            logger.info("{} SUCCESS", appName);
        } catch ( final Exception e) {
            logger.info( "+-----------------------------------------------------+" );
            logger.info("{} FAILURE", appName);
            logger.info( "+-----------------------------------------------------+" );

            if (debug) {
                logger.error("Unable to analyse feature {}:", featureFile, e);
            } else {
                logger.error("Unable to analyse feature {}: {}", featureFile, e.getMessage());
            }

            logger.info( "" );

            System.exit(1);
        }

        logger.info( "+-----------------------------------------------------+" );
    }

    private static void printVersion(final Logger logger) {
        logger.info("{} v{} (built on {})",
                System.getProperty("project.artifactId"),
                System.getProperty("project.version"),
                System.getProperty("build.timestamp"));
        logger.info("Java version: {}, vendor: {}",
                System.getProperty("java.version"),
                System.getProperty("java.vendor"));
        logger.info("Java home: {}", System.getProperty("java.home"));
        logger.info("Default locale: {}_{}, platform encoding: {}",
                System.getProperty("user.language"),
                System.getProperty("user.country"),
                System.getProperty("sun.jnu.encoding"));
        logger.info("Default Time Zone: {}", TimeZone.getDefault().getDisplayName());
        logger.info("OS name: \"{}\", version: \"{}\", arch: \"{}\", family: \"{}\"",
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch"),
                getOsFamily());
        logger.info("+-----------------------------------------------------+");
    }

    private static final String getOsFamily() {
        String osName = System.getProperty("os.name").toLowerCase();
        String pathSep = System.getProperty("path.separator");

        if (osName.indexOf("windows") != -1) {
            return "windows";
        } else if (osName.indexOf("os/2") != -1) {
            return "os/2";
        } else if (osName.indexOf("z/os") != -1 || osName.indexOf("os/390") != -1) {
            return "z/os";
        } else if (osName.indexOf("os/400") != -1) {
            return "os/400";
        } else if (pathSep.equals(";")) {
            return "dos";
        } else if (osName.indexOf("mac") != -1) {
            if (osName.endsWith("x")) {
                return "mac"; // MACOSX
            }
            return "unix";
        } else if (osName.indexOf("nonstop_kernel") != -1) {
            return "tandem";
        } else if (osName.indexOf("openvms") != -1) {
            return "openvms";
        } else if (pathSep.equals(":")) {
            return "unix";
        }

        return "undefined";
    }

    public static void main(String[] args) {
        CommandLine.run(new Main(), args);
    }
}
