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
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
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

    private static final String FILE_PACKAGE_PROPS = "META-INF/vault/properties.xml";

    private static final String FILE_MANIFEST = "META-INF/MANIFEST.MF";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final byte[] buffer = new byte[65536];

    enum FileType {
        BUNDLE,
        CONFIG,
        PACKAGE,
    }

    private static final List<String> CFG_EXTENSIONS = Arrays.asList(".config", ".cfg", ".cfg.json", ".xml");

    /**
     * Scan the content package for embedded artifacts
     * @param artifact The content package
     * @param url The url to the binary
     * @return A set of artifacts
     * @throws IOException If processing fails
     */
    public Set<ContentPackageDescriptorImpl> scan(final Artifact artifact, final URL url) throws IOException {
        final Set<ContentPackageDescriptorImpl> contentPackages = new HashSet<>();
        if (url != null) {
            final String path = url.getPath();
            final int lastDotInUrl = path.lastIndexOf(".");
            final String name = path.substring(path.lastIndexOf("/") + 1, lastDotInUrl);
             extractContentPackage(null, null, artifact, name, url, contentPackages);
        }

        return contentPackages;
    }

    /**
     * Detect the file type, bundle, configuration or embedded package from the content path
     * @param contentPath The content path
     * @return The detected file type or {@code null}
     */
    FileType detectContentFileType(final String contentPath) {
        FileType fileType = null;

        // check for install folders in libs or apps
        if (contentPath.startsWith("/libs/") || contentPath.startsWith("/apps/")) {

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

                } else if (contentPath.endsWith(".zip")) {
                    fileType = FileType.PACKAGE;

                } else {
                    for(final String ext : CFG_EXTENSIONS) {
                        if ( contentPath.endsWith(ext) ) {
                            fileType = FileType.CONFIG;
                            break;
                        }
                    }
                }
            }
        } else if ( contentPath.startsWith("/etc/packages/") && contentPath.endsWith(".zip")) {
            // embedded content package
            fileType = FileType.PACKAGE;
        }
        return fileType;
    }

    private ContentPackageDescriptorImpl extractContentPackage(final ContentPackageDescriptorImpl parentPackage,
            final String parentContentPath,
            final Artifact packageArtifact,
            final String name,
            final URL archiveUrl,
            final Set<ContentPackageDescriptorImpl> infos)
    throws IOException {
        logger.debug("Analyzing Content Package {}", archiveUrl);

        final File tempDir = Files.createTempDirectory(null).toFile();
        try {
            final File toDir = new File(tempDir, archiveUrl.getPath().substring(archiveUrl.getPath().lastIndexOf("/") + 1));
            toDir.mkdirs();

            Manifest manifest = null;
            final List<File> toProcess = new ArrayList<>();
            final List<String> contentPaths = new ArrayList<>();
            final List<BundleDescriptor> bundles = new ArrayList<>();
            final List<Configuration> configs = new ArrayList<>();
            final Properties packageProps = new Properties();

            try (final JarFile zipFile = IOUtils.getJarFileFromURL(archiveUrl, true, null)) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    final ZipEntry entry = entries.nextElement();
                    final String entryName = entry.getName();

                    // skip dirs
                    if ( entryName.endsWith("/") ) {
                        continue;
                    }

                    logger.debug("Content package entry {}", entryName);

                    if ( entryName.startsWith("jcr_root/") ) {
                        final String contentPath = entryName.substring(8);
                        contentPaths.add(contentPath);

                        final FileType fileType = detectContentFileType(contentPath);
                        if (fileType != null) {
                            logger.debug("- extracting : {}", contentPath);
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
                                bundle.getMetadata().put(ContentPackageDescriptorImpl.METADATA_PACKAGE,
                                        packageArtifact.getId().toMvnId());
                                bundle.getMetadata().put(ContentPackageDescriptorImpl.METADATA_PATH, contentPath);

                                bundles.add(info);

                            } else if (fileType == FileType.CONFIG) {

                                final Configuration configEntry = this.processConfiguration(newFile, packageArtifact.getId(), contentPath);
                                if (configEntry != null) {
                                    configs.add(configEntry);
                                }

                            } else if (fileType == FileType.PACKAGE) {
                                toProcess.add(newFile);
                            }
                        }
                    } else if ( FILE_MANIFEST.equals(entry.getName()) ) {
                        try ( final InputStream zis = zipFile.getInputStream(entry)) {
                            manifest = new Manifest(zis);
                        } catch ( final IOException ignore ) {
                            logger.warn("Failure reading manifest from {} : {}", packageArtifact.getId(), ignore.getMessage());
                        }
                    } else if ( FILE_PACKAGE_PROPS.equals(entry.getName()) ) {
                        try ( final InputStream zis = zipFile.getInputStream(entry)) {
                            packageProps.loadFromXML(zis);
                        }
                    }
                }

                final ContentPackageDescriptorImpl desc = new ContentPackageDescriptorImpl(name, packageArtifact, archiveUrl, manifest,
                    bundles, contentPaths, configs, packageProps);
                if ( parentPackage != null ) {
                    desc.setParentContentPackageInfo(parentPackage, parentContentPath);
                }

                for (final File f : toProcess) {
                    final int lastDot = f.getName().lastIndexOf(".");
                    final String subName = f.getName().substring(0, lastDot);
                    final String contentPath = f.getAbsolutePath().substring(toDir.getAbsolutePath().length()).replace(File.separatorChar, '/');

                    // create synthetic artifact with a synthetic id containing the file name
                    final Artifact subArtifact = new Artifact(packageArtifact.getId().changeClassifier(subName));

                    extractContentPackage(desc, contentPath, subArtifact, subName, f.toURI().toURL(), infos);
                }

                infos.add(desc);
                desc.lock();
                return desc;
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

    Configuration processConfiguration(final File configFile,
            final ArtifactId packageArtifactId,
            final String contentPath)
    throws IOException {

        boolean isConfig = true;
        if ( contentPath.endsWith(".xml") ) {
            final String contents = Files.readAllLines(configFile.toPath()).toString();
            if ( contents.indexOf("jcr:primaryType=\"sling:OsgiConfig\"") == -1 ) {
                isConfig = false;
            }
        }

        if ( isConfig ) {
            final String id;
            if ( contentPath.endsWith("/.content.xml") ) {
                final int lastSlash = contentPath.lastIndexOf('/');
                final int previousSlash = contentPath.lastIndexOf('/', lastSlash - 1);
                id = contentPath.substring(previousSlash + 1, lastSlash);
            } else {
                String name = contentPath;
                final int lastSlash = contentPath.lastIndexOf('/');
                for(final String ext : CFG_EXTENSIONS) {
                    if ( name.endsWith(ext) ) {
                        name = name.substring(lastSlash + 1, name.length() - ext.length());
                    }
                }
                id = name;
            }

            final String pid;
            final int slashPos = id.indexOf('-');
            if ( slashPos == -1 ) {
                pid = id;
            } else {
                pid = id.substring(0, slashPos) + '~' + id.substring(slashPos + 1);
            }

            final Configuration cfg = new Configuration(pid);
            cfg.getProperties().put(Configuration.PROP_PREFIX + ContentPackageDescriptorImpl.METADATA_PATH, contentPath);
            cfg.getProperties().put(Configuration.PROP_PREFIX + ContentPackageDescriptorImpl.METADATA_PACKAGE,
                    packageArtifactId.toMvnId());

            return cfg;
        }

        return null;
    }
}
