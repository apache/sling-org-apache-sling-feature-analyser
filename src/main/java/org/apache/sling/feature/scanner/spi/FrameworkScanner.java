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
import java.util.Map;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.builder.ArtifactProvider;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * The framework scanner scans the framework
 */
@ConsumerType
public interface FrameworkScanner {

    /**
     * Try to scan the artifact
     * @param framework The framework artifact id
     * @param frameworkProps framework properties to launch the framework
     * @param provider Artifact provider
     * @return A descriptor or {@code null}
     * @throws IOException If an error occurs while scanning the platform or the artifact is invalid
     */
    BundleDescriptor scan(ArtifactId framework, Map<String, String> frameworkProps, ArtifactProvider provider)
            throws IOException;
}
