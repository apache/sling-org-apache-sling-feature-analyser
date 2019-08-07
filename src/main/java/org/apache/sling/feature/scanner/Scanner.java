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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Bundles;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.builder.ArtifactProvider;
import org.apache.sling.feature.scanner.impl.BundleDescriptorImpl;
import org.apache.sling.feature.scanner.impl.FeatureDescriptorImpl;
import org.apache.sling.feature.scanner.spi.ExtensionScanner;
import org.apache.sling.feature.scanner.spi.FrameworkScanner;

/**
 * The scanner is a service that scans items and provides descriptions for
 * these. The following items can be scanned individually
 * <ul>
 * <li>A bundle artifact
 * <li>A feature (requires {@link ExtensionScanner}s)
 * <li>A framework (requires {@link FrameworkScanner}s)
 * </ul>
 *
 * The scanner uses an internal cache for the scanned results, subsequent scan
 * calls with the same input will be directly served from the cache. The cache
 * is an in memory cache and its lifetime is bound to the lifetime of the used
 * scanner instance.
 *
 */
public class Scanner {

    private final ArtifactProvider artifactProvider;

    private final List<ExtensionScanner> extensionScanners;

    private final List<FrameworkScanner> frameworkScanners;

    /** The in memory cache for the scanned descriptors. */
    private final Map<String, Object> cache = new ConcurrentHashMap<>();

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
        final String key = bundle.getId().toMvnId().concat(":").concat(String.valueOf(startLevel));
        BundleDescriptor desc = (BundleDescriptor) this.cache.get(key);
        if (desc == null) {
            final URL file = artifactProvider.provide(bundle.getId());
            if (file == null) {
                throw new IOException("Unable to find file for " + bundle.getId());
            }

            desc = new BundleDescriptorImpl(bundle, file, startLevel);
            this.cache.put(key, desc);
        }
        return desc;
    }

    /**
     * Get all bundle descriptors
     *
     * @param bundles The bundles
     * @param desc    The container descriptor
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

    /**
     * Scan all extensions of a feature
     *
     * @param f    The feature
     * @param desc The container descriptor
     * @throws IOException If something goes wrong or no suitable scanner is found.
     */
    private void scanExtensions(final Feature f, final ContainerDescriptor desc)
    throws IOException {
        for (final Extension ext : f.getExtensions()) {
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

    /**
     * Compact the container description
     * 
     * @param desc The contaier description
     */
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
        final String key = feature.getId().toMvnId();

        FeatureDescriptorImpl desc = (FeatureDescriptorImpl) this.cache.get(key);
        if (desc == null) {
            desc = new FeatureDescriptorImpl(feature);

            getBundleInfos(feature.getBundles(), desc);
            scanExtensions(feature, desc);

            compact(desc);

            desc.lock();

            this.cache.put(key, desc);
        }
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
        final StringBuilder sb = new StringBuilder();
        sb.append(framework.toMvnId());
        if (props != null) {
            final Map<String, String> sortedMap = new TreeMap<String, String>(props);
            for (final Map.Entry<String, String> entry : sortedMap.entrySet()) {
                sb.append(":").append(entry.getKey()).append("=").append(entry.getValue());
            }
        }
        final String key = sb.toString();
        BundleDescriptor desc = (BundleDescriptor) this.cache.get(key);
        if (desc == null) {
            for (final FrameworkScanner scanner : this.frameworkScanners) {
                desc = scanner.scan(framework, props, artifactProvider);
                if (desc != null) {
                    break;
                }
            }
            if (desc == null) {
                throw new IOException("No scanner found for framework " + framework.toMvnId());
            }
            this.cache.put(key, desc);
        }

        return desc;
    }
}
