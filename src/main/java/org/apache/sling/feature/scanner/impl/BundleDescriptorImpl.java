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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

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

    /** The start level of this artifact. */
    private final int startLevel;

    /** Manifest */
    private final Manifest manifest;

    /** The physical file for analyzing. */
    private final URL artifactFile;


    /** The corresponding artifact from the feature. */
    private final Artifact artifact;

    private static Manifest getManifest(URL file) throws IOException {
        try (JarFile jarFile = IOUtils.getJarFileFromURL(file, true, null)) {
            return jarFile.getManifest();
        }
    }

    public BundleDescriptorImpl(final Artifact a,
            final URL file,
            final int startLevel) throws IOException  {
        this(a, file, null, getManifest(file), startLevel);
    }

    public BundleDescriptorImpl(final Artifact a,
                                final ArtifactProvider provider,
                                final Manifest manifest,
                                final int startLevel) throws IOException {
        this(a, null, provider, manifest, startLevel);
    }

    public BundleDescriptorImpl(final Artifact a,
                                final URL file,
                                final ArtifactProvider provider,
                                final Manifest manifest,
                                final int startLevel) throws IOException  {
        super(a.getId().toMvnId());
        this.artifact = a;
        this.startLevel = startLevel;
        this.artifactFile = file;
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

    /**
     * Get the start level
     * @return The start level or {@code 0} for the default.
     */
    @Override
    public int getBundleStartLevel() {
        return startLevel;
    }

    @Override
    public URL getArtifactFile() {
        URL result = null;
        if (artifactFile == null && artifactProvider != null) {
            try {
                result = artifactProvider.provide(artifact.getId());
            } catch (Exception ex) {
                // Ignore, we assume this is a best effort and callers can handle a null.
                logger.debug("Unable to get artifact file for: " + artifact.getId(), ex);
            }
        }
        return result;
    }

    @Override
    public Artifact getArtifact() {
        return artifact;
    }

    @Override
    public Manifest getManifest() {
        return this.manifest;
    }

    protected void analyze() throws IOException {
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
                ResourceImpl resource = ResourceBuilder.build(null, this.manifest.getMainAttributes().entrySet().stream()
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

    public static List<PackageInfo> extractPackages(final Manifest m,
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

    public static List<PackageInfo> extractExportedPackages(final Manifest m) {
        return extractPackages(m, Constants.EXPORT_PACKAGE, "0.0.0", false);
    }

    public static List<PackageInfo> extractImportedPackages(final Manifest m) {
        return extractPackages(m, Constants.IMPORT_PACKAGE, null, true);
    }

    public static List<PackageInfo> extractDynamicImportedPackages(final Manifest m) {
        return extractPackages(m, Constants.DYNAMICIMPORT_PACKAGE, null, false);
    }
}
