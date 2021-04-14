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
package org.apache.sling.feature.analyser;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.scanner.Scanner;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class AnalyserTest {
    @Test
    public void testConfiguration() throws Exception {
        Map<String, Map<String,String>> cfgs = new HashMap<String, Map<String,String>>();
        cfgs.put("all", Collections.singletonMap("aa", "bb"));
        cfgs.put("an-analyser", Collections.singletonMap("cc", "dd"));
        cfgs.put("another-analyser", Collections.singletonMap("ee", "ff"));

        Analyser a = new Analyser(null, cfgs, new String [] {});

        Map<String, String> cfg = a.getConfiguration("an-analyser");
        assertEquals(2, cfg.size());
        assertEquals("bb", cfg.get("aa"));
        assertEquals("dd", cfg.get("cc"));
    }

    @Test
    public void testConfiguration1() throws Exception {
        Map<String, Map<String,String>> cfgs = new HashMap<String, Map<String,String>>();
        cfgs.put("another-analyser", Collections.singletonMap("ee", "ff"));

        Analyser a = new Analyser(null, cfgs, new AnalyserTask [] {});

        Map<String, String> cfg = a.getConfiguration("an-analyser");
        assertEquals(0, cfg.size());
    }

    @Test public void testResult() throws Exception {
        final Feature f = new Feature(ArtifactId.parse("g:a:1"));
        final Configuration c = new Configuration("config.pid");
        final Artifact bundle = new Artifact(ArtifactId.parse("g:b:1"));

        final Scanner scanner = new Scanner(null);
        final Analyser a = new Analyser(scanner, new AnalyserTask(){

            @Override
            public void execute(AnalyserTaskContext ctx) throws Exception {
                ctx.reportArtifactError(bundle.getId(), "artifact-error");
                ctx.reportArtifactWarning(bundle.getId(), "artifact-warn");
                ctx.reportExtensionError("name", "extension-error");
                ctx.reportExtensionWarning("name", "extension-warn");
                ctx.reportConfigurationError(c, "config-error");
                ctx.reportConfigurationWarning(c, "config-warn");
                ctx.reportError("global-error");
                ctx.reportWarning("global-warn");                
            }            
        });
        final AnalyserResult result = a.analyse(f);
        assertNotNull(result);

        assertEquals(4, result.getErrors().size());
        assertEquals(Arrays.asList("global-error",
            bundle.getId().toString()+": artifact-error",
            "name: extension-error", 
            "Configuration " + c.getPid() + ": config-error"), result.getErrors());
        assertEquals(4, result.getWarnings().size());
        assertEquals(Arrays.asList("global-warn",
            bundle.getId().toString()+": artifact-warn",
            "name: extension-warn", 
            "Configuration " + c.getPid() + ": config-warn"), result.getWarnings());

        assertEquals(1, result.getGlobalErrors().size());
        assertEquals("global-error", result.getGlobalErrors().get(0).toString());
        assertEquals("global-error", result.getGlobalErrors().get(0).getValue());
        assertNull(result.getGlobalErrors().get(0).getKey());

        assertEquals(1, result.getGlobalWarnings().size());
        assertEquals("global-warn", result.getGlobalWarnings().get(0).toString());
        assertEquals("global-warn", result.getGlobalWarnings().get(0).getValue());
        assertNull(result.getGlobalWarnings().get(0).getKey());

        assertEquals(1, result.getArtifactErrors().size());
        assertEquals(bundle.getId().toString()+": artifact-error", result.getArtifactErrors().get(0).toString());
        assertEquals("artifact-error", result.getArtifactErrors().get(0).getValue());
        assertEquals(bundle.getId(), result.getArtifactErrors().get(0).getKey());

        assertEquals(1, result.getArtifactWarnings().size());
        assertEquals(bundle.getId().toString()+": artifact-warn", result.getArtifactWarnings().get(0).toString());
        assertEquals("artifact-warn", result.getArtifactWarnings().get(0).getValue());
        assertEquals(bundle.getId(), result.getArtifactWarnings().get(0).getKey());

        assertEquals(1, result.getExtensionErrors().size());
        assertEquals("name: extension-error", result.getExtensionErrors().get(0).toString());
        assertEquals("extension-error", result.getExtensionErrors().get(0).getValue());
        assertEquals("name", result.getExtensionErrors().get(0).getKey());

        assertEquals(1, result.getExtensionWarnings().size());
        assertEquals("name: extension-warn", result.getExtensionWarnings().get(0).toString());
        assertEquals("extension-warn", result.getExtensionWarnings().get(0).getValue());
        assertEquals("name", result.getExtensionWarnings().get(0).getKey());

        assertEquals(1, result.getConfigurationErrors().size());
        assertEquals("Configuration " + c.getPid() + ": config-error", result.getConfigurationErrors().get(0).toString());
        assertEquals("config-error", result.getConfigurationErrors().get(0).getValue());
        assertEquals(c, result.getConfigurationErrors().get(0).getKey());

        assertEquals(1, result.getConfigurationWarnings().size());
        assertEquals("Configuration " + c.getPid() + ": config-warn", result.getConfigurationWarnings().get(0).toString());
        assertEquals("config-warn", result.getConfigurationWarnings().get(0).getValue());
        assertEquals(c, result.getConfigurationWarnings().get(0).getKey());
    }
}
