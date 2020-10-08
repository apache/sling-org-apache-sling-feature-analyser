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

import java.io.*;
import java.net.URL;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.io.json.FeatureJSONReader;
import org.apache.sling.feature.scanner.FeatureDescriptor;
import org.apache.sling.feature.scanner.PackageInfo;
import org.apache.sling.feature.scanner.Scanner;
import org.junit.Test;
import org.osgi.framework.Version;

import static org.junit.Assert.*;

public class BundleDescriptorImplTest
{

    private void assertPackageInfo(Set<PackageInfo> infos, final String name, final Version version) {
        for (PackageInfo info : infos)
        {
            if (name.equals(info.getName()) && version.equals(info.getPackageVersion()))
            {
                return;
            }
        }
        fail();
    }

    @Test public void testExportPackage() throws Exception {
        String bmf = "Bundle-SymbolicName: pkg.bundle\n"
            + "Bundle-Version: 1\n"
            + "Bundle-ManifestVersion: 2\n"
            + "Export-Package: org.apache.sling;version=1.0,org.apache.felix;version=2.0\n";
        URL f = new URL("jar:" + createBundle(bmf).toURI().toURL() + "!/");
        BundleDescriptorImpl bdf = new BundleDescriptorImpl(new Artifact(new ArtifactId("foo", "bar", "1.0", "bla", "bundle")), f, 1);
        final Set<PackageInfo> infos = bdf.getExportedPackages();
        assertEquals(2, infos.size());
        assertPackageInfo(infos ,"org.apache.sling", Version.parseVersion("1.0"));
        assertPackageInfo(infos,"org.apache.felix", Version.parseVersion("2.0"));
    }

    @Test
    public void testBundleManifest() throws Exception {
        Feature feature = FeatureJSONReader.read(new InputStreamReader(getClass().getResourceAsStream("/metadata-feature.json")), null);
        Scanner scanner = new Scanner(artifactId -> null);
        FeatureDescriptor descriptor = scanner.scan(feature);
        assertEquals(feature.getBundles().size(), descriptor.getBundleDescriptors().size());
        assertNull(descriptor.getBundleDescriptors().iterator().next().getArtifactFile());
        assertEquals("2.0", descriptor.getBundleDescriptors().stream()
                .filter(b -> b.getBundleSymbolicName().equals("log4j.over.slf4j")).findFirst().get()
                .getManifest().getMainAttributes().getValue("Manifest-Version"));
    }

    private File createBundle(String manifest) throws IOException
    {
        File f = File.createTempFile("bundle", ".jar");
        f.deleteOnExit();
        Manifest mf = new Manifest(new ByteArrayInputStream(manifest.getBytes("UTF-8")));
        mf.getMainAttributes().putValue("Manifest-Version", "1.0");
        JarOutputStream os = new JarOutputStream(new FileOutputStream(f), mf);
        os.close();
        return f;
    }
}
