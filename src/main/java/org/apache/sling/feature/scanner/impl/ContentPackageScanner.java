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
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.io.IOUtils;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scan the contents of a content package.
 */
public class ContentPackageScanner {

    private static final Logger logger = LoggerFactory.getLogger(ContentPackageScanner.class);

    private final byte[] buffer = new byte[65536];

    private enum FileType {
        BUNDLE,
        CONFIG,
        PACKAGE
    }

    /**
     * Scan the content package for embedded artifacts
     * @param artifact The content package
     * @param url The url to the binary
     * @return A set of artifacts
     * @throws IOException If processing fails
     */
    public Set<ContentPackageDescriptor> scan(final Artifact artifact, final URL url) throws IOException {
        final Set<ContentPackageDescriptor> contentPackages = new HashSet<>();
        if (url != null) {
            final int lastDot = url.getPath().lastIndexOf(".");
            final ContentPackageDescriptor cp = new ContentPackageDescriptor(url.getPath().substring(url.getPath().lastIndexOf("/") + 1, lastDot), artifact, url);

            extractContentPackage(cp, contentPackages, url);

            contentPackages.add(cp);
            cp.lock();
        }

        return contentPackages;
    }

    private void extractContentPackage(final ContentPackageDescriptor cp,
            final Set<ContentPackageDescriptor> infos,
            final URL archive)
    throws IOException {
        logger.debug("Analyzing Content Package {}", archive);

        final File tempDir = Files.createTempDirectory(null).toFile();
        try {
            final File toDir = new File(tempDir, archive.getPath().substring(archive.getPath().lastIndexOf("/") + 1));
            toDir.mkdirs();

            final List<File> toProcess = new ArrayList<>();

            try (final JarFile zipFile = IOUtils.getJarFileFromURL(archive, true, null)) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    final ZipEntry entry = entries.nextElement();
                    final String entryName = entry.getName();
                    logger.debug("Content package entry {}", entryName);

                    if ( !entryName.endsWith("/") && entryName.startsWith("jcr_root/") ) {
                        final String contentPath = entryName.substring(8);
                        cp.paths.add(contentPath);

                        FileType fileType = null;

                        if (entryName.endsWith(".zip")) {
                            // embedded content package
                            fileType = FileType.PACKAGE;

                            // check for libs or apps
                        } else if (contentPath.startsWith("/libs/") || contentPath.startsWith("/apps/")) {

                            // check if this is an install folder (I)
                            // install folders are either named:
                            // "install" or
                            // "install.{runmode}"
                            boolean isInstall = contentPath.indexOf("/install/") != -1;
                            if (!isInstall) {
                                final int pos = contentPath.indexOf("/install.");
                                if (pos != -1) {
                                    final int endSlashPos = contentPath.indexOf('/', pos + 1);
                                    if (endSlashPos != -1) {
                                        isInstall = true;
                                    }
                                }
                            }
                            if (!isInstall) {
                                // check if this is an install folder (II)
                                // config folders are either named:
                                // "config" or
                                // "config.{runmode}"
                                isInstall = contentPath.indexOf("/config/") != -1;
                                if (!isInstall) {
                                    final int pos = contentPath.indexOf("/config.");
                                    if (pos != -1) {
                                        final int endSlashPos = contentPath.indexOf('/', pos + 1);
                                        if (endSlashPos != -1) {
                                            isInstall = true;
                                        }
                                    }
                                }
                            }

                            if (isInstall) {

                                if (contentPath.endsWith(".jar")) {
                                    fileType = FileType.BUNDLE;
                                } else if (contentPath.endsWith(".xml") || contentPath.endsWith(".config") || contentPath.endsWith(".cfg.json")) {
                                    fileType = FileType.CONFIG;
                                }
                            }
                        }

                        if (fileType != null) {
                            logger.debug("- extracting : {}", entryName);
                            final File newFile = new File(toDir, entryName.replace('/', File.separatorChar));
                            newFile.getParentFile().mkdirs();

                            try (
                                    final FileOutputStream fos = new FileOutputStream(newFile);
                                    final InputStream zis = zipFile.getInputStream(entry);
                            ) {
                                int len;
                                while ((len = zis.read(buffer)) > -1) {
                                    fos.write(buffer, 0, len);
                                }
                            }

                            if (fileType == FileType.BUNDLE) {
                                int startLevel = 20;
                                final int lastSlash = contentPath.lastIndexOf('/');
                                final int nextSlash = contentPath.lastIndexOf('/', lastSlash - 1);
                                final String part = contentPath.substring(nextSlash + 1, lastSlash);
                                try {
                                    startLevel = Integer.valueOf(part);
                                } catch (final NumberFormatException ignore) {
                                    // ignore
                                }

                                final Artifact bundle = new Artifact(extractArtifactId(tempDir, newFile));
                                bundle.setStartOrder(startLevel);
                                final BundleDescriptor info = new BundleDescriptorImpl(bundle, newFile.toURI().toURL(),
                                        startLevel);
                                bundle.getMetadata().put(ContentPackageDescriptor.METADATA_PACKAGE,
                                        cp.getArtifact().getId().toMvnId());
                                bundle.getMetadata().put(ContentPackageDescriptor.METADATA_PATH, contentPath);

                                cp.bundles.add(info);

                            } else if (fileType == FileType.CONFIG) {

                                final Configuration configEntry = this.process(newFile, cp.getArtifact(), contentPath);
                                if (configEntry != null) {

                                    cp.configs.add(configEntry);
                                }

                            } else if (fileType == FileType.PACKAGE) {
                                toProcess.add(newFile);
                            }

                        }

                    }

                }

                for (final File f : toProcess) {
                    extractContentPackage(cp, infos, f.toURI().toURL());
                    final int lastDot = f.getName().lastIndexOf(".");
                    // create synthetic artifact with a synthetic id containing the file name
                    final Artifact artifact = new Artifact(cp.getArtifact().getId().changeClassifier(f.getName()));
                    final ContentPackageDescriptor i = new ContentPackageDescriptor(f.getName().substring(0, lastDot), artifact, f.toURI().toURL());
                    i.setContentPackageInfo(cp.getArtifact(), f.getName());
                    infos.add(i);

                    i.lock();
                }
            }
        } finally {
            deleteOnExitRecursive(tempDir);
        }
    }

    private void deleteOnExitRecursive(File file) {
        file.deleteOnExit();
        if (file.isDirectory()) {
            File[] childs = file.listFiles();
            if (childs != null) {
                for (File child : childs) {
                    deleteOnExitRecursive(child);
                }
            }
        }
    }

    private ArtifactId extractArtifactId(final File tempDir, final File bundleFile)
    throws IOException {
        logger.debug("Extracting Bundle {}", bundleFile.getName());

        final File toDir = new File(tempDir, bundleFile.getName());
        toDir.mkdirs();

        try (final JarFile zipFile = new JarFile(bundleFile)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while ( entries.hasMoreElements() ) {
                final ZipEntry entry = entries.nextElement();

                final String entryName = entry.getName();
                if ( !entryName.endsWith("/") && entryName.startsWith("META-INF/maven/") && entryName.endsWith("/pom.properties")) {
                    logger.debug("- extracting : {}", entryName);
                    final File newFile = new File(toDir, entryName.replace('/', File.separatorChar));
                    newFile.getParentFile().mkdirs();

                    try (
                            final FileOutputStream fos = new FileOutputStream(newFile);
                            final InputStream zis = zipFile.getInputStream(entry)
                    ) {
                        int len;
                        while ((len = zis.read(buffer)) > -1) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
            }
        }

        // check for maven

        final File metaInfDir = new File(toDir, "META-INF");
        if ( metaInfDir.exists() ) {
            final File mavenDir = new File(metaInfDir, "maven");
            if ( mavenDir.exists() ) {
                File groupDir = null;
                for(final File d : mavenDir.listFiles()) {
                    if ( d.isDirectory() && !d.getName().startsWith(".") ) {
                        groupDir = d;
                        break;
                    }
                }
                if ( groupDir != null ) {
                    File artifactDir = null;
                    for(final File d : groupDir.listFiles()) {
                        if ( d.isDirectory() && !d.getName().startsWith(".") ) {
                            artifactDir = d;
                            break;
                        }
                    }
                    if ( artifactDir != null ) {
                        final File propsFile = new File(artifactDir, "pom.properties");
                        if ( propsFile.exists() ) {
                            final Properties props = new Properties();
                            try ( final Reader r = new FileReader(propsFile) ) {
                                props.load(r);
                            }
                            String groupId = props.getProperty("groupId");
                            String artifactId = props.getProperty("artifactId");
                            String version = props.getProperty("version");
                            String classifier = null;

                            // Capture classifier
                            final int pos = bundleFile.getName().indexOf(version) + version.length();
                            if ( bundleFile.getName().charAt(pos) == '-') {
                                classifier = bundleFile.getName().substring(pos + 1, bundleFile.getName().lastIndexOf('.'));
                            }

                            final String parts[] = version.split("\\.");
                            if ( parts.length == 4 ) {
                                final int lastDot = version.lastIndexOf('.');
                                version = version.substring(0, lastDot) + '-' + version.substring(lastDot + 1);
                            }

                            if ( groupId != null && artifactId != null && version != null ) {
                                return new ArtifactId(groupId,
                                        artifactId,
                                        version, classifier, null);
                            }
                        }
                    }
                }
            }
        }

        throw new IOException(bundleFile.getName() + " has no maven coordinates!");
    }

    private Configuration process(final File configFile,
            final Artifact packageArtifact,
            final String contentPath)
    throws IOException {

        boolean isConfig = true;
        if ( configFile.getName().endsWith(".xml") ) {
            final String contents = Files.readAllLines(configFile.toPath()).toString();
            if ( contents.indexOf("jcr:primaryType=\"sling:OsgiConfig\"") == -1 ) {
                isConfig = false;
            }
        }

        if ( isConfig ) {
            final String id;
            if ( ".content.xml".equals(configFile.getName()) ) {
                final int lastSlash = contentPath.lastIndexOf('/');
                final int previousSlash = contentPath.lastIndexOf('/', lastSlash - 1);
                id = contentPath.substring(previousSlash + 1, lastSlash);
            } else {
                final int lastDot = configFile.getName().lastIndexOf('.');
                id = configFile.getName().substring(0, lastDot);
            }

            final String pid;
            final int slashPos = id.indexOf('-');
            if ( slashPos == -1 ) {
                pid = id;
            } else {
                pid = id.substring(0, slashPos) + '~' + id.substring(slashPos + 1);
            }

            final Configuration cfg = new Configuration(pid);
            cfg.getProperties().put(Configuration.PROP_PREFIX + ContentPackageDescriptor.METADATA_PATH, contentPath);
            cfg.getProperties().put(Configuration.PROP_PREFIX + ContentPackageDescriptor.METADATA_PACKAGE,
                    packageArtifact.getId().toMvnId());

            return cfg;
        }

        return null;
    }
}
