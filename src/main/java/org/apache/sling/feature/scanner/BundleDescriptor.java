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
package org.apache.sling.feature.scanner;

import java.util.jar.Manifest;

import org.apache.sling.feature.scanner.impl.BundleDescriptorImpl;

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
        for(final PackageInfo i : getExportedPackages()) {
            if ( i.getName().equals(packageName) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Is the bundle exporting a package?
     * @param info Package info
     * @return {@code true} if that package is exported.
     */
    public boolean isExportingPackage(final PackageInfo info) {
        for(final PackageInfo i : getExportedPackages()) {
            if ( i.getName().equals(info.getName())
                 && (info.getVersion() == null || info.getPackageVersionRange().includes(i.getPackageVersion()))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if ( obj instanceof BundleDescriptorImpl ) {
            return this.getBundleSymbolicName().equals(((BundleDescriptorImpl)obj).getBundleSymbolicName()) && this.getBundleVersion().equals(((BundleDescriptorImpl)obj).getBundleVersion());
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
        return (this.getBundleSymbolicName() + ':' + this.getBundleVersion()).compareTo((o.getBundleSymbolicName() + ':' + o.getBundleVersion()));
    }
}