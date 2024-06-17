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
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.Manifest;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.osgi.framework.Constants;

/**
 * Bundle descriptor that describes the system bundle
 * 
 * <p>It is based on an existing artifact id and the associated platform file.</p>
 */
public final class SystemBundleDescriptor extends BundleDescriptor {
    
    public static String createCacheKey(final ArtifactId framework, final Map<String, String> props) {
        final StringBuilder sb = new StringBuilder();
        sb.append(framework.toMvnId());
        if (props != null) {
            final Map<String, String> sortedMap = new TreeMap<>(props);
            for (final Map.Entry<String, String> entry : sortedMap.entrySet()) {
                sb.append(":").append(entry.getKey()).append("=").append(entry.getValue());
            }
        }
        return sb.toString();
    }
    
    private final URL platformFile;
    private final ArtifactId framework;

    public SystemBundleDescriptor(ArtifactId framework, URL platformFile) {
        super(framework.toMvnId());
        this.platformFile = platformFile;
        this.framework = framework;
    }

    @Override
    public String getBundleSymbolicName() {
        return Constants.SYSTEM_BUNDLE_SYMBOLICNAME;
    }

    @Override
    public String getBundleVersion() {
        return framework.getOSGiVersion().toString();
    }

    @Override
    public URL getArtifactFile() {
        return platformFile;
    }

    @Override
    public Artifact getArtifact() {
        return new Artifact(framework);
    }

    @Override
    public Manifest getManifest() {
        return new Manifest();
    }
}