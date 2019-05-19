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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.junit.Before;
import org.junit.Test;

public class ContentPackageScannerTest {
    
    private static final String COORDINATES_TEST_PACKAGE_A_10 = "my_packages:test_a:1.0";
    private static ArtifactId TEST_PACKAGE_AID_A_10 = ArtifactId.fromMvnId(COORDINATES_TEST_PACKAGE_A_10);

    private static final String COORDINATES_TEST_PACKAGE_B_10 = "my_packages:test_b:1.0";
    private static ArtifactId TEST_PACKAGE_AID_B_10 = ArtifactId.fromMvnId(COORDINATES_TEST_PACKAGE_B_10);

    private static final String COORDINATES_TEST_PACKAGE_C_10 = "my_packages:test_c:1.0";
    private static ArtifactId TEST_PACKAGE_AID_C_10 = ArtifactId.fromMvnId(COORDINATES_TEST_PACKAGE_C_10);

    private File file_a; 
    private File file_b;
    private File file_c;
    
    private Artifact artifact_a;
    private Artifact artifact_b;
    private Artifact artifact_c;
    
    ContentPackageDescriptor test_descriptor_a;
    ContentPackageDescriptor test_descriptor_b;
    ContentPackageDescriptor test_descriptor_c;
    
    @Before
    public void setUp() throws Exception {
        file_a = getTestFile("/test-content-a.zip");
        file_b = getTestFile("/test-content-b.zip");
        file_c = getTestFile("/test-content-c.zip");
        
        artifact_a = new Artifact(TEST_PACKAGE_AID_A_10);
        artifact_b = new Artifact(TEST_PACKAGE_AID_B_10);
        artifact_c = new Artifact(TEST_PACKAGE_AID_C_10);
        
        test_descriptor_a = new ContentPackageDescriptor(file_a.getName());
        test_descriptor_a.setName("test-content-a");
        test_descriptor_a.setArtifact(artifact_a);
        test_descriptor_a.setArtifactFile(file_a);
        
        test_descriptor_b  = new ContentPackageDescriptor(file_b.getName());
        test_descriptor_b.setName("test-content-b");
        test_descriptor_b.setArtifact(artifact_b);
        test_descriptor_b.setArtifactFile(file_b);
        
        test_descriptor_c  = new ContentPackageDescriptor(file_c.getName());
        test_descriptor_c.setName("test-content-c");
        test_descriptor_c.setArtifact(artifact_c);
        test_descriptor_c.setArtifactFile(file_c);
    }
    
    @Test
    public void testScan() throws URISyntaxException, IOException {
        
        ContentPackageScanner scanner = new ContentPackageScanner();
        Set<ContentPackageDescriptor> descriptors_a = scanner.scan(artifact_a, file_a);
        Set<ContentPackageDescriptor> descriptors_b = scanner.scan(artifact_b, file_b);
        Set<ContentPackageDescriptor> descriptors_c = scanner.scan(artifact_c, file_c);
        
        assetDescriptor((ContentPackageDescriptor)descriptors_a.toArray()[0], test_descriptor_a);
        assetDescriptor((ContentPackageDescriptor)descriptors_b.toArray()[0], test_descriptor_b);
        assetDescriptor((ContentPackageDescriptor)descriptors_c.toArray()[0], test_descriptor_c);
    }
    
    private File getTestFile(String path) throws URISyntaxException {
        return new File(getClass().getResource(path).toURI());
    }
    
    private void assetDescriptor(ContentPackageDescriptor desc1, ContentPackageDescriptor desc2) {
        assertEquals(desc1.getName(), desc2.getName());
        assertEquals(desc1.getArtifact().getId().getArtifactId(), desc2.getArtifact().getId().getArtifactId());
        assertEquals(desc1.getArtifactFile().getAbsolutePath(), desc2.getArtifactFile().getAbsolutePath());
    }
   
}
