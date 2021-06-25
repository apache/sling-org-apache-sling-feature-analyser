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

import java.io.IOException;
import java.net.URL;
import java.util.jar.Manifest;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.scanner.ArtifactDescriptor;

/**
 * Information about a bundle
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
     * @param artifact Optional artifact
     * @param url Optional url
     * @param hasManifest Whether that artifact must have a metafest
     * @param isManifestOptional Whether the manifest is optional
     * @throws IOException If processing fails
     */
    public ArtifactDescriptorImpl(
            final String name,
            final Artifact artifact,
            final URL url,
            final boolean hasManifest,
            final boolean isManifestOptional) throws IOException  {
        super(name != null ? name : artifact.getId().toMvnId());
        this.artifact = artifact;
        this.artifactFile = url;
        Manifest mf = null;
        if ( hasManifest ) {
            try {
                final Manifest origMf = BundleDescriptorImpl.getManifest(url);
                if ( origMf != null ) {
                    mf = new Manifest(origMf);
                } else if ( !isManifestOptional ) {
                    throw new IOException(url + " : No manifest found.");
                }
            } catch ( final IOException ioe) {
                if ( !isManifestOptional ) {
                    throw ioe;
                }
            }
        }
        this.manifest = mf;
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
