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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.junit.Before;
import org.junit.Test;

public class ContentPackageScannerTest {
    
    private static final String COORDINATES_TEST_PACKAGE_A_10 = "my_packages:test_a:1.0";
    private static ArtifactId TEST_PACKAGE_AID_A_10 = ArtifactId.fromMvnId(COORDINATES_TEST_PACKAGE_A_10);

    private File file;
    
    private Artifact artifact;
    
    ContentPackageDescriptor test_descriptor;
    
    @Before
    public void setUp() throws Exception {
        file = getTestFile("/test-content.zip");
        
        artifact = new Artifact(TEST_PACKAGE_AID_A_10);
        
        test_descriptor = new ContentPackageDescriptor(file.getName());
        test_descriptor.setName("test-content");
        test_descriptor.setArtifact(artifact);
        test_descriptor.setArtifactFile(file);
    }
    
    @Test
    public void testScan() throws URISyntaxException, IOException {
        ContentPackageScanner scanner = new ContentPackageScanner();
        Set<ContentPackageDescriptor> descriptors_a = scanner.scan(artifact, file); 
        assetDescriptor((ContentPackageDescriptor)descriptors_a.toArray()[0]);
    }
    
    private File getTestFile(String path) throws URISyntaxException {
        return new File(getClass().getResource(path).toURI());
    }
    
    private void assetDescriptor(ContentPackageDescriptor desc) {
        assertEquals(desc.getName(), test_descriptor.getName());
        assertEquals(desc.getArtifact().getId().getArtifactId(), test_descriptor.getArtifact().getId().getArtifactId());
        assertEquals(desc.getArtifactFile().getAbsolutePath(), test_descriptor.getArtifactFile().getAbsolutePath());
        
        assertTrue(desc.bundles != null && !desc.bundles.isEmpty());
        BundleDescriptor bundles[] = desc.bundles.toArray(new BundleDescriptor[desc.bundles.size()]);
        
        assertEquals(bundles[0].getArtifact().getId().toString(), "org.apache.felix:org.apache.felix.framework:jar:bundle:6.0.1");
        
        assertTrue(desc.configs != null && !desc.configs.isEmpty());
        Configuration configs[] = desc.configs.toArray(new Configuration[desc.configs.size()]);
        assertConfiguration(configs[0]);
    }
    
    private void assertConfiguration(Configuration c) {
        Dictionary<String, Object> props = c.getProperties();
        String contentPath = (String) props.get(":configurator:feature:content-path");
        assertEquals(contentPath, "/libs/config/com.example.some.Component.xml");
    }
    
    private void printPackageEntries(File archive) throws IOException {
        ZipFile zip = new ZipFile(archive);
        Enumeration<? extends ZipEntry> entries = zip.entries();
        System.out.println("Archive: " + archive.getAbsolutePath());
        while(entries.hasMoreElements()) 
            System.out.println("    " + entries.nextElement().getName());
        System.out.println();
    }
   
}
