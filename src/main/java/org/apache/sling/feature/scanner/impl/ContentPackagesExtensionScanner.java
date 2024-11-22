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
package org.apache.sling.feature.scanner.impl;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.Set;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.builder.ArtifactProvider;
import org.apache.sling.feature.scanner.ContainerDescriptor;
import org.apache.sling.feature.scanner.spi.ExtensionScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scanner for the content package extension
 */
public class ContentPackagesExtensionScanner implements ExtensionScanner {

    private static final Logger logger = LoggerFactory.getLogger(ContentPackageScanner.class);

    @Override
    public String getId() {
        return Extension.EXTENSION_NAME_CONTENT_PACKAGES;
    }

    @Override
    public String getName() {
        return "Content Packages Scanner";
    }

    @Override
    public ContainerDescriptor scan(final Feature feature, final Extension extension, final ArtifactProvider provider)
            throws IOException {
        if (!Extension.EXTENSION_NAME_CONTENT_PACKAGES.equals(extension.getName())) {
            return null;
        }
        if (extension.getType() != ExtensionType.ARTIFACTS) {
            return null;
        }

        final ContentPackageScanner scanner = new ContentPackageScanner();
        final ContainerDescriptor cd = new ContainerDescriptor(feature.getId().toMvnId() + "(" + getId() + ")") {};

        for (final Artifact a : extension.getArtifacts()) {
            URL url = null;
            try {
                url = provider.provide(a.getId());
            } catch (Exception ex) {
                logger.debug("Unable to get artifact file for: " + a.getId(), ex);
            }

            if (url != null) {
                final Set<ContentPackageDescriptorImpl> pcks = scanner.scan(a, url);
                for (final ContentPackageDescriptorImpl desc : pcks) {
                    cd.getArtifactDescriptors().add(desc);
                    cd.getBundleDescriptors().addAll(desc.getBundles());
                }
            } else {
                final int lastDot = a.getId().toMvnPath().lastIndexOf(".");
                ContentPackageDescriptorImpl desc = new ContentPackageDescriptorImpl(
                        a.getId().toMvnPath().substring(a.getId().toMvnPath().lastIndexOf("/") + 1, lastDot),
                        a,
                        url,
                        null,
                        null,
                        null,
                        null,
                        new Properties());
                desc.lock();
                cd.getArtifactDescriptors().add(desc);
            }
        }

        cd.lock();

        return cd;
    }
}
