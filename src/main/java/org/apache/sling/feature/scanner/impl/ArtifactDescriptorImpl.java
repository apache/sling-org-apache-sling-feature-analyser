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
import java.util.jar.Manifest;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.scanner.ArtifactDescriptor;

/**
 * Information about an artifact.
 */
public class ArtifactDescriptorImpl
    extends ArtifactDescriptor {

    /** Manifest */
    private final Manifest manifest;

    /** The physical file for analyzing. */
    private final URL artifactFile;

    /** The corresponding artifact from the feature. */
    private final Artifact artifact;

    /**
     * Constructor for an artifact descriptor
     * @param name Optional name
     * @param artifact The artifact, must be provided
     * @param url Optional url
     * @param Manifest manifest (optional)
     * @throws NullPointerException If artifact is {@code null}
     */
    public ArtifactDescriptorImpl(
            final String name,
            final Artifact artifact,
            final URL url,
            final Manifest manifest) {
        super(name != null ? name : artifact.getId().toMvnId());
        this.artifact = artifact;
        this.artifact.getId(); // throw NPE if artifact is null
        this.artifactFile = url;
        this.manifest = manifest;
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
}
