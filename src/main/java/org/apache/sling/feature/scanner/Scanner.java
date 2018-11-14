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

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Bundles;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Extensions;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.builder.ArtifactProvider;
import org.apache.sling.feature.scanner.impl.BundleDescriptorImpl;
import org.apache.sling.feature.scanner.impl.FeatureDescriptorImpl;
import org.apache.sling.feature.scanner.spi.ExtensionScanner;
import org.apache.sling.feature.scanner.spi.FrameworkScanner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * The scanner is a service that scans items and provides descriptions for these.
 * The following items can be scanned individually
 * <ul>
 *   <li>A bundle artifact
 *   <li>An extension (requires {@link ExtensionScanner}s)
 *   <li>A feature (requires {@link ExtensionScanner}s)
 *   <li>A framework (requires {@link FrameworkScanner}s)
 * </ul>
 */
public class Scanner {

    private final ArtifactProvider artifactProvider;

    private final List<ExtensionScanner> extensionScanners;

    private final List<FrameworkScanner> frameworkScanners;

    /**
     * Create a new scanner
     *
     * @param artifactProvider The artifact provider
     * @param extensionScanners A list of extension scanners
     * @param frameworkScanners A list of framework scanners
     * @throws IOException If something goes wrong
     */
    public Scanner(final ArtifactProvider artifactProvider,
            final List<ExtensionScanner> extensionScanners,
            final List<FrameworkScanner> frameworkScanners)
    throws IOException {
        this.artifactProvider = artifactProvider;
        this.extensionScanners = extensionScanners == null ? getServices(ExtensionScanner.class) : extensionScanners;
        this.frameworkScanners = frameworkScanners == null ? getServices(FrameworkScanner.class) : frameworkScanners;
    }

    /**
     * Create a new scanner and use the service loader to find the scanners
     *
     * @param artifactProvider The artifact provider
     * @throws IOException If something goes wrong
     */
    public Scanner(final ArtifactProvider artifactProvider)
    throws IOException {
        this(artifactProvider, null, null);
    }

    /**
     * Get services from the service loader
     *
     * @param clazz The service class
     * @return The list of services might be empty.
     */
    private static <T> List<T> getServices(final Class<T> clazz) {
        final ServiceLoader<T> loader = ServiceLoader.load(clazz);
        final List<T> list = new ArrayList<>();
        for(final T task : loader) {
            list.add(task);
        }
        return list;
    }

    /**
     * Scan a bundle
     *
     * @param bundle The bundle artifact
     * @param startLevel The start level of the bundle
     * @return The bundle descriptor
     * @throws IOException If something goes wrong or the provided artifact is not a bundle.
     */
    public BundleDescriptor scan(final Artifact bundle, final int startLevel) throws IOException {
        final File file = artifactProvider.provide(bundle.getId());
        if ( file == null ) {
            throw new IOException("Unable to find file for " + bundle.getId());
        }

        return new BundleDescriptorImpl(bundle, file, startLevel);
    }

    /**
     * Get all bundle descriptors for a feature / application
     * @param bundles The bundles
     * @param desc The descriptor
     * @throws IOException If something goes wrong or no suitable scanner is found.
     */
    private void getBundleInfos(final Bundles bundles, final ContainerDescriptor desc)
    throws IOException {
        for(final Map.Entry<Integer, List<Artifact>> entry : bundles.getBundlesByStartOrder().entrySet()) {
            for(final Artifact bundle : entry.getValue() ) {
                final BundleDescriptor bundleDesc = scan(bundle, entry.getKey());
                desc.getBundleDescriptors().add(bundleDesc);
            }
        }
    }

    private void scan(final Feature f, final Extensions extensions, final ContainerDescriptor desc)
    throws IOException {
        for(final Extension ext : extensions) {
            ContainerDescriptor extDesc = null;
            for(final ExtensionScanner scanner : this.extensionScanners) {
                extDesc = scanner.scan(f, ext, this.artifactProvider);
                if ( extDesc != null ) {
                    break;
                }
            }
            if ( extDesc == null ) {
                throw new IOException("No extension scanner found for extension named " + ext.getName() + " of type " + ext.getType().name());
            }
            desc.getRequirements().addAll(extDesc.getRequirements());
            desc.getCapabilities().addAll(extDesc.getCapabilities());
            desc.getExportedPackages().addAll(extDesc.getExportedPackages());
            desc.getImportedPackages().addAll(extDesc.getImportedPackages());
            desc.getDynamicImportedPackages().addAll(extDesc.getDynamicImportedPackages());

            desc.getArtifactDescriptors().addAll(extDesc.getArtifactDescriptors());
            desc.getBundleDescriptors().addAll(extDesc.getBundleDescriptors());
        }
    }

    private void compact(final ContainerDescriptor desc) {
        // TBD remove all import packages / dynamic import packages which are resolved by this bundle set
        // same with requirements

    }

    /**
     * Scan a feature
     *
     * @param feature The feature
     * @return The feature descriptor
     * @throws IOException If something goes wrong or a scanner is missing
     */
    public FeatureDescriptor scan(final Feature feature) throws IOException {
        final FeatureDescriptorImpl desc = new FeatureDescriptorImpl(feature);

        getBundleInfos(feature.getBundles(), desc);
        scan(feature, feature.getExtensions(), desc);

        compact(desc);

        desc.lock();

        return desc;
    }

    /**
     * Scan a framework
     *
     * @param framework The framework
     * @param props framework properties to launch the framework
     * @return The framework descriptor
     * @throws IOException If something goes wrong or a scanner is missing
     */
    public BundleDescriptor scan(final ArtifactId framework, final Map<String,String> props) throws IOException {
        BundleDescriptor fwk = null;
        for(final FrameworkScanner scanner : this.frameworkScanners) {
            fwk = scanner.scan(framework, props, artifactProvider);
            if ( fwk != null ) {
                break;
            }
        }
        if ( fwk == null ) {
            throw new IOException("No scanner found for framework " + framework.toMvnId());
        }

        return fwk;
    }
}
