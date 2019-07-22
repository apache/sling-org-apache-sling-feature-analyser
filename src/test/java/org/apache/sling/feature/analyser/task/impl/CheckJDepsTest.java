/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.feature.analyser.task.impl;

import static org.junit.Assert.*;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.junit.Test;

public class CheckJDepsTest {

    @Test
    public void jDepsApisJarExecution() throws Exception {
        AnalyserTaskContext ctx = mock(AnalyserTaskContext.class);

        List<String> warnings = new LinkedList<>();
        doAnswer(invocation -> {
            String error = invocation.getArgument(0);
            warnings.add(error);
            return null;
        }).when(ctx).reportWarning(anyString());

        List<String> errors = new LinkedList<>();
        doAnswer(invocation -> {
            String error = invocation.getArgument(0);
            errors.add(error);
            return null;
        }).when(ctx).reportError(anyString());

        // setup the testing feature

        Feature testFeature = new Feature(ArtifactId.parse("org.apache.sling:slingfeature-maven-plugin-test:1.0.0-SNAPSHOT"));

        for (String bundleId : new String[] {
                "org.apache.felix:org.apache.felix.inventory:1.0.6",
                "org.apache.felix:org.apache.felix.metatype:1.2.2",
                "org.apache.felix:org.apache.felix.scr:2.1.14"
        }) {
            testFeature.getBundles().add(new Artifact(ArtifactId.parse(bundleId)));
        }

        Extension apiRegionsExtension = new Extension(ExtensionType.JSON, "api-regions", false);
        apiRegionsExtension.setJSON("[\n" + 
                "    {\n" + 
                "      \"name\": \"base\",\n" + 
                "      \"exports\": [\n" + 
                "        \"org.apache.felix.inventory\",\n" + 
                "        \"org.apache.felix.metatype\"\n" + 
                "      ]\n" + 
                "    },\n" + 
                "    {\n" + 
                "      \"name\": \"extended\",\n" + 
                "      \"exports\": [\n" + 
                "        \"org.apache.felix.scr.component\",\n" + 
                "        \"org.apache.felix.scr.info\"\n" + 
                "      ]\n" + 
                "    }\n" + 
                "  ]");
        testFeature.getExtensions().add(apiRegionsExtension);

        when(ctx.getFeature()).thenReturn(testFeature);

        // setup the configurations parameters

        Map<String,String> configuration = new HashMap<>();
        File apisJarDir = FileUtils.toFile(getClass().getClassLoader().getResource("jdeps"));
        configuration.put("apis-jars-dir", apisJarDir.getAbsolutePath());

        when(ctx.getConfiguration()).thenReturn(configuration);

        // execute the jdeps check

        CheckJDeps jDepsAnalyser = new CheckJDeps();
        jDepsAnalyser.execute(ctx);

        assertFalse(warnings.isEmpty());
        assertTrue(errors.isEmpty());
    }

}
