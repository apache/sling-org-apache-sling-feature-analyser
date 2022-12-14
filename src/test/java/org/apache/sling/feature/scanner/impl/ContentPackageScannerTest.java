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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Set;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Configuration;
import org.junit.Test;

public class ContentPackageScannerTest {


    @Test
    public void testScan() throws URISyntaxException, IOException {
        final File file = getTestFile("/test-content.zip");

        final String COORDINATES_TEST_PACKAGE_A_10 = "my_packages:test_a:1.0";
        final ArtifactId TEST_PACKAGE_AID_A_10 = ArtifactId.fromMvnId(COORDINATES_TEST_PACKAGE_A_10);

        ContentPackageScanner scanner = new ContentPackageScanner();
        Set<ContentPackageDescriptorImpl> descriptors = scanner.scan(new Artifact(TEST_PACKAGE_AID_A_10), file.toURI().toURL());
        assertEquals(2, descriptors.size());
        for(ContentPackageDescriptorImpl desc : descriptors) {
            if (desc.getName().equals("test-content")) {
                assetDescriptor(desc, "test-content", TEST_PACKAGE_AID_A_10, file.toURI().toURL());
            } else {
                assertEquals(desc.getName(), "sub-content");
            }
            assertNotNull(desc.getManifest());
        }
    }
    
    @Test
    public void testMultipleMavenPropertyDirectoryPicking() throws URISyntaxException, IOException {
        // this test case is to cover where we have multiple pom.properties files present in our package.
        final File file = getTestFile("/test-content-felix-bundle-multi-maven-properties.zip");

        final String COORDINATES_TEST_PACKAGE_A_10 = "org.apache.felix:org.apache.felix.framework:6.0.1";
        final ArtifactId TEST_PACKAGE_AID_A_10 = ArtifactId.fromMvnId(COORDINATES_TEST_PACKAGE_A_10);

        ContentPackageScanner scanner = new ContentPackageScanner();
        Set<ContentPackageDescriptorImpl> descriptors = scanner.scan(new Artifact(TEST_PACKAGE_AID_A_10), file.toURI().toURL());
        
        for(ContentPackageDescriptorImpl descriptor: descriptors){
            if(descriptor.getName().equals("test-content-felix-bundle-multi-maven-properties")){
                assertEquals("org.apache.felix:org.apache.felix.framework:jar:bundle:6.0.1", descriptor.getBundles().get(0).getName());
            }
       
        }
    }

    private File getTestFile(String path) throws URISyntaxException {
        return new File(getClass().getResource(path).toURI());
    }

    private void assetDescriptor(ContentPackageDescriptorImpl desc, String descName, final ArtifactId descArtifactId, final URL descUrl) {
        assertEquals(descName, desc.getName());
        assertEquals(descArtifactId, desc.getArtifact().getId());
        assertEquals(descUrl, desc.getArtifactFile());

        assertEquals(1, desc.getBundles().size());

        assertEquals(desc.getBundles().get(0).getArtifact().getId().toString(), "org.apache.felix:org.apache.felix.framework:jar:bundle:6.0.1");
        assertEquals("artifact start order",20, desc.getBundles().get(0).getArtifact().getStartOrder());

        assertEquals(1, desc.getConfigurations().size());
        assertConfiguration(desc.getConfigurations().get(0));

        assertEquals(6, desc.getContentPaths().size());
        assertTrue(desc.getContentPaths().contains("/.content.xml"));
        assertTrue(desc.getContentPaths().contains("/libs/.content.xml"));
        assertTrue(desc.getContentPaths().contains("/libs/a/.content.xml"));
        assertTrue(desc.getContentPaths().contains("/libs/install/test-bundle.jar"));
        assertTrue(desc.getContentPaths().contains("/libs/config/com.example.some.Component.xml"));
        assertTrue(desc.getContentPaths().contains("/etc/packages/org/sling/sub-content.zip"));

        assertFalse(desc.getPackageProperties().isEmpty());
    }

    private void assertConfiguration(Configuration c) {
        Dictionary<String, Object> props = c.getProperties();
        String contentPath = (String) props.get(":configurator:feature-content-path");
        assertEquals("/libs/config/com.example.some.Component.xml", contentPath);
    }

    @Test
    public void testDetectContentFileType() {
        final ContentPackageScanner scanner = new ContentPackageScanner();

        // content packages need to be below /libs, /apps or /etc/packages/
        assertEquals(ContentPackageScanner.FileType.PACKAGE, scanner.detectContentFileType("/etc/packages/a/b/my-package.zip"));
        assertEquals(ContentPackageScanner.FileType.PACKAGE, scanner.detectContentFileType("/libs/app/install/component.zip"));
        assertNull(scanner.detectContentFileType("/content/app/install/component.zip"));

        // configs need to be below /libs, /apps in install or config folders
        assertEquals(ContentPackageScanner.FileType.CONFIG, scanner.detectContentFileType("/libs/app/install/component.cfg"));
        assertEquals(ContentPackageScanner.FileType.CONFIG, scanner.detectContentFileType("/libs/app/install/component.config"));
        assertEquals(ContentPackageScanner.FileType.CONFIG, scanner.detectContentFileType("/libs/app/install/component.xml"));
        assertEquals(ContentPackageScanner.FileType.CONFIG, scanner.detectContentFileType("/libs/app/install/component.cfg.json"));
        assertNull(scanner.detectContentFileType("/libs/app/install/component.txt"));
        assertNull(scanner.detectContentFileType("/content/app/install/component.cfg"));
        assertNull(scanner.detectContentFileType("/content/app/install/component.config"));
        assertNull(scanner.detectContentFileType("/content/app/install/component.xml"));
        assertNull(scanner.detectContentFileType("/content/app/install/component.cfg.json"));

        // bundles need to be below /libs, /apps in install orfolders
        assertEquals(ContentPackageScanner.FileType.BUNDLE, scanner.detectContentFileType("/libs/app/install/component.jar"));
        assertNull(scanner.detectContentFileType("/content/app/install/component.jar"));
    }

    @Test
    public void testProcessConfiguration() throws IOException, URISyntaxException {
        final ContentPackageScanner scanner = new ContentPackageScanner();

        final ArtifactId pckId = ArtifactId.parse("g:a:1");

        Configuration cfg = scanner.processConfiguration(null, pckId, "/libs/app/install/my.component.cfg");
        assertEquals("my.component", cfg.getPid());
        assertNull(cfg.getFactoryPid());

        cfg = scanner.processConfiguration(null, pckId, "/libs/app/install/my.component.config");
        assertEquals("my.component", cfg.getPid());
        assertNull(cfg.getFactoryPid());

        cfg = scanner.processConfiguration(null, pckId, "/libs/app/install/my.component.cfg.json");
        assertEquals("my.component", cfg.getPid());
        assertNull(cfg.getFactoryPid());

        cfg = scanner.processConfiguration(null, pckId, "/libs/app/install/my.component-name.cfg");
        assertEquals("my.component~name", cfg.getPid());
        assertEquals("my.component", cfg.getFactoryPid());
        assertEquals("name", cfg.getName());

        cfg = scanner.processConfiguration(null, pckId, "/libs/app/install/my.component-name.config");
        assertEquals("my.component~name", cfg.getPid());
        assertEquals("my.component", cfg.getFactoryPid());
        assertEquals("name", cfg.getName());

        cfg = scanner.processConfiguration(null, pckId, "/libs/app/install/my.component~name.cfg.json");
        assertEquals("my.component~name", cfg.getPid());
        assertEquals("my.component", cfg.getFactoryPid());
        assertEquals("name", cfg.getName());

        final File cfgFile = getTestFile("/test-config.xml");
        cfg = scanner.processConfiguration(cfgFile, pckId, "/libs/app/install/my.component.xml");
        assertEquals("my.component", cfg.getPid());
        assertNull(cfg.getFactoryPid());

        cfg = scanner.processConfiguration(cfgFile, pckId, "/libs/app/install/my.component~name.xml");
        assertEquals("my.component~name", cfg.getPid());
        assertEquals("my.component", cfg.getFactoryPid());
        assertEquals("name", cfg.getName());

        cfg = scanner.processConfiguration(cfgFile, pckId, "/libs/app/install/my.component/.content.xml");
        assertEquals("my.component", cfg.getPid());
        assertNull(cfg.getFactoryPid());

        cfg = scanner.processConfiguration(cfgFile, pckId, "/libs/app/install/my.component~name/.content.xml");
        assertEquals("my.component~name", cfg.getPid());
        assertEquals("my.component", cfg.getFactoryPid());
        assertEquals("name", cfg.getName());

        final File nocfgFile = getTestFile("/test-noconfig.xml");
        assertNull(scanner.processConfiguration(nocfgFile, pckId, "/libs/app/install/my.component.xml"));
    }
}
