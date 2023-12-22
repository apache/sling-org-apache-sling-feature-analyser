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
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.felix.utils.resource.ResourceBuilder;
import org.apache.felix.utils.resource.ResourceImpl;
import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.builder.ArtifactProvider;
import org.apache.sling.feature.io.IOUtils;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.PackageInfo;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Information about a bundle
 */
public class BundleDescriptorImpl
    extends BundleDescriptor {

    private static final Logger logger = LoggerFactory.getLogger(BundleDescriptorImpl.class);

    /** The provider to use if now file is given up-front **/
    private final ArtifactProvider artifactProvider;

    /** The bundle symbolic name. */
    private String symbolicName;

    /** The bundle version. */
    private String bundleVersion;

    /** Manifest */
    private final Manifest manifest;

    /** The physical file for analyzing. */
    private URL artifactFile;


    /** The corresponding artifact from the feature. */
    private final Artifact artifact;

    static Manifest getManifest(URL file) throws IOException {
        Manifest manifest = null;
        try {
            if ("file".equals(file.getProtocol()) && new File(file.toURI()).isDirectory()) {
                // it's a directory
                Path manifestPath = Paths.get(file.toURI()).resolve("META-INF/MANIFEST.MF");
                if (Files.exists(manifestPath)) {
                    try (FileInputStream is = new FileInputStream(manifestPath.toFile())) {
                        manifest = new Manifest(is);
                    }
                }
            } else {
                try (JarFile jarFile = IOUtils.getJarFileFromURL(file, true, null)) {
                    manifest = jarFile.getManifest();
                } catch ( final IOException ioe) {
                    // rethrow with more info
                    throw new IOException(file + " : " + ioe.getMessage(), ioe);
                }
            }
        } catch (URISyntaxException e) {
            throw new IOException("Failed to determine if the file is a directory", e);
        }
        return manifest;
    }

    /**
     * Constructor for a new descriptor
     * @param artifact The artifact
     * @param url The URL
     * @throws IOException If the manifest can't be get
     * @throws NullPointerException If artifact is {@code null}
     */
    public BundleDescriptorImpl(final Artifact artifact,
            final URL url) throws IOException  {
        this(artifact, url, null, getManifest(url));
    }

    /**
     * Constructor for a new descriptor
     * @param artifact The artifact
     * @param provider The artifact provider
     * @param manifest The manifest
     * @throws IOException If the manifest can't be get
     * @throws NullPointerException If artifact is {@code null}
     */
    public BundleDescriptorImpl(final Artifact artifact,
                                final ArtifactProvider provider,
                                final Manifest manifest) throws IOException {
        this(artifact, null, provider, manifest);
    }

    /**
     * Constructor for a new descriptor
     * @param artifact The artifact
     * @param url The URL
     * @param provider The artifact provider
     * @param manifest The manifest
     * @throws IOException If the manifest can't be get
     * @throws NullPointerException If artifact is {@code null}
     */
    public BundleDescriptorImpl(final Artifact artifact,
                                final URL url,
                                final ArtifactProvider provider,
                                final Manifest manifest) throws IOException  {
        super(artifact.getId().toMvnId());
        this.artifact = artifact;
        this.artifactFile = url;
        this.artifactProvider = provider;
        if ( manifest == null ) {
            throw new IOException("File has no manifest");
        }
        this.manifest = new Manifest(manifest);

        this.analyze();
        this.lock();
    }

    /**
     * Get the bundle symbolic name.
     * @return The bundle symbolic name
     */
    @Override
    public String getBundleSymbolicName() {
        return symbolicName;
    }

    /**
     * Get the bundle version
     * @return The bundle version
     */
    @Override
    public String getBundleVersion() {
        return bundleVersion;
    }

    @Override
    public URL getArtifactFile() {
        if (artifactFile == null && artifactProvider != null) {
            try {
                artifactFile = artifactProvider.provide(artifact.getId());
            } catch (Exception ex) {
                // Ignore, we assume this is a best effort and callers can handle a null.
                logger.debug("Unable to get artifact file for: " + artifact.getId(), ex);
            }
        }
        return artifactFile;
    }

    @Override
    public Artifact getArtifact() {
        return artifact;
    }

    @Override
    public Manifest getManifest() {
        return this.manifest;
    }

    private void analyze() throws IOException {
        final String name = this.manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
        if ( name != null ) {
            final String version = this.manifest.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
            if ( version == null ) {
                throw new IOException("Unable to get bundle version from artifact " + getArtifact().getId().toMvnId());
            }
            this.symbolicName = name;
            this.bundleVersion = version;
            final String newBundleName = this.getArtifact().getMetadata().get("bundle:rename-bsn");
            if (newBundleName != null) {
                this.symbolicName = newBundleName;
            }

            this.getExportedPackages().addAll(extractExportedPackages(this.manifest));
            this.getImportedPackages().addAll(extractImportedPackages(this.manifest));
            this.getDynamicImportedPackages().addAll(extractDynamicImportedPackages(this.manifest));
            try {
                ResourceImpl resource = ResourceBuilder.build(this.artifact.getId().toMvnUrl(), this.manifest.getMainAttributes().entrySet().stream()
                    .collect(Collectors.toMap(entry -> entry.getKey().toString(), entry -> entry.getValue().toString())));
                this.getCapabilities().addAll(resource.getCapabilities(null));
                this.getRequirements().addAll(resource.getRequirements(null).stream()
                        .map(entry -> new MatchingRequirementImpl(entry)).collect(Collectors.toList()));
            } catch (Exception ex) {
                throw new IOException(ex);
            }
        } else {
            throw new IOException("Unable to get bundle symbolic name from artifact " + getArtifact().getId().toMvnId());
        }
    }

    private static List<PackageInfo> extractPackages(final Manifest m,
        final String headerName,
        final String defaultVersion,
        final boolean checkOptional) {
        final String pckInfo = m.getMainAttributes().getValue(headerName);
        if (pckInfo != null) {
            final Clause[] clauses = Parser.parseHeader(pckInfo);

            final List<PackageInfo> pcks = new ArrayList<>();
            for(final Clause entry : clauses) {
                Object versionObj = entry.getAttribute("version");
                final String version;
                if ( versionObj == null ) {
                    version = defaultVersion;
                } else {
                    version = versionObj.toString();
                }

                boolean optional = false;
                if ( checkOptional ) {
                    final String resolution = entry.getDirective("resolution");
                    optional = "optional".equalsIgnoreCase(resolution);
                }

                Set<String> uses = new HashSet<>();
                String usesAttribute = entry.getDirective("uses");
                if (usesAttribute != null && !usesAttribute.isEmpty()) {
                    StringTokenizer tokenizer = new StringTokenizer(usesAttribute, ",");
                    while (tokenizer.hasMoreTokens()) {
                        String usePackage = tokenizer.nextToken();
                        uses.add(usePackage);
                    }
                }

                final PackageInfo pck = new PackageInfo(entry.getName(),
                    version,
                    optional,
                    uses);
                pcks.add(pck);
            }

            return pcks;
        }
        return Collections.emptyList();
    }

    private static List<PackageInfo> extractExportedPackages(final Manifest m) {
        return extractPackages(m, Constants.EXPORT_PACKAGE, "0.0.0", false);
    }

    private static List<PackageInfo> extractImportedPackages(final Manifest m) {
        return extractPackages(m, Constants.IMPORT_PACKAGE, null, true);
    }

    private static List<PackageInfo> extractDynamicImportedPackages(final Manifest m) {
        return extractPackages(m, Constants.DYNAMICIMPORT_PACKAGE, null, false);
    }
}
