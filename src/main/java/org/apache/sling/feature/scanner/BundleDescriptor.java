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

import java.util.Set;
import java.util.jar.Manifest;

/**
 * Information about a bundle.
 *
 * Note that this implementation is not synchronized. If multiple threads access
 * a descriptor concurrently, and at least one of the threads modifies the
 * descriptor structurally, it must be synchronized externally. However, once a
 * descriptor is locked, it is safe to access it concurrently.
 */
public abstract class BundleDescriptor extends ArtifactDescriptor implements Comparable<BundleDescriptor> {

    /**
     * Constructor for a new descriptor
     * @param name The name
     * @throws IllegalArgumentException if name is {@code null}
     */
    protected BundleDescriptor(final String name) {
        super(name);
    }

    /**
     * Get the bundle symbolic name.
     * @return The bundle symbolic name
     */
    public abstract String getBundleSymbolicName();

    /**
     * Get the bundle version
     * @return The bundle version
     */
    public abstract String getBundleVersion();

    /**
     * Return the bundle manifest
     * @return The manifest
     */
    @Override
    public abstract Manifest getManifest();

    /**
     * Is the bundle exporting a package?
     * @param packageName Package name
     * @return {@code true} if that package is exported.
     */
    public boolean isExportingPackage(final String packageName) {
        Set<PackageInfo> exportedPackages = getExportedPackages(packageName);
        return exportedPackages != null && !exportedPackages.isEmpty();
    }

    /**
     * Is the bundle exporting a package?
     * @param info Package info
     * @return {@code true} if that package is exported.
     */
    public boolean isExportingPackage(final PackageInfo info) {
        String packageName = info.getName();
        Set<PackageInfo> exportedPackages = getExportedPackages(packageName);
        if (exportedPackages == null) {
            return false;
        }
        return exportedPackages.stream()
                .anyMatch(packageInfo -> info.getVersion() == null
                        || info.getPackageVersionRange().includes(packageInfo.getPackageVersion()));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BundleDescriptor) {
            return this.getBundleSymbolicName().equals(((BundleDescriptor) obj).getBundleSymbolicName())
                    && this.getBundleVersion().equals(((BundleDescriptor) obj).getBundleVersion());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (this.getBundleSymbolicName() + ':' + this.getBundleVersion()).hashCode();
    }

    @Override
    public String toString() {
        return "BundleInfo [symbolicName=" + getBundleSymbolicName() + ", version=" + this.getBundleVersion() + "]";
    }

    @Override
    public int compareTo(final BundleDescriptor o) {
        return (this.getBundleSymbolicName() + ':' + this.getBundleVersion())
                .compareTo((o.getBundleSymbolicName() + ':' + o.getBundleVersion()));
    }
}
