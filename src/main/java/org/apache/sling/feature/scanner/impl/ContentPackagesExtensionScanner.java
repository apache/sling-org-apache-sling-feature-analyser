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

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.FeatureConstants;
import org.apache.sling.feature.builder.ArtifactProvider;
import org.apache.sling.feature.scanner.ContainerDescriptor;
import org.apache.sling.feature.scanner.spi.ExtensionScanner;

public class ContentPackagesExtensionScanner implements ExtensionScanner {

    @Override
    public String getId() {
        return FeatureConstants.EXTENSION_NAME_CONTENT_PACKAGES;
    }

    @Override
    public String getName() {
        return "Content Packages Scanner";
    }

    @Override
    public ContainerDescriptor scan(final Feature feature,
            final Extension extension,
            final ArtifactProvider provider)
    throws IOException {
        if (!FeatureConstants.EXTENSION_NAME_CONTENT_PACKAGES.equals(extension.getName()) ) {
            return null;
        }
        if ( extension.getType() != ExtensionType.ARTIFACTS ) {
            return null;
        }

        final ContentPackageScanner scanner = new ContentPackageScanner();
        final ContainerDescriptor cd = new ContainerDescriptor() {};

        for(final Artifact a : extension.getArtifacts()) {
            final File file = provider.provide(a.getId());
            if ( file == null ) {
                throw new IOException("Unable to find file for " + a.getId());
            }

            final Set<ContentPackageDescriptor> pcks = scanner.scan(a, file);
            for(final ContentPackageDescriptor desc : pcks) {
                cd.getArtifactDescriptors().add(desc);
                cd.getBundleDescriptors().addAll(desc.bundles);
            }
        }

        cd.lock();

        return cd;
    }
}
