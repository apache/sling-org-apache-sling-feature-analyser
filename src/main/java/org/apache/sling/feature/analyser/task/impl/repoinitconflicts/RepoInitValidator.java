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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.repoinit.parser.impl.ParseException;
import org.apache.sling.repoinit.parser.impl.RepoInitParserImpl;
import org.apache.sling.repoinit.parser.operations.CreatePath;
import org.apache.sling.repoinit.parser.operations.Operation;
import org.apache.sling.repoinit.parser.operations.PathSegmentDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("java:S1874")
public class RepoInitValidator {
    private RepoInitValidator() {}

    private static final Logger LOGGER = LoggerFactory.getLogger(RepoInitValidator.class);

    public static RepoInitValidationReport validateRepoinit(final Feature feature) {
        RepoInitValidationReport report = new RepoInitValidationReport();

        if (feature.getExtensions().getByName("repoinit") == null) {
            return report;
        }

        final Extension repoinitExtension = feature.getExtensions().getByName("repoinit");
        List<CreatePath[]> conflicts = doesRepoinitHaveConflicts(repoinitExtension);

        report.addConflicts(feature, conflicts);

        return report;
    }

    private static List<CreatePath[]> doesRepoinitHaveConflicts(Extension repoinitExtension) {
        if (repoinitExtension == null
                || repoinitExtension.getText() == null
                || repoinitExtension.getType() != ExtensionType.TEXT) {
            return Collections.emptyList();
        }

        try {
            List<Operation> operations = new RepoInitParserImpl(new StringReader(repoinitExtension.getText())).parse();

            List<CreatePath> createPaths = operations.stream()
                    .filter(CreatePath.class::isInstance)
                    .map(op -> (CreatePath) op)
                    .collect(Collectors.toList());

            return hasConflicts(createPaths);
        } catch (ParseException e) {
            LOGGER.error(
                    "Failed to parse repoinit statements, skipping conflict validation. Error: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private static List<CreatePath[]> hasConflicts(List<CreatePath> createPaths) {
        int size = createPaths.size();
        List<CreatePath[]> conflicts = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                List<CreatePath[]> conflict = hasConflict(createPaths.get(i), createPaths.get(j));
                if (!conflict.isEmpty()) {
                    conflicts.addAll(conflict);
                }
            }
        }
        return conflicts;
    }

    private static List<CreatePath[]> hasConflict(CreatePath a, CreatePath b) {
        List<PathSegmentDefinition> aDefs = a.getDefinitions();
        List<PathSegmentDefinition> bDefs = b.getDefinitions();

        // different depth → no conflict
        if (aDefs.size() != bDefs.size()) {
            return Collections.emptyList();
        }
        List<CreatePath[]> conflicts = new ArrayList<>();
        for (int i = 0; i < aDefs.size(); i++) {
            PathSegmentDefinition aSeg = aDefs.get(i);
            PathSegmentDefinition bSeg = bDefs.get(i);

            // segments diverge → stop comparing this pair
            if (!Objects.equals(aSeg.getSegment(), bSeg.getSegment())) {
                return Collections.emptyList();
            }

            // same segment but different type → conflict
            if (!Objects.equals(aSeg.getPrimaryType(), bSeg.getPrimaryType())) {
                CreatePath[] conflict = new CreatePath[2];
                conflict[0] = a;
                conflict[1] = b;

                conflicts.add(conflict);
                break;
            }
        }
        return conflicts;
    }
}
