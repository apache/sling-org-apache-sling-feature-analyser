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
package org.apache.sling.feature.analyser.task.impl;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Artifacts;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.builder.FeatureProvider;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class CheckCompareFeaturesTest {
    @Test
    public void testAssertEmptyArtifactsSame() {
        Artifacts a1 = new Artifacts();
        Artifacts a2 = new Artifacts();

        assertNull(CheckCompareFeatures.assertArtifactsSame(a1, a2, true));
        assertNull(CheckCompareFeatures.assertArtifactsSame(a1, a2, false));
    }

    @Test
    public void testAssertDiffSizeArtifactsNotSame() {
        Artifacts a1 = new Artifacts();
        Artifacts a2 = new Artifacts();
        a2.add(new Artifact(ArtifactId.fromMvnId("a:b:1")));

        assertNotNull(CheckCompareFeatures.assertArtifactsSame(a1, a2, true));
        assertNotNull(CheckCompareFeatures.assertArtifactsSame(a1, a2, false));
    }

    @Test
    public void testAssertDiffSizeArtifactsNotSame2() {
        Artifacts a1 = new Artifacts();
        a1.add(new Artifact(ArtifactId.fromMvnId("a:b:1")));
        Artifacts a2 = new Artifacts();
        a2.add(new Artifact(ArtifactId.fromMvnId("a:b:2")));

        assertNotNull(CheckCompareFeatures.assertArtifactsSame(a1, a2, true));
        assertNotNull(CheckCompareFeatures.assertArtifactsSame(a1, a2, false));
    }

    @Test
    public void testAssertArtifactsSame() {
        Artifacts a1 = new Artifacts();
        a1.add(new Artifact(ArtifactId.fromMvnId("a:b:1")));
        a1.add(new Artifact(ArtifactId.fromMvnId("a:b:2")));
        Artifacts a2 = new Artifacts();
        a2.add(new Artifact(ArtifactId.fromMvnId("a:b:2")));
        a2.add(new Artifact(ArtifactId.fromMvnId("a:b:1")));

        assertNull(CheckCompareFeatures.assertArtifactsSame(a1, a2, true));
        assertNull(CheckCompareFeatures.assertArtifactsSame(a1, a2, false));
    }

    @Test
    public void testAssertArtifactsSameDifferentMetadata() {
        Artifact art1 = new Artifact(ArtifactId.fromMvnId("a:b:1"));
        art1.getMetadata().put("foo", "bar");
        Artifacts a1 = new Artifacts();
        a1.add(art1);

        Artifact art2 = new Artifact(ArtifactId.fromMvnId("a:b:1"));
        Artifacts a2 = new Artifacts();
        a2.add(art2);

        assertNotNull(CheckCompareFeatures.assertArtifactsSame(a1, a2, true));
        assertNull(CheckCompareFeatures.assertArtifactsSame(a1, a2, false));
    }

    @Test
    public void testFindArtifactsToCompare() throws Exception {
        Feature f = new Feature(ArtifactId.fromMvnId("x:y:123"));
        f.getBundles().add(new Artifact(ArtifactId.fromMvnId("grp:bundle1:1")));
        Extension ext = new Extension(ExtensionType.TEXT, "textext", ExtensionState.REQUIRED);
        ext.setText("hello");
        f.getExtensions().add(ext);
        Extension ext2 = new Extension(ExtensionType.ARTIFACTS, "artext", ExtensionState.REQUIRED);
        ext2.getArtifacts().add(new Artifact(ArtifactId.fromMvnId("grp:extart1:2")));
        ext2.getArtifacts().add(new Artifact(ArtifactId.fromMvnId("grp:extart2:5")));
        f.getExtensions().add(ext2);

        Artifacts arts1 = CheckCompareFeatures.getArtifactsToCompare(f, null);
        assertEquals(arts1, f.getBundles());

        Artifacts arts2 = CheckCompareFeatures.getArtifactsToCompare(f, "artext");
        assertEquals(ext2.getArtifacts(), arts2);

        try {
            CheckCompareFeatures.getArtifactsToCompare(f, "textext");
            fail("Expected an exception, because textext is not of type Artifacts");
        } catch (Exception ex) {
            // good
        }

        try {
            CheckCompareFeatures.getArtifactsToCompare(f, "nonexisting");
            fail("Expected an exception, because a non-existing extension was requested");
        } catch (Exception ex) {
            // good
        }
    }

    @Test
    public void testExecute1() throws Exception {
        Feature f = new Feature(ArtifactId.fromMvnId("g:a:123"));
        f.getBundles().add(new Artifact(ArtifactId.fromMvnId("g:b:1")));
        Feature fc = new Feature(ArtifactId.fromMvnId("g:b:456"));
        fc.getBundles().add(new Artifact(ArtifactId.fromMvnId("g:b:1")));

        AnalyserTaskContext ctx = testExecute(f, fc);

        Mockito.verify(ctx, Mockito.never()).reportError(Mockito.anyString());
    }

    @Test
    public void testExecute2() throws Exception {
        Feature f = new Feature(ArtifactId.fromMvnId("g:a:123"));
        f.getBundles().add(new Artifact(ArtifactId.fromMvnId("g:b:1")));
        Feature fc = new Feature(ArtifactId.fromMvnId("g:b:456"));

        AnalyserTaskContext ctx = testExecute(f, fc);

        Mockito.verify(ctx).reportError(Mockito.anyString());
    }

    @Test
    public void testExecute3() throws Exception {
        Feature f1 = new Feature(ArtifactId.fromMvnId("g:a:123"));
        Extension e1 = new Extension(ExtensionType.ARTIFACTS, "myarts", ExtensionState.REQUIRED);
        e1.getArtifacts().add(new Artifact(ArtifactId.fromMvnId("g:c:1")));
        e1.getArtifacts().add(new Artifact(ArtifactId.fromMvnId("g:c:2")));
        f1.getExtensions().add(e1);

        Feature f2 = new Feature(ArtifactId.fromMvnId("g:b:456"));
        Extension e2 = new Extension(ExtensionType.ARTIFACTS, "myarts", ExtensionState.REQUIRED);
        e2.getArtifacts().add(new Artifact(ArtifactId.fromMvnId("g:c:2")));
        e2.getArtifacts().add(new Artifact(ArtifactId.fromMvnId("g:c:1")));
        f2.getExtensions().add(e2);

        AnalyserTaskContext ctx = testExecute(f1, f2, "myarts");

        Mockito.verify(ctx, Mockito.never()).reportError(Mockito.anyString());
    }

    @Test
    public void testExecute4() throws Exception {
        Feature f1 = new Feature(ArtifactId.fromMvnId("g:a:123"));
        Extension e1 = new Extension(ExtensionType.ARTIFACTS, "myarts", ExtensionState.REQUIRED);
        f1.getExtensions().add(e1);

        Feature f2 = new Feature(ArtifactId.fromMvnId("g:b:456"));
        Extension e2 = new Extension(ExtensionType.ARTIFACTS, "myarts", ExtensionState.REQUIRED);
        e2.getArtifacts().add(new Artifact(ArtifactId.fromMvnId("g:c:2")));
        f2.getExtensions().add(e2);

        AnalyserTaskContext ctx = testExecute("SAME", f1, f2, "myarts");

        Mockito.verify(ctx).reportError(Mockito.anyString());
    }

    @Test
    public void testAssertEmptyArtifactsNotDifferent() throws Exception {
        Feature f = new Feature(ArtifactId.fromMvnId("g:a:123"));
        Feature fc = new Feature(ArtifactId.fromMvnId("g:b:456"));

        AnalyserTaskContext ctx = testExecute("DIFFERENT", f, fc);

        Mockito.verify(ctx).reportError(Mockito.anyString());
    }

    @Test
    public void testAssertDiffSizeArtifactsDifferent() throws Exception {
        Artifact art1 = new Artifact(ArtifactId.fromMvnId("a:b:1"));
        art1.getMetadata().put("foo", "bar");

        Artifact art2 = new Artifact(ArtifactId.fromMvnId("a:b:1"));

        Feature f = new Feature(ArtifactId.fromMvnId("g:a:123"));
        f.getBundles().add(art1);
        Feature fc = new Feature(ArtifactId.fromMvnId("g:b:456"));
        fc.getBundles().add(art2);

        @SuppressWarnings("unchecked")
        AnalyserTaskContext ctx = testExecute("DIFFERENT", f, fc, null,
                new AbstractMap.SimpleEntry<>("compare-metadata", "false"));
        Mockito.verify(ctx).reportError(Mockito.anyString());

        @SuppressWarnings("unchecked")
        AnalyserTaskContext ctx2 = testExecute("DIFFERENT", f, fc, null,
                new AbstractMap.SimpleEntry<>("compare-metadata", "true"));
        Mockito.verify(ctx2, Mockito.never()).reportError(Mockito.anyString());
    }

    private AnalyserTaskContext testExecute(String mode, Feature f, Feature fc) throws Exception {
        return testExecute(mode, f, fc, null);
    }

    private AnalyserTaskContext testExecute(Feature f, Feature fc) throws Exception {
        return testExecute(null, f, fc, null);
    }

    private AnalyserTaskContext testExecute(Feature f, Feature fc, String extension) throws Exception {
        return testExecute(null, f, fc, extension);
    }

    @SuppressWarnings("unchecked")
    private AnalyserTaskContext testExecute(String mode, Feature f, Feature fc, String extension) throws Exception {
        return testExecute(mode, f, fc, extension, new Map.Entry[0]);
    }

    private AnalyserTaskContext testExecute(String mode, Feature f, Feature fc, String extension,
            @SuppressWarnings("unchecked") Map.Entry<String, String> ... cfgEntries) throws Exception {
        Map<String, String> cfg = new HashMap<>();
        cfg.put("compare-with", f.getId().toMvnId());

        if (mode != null) {
            cfg.put("compare-mode", mode);
        }

        if (extension != null) {
            cfg.put("compare-extension", extension);
        }

        for (Map.Entry<String, String> entry : cfgEntries) {
            cfg.put(entry.getKey(), entry.getValue());
        }

        CheckCompareFeatures cfe = new CheckCompareFeatures();

        FeatureProvider fp = new FeatureProvider() {
            @Override
            public Feature provide(ArtifactId id) {
                if ("g:a:123".equals(id.toMvnId())) {
                    return f;
                }
                return null;
            }
        };

        AnalyserTaskContext ctx = Mockito.mock(AnalyserTaskContext.class);
        Mockito.when(ctx.getConfiguration()).thenReturn(cfg);
        Mockito.when(ctx.getFeatureProvider()).thenReturn(fp);
        Mockito.when(ctx.getFeature()).thenReturn(fc);

        cfe.execute(ctx);
        return ctx;
    }
}
