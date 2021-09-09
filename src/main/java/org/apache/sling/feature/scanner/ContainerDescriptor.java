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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Information about a container (feature). This is the aggregated information.
 *
 * Note that this implementation is not synchronized. If multiple threads access
 * a descriptor concurrently, and at least one of the threads modifies the
 * descriptor structurally, it must be synchronized externally. However, once a
 * descriptor is locked, it is safe to access it concurrently.
 */
public abstract class ContainerDescriptor extends Descriptor {

    private final Set<BundleDescriptor> bundles = new HashSet<>();

    private final Set<ArtifactDescriptor> artifacts = new HashSet<>();

    /**
     * Constructor for a new descriptor
     * @param name The name
     * @throws IllegalArgumentException if name is {@code null}
     */
    protected ContainerDescriptor(final String name) {
        super(name);
    }

    /**
     * Return a set of bundle descriptors.
     *
     * The requirements and capabilities of the returned bundles are
     * available as an aggregate from {@link Descriptor#getCapabilities()},
     * {@link Descriptor#getRequirements()}, {@link Descriptor#getDynamicImportedPackages()}
     * {@link Descriptor#getExportedPackages()} and {@link Descriptor#getImportedPackages()}
     * @return The set of bundle descriptors (might be empty)
     */
    public final Set<BundleDescriptor> getBundleDescriptors() {
        return this.isLocked() ? Collections.unmodifiableSet(bundles) : bundles;
    }

    /**
     * Return a set of artifact descriptors
     * The requirements and capabilities of the returned artifacts are
     * available as an aggregate from {@link Descriptor#getCapabilities()},
     * {@link Descriptor#getRequirements()}.
     * @return The set of artifact descriptors (might be empty)
     */
    public final Set<ArtifactDescriptor> getArtifactDescriptors() {
        return this.isLocked() ? Collections.unmodifiableSet(artifacts) : artifacts;
    }

    @Override
    public void lock() {
        if ( this.isLocked() ) {
            return;
        }
        for(final BundleDescriptor bd : this.bundles) {
            this.aggregate(bd);
        }
        for(final ArtifactDescriptor d : this.artifacts) {
            this.aggregate(d);
        }
        super.lock();
    }
}