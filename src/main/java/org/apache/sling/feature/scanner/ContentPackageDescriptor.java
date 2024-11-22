/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.feature.scanner;

import java.util.List;
import java.util.Properties;

import org.apache.sling.feature.Configuration;

/**
 * Information about a content package.
 * @since 2.3.0
 */
public abstract class ContentPackageDescriptor extends ArtifactDescriptor {

    /**
     * Constructor for the descriptor
     * @param name The name
     * @throws IllegalArgumentException if name is {@code null}
     */
    public ContentPackageDescriptor(final String name) {
        super(name);
    }

    /**
     * Get the content paths
     * @return The list of content paths
     */
    public abstract List<String> getContentPaths();

    /**
     * Get the included bundles
     * @return The list of bundles, might be empty
     */
    public abstract List<BundleDescriptor> getBundles();

    /**
     * Get the included configurations
     * @return The list of configurations, might be empty
     */
    public abstract List<Configuration> getConfigurations();

    /**
     * Get the parent content package
     * @return The parent content package or {@code null}
     */
    public abstract ContentPackageDescriptor getParentContentPackage();

    /**
     * Get the parent content path
     * @return The parent content path or {@code null}
     */
    public abstract String getParentContentPath();

    /**
     * Whether this artifact is embedded in a content package
     * @return {@code true} if embedded.
     */
    public boolean isEmbeddedInContentPackage() {
        return this.getParentContentPath() != null;
    }

    /**
     * Check whether the package has embedded artifacts
     * @return {@code true} if the package has embedded artifacts
     */
    public boolean hasEmbeddedArtifacts() {
        return !this.getBundles().isEmpty() || !this.getConfigurations().isEmpty();
    }

    /**
     * Get the package properties
     * @return The package properties
     */
    public abstract Properties getPackageProperties();

    @Override
    public String toString() {
        return "ContentPackage [" + getName() + "]";
    }
}
