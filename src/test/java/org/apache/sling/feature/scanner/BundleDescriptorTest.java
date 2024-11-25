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
package org.apache.sling.feature.scanner;

import java.net.URL;
import java.util.jar.Manifest;

import org.apache.sling.feature.Artifact;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class BundleDescriptorTest {

    @Test
    void name() {
        BundleDescriptor bundleDescriptor = new TestBundleDescriptor();
        assertEquals("test-bundle-descriptor", bundleDescriptor.getBundleSymbolicName());
    }

    @Test
    void locking() {
        BundleDescriptor bundleDescriptor = new TestBundleDescriptor();
        assertFalse(bundleDescriptor.isLocked());
        bundleDescriptor.lock();
        assertTrue(bundleDescriptor.isLocked());
        assertThrows(IllegalStateException.class, bundleDescriptor::checkLocked);
    }

    @Test
    void isExportingPackage() {
        PackageInfo exportPackage = new PackageInfo("a.b.c", "1.2.0", false);
        PackageInfo matchingImportPackage = new PackageInfo("a.b.c", "[1.1.0,2.0.0)", false);
        PackageInfo matchingUnspecificImportPackage = new PackageInfo("a.b.c", null, false);
        PackageInfo nonMatchingImportPackage = new PackageInfo("a.b.c", "[2,3)", false);
        assertTrue(matchingImportPackage.getPackageVersionRange().includes(exportPackage.getPackageVersion()));

        BundleDescriptor bundleDescriptor = new TestBundleDescriptor();
        bundleDescriptor.getExportedPackages().add(exportPackage);
        assertTrue(bundleDescriptor.isExportingPackage(matchingImportPackage.getName()));
        assertTrue(bundleDescriptor.isExportingPackage(matchingImportPackage));
        assertTrue(bundleDescriptor.isExportingPackage(matchingUnspecificImportPackage));
        assertFalse(bundleDescriptor.isExportingPackage(nonMatchingImportPackage));
    }

    @Test
    void equals() {
        BundleDescriptor bd1 = new TestBundleDescriptor();
        BundleDescriptor bd2 = new TestBundleDescriptor();
        assertEquals(bd1, bd2);
        assertNotEquals(bd1, new Object());
        assertNotEquals(bd1, new TestBundleDescriptor() {
            @Override
            public String getBundleSymbolicName() {
                return "other";
            }
        });
    }

    // generate test for aggregate
    @Test
    void aggregate() {
        BundleDescriptor bd1 = new TestBundleDescriptor();
        bd1.getExportedPackages().add(new PackageInfo("a.a", "1.0.0", false));
        bd1.getExportedPackages().add(new PackageInfo("a.b", "1.0.0", false));
        bd1.getExportedPackages().add(new PackageInfo("a.c", "1.0.0", false));
        BundleDescriptor bd2 = new TestBundleDescriptor();
        bd1.getExportedPackages().add(new PackageInfo("b.a", "1.0.0", false));
        bd1.getExportedPackages().add(new PackageInfo("b.b", "1.0.0", false));
        bd1.getExportedPackages().add(new PackageInfo("b.c", "1.0.0", false));

        TestBundleDescriptor copyBd1 = new TestBundleDescriptor();
        copyBd1.aggregate(bd1);
        assertThat(bd1.getExportedPackages()).isEqualTo(copyBd1.getExportedPackages());

        bd1.aggregate(bd2);
        assertEquals(6, bd1.getExportedPackages().size());
        assertThat(bd1.getExportedPackages())
                .containsAll(copyBd1.getExportedPackages())
                .containsAll(bd2.getExportedPackages());
    }

    static class TestBundleDescriptor extends BundleDescriptor {

        /**
         * Constructor for a new descriptor
         *
         * @throws IllegalArgumentException if name is {@code null}
         */
        protected TestBundleDescriptor() {
            super("test-bundle-descriptor");
        }

        @Override
        public String getBundleSymbolicName() {
            return getName();
        }

        @Override
        public String getBundleVersion() {
            return "1.0.0";
        }

        @Override
        public Manifest getManifest() {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public URL getArtifactFile() {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public Artifact getArtifact() {
            throw new UnsupportedOperationException("not implemented");
        }
    }
}
