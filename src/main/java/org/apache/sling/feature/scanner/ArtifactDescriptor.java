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

import java.net.URL;
import java.util.jar.Manifest;

import org.apache.sling.feature.Artifact;

/**
 * Information about an artifact.
 *
 * At a minimumm the descriptor returns the artifact.
 *
 * Note that this implementation is not synchronized. If multiple threads access
 * a descriptor concurrently, and at least one of the threads modifies the
 * descriptor structurally, it must be synchronized externally. However, once a
 * descriptor is locked, it is safe to access it concurrently.
 */
public abstract class ArtifactDescriptor extends Descriptor {

    /**
     * Constructor for a new descriptor
     * @param name The name
     * @throws IllegalArgumentException if name is {@code null}
     */
    protected ArtifactDescriptor(final String name) {
        super(name);
    }

    /**
     * If the artifact has a manifest, return it
     * @return The manifest or {@code null}
     * @since 2.2.0
     */
    public abstract Manifest getManifest();

    /**
     * Get the artifact file
     * @return The artifact URL or {@code null} if not present.
     */
    public abstract URL getArtifactFile();

    /**
     * Get the artifact
     * @return The artifact
     */
    public abstract Artifact getArtifact();
}
