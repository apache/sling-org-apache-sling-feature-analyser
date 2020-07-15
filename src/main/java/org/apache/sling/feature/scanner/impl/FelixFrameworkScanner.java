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
package org.apache.sling.feature.scanner.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.felix.utils.manifest.Parser;
import org.apache.felix.utils.resource.ResourceBuilder;
import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.builder.ArtifactProvider;
import org.apache.sling.feature.io.IOUtils;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.PackageInfo;
import org.apache.sling.feature.scanner.impl.fwk.FrameworkPropertiesGatherer;
import org.apache.sling.feature.scanner.spi.FrameworkScanner;
import org.osgi.framework.Constants;
import org.osgi.resource.Capability;

public class FelixFrameworkScanner implements FrameworkScanner {

    @Override
    public BundleDescriptor scan(final ArtifactId framework,
            final Map<String,String> frameworkProps,
            final ArtifactProvider provider)
    throws IOException {
        final URL platformFile = provider.provide(framework);
        if ( platformFile == null ) {
            throw new IOException("Unable to find file for " + framework.toMvnId());
        }
        final Map<String,String> fwkProps = getFrameworkProperties(frameworkProps, platformFile);
        if ( fwkProps == null ) {
            return null;
        }
        final Set<PackageInfo> pcks = calculateSystemPackages(fwkProps);
        final List<Capability> capabilities = calculateSystemCapabilities(fwkProps);

        final BundleDescriptor d = new BundleDescriptor(framework.toMvnId()) {

            @Override
            public String getBundleSymbolicName() {
                return Constants.SYSTEM_BUNDLE_SYMBOLICNAME;
            }

            @Override
            public String getBundleVersion() {
                return framework.getOSGiVersion().toString();
            }

            @Override
            public int getBundleStartLevel() {
                return 0;
            }

            @Override
            public URL getArtifactFile() {
                return platformFile;
            }

            @Override
            public Artifact getArtifact() {
                return new Artifact(framework);
            }

            @Override
            public Manifest getManifest() {
                return new Manifest();
            }
        };
        d.getCapabilities().addAll(capabilities);
        d.getExportedPackages().addAll(pcks);
        d.lock();
        return d;
    }

    private List<Capability> calculateSystemCapabilities(final Map<String,String> fwkProps) throws IOException {
        Map<String, String> mf = new HashMap<>();
        mf.put(Constants.PROVIDE_CAPABILITY, fwkProps.get(Constants.FRAMEWORK_SYSTEMCAPABILITIES));
        mf.put(Constants.BUNDLE_SYMBOLICNAME, Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
        mf.put(Constants.BUNDLE_MANIFESTVERSION, "2");

        try
        {
            return ResourceBuilder.build(null, mf).getCapabilities(null);
        }
        catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    private Set<PackageInfo> calculateSystemPackages(final Map<String,String> fwkProps) {
        return Stream.of(
                Parser.parseHeader(fwkProps.get(Constants.FRAMEWORK_SYSTEMPACKAGES)))
            .map(
                clause -> new PackageInfo(clause.getName(), clause.getAttribute("version") != null ? clause.getAttribute("version") : "0.0.0", false))
            .collect(Collectors.toSet());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    Map<String,String> getFrameworkProperties(final Map<String,String> appProps, final URL framework)
            throws IOException {
        Path appPropsFile = Files.createTempFile("appProps", ".properties");
        Properties appPropsProperties = new Properties();
        appPropsProperties.putAll(appProps);
        appPropsProperties.store(new FileOutputStream(appPropsFile.toFile()), "appProps");

        File frameworkJar = IOUtils.getFileFromURL(framework, true, null);
        File gathererCP = getGathererClassPath();

        Path outFile = Files.createTempFile("frameworkCaps", ".properties");
        Path runDir = Files.createTempDirectory("frameworkCaps");

        List<String> commandLine = Arrays.asList(
                "-cp",
                gathererCP + File.pathSeparator + frameworkJar.getAbsolutePath(),
                FrameworkPropertiesGatherer.class.getName(),
                appPropsFile.toString(),
                outFile.toString());

        try {
            runJava(new ArrayList<>(commandLine), runDir);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }

        Properties gatheredProps = new Properties();
        gatheredProps.load(new FileInputStream(outFile.toFile()));

        // after reading, delete all temp files and dirs
        Files.delete(appPropsFile);
        Files.delete(outFile);

        try (Stream<Path> fileStream = Files.walk(runDir)) {
            fileStream.sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
        }

        return (Map) gatheredProps;
    }

    private File getGathererClassPath() throws MalformedURLException {
        return getClasspathForClass(FrameworkPropertiesGatherer.class);
    }

    static File getClasspathForClass(Class<?> cls) throws MalformedURLException {
        String clsName = cls.getName();
        String resName = "/" + clsName.replace('.', '/') + ".class";
        URL resource = cls.getResource(resName);
        String resURL = resource.toString();
        if (!resURL.startsWith("jar:file:")) {
            String urlFile = resource.getFile();
            return new File(urlFile.substring(0, urlFile.length() - resName.length()));
        }

        int pingSlash = resURL.indexOf("!/");
        String fileURL = resURL.substring("jar:".length(), pingSlash);
        return new File(new URL(fileURL).getFile());
    }

    private static void runJava(List<String> commandLine, Path execDir)
            throws IOException, InterruptedException {
        String java = System.getProperty("java.home") + "/bin/java";
        commandLine.add(0, java);
        runCommand(commandLine, execDir);
    }

    private static void runCommand(List<String> commandLine, Path execDir)
            throws IOException, InterruptedException {
        Process process = new ProcessBuilder(commandLine)
            .directory(execDir.toFile())
            .inheritIO()
            .start();
        int res = process.waitFor();
        if (res != 0) {
            throw new IOException("Process returned with a failure: " + res);
        }
    }
}
