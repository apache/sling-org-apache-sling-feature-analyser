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
package org.apache.sling.feature.analyser.task.impl;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CheckRepoInitConflictsTest {

    @Test
    void testConstants() {
        final AnalyserTask task = new CheckRepoInitConflicts();

        assertEquals("repoinit-conflict-validation", task.getId());
        assertEquals("Repoinit Conflict Validation", task.getName());
    }

    @Test
    void shouldNotReportWarningWhenNoRepoinit() {
        final AnalyserTaskContext ctx = Mockito.mock(AnalyserTaskContext.class);

        Feature feature = Mockito.mock(Feature.class);
        org.apache.sling.feature.Extensions extensions = Mockito.mock(org.apache.sling.feature.Extensions.class);

        Mockito.when(ctx.getFeature()).thenReturn(feature);
        Mockito.when(feature.getExtensions()).thenReturn(extensions);
        Mockito.when(extensions.getByName("repoinit")).thenReturn(null);

        CheckRepoInitConflicts task = new CheckRepoInitConflicts();
        task.execute(ctx);

        Mockito.verify(ctx).getFeature();
        Mockito.verifyNoMoreInteractions(ctx);
    }

    @Test
    void shouldNotReportWarningWhenNoConflicts() {
        final AnalyserTaskContext ctx = Mockito.mock(AnalyserTaskContext.class);

        Feature feature = featureWithExtension(
                textExtension("create path (sling:Folder) /apps/a/b\n" + "create path (sling:Folder) /apps/a/c"));

        Mockito.when(ctx.getFeature()).thenReturn(feature);

        CheckRepoInitConflicts task = new CheckRepoInitConflicts();
        task.execute(ctx);

        Mockito.verify(ctx).getFeature();
        Mockito.verifyNoMoreInteractions(ctx);
    }

    @Test
    void shouldReportWarningWhenConflictExists() {
        final AnalyserTaskContext ctx = Mockito.mock(AnalyserTaskContext.class);

        Feature feature =
                featureWithExtension(textExtension("create path (sling:Folder) /apps/a/b(cq:ClientLibraryFolder)\n"
                        + "create path (sling:Folder) /apps/a/b"));

        Mockito.when(ctx.getFeature()).thenReturn(feature);

        CheckRepoInitConflicts task = new CheckRepoInitConflicts();
        task.execute(ctx);

        Mockito.verify(ctx).getFeature();
        Mockito.verify(ctx).reportWarning(Mockito.contains("conflicting repoinit"));
        Mockito.verify(ctx).reportWarning(Mockito.contains("Conflicting statement"));
    }

    @Test
    void shouldIgnoreInvalidRepoinitSyntax() {
        final AnalyserTaskContext ctx = Mockito.mock(AnalyserTaskContext.class);

        Feature feature = featureWithExtension(textExtension("invalid $$$"));

        Mockito.when(ctx.getFeature()).thenReturn(feature);

        CheckRepoInitConflicts task = new CheckRepoInitConflicts();
        task.execute(ctx);

        Mockito.verify(ctx).getFeature();
        Mockito.verifyNoMoreInteractions(ctx);
    }

    private Extension textExtension(String text) {
        Extension extension = Mockito.mock(Extension.class);
        Mockito.when(extension.getType()).thenReturn(ExtensionType.TEXT);
        Mockito.when(extension.getText()).thenReturn(text);
        return extension;
    }

    private Feature featureWithExtension(Extension extension) {
        Feature feature = Mockito.mock(Feature.class);
        org.apache.sling.feature.Extensions extensions = Mockito.mock(org.apache.sling.feature.Extensions.class);

        Mockito.when(feature.getExtensions()).thenReturn(extensions);
        Mockito.when(extensions.getByName("repoinit")).thenReturn(extension);

        return feature;
    }
}
