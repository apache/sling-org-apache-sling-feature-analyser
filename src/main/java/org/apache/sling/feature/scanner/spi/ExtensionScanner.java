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
package org.apache.sling.feature.scanner.spi;

import java.io.IOException;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.builder.ArtifactProvider;
import org.apache.sling.feature.scanner.ContainerDescriptor;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * The extension scanner scans an extension.
 */
@ConsumerType
public interface ExtensionScanner {

    /** A unique (short) ID.
     * @return the ID.
     */
    String getId();

    /** A human readable name to identify the scanner.
     * @return the name.
     */
    String getName();

    /**
     * Try to scan the extension and return a descriptor
     *
     * @param feature The feature the extension belongs to
     * @param extension The extension
     * @param provider Artifact provider
     * @return The descriptor or {@code null} if the scanner does not know the extension
     * @throws IOException If an error occurs while scanning the extension or the extension is invalid
     */
    ContainerDescriptor scan(Feature feature, Extension extension, ArtifactProvider provider) throws IOException;
}
