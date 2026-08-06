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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CheckRepoInitConflictsTest {

    @Test
    void testConstants() {
        final AnalyserTask task = new CheckRepoInitConflicts();

        assertEquals("repoinit-conflicts", task.getId());
        assertEquals("Repoinit Conflicts check", task.getName());
    }

    @Test
    void shouldNotReportWarningWhenNoRepoinit() {
        final AnalyserTaskContext ctx = mock(AnalyserTaskContext.class);

        Feature feature = mock(Feature.class);
        org.apache.sling.feature.Extensions extensions = mock(org.apache.sling.feature.Extensions.class);

        when(ctx.getFeature()).thenReturn(feature);
        when(feature.getExtensions()).thenReturn(extensions);
        when(extensions.getByName("repoinit")).thenReturn(null);

        CheckRepoInitConflicts task = new CheckRepoInitConflicts();
        task.execute(ctx);

        verify(ctx).getFeature();
        verifyNoMoreInteractions(ctx);
    }

    @Test
    void shouldNotReportWarningWhenNoConflicts() {
        final AnalyserTaskContext ctx = mock(AnalyserTaskContext.class);

        Feature feature = featureWithExtension(
                textExtension("create path (sling:Folder) /apps/a/b\n" + "create path (sling:Folder) /apps/a/c"));

        when(ctx.getFeature()).thenReturn(feature);

        CheckRepoInitConflicts task = new CheckRepoInitConflicts();
        task.execute(ctx);

        verify(ctx).getFeature();
        verifyNoMoreInteractions(ctx);
    }

    @Test
    void shouldNotReportWarningWhenDifferentTypesOnTheBeginning() {
        final AnalyserTaskContext ctx = mock(AnalyserTaskContext.class);

        Feature feature = featureWithExtension(textExtension(
                "create path (sling:Folder) /content/dam/a\n" + "create path (sling:OrderedFolder) /content/dam/b"));

        when(ctx.getFeature()).thenReturn(feature);

        CheckRepoInitConflicts task = new CheckRepoInitConflicts();
        task.execute(ctx);

        verify(ctx).getFeature();
        verifyNoMoreInteractions(ctx);
    }

    @Test
    void shouldNotReportWarningWhenSimilarPaths() {
        final AnalyserTaskContext ctx = mock(AnalyserTaskContext.class);

        Feature feature = featureWithExtension(textExtension("create path (sling:Folder) /con/tent/my/nestedpath\n"
                + "create path (sling:Folder) /content/my/nested/path(cq:ClientLibraryFolder)"));

        when(ctx.getFeature()).thenReturn(feature);

        CheckRepoInitConflicts task = new CheckRepoInitConflicts();
        task.execute(ctx);

        verify(ctx).getFeature();
        verifyNoMoreInteractions(ctx);
    }

    @Test
    void shouldNotReportWarningWhenOddNumberOfPaths() {
        final AnalyserTaskContext ctx = mock(AnalyserTaskContext.class);

        Feature feature = featureWithExtension(textExtension("create path (sling:Folder) /content/dam/a\n"));

        when(ctx.getFeature()).thenReturn(feature);

        CheckRepoInitConflicts task = new CheckRepoInitConflicts();
        task.execute(ctx);

        verify(ctx).getFeature();
        verifyNoMoreInteractions(ctx);
    }

    @Test
    void shouldReportWarningWhenConflictExists() {
        final AnalyserTaskContext ctx = mock(AnalyserTaskContext.class);

        Feature feature =
                featureWithExtension(textExtension("create path (sling:Folder) /apps/a/b(cq:ClientLibraryFolder)\n"
                        + "create path (sling:Folder) /apps/a/b"));

        when(ctx.getFeature()).thenReturn(feature);

        CheckRepoInitConflicts task = new CheckRepoInitConflicts();
        task.execute(ctx);

        verify(ctx).getFeature();
        verify(ctx).reportWarning(contains("conflicting repoinit"));
        verify(ctx).reportWarning(contains("Conflicting statement"));
    }

    @Test
    void shouldReportWarningWhenConflictExistsInTheFirstPrimaryType() {
        final AnalyserTaskContext ctx = mock(AnalyserTaskContext.class);

        Feature feature = featureWithExtension(textExtension(
                "create path (sling:Folder) /content/dam/a\n" + "create path (sling:OrderedFolder) /content/dam/a"));

        when(ctx.getFeature()).thenReturn(feature);

        CheckRepoInitConflicts task = new CheckRepoInitConflicts();
        task.execute(ctx);

        verify(ctx).getFeature();
        verify(ctx).reportWarning(contains("conflicting repoinit"));
        verify(ctx).reportWarning(contains("Conflicting statement"));
    }

    @Test
    void shouldIgnoreInvalidRepoinitSyntax() {
        final AnalyserTaskContext ctx = mock(AnalyserTaskContext.class);

        Feature feature = featureWithExtension(textExtension("invalid $$$"));

        when(ctx.getFeature()).thenReturn(feature);

        CheckRepoInitConflicts task = new CheckRepoInitConflicts();
        task.execute(ctx);

        verify(ctx).getFeature();
        verifyNoMoreInteractions(ctx);
    }

    private Extension textExtension(String text) {
        Extension extension = mock(Extension.class);
        when(extension.getType()).thenReturn(ExtensionType.TEXT);
        when(extension.getText()).thenReturn(text);
        return extension;
    }

    private Feature featureWithExtension(Extension extension) {
        Feature feature = mock(Feature.class);
        org.apache.sling.feature.Extensions extensions = mock(org.apache.sling.feature.Extensions.class);

        when(feature.getExtensions()).thenReturn(extensions);
        when(extensions.getByName("repoinit")).thenReturn(extension);

        return feature;
    }
}
