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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Encapsulates the handling of sets of namespaced objects, e.g. PackageInfo objects or Capabilities,
 * in a way that allows for optimized queries, by allowing preselection of namespaced subsets,
 * when only certain namespaces are of interest.
 * <br>
 * E.g. {@link org.apache.sling.feature.scanner.BundleDescriptor#isExportingPackage(org.apache.sling.feature.scanner.PackageInfo)}.
 */
public class NamespacedSets<T> {

    private final Map<String, Set<T>> sets = new HashMap<>();

    private final AtomicInteger size = new AtomicInteger(0);

    private final Set<T> setView;

    private final Function<T, String> getNamespace;

    public NamespacedSets(Class<T> type, Function<T, String> getNamespace) {
        this.setView = new SetView(type);
        this.getNamespace = getNamespace;
    }

    public boolean add(T t) {
        int before = size.get();
        sets.compute(getNamespace.apply(t), (namespace, set) -> {
            if (set == null) {
                size.incrementAndGet();
                return Collections.singleton(t);
            }
            if (set.size() == 1) {
                set = new LinkedHashSet<>(set);
            }
            if (set.add(t)) {
                size.incrementAndGet();
            }
            return set;
        });
        return before < size.get();
    }

    public boolean remove(T t) {
        int before = size.get();
        sets.computeIfPresent(getNamespace.apply(t), (namespace, set) -> {
            if (set.size() <= 1) {
                if (set.contains(t)) {
                    size.decrementAndGet();
                    return null;
                }
            } else if (set.remove(t)) {
                size.decrementAndGet();
                if (set.size() == 1) {
                    return Collections.singleton(set.iterator().next());
                }
            }
            return set;
        });
        return before > size.get();
    }

    public @NotNull Set<T> asSet() {
        return setView;
    }

    /**
     * Gets the sub-set for the specified namespace.
     *
     * @param namespace The namespace to get the sub-set for.
     * @return The sub-set for the specified namespace, or {@code null} if the namespace does not exist.
     */
    public @Nullable Set<T> getNamespacedSet(String namespace) {
        return sets.get(namespace);
    }

    private class SetView extends AbstractSet<T> {

        private final Class<T> type;

        public SetView(Class<T> type) {
            this.type = type;
        }

        @Override
        public boolean contains(Object o) {
            if (type.isInstance(o)) {
                T item = type.cast(o);
                return Optional.ofNullable(NamespacedSets.this.getNamespacedSet(getNamespace.apply(item)))
                        .map(Collection::stream)
                        .map(s -> s.anyMatch(i -> Objects.equals(i, item)))
                        .orElse(false);
            }
            return false;
        }

        @Override
        public boolean add(T item) {
            return NamespacedSets.this.add(item);
        }

        @Override
        public boolean remove(Object o) {
            if (type.isInstance(o)) {
                T item = type.cast(o);
                return NamespacedSets.this.remove(item);
            }
            return false;
        }

        @Override
        public @NotNull Iterator<T> iterator() {
            return new Iterator<T>() {

                private final List<Set<T>> listOfSets = NamespacedSets.this.sets.values().stream()
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));

                private int currentListIndex = -1;

                private T latestItem;

                private Iterator<T> currentIterator;

                @Override
                public boolean hasNext() {
                    latestItem = null;
                    while ((currentIterator == null || !currentIterator.hasNext())
                            && ++currentListIndex < listOfSets.size()) {
                        currentIterator = listOfSets.get(currentListIndex).iterator();
                    }
                    return currentIterator != null && currentIterator.hasNext();
                }

                @Override
                public T next() {
                    if (hasNext()) {
                        latestItem = currentIterator.next();
                        return latestItem;
                    }
                    throw new NoSuchElementException();
                }

                @Override
                public void remove() {
                    if (latestItem != null) {
                        Set<T> currentSet = listOfSets.get(currentListIndex);
                        if (currentSet.contains(latestItem) && currentSet.size() > 1) {
                            // replace the underlying mutable Set with a copy before removal ...
                            NamespacedSets.this.sets.put(
                                    getNamespace.apply(latestItem), new LinkedHashSet<>(currentSet));
                            // ... but also remove from our copy
                            currentIterator.remove();
                        }
                        NamespacedSets.this.remove(latestItem);
                        latestItem = null;
                    } else {
                        throw new IllegalStateException();
                    }
                }
            };
        }

        @Override
        public int size() {
            return NamespacedSets.this.size.get();
        }
    }
}
