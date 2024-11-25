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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.sling.feature.MatchingRequirement;
import org.apache.sling.feature.scanner.impl.NamespacedSets;
import org.jetbrains.annotations.Contract;
import org.osgi.resource.Capability;

/**
 * A descriptor holds information about requirements and capabilities.
 *
 * Note that this implementation is not synchronized. If multiple threads access
 * a descriptor concurrently, and at least one of the threads modifies the
 * descriptor structurally, it must be synchronized externally. However, once a
 * descriptor is locked, it is safe to access it concurrently.
 */
public abstract class Descriptor {

    private final String name;

    private boolean locked;

    private final NamespacedSets<PackageInfo> exports = new NamespacedSets<>(PackageInfo.class, PackageInfo::getName);

    private final Set<PackageInfo> imports = new HashSet<>();

    private final Set<PackageInfo> dynImports = new HashSet<>();

    private final Set<MatchingRequirement> reqs = new HashSet<>();

    private final NamespacedSets<Capability> caps = new NamespacedSets<>(Capability.class, Capability::getNamespace);

    /**
     * Constructor for a new descriptor
     * @param name Name
     * @throws IllegalArgumentException if name is {@code null}
     */
    protected Descriptor(final String name) {
        this.name = name;
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
    }

    /**
     * Lock the descriptor. Once invoked no changes can be made to the descriptor.
     */
    public void lock() {
        this.locked = true;
    }

    /**
     * Check if the descriptor is locked
     * @return {@code true} if locked.
     */
    public final boolean isLocked() {
        return this.locked;
    }

    /**
     * Internal method for checking locked state
     * @throws IllegalStateException If locked
     */
    protected void checkLocked() {
        if (this.locked) {
            throw new IllegalStateException("Descriptor is locked.");
        }
    }

    /**
     * Aggregate with data from provided descriptor
     * @param d The other descriptor
     */
    protected void aggregate(final Descriptor d) {
        reqs.addAll(d.getRequirements());
        d.getCapabilities().forEach(caps::add);
        dynImports.addAll(d.getDynamicImportedPackages());
        imports.addAll(d.getImportedPackages());
        d.getExportedPackages().forEach(exports::add);
    }

    /**
     * Return a set of exported packages
     * @return The exported packages. Might be empty.
     */
    public final Set<PackageInfo> getExportedPackages() {
        return unmodifiableIfLocked(exports.asSet());
    }

    public final Set<PackageInfo> getExportedPackages(String packageName) {
        return unmodifiableIfLocked(exports.getNamespacedSet(packageName));
    }

    /**
     * Return a set of imported packages
     * @return The imported packages. Might be empty.
     */
    public final Set<PackageInfo> getImportedPackages() {
        return unmodifiableIfLocked(imports);
    }

    /**
     * Return a set of dynamic imported packages
     * @return The dynamic imported packages. Might be empty.
     */
    public final Set<PackageInfo> getDynamicImportedPackages() {
        return unmodifiableIfLocked(dynImports);
    }

    /**
     * Returns the name of the entity associated with this descriptor.
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Return the list of requirements.
     * @return The list of requirements. The list might be empty.
     */
    public final Set<MatchingRequirement> getRequirements() {
        return unmodifiableIfLocked(reqs);
    }

    /**
     * Return the list of capabilities.
     * @return The list of capabilities. The list might be empty.
     */
    public final Set<Capability> getCapabilities() {
        return unmodifiableIfLocked(caps.asSet());
    }

    public final Set<Capability> getCapabilities(String namespace) {
        return unmodifiableIfLocked(caps.getNamespacedSet(namespace));
    }

    @Contract("null -> null")
    private <T> Set<T> unmodifiableIfLocked(Set<T> set) {
        return locked && set != null ? Collections.unmodifiableSet(set) : set;
    }

    @Override
    public String toString() {
        return name;
    }
}
