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
package org.apache.sling.feature.analyser.task.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;

import org.apache.commons.lang3.SystemUtils;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;

public class CheckJDeps extends AbstractApiRegionsAnalyserTask {

    private static final String CLASSIFIER_APIS = "apis";

    private static final String DEP_NOT_FOUND_TOKEN = "not found";

    private static final String JDEPS_CMD = "jdeps";

    private static final String PARAMETER_APIS_JARS_DIR = "apis-jars-dir";

    private static final String PARAMETER_OWNED_PACKAGES = "owned-packages";

    private static final String PARAMETER_EXCEPTION_PACKAGES = "exception-packages";

    private static final String PARAMETER_SYSTEM_PACKAGES = "system-packages";

    @Override
    protected void execute(ApiRegions apiRegions, AnalyserTaskContext ctx) throws Exception {
        File jDepsExe;
        try {
            jDepsExe = getJDepsExecutable();
        } catch (Exception e) {
            ctx.reportError(e.getMessage());
            return;
        }

        Optional<String> apisJarDirLocation = getConfigurationParameterValue(PARAMETER_APIS_JARS_DIR, ctx);
        if (!apisJarDirLocation.isPresent()) {
            ctx.reportWarning("No apis-jars directory specified, skipping current analyser execution");
            return;
        }

        File apisJarDir = new File(apisJarDirLocation.get());
        if (!apisJarDir.exists() || !apisJarDir.isDirectory()) {
            ctx.reportWarning("apis-jars directory "
                              + apisJarDir
                              + " does not exist or it is not a valid directory, skipping current analyser execution");
            return;
        }

        String[] ownedPackages = getConfigurationParameterValues(PARAMETER_OWNED_PACKAGES, ctx);
        String[] exceptionPackages = getConfigurationParameterValues(PARAMETER_EXCEPTION_PACKAGES, ctx);
        String[] systemPackages = getConfigurationParameterValues(PARAMETER_SYSTEM_PACKAGES, ctx);

        for (String apiRegion : apiRegions.getRegions()) {
            execute(jDepsExe, apisJarDir, apiRegion, ownedPackages, exceptionPackages, systemPackages, ctx);
        }
    }

    private void execute(File jDepsExe,
                         File apisJarDir,
                         String apiRegion,
                         String[] ownedPackages,
                         String[] exceptionPackages,
                         String[] systemPackages,
                         AnalyserTaskContext ctx) throws Exception {
        ArtifactId featureId = ctx.getFeature().getId();

        // classifier is built according to ApisJarMojo

        StringBuilder classifierBuilder = new StringBuilder();
        if (featureId.getClassifier() != null) {
            classifierBuilder.append(featureId.getClassifier())
                             .append('-');
        }
        String finalClassifier = classifierBuilder.append(apiRegion)
                                                  .append('-')
                                                  .append(CLASSIFIER_APIS)
                                                  .toString();

        String targetName = String.format("%s-%s-%s.jar", featureId.getArtifactId(), featureId.getVersion(), finalClassifier);
        File apisJar = new File(apisJarDir, targetName);

        if (!apisJar.exists() || !apisJar.isFile()) {
            ctx.reportWarning("apis-jar file "
                              + apisJar
                              + " does not exist or it is not a valid file, skipping current region '"
                              + apiRegion
                              + "'analyser execution");
            return;
        }

        String[] command = { jDepsExe.getAbsolutePath(), "-apionly", "-verbose", apisJar.getAbsolutePath() };
        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();

        InputStream is = process.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);

        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();

            if (line.contains(DEP_NOT_FOUND_TOKEN)
                    && isAbout(line, ownedPackages)
                    && !isAbout(line, exceptionPackages)
                    && !isAbout(line, systemPackages)) {
                line = line.replaceAll(DEP_NOT_FOUND_TOKEN, "");
                ctx.reportError(line);
            }
        }

        int exitValue = process.waitFor();
        if (exitValue != 0) {
            ctx.reportError(JDEPS_CMD + " terminated with code " + exitValue);
        }
    }

    private static File getJDepsExecutable() throws Exception {
        String jDepsCommand = JDEPS_CMD + (SystemUtils.IS_OS_WINDOWS ? ".exe" : "");

        File jDepsExe;

        // For IBM's JDK 1.2
        if (SystemUtils.IS_OS_AIX) {
            jDepsExe = getFile(SystemUtils.getJavaHome(), "..", "sh", jDepsCommand);
        } else {
            jDepsExe = getFile(SystemUtils.getJavaHome(), "..", "bin", jDepsCommand);
        }

        // ----------------------------------------------------------------------
        // Try to find jdeps exe from JAVA_HOME environment variable
        // ----------------------------------------------------------------------
        if (!jDepsExe.exists() || !jDepsExe.isFile()) {
            String javaHome = System.getenv().get("JAVA_HOME");
            if (javaHome == null || javaHome.isEmpty()) {
                throw new Exception("The environment variable JAVA_HOME is not correctly set.");
            }

            File javaHomeDir = new File(javaHome);
            if ((!javaHomeDir.exists()) || javaHomeDir.isFile()) {
                throw new Exception("The environment variable JAVA_HOME="
                                    + javaHome
                                    + " does not exist or is not a valid directory.");
            }

            jDepsExe = getFile(javaHomeDir, "bin", jDepsCommand);
            if (!jDepsExe.exists() || !jDepsExe.isFile()) {
                throw new Exception("The jdeps executable '"
                                    + jDepsExe
                                    + "' doesn't exist or is not a file. Verify the JAVA_HOME environment variable.");
            }
        }

        return jDepsExe;
    }

    private static Optional<String> getConfigurationParameterValue(String key, AnalyserTaskContext ctx) {
        String value = ctx.getConfiguration().get(key);

        if (value == null || value.isEmpty()) {
            ctx.reportWarning("Configuration parameter '" + key + "' is missing.");
            return Optional.empty();
        }

        return Optional.of(value);
    }

    private static String[] getConfigurationParameterValues(String key, AnalyserTaskContext ctx) {
        Optional<String> value = getConfigurationParameterValue(key, ctx);

        if (!value.isPresent()) {
            return new String[] {};
        }

        return value.get().split(",");
    }

    private static boolean isAbout(String line, String[] packages) {
        for (String adobePackage : packages) {
            // adding . to avoid cases with packages that have the same prefixes
            if (line.startsWith(adobePackage + ".")) {
                return true;
            }
        }
        return false;
    }

    private static File getFile(File parent, String... path) {
        File tmp = parent;
        for (String current : path) {
            tmp = new File(tmp, current);
        }
        return tmp;
    }

}
