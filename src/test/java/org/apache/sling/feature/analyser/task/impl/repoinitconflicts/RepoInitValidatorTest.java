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
package org.apache.sling.feature.analyser.task.impl.repoinitconflicts;

import java.util.List;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RepoInitValidatorTest {

    @Test
    void shouldReturnNoIssuesWhenFeatureHasNoRepoinit() {
        Feature feature = mock(Feature.class);
        when(feature.getExtensions()).thenReturn(mock(org.apache.sling.feature.Extensions.class));
        when(feature.getExtensions().getByName("repoinit")).thenReturn(null);

        RepoInitValidationReport report = RepoInitValidator.validateRepoinit(feature);

        assertFalse(report.hasConflicts());
        assertTrue(report.generate().contains("No issues found"));
    }

    @Test
    void shouldReturnNoIssuesForCorrectRepoinit() {
        Extension extension =
                textExtension("create path (sling:Folder) /apps/a/b\n" + "create path (sling:Folder) /apps/a/c\n"
                        + "create path (sling:Folder) /apps/a/c/d(cq:ClientLibraryFolder)");

        Feature feature = featureWithExtension(extension);

        RepoInitValidationReport report = RepoInitValidator.validateRepoinit(feature);

        assertFalse(report.hasConflicts());
        assertTrue(report.generate().contains("No issues found"));
    }

    @Test
    void shouldReportConflictForSamePathDifferentType() {
        Extension extension = textExtension("create path (sling:Folder) /apps/a/b(cq:ClientLibraryFolder)\n"
                + "create path (sling:Folder) /apps/a/b");

        Feature feature = featureWithExtension(extension);

        RepoInitValidationReport report = RepoInitValidator.validateRepoinit(feature);
        assertTrue(report.hasConflicts());

        List<String> result = report.generate();
        assertTrue(result.get(1).contains("Incorrect repoinit for feature"));
        assertTrue(result.get(2).contains("Found 1 sets of conflicting repoinit statements"));
        assertTrue(result.get(3).contains("/apps/a/b"));
    }

    @Test
    void shouldReportMultipleConflicts() {
        Extension extension = textExtension("create path (sling:Folder) /apps/a/b(cq:ClientLibraryFolder)\n"
                + "create path (sling:Folder) /apps/a/b\n"
                + "create path (sling:Folder) /apps/x/y(cq:ClientLibraryFolder)\n"
                + "create path (sling:Folder) /apps/x/y");

        Feature feature = featureWithExtension(extension);

        RepoInitValidationReport report = RepoInitValidator.validateRepoinit(feature);
        assertTrue(report.hasConflicts());

        List<String> result = report.generate();
        assertTrue(result.get(2).contains("Found 2 sets of conflicting repoinit statements"));
    }

    @Test
    void shouldIgnoreInvalidRepoinitSyntax() {
        Extension extension = textExtension("invalid $$$");

        Feature feature = featureWithExtension(extension);

        RepoInitValidationReport report = RepoInitValidator.validateRepoinit(feature);

        assertFalse(report.hasConflicts());
        assertTrue(report.generate().contains("No issues found"));
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
