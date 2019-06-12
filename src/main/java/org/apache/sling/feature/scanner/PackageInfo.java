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

import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

/**
 * A package info object contains information about a package, its name, its
 * version and the uses constraints.
 *
 * A package info object is immutable.
 */
public class PackageInfo implements Comparable<PackageInfo> {

    private final boolean optional;
    private final String name;
    private final String version;
    private final Set<String> uses;

    public PackageInfo(final String name, final String version, final boolean optional) {
        this(name, version, optional, null);
    }

    public PackageInfo(final String name, final String version, final boolean optional, Set<String> uses) {
        this.name = name;
        this.version = version;
        this.optional = optional;
        this.uses = uses == null ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(uses));
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public boolean isOptional() {
        return optional;
    }

    public Version getPackageVersion() {
        if (this.version == null)
            return null;

        return new Version(this.version);
    }

    public VersionRange getPackageVersionRange() {
        if (this.version == null)
            return null;

        return new VersionRange(this.version);
    }

    public Set<String> getUses() {
        return uses;
    }

    @Override
    public String toString() {
        return "Package " + name
                + ";version=" + version
                + (this.optional ? " (optional)" : "");
    }

    @Override
    public int compareTo(final PackageInfo o) {
        int result = this.name.compareTo(o.name);
        if ( result == 0 ) {
            result = this.version.compareTo(o.version);
        }
        return result;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + (optional ? 1231 : 1237);
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PackageInfo other = (PackageInfo) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (optional != other.optional)
            return false;
        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;
        return true;
    }

}
