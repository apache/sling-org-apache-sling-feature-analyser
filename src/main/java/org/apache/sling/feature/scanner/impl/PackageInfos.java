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

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.sling.feature.scanner.PackageInfo;
import org.jetbrains.annotations.NotNull;

/**
 * Encapsulates the handling of PackageInfo objects in a way that allows for optimized implementations
 * of e.g. {@link org.apache.sling.feature.scanner.BundleDescriptor#isExportingPackage(PackageInfo)}.
 * <br>
 * The {@code Set} view is deliberately not directly implemented, as this allows for greater flexibility
 * to evolve this internal API.
 */
public class PackageInfos {

    private final Map<String, Set<PackageInfo>> packages = new HashMap<>();

    private final AtomicInteger size = new AtomicInteger(0);

    private final Set<PackageInfo> packageInfos = new SetView();

    public boolean add(PackageInfo p) {
        int before = size.get();
        packages.compute(p.getName(), (packageName, packageInfos) -> {
            if (packageInfos == null) {
                size.incrementAndGet();
                return Collections.singleton(p);
            }
            if (packageInfos.size() == 1) {
                packageInfos = new TreeSet<>(packageInfos);
            }
            if (packageInfos.add(p)) {
                size.incrementAndGet();
            }
            return packageInfos;
        });
        return before < size.get();
    }

    public boolean remove(PackageInfo p) {
        int before = size.get();
        packages.computeIfPresent(p.getName(), (packageName, packageInfos) -> {
            if (packageInfos.size() <= 1) {
                if (packageInfos.contains(p)) {
                    size.decrementAndGet();
                    return null;
                }
            } else if (packageInfos.remove(p)) {
                size.decrementAndGet();
                if (packageInfos.size() == 1) {
                    return Collections.singleton(packageInfos.iterator().next());
                }
            }
            return packageInfos;
        });
        return before > size.get();
    }

    public Set<PackageInfo> getPackageInfos() {
        return packageInfos;
    }

    public boolean has(String packageName) {
        return packages.containsKey(packageName);
    }

    public boolean has(String packageName, Predicate<PackageInfo> packageInfoPredicate) {
        return Optional.ofNullable(packages.get(packageName))
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .anyMatch(packageInfoPredicate);
    }

    private class SetView extends AbstractSet<PackageInfo> {

        @Override
        public boolean contains(Object o) {
            if (o instanceof PackageInfo) {
                PackageInfo packageInfo = (PackageInfo) o;
                return PackageInfos.this.has(packageInfo.getName(), pi -> Objects.equals(pi, packageInfo));
            }
            return false;
        }

        @Override
        public boolean add(PackageInfo packageInfo) {
            return PackageInfos.this.add(packageInfo);
        }

        @Override
        public boolean remove(Object o) {
            if (o instanceof PackageInfo) {
                PackageInfo packageInfo = (PackageInfo) o;
                return PackageInfos.this.remove(packageInfo);
            }
            return false;
        }

        @Override
        public @NotNull Iterator<PackageInfo> iterator() {
            return new Iterator<PackageInfo>() {

                private final List<Set<PackageInfo>> listOfSets = PackageInfos.this.packages.values().stream()
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
                ;

                private int currentListIndex = -1;

                private PackageInfo latestPackageInfo;

                private Iterator<PackageInfo> currentIterator;

                @Override
                public boolean hasNext() {
                    latestPackageInfo = null;
                    while ((currentIterator == null || !currentIterator.hasNext())
                            && ++currentListIndex < listOfSets.size()) {
                        currentIterator = listOfSets.get(currentListIndex).iterator();
                    }
                    return currentIterator != null && currentIterator.hasNext();
                }

                @Override
                public PackageInfo next() {
                    if (hasNext()) {
                        return (latestPackageInfo = currentIterator.next());
                    }
                    throw new NoSuchElementException();
                }

                @Override
                public void remove() {
                    if (latestPackageInfo != null) {
                        Set<PackageInfo> currentPackageInfoSet = listOfSets.get(currentListIndex);
                        if (currentPackageInfoSet.contains(latestPackageInfo) && currentPackageInfoSet.size() > 1) {
                            // replace the underlying TreeSet with a copy before removal ...
                            PackageInfos.this.packages.put(
                                    latestPackageInfo.getName(), new TreeSet<>(currentPackageInfoSet));
                            // ... but also remove from our copy
                            currentIterator.remove();
                        }
                        PackageInfos.this.remove(latestPackageInfo);
                        latestPackageInfo = null;
                    } else {
                        throw new IllegalStateException();
                    }
                }
            };
        }

        @Override
        public int size() {
            return PackageInfos.this.size.get();
        }
    }
}
