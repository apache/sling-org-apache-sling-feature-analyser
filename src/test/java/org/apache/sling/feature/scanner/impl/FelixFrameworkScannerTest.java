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

import org.apache.felix.framework.Felix;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.builder.ArtifactProvider;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.PackageInfo;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.resource.Capability;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FelixFrameworkScannerTest {
    @Test
    public void testGetFrameworkProperties() throws Exception {
        URL url = getFelixFrameworkJar();
        File fwFile = new File(url.toURI());

        FelixFrameworkScanner ffs = new FelixFrameworkScanner();

        String additionalVersions = getAdditionalVersionNumbers();

        Map<String,String> kvmap = new HashMap<>();
        Map<String,String> props = ffs.getFrameworkProperties(kvmap, fwFile.toURI().toURL());
        String expected = "osgi.service; objectClass:List<String>=org.osgi.service.resolver.Resolver; "
                    + "uses:=org.osgi.service.resolver, "
                + "osgi.service; objectClass:List<String>=org.osgi.service.startlevel.StartLevel; "
                    + "uses:=org.osgi.service.startlevel, "
                + "osgi.service; objectClass:List<String>=org.osgi.service.packageadmin.PackageAdmin; "
                    + "uses:=org.osgi.service.packageadmin , "
                + "osgi.ee; osgi.ee=\"OSGi/Minimum\"; version:List<Version>=\"1.0,1.1,1.2\", "
                + "osgi.ee; osgi.ee=\"JavaSE\"; version:List<Version>=\"1.0,1.1,1.2,1.3,1.4,1.5,1.6,1.7,1.8"
                    + additionalVersions + "\", "
                + "osgi.ee; osgi.ee=\"JavaSE/compact1\"; version:List<Version>=\"1.8"
                    + additionalVersions + "\", "
                + "osgi.ee; osgi.ee=\"JavaSE/compact2\"; version:List<Version>=\"1.8"
                    + additionalVersions + "\", "
                + "osgi.ee; osgi.ee=\"JavaSE/compact3\"; version:List<Version>=\"1.8"
                    + additionalVersions + "\"";
        String actual = props.get("org.osgi.framework.system.capabilities");

        // Remove spaces
        assertEquals(expected.replaceAll("\\s",""), actual.replaceAll("\\s",""));
    }

    private String getAdditionalVersionNumbers() {
        StringBuilder sb = new StringBuilder();

        String javaVersion = System.getProperty("java.specification.version");
        if (!javaVersion.startsWith("1.")) {
            int curVersion = Integer.parseInt(javaVersion);

            for (int i=9; i<=curVersion; i++) {
                sb.append(',');
                sb.append(i);
            }
        }

        return sb.toString();
    }

    @Test
    public void testGetFrameworkExports() throws Exception {
        URL fwFile = getFelixFrameworkJar();
        FelixFrameworkScanner ffs = new FelixFrameworkScanner();

        Map<String,String> kvmap = new HashMap<>();
        kvmap.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, "org.foo.bar;version=\"{dollar}{java.specification.version}\","
                + "org.zaa.zaa;version=\"{dollar}{java.class.version}\"");
        kvmap.put(Constants.FRAMEWORK_SYSTEMCAPABILITIES_EXTRA, "ding.dong;ding.dong=\"yeah!\"");
        kvmap.put("felix.systempackages.substitution", "true");

        BundleDescriptor bundleDescriptor = ffs.scan(new ArtifactId("org.apache.felix",
                "org.apache.felix.framework",
                "6.0.1", null, null), kvmap, new ArtifactProvider() {

                    @Override
                    public URL provide(ArtifactId id) {
                        return fwFile;
                    }
                });

        String javaVersion = System.getProperty("java.specification.version");

        Set<PackageInfo> exportedPackages = bundleDescriptor.getExportedPackages();
        assertFalse(exportedPackages.isEmpty());
        boolean foundFooBar = false;
        for (PackageInfo pi : exportedPackages) {
            if (pi.getName().equals("org.foo.bar")) {
                assertTrue(pi.getVersion().startsWith(javaVersion));
                foundFooBar = true;
            }
        }
        assertTrue(foundFooBar);

        String classVersion = System.getProperty("java.class.version");
        boolean foundZaaZaa = false;
        for (PackageInfo pi : exportedPackages) {
            if (pi.getName().equals("org.zaa.zaa")) {
                assertTrue(pi.getVersion().startsWith(classVersion));
                foundZaaZaa = true;
            }
        }
        assertTrue(foundZaaZaa);

        Set<Capability> providedCaps = bundleDescriptor.getCapabilities();
        assertFalse(providedCaps.isEmpty());
        boolean foundDingDong = false;
        for (Capability cap : providedCaps) {
            if (cap.getNamespace().equals("ding.dong") && "yeah!".equals(cap.getAttributes().get("ding.dong")))
                foundDingDong = true;
        }
        assertTrue(foundDingDong);
    }

    private URL getFelixFrameworkJar() throws IOException {
        return FelixFrameworkScanner.getClasspathForClass(Felix.class).toURI().toURL();
    }
}
