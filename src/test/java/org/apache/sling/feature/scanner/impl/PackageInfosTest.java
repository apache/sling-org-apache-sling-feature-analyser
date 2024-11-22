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

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import org.apache.sling.feature.scanner.PackageInfo;
import org.assertj.core.api.AbstractAssert;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PackageInfosTest {

    private PackageInfos packageInfos;
    private List<PackageInfo> exportPackageList;

    @BeforeEach
    void setUp() {
        exportPackageList = Collections.unmodifiableList(asList(
                exportPackage("a.b.c", "1.0.0"), exportPackage("a.b.c", "1.1.0"), exportPackage("a.b.d", "1.1.0")));
        packageInfos = new PackageInfos();
        exportPackageList.forEach(packageInfos::add);
    }

    @Test
    void addAndremove() {
        PackageInfos infos = new PackageInfos();

        assertThat(infos.getPackageInfos()).hasSize(0);

        assertThat(infos.add(exportPackage("a.b.c", "1.0.0"))).isTrue();
        assertThat(infos.add(exportPackage("a.b.c", "1.0.0")))
                .describedAs("adding duplicate returns false")
                .isFalse();
        assertThat(infos.add(exportPackage("a.b.c", "1.1.0"))).isTrue();
        assertThat(infos.add(exportPackage("a.b.c", "1.2.0"))).isTrue();
        assertThat(infos.add(exportPackage("a.b.c", "1.3.0"))).isTrue();
        assertThat(infos.add(exportPackage("a.b.c", "1.4.0"))).isTrue();

        assertThat(infos.getPackageInfos()).hasSize(5);

        assertThat(infos.remove(exportPackage("a.b.c", "1.3.0"))).isTrue();
        assertThat(infos.remove(exportPackage("a.b.c", "1.4.0"))).isTrue();
        assertThat(infos.remove(exportPackage("a.b.c", "1.4.0")))
                .describedAs("removing non-existing PackageInfo returns false")
                .isFalse();

        assertThat(infos.getPackageInfos()).hasSize(3);

        assertThat(infos.remove(exportPackage("a.b.c", "1.0.0"))).isTrue();
        assertThat(infos.remove(exportPackage("a.b.c", "1.1.0"))).isTrue();
        assertThat(infos.remove(exportPackage("a.b.c", "1.1.0")))
                .describedAs("removing non-existing PackageInfo returns false")
                .isFalse();
        assertThat(infos.remove(exportPackage("a.b.c", "1.2.0"))).isTrue();

        assertThat(infos.getPackageInfos()).hasSize(0);
    }

    @Test
    void hasPackageName() {
        assertPackageInfos(packageInfos).has("a.b.c");
        assertPackageInfos(packageInfos).has("a.b.d");

        assertPackageInfos(packageInfos).doesNotHave("a.b.z");
    }

    @Test
    void hasPackageNameAndVersion() {
        assertPackageInfos(packageInfos).has("a.b.c", "1.0.0");
        assertPackageInfos(packageInfos).has("a.b.c", "1.1.0");
        assertPackageInfos(packageInfos).has("a.b.d", "1.1.0");

        assertPackageInfos(packageInfos).doesNotHave("a.b.c", "1.2.0");
        assertPackageInfos(packageInfos).doesNotHave("a.b.d", "1.2.0");
        assertPackageInfos(packageInfos).doesNotHave("a.b.z", "1.2.0");
    }

    @Test
    void setView() {
        Set<PackageInfo> setView = packageInfos.getPackageInfos();
        assertThat(setView).hasSize(3);
        assertThat(setView).containsExactlyInAnyOrderElementsOf(exportPackageList);
        assertThat(setView.contains(exportPackage("a.b.c", "1.0.0"))).isTrue();
        assertThat(setView.contains(exportPackage("a.b.c", "2.0.0"))).isFalse();
        assertThat(setView.contains(new Object())).isFalse();
    }

    @Test
    void setViewMutability() {
        PackageInfos infos = new PackageInfos();
        Set<PackageInfo> setView = infos.getPackageInfos();

        for (PackageInfo packageInfo : exportPackageList) {
            assertThat(setView.add(packageInfo)).isTrue();
            assertThat(setView.add(packageInfo)).isFalse();
        }
        assertThat(setView).containsExactlyInAnyOrderElementsOf(exportPackageList);
        assertThat(setView).hasSize(exportPackageList.size());

        assertThat(setView.remove(new Object())).isFalse();

        for (PackageInfo packageInfo : exportPackageList) {
            assertThat(setView.remove(packageInfo)).isTrue();
            assertThat(setView.remove(packageInfo)).isFalse();
        }

        assertThat(setView).hasSize(0);
    }

    @Test
    void setViewIterator() {
        Set<PackageInfo> setView = packageInfos.getPackageInfos();
        Set<PackageInfo> copy = new HashSet<>(exportPackageList);
        Iterator<PackageInfo> iterator = setView.iterator();
        while (iterator.hasNext()) {
            PackageInfo packageInfo = iterator.next();
            assertThat(copy.remove(packageInfo))
                    .describedAs("PackageInfo must be contained in copied Set")
                    .isTrue();
        }
        assertThat(copy).describedAs("copied Set of PackageInfos must be empty").isEmpty();
    }

    @Test
    void setViewIteratorRemove() {
        Set<PackageInfo> setView = packageInfos.getPackageInfos();
        Iterator<PackageInfo> iterator = setView.iterator();
        int initialSize = packageInfos.getPackageInfos().size();
        for (int i = 1; i <= initialSize; i++) {
            if (iterator.hasNext()) {
                PackageInfo packageInfo = iterator.next();
                iterator.remove();
                assertPackageInfos(packageInfos).doesNotHave(packageInfo.getName(), packageInfo.getVersion());
            }

            assertThat(setView).hasSize(initialSize - i);
        }
    }

    @Test
    void setViewIteratorEdgeCases() {
        PackageInfos infos = new PackageInfos();
        infos.add(exportPackage("a.b.c", "1.0.0"));
        infos.add(exportPackage("a.b.c", "1.1.0"));
        Iterator<PackageInfo> iterator = infos.getPackageInfos().iterator();

        // remove before next
        assertThatThrownBy(iterator::remove).isInstanceOf(IllegalStateException.class);

        assertThat(iterator.next()).isEqualTo(exportPackage("a.b.c", "1.0.0"));
        iterator.remove();

        // 2nd remove before next
        assertThatThrownBy(iterator::remove).isInstanceOf(IllegalStateException.class);

        assertThat(iterator.next()).isEqualTo(exportPackage("a.b.c", "1.1.0"));
        iterator.remove();

        // next on empty iterator
        assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
    }

    private static @NotNull PackageInfo exportPackage(String name, String version) {
        return new PackageInfo(name, version, false);
    }

    private static PackageInfosAssert assertPackageInfos(PackageInfos packageInfos) {
        return new PackageInfosAssert(packageInfos);
    }

    private static class PackageInfosAssert extends AbstractAssert<PackageInfosAssert, PackageInfos> {
        protected PackageInfosAssert(PackageInfos packageInfos) {
            super(packageInfos, PackageInfosAssert.class);
        }

        public void has(String packageName) {
            assertThat(this.actual.has(packageName))
                    .describedAs("has %s", packageName)
                    .isTrue();
        }

        public void doesNotHave(String packageName) {
            assertThat(this.actual.has(packageName))
                    .describedAs("doesNotHave %s", packageName)
                    .isFalse();
        }

        public void has(String packageName, String version) {
            assertThat(this.actual.has(packageName, p -> Objects.equals(p.getVersion(), version)))
                    .describedAs("has %s:%s", packageName, version)
                    .isTrue();
        }

        public void doesNotHave(String packageName, String version) {
            assertThat(this.actual.has(packageName, p -> Objects.equals(p.getVersion(), version)))
                    .describedAs("doesNotHave %s:%s", packageName, version)
                    .isFalse();
        }
    }
}
