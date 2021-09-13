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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.jar.Manifest;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.ContentPackageDescriptor;

/**
 * Information about a content package.
 */
public class ContentPackageDescriptorImpl extends ContentPackageDescriptor {

    /**
     * The metadata added to bundles and configurations for the package they are in.
     */
    public static final String METADATA_PACKAGE = "content-package";

    /**
     * The metadata added to bundles and configurations for the path in the package
     */
    public static final String METADATA_PATH = "content-path";

    /** Bundles in the content package. */
    private final List<BundleDescriptor> bundles;

    /** Configurations in the content package. */
    private final List<Configuration> configs;

    /** Paths in the content package. */
    private final List<String> paths;

    /** Optional: the descriptor of the parent content package. */
    private ContentPackageDescriptor parentContentPackage;

    /** Optional: the path inside of the parent content package. */
    private String parentContentPath;

    /** Manifest */
    private final Manifest manifest;

    /** The physical file for analyzing. */
    private final URL artifactFile;
    
    /** The corresponding artifact from the feature. */
    private final Artifact artifact;
    
    /** The package properties */
    private final Properties packageProperties;

    /**
     * Constructor for the descriptor
     * @param name The name
     * @param artifact The artifact
     * @param url The url to the binary
     * @param manifest The manifest (optional)
     * @param bundles Mutable list of contained bundles or {@code null}
     * @param paths Mutable list of content paths or {@code null}
     * @param configs Mutable list of configurations or {@code null}
     * @param properties Package properties
     * @throws NullPointerException If artifact is {@code null}
     */
    public ContentPackageDescriptorImpl(final String name,
            final Artifact artifact,
            final URL url,
            final Manifest manifest,
            final List<BundleDescriptor> bundles,
            final List<String> paths,
            final List<Configuration> configs,
            final Properties packageProps) {
        super(name);
        this.bundles = bundles == null ? new ArrayList<>() : bundles;
        this.paths = paths == null ? new ArrayList<>() : paths;
        this.configs = configs == null ? new ArrayList<>() : configs;
        this.artifact = artifact;
        this.artifact.getId(); // throw NPE if artifact is null
        this.artifactFile = url;
        this.manifest = manifest;
        this.packageProperties = packageProps;
    }

    @Override
    public URL getArtifactFile() {
        return this.artifactFile;
    }

    @Override
    public Artifact getArtifact() {
        return artifact;
    }

    @Override
    public Manifest getManifest() {
        return this.manifest;
    }

    @Override
    public List<String> getContentPaths() {
        return this.isLocked() ? Collections.unmodifiableList(this.paths) : this.paths;
    }

    @Override
    public List<BundleDescriptor> getBundles() {
        return this.isLocked() ? Collections.unmodifiableList(this.bundles) : this.bundles;
    }

    @Override
    public List<Configuration> getConfigurations() {
        return this.isLocked() ? Collections.unmodifiableList(this.configs) : this.configs;
    }

    @Override
    public ContentPackageDescriptor getParentContentPackage() {
        return parentContentPackage;
    }

    @Override
    public String getParentContentPath() {
        return this.parentContentPath;
    }

    /**
     * Set the information about the parent content package containing this artifact
     * @param desc The package
     * @param path The path inside the package
     */
    public void setParentContentPackageInfo(final ContentPackageDescriptor desc, final String path) {
        checkLocked();
        this.parentContentPackage = desc;
        this.parentContentPath = path;
    }

    @Override
    public Properties getPackageProperties() {
        return this.packageProperties;
    }
}
