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

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RepoInitConflictsValidatorTest {

    @Test
    void shouldReturnNoIssuesWhenFeatureHasNoRepoinit() {
        Feature feature = mock(Feature.class);
        when(feature.getExtensions()).thenReturn(mock(org.apache.sling.feature.Extensions.class));
        when(feature.getExtensions().getByName("repoinit")).thenReturn(null);

        ValidationReport report = RepoInitConflictsValidator.validate(feature);

        assertFalse(report.hasConflicts());
        assertTrue(report.generate().contains("No issues found"));
    }

    @Test
    void shouldReturnNoIssuesForCorrectRepoinit() {
        Extension extension =
                textExtension("create path (sling:Folder) /apps/a/b\n" + "create path (sling:Folder) /apps/a/c\n"
                        + "create path (sling:Folder) /apps/a/c/d(sling:OrderedFolder)");

        Feature feature = featureWithExtension(extension);

        ValidationReport report = RepoInitConflictsValidator.validate(feature);

        assertFalse(report.hasConflicts());
        assertTrue(report.generate().contains("No issues found"));
    }

    @Test
    void shouldReportConflictForSamePathDifferentType() {
        Extension extension = textExtension(
                "create path (sling:Folder) /apps/a/b(sling:OrderedFolder)\n" + "create path (sling:Folder) /apps/a/b");

        Feature feature = featureWithExtension(extension);

        ValidationReport report = RepoInitConflictsValidator.validate(feature);
        assertTrue(report.hasConflicts());

        String result = report.generate();
        String[] lines = result.split("\n");
        assertEquals(5, lines.length);
        assertEquals("Repoinit validation results:", lines[0]);
        assertTrue(lines[1].contains("Repoinit validation conflict summary"));
        assertTrue(lines[1].contains("found 1 conflict(s)"));
        assertTrue(lines[2].contains("Conflicting repoinit statement set 1/1:"));
        assertTrue(lines[3].contains("/apps/a/b(sling:OrderedFolder)"));
        assertTrue(lines[4].contains("/apps/a/b"));
    }

    @Test
    void shouldReportMultipleConflicts() {
        Extension extension = textExtension("create path (sling:Folder) /apps/a/b(sling:OrderedFolder)\n"
                + "create path (sling:Folder) /apps/a/b\n"
                + "create path (sling:Folder) /apps/x/y(sling:OrderedFolder)\n"
                + "create path (sling:Folder) /apps/x/y");

        Feature feature = featureWithExtension(extension);

        ValidationReport report = RepoInitConflictsValidator.validate(feature);
        assertTrue(report.hasConflicts());

        String result = report.generate();
        assertTrue(result.contains("2 conflict(s)"));
    }

    @Test
    void shouldIgnoreInvalidRepoinitSyntax() {
        Extension extension = textExtension("invalid $$$");

        Feature feature = featureWithExtension(extension);

        ValidationReport report = RepoInitConflictsValidator.validate(feature);

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

/*
    public static void main(String[] args) {
        Extension extension = org.mockito.Mockito.mock(Extension.class);
        org.mockito.Mockito.when(extension.getType()).thenReturn(ExtensionType.TEXT);
        org.mockito.Mockito.when(extension.getText()).thenReturn(
            "create path (sling:Folder) /apps/a/b(sling:OrderedFolder)\n" + "create path (sling:Folder) /apps/a/b");

        Feature feature = org.mockito.Mockito.mock(Feature.class);
        org.apache.sling.feature.Extensions extensions = org.mockito.Mockito.mock(org.apache.sling.feature.Extensions.class);
        org.mockito.Mockito.when(feature.getExtensions()).thenReturn(extensions);
        org.mockito.Mockito.when(extensions.getByName("repoinit")).thenReturn(extension);

        ValidationReport report = RepoInitConflictsValidator.validate(feature);
        String result = report.generate();
        System.out.println("===START===");
        System.out.println(result);
        System.out.println("===END===");
    }
*/
