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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.feature.Feature;
import org.apache.sling.repoinit.parser.operations.CreatePath;

@SuppressWarnings("java:S1874")
public class ValidationReport {

    private final Map<Feature, List<CreatePath[]>> conflicts = new LinkedHashMap<>();

    public Map<Feature, List<CreatePath[]>> getConflicts() {
        return conflicts;
    }

    public void addConflicts(Feature feature, List<CreatePath[]> featureConflicts) {
        if (!featureConflicts.isEmpty()) {
            conflicts.put(feature, featureConflicts);
        }
    }

    public boolean hasConflicts() {
        return !conflicts.isEmpty();
    }

    public List<String> generate() {
        List<String> messages = new ArrayList<>();
        messages.add("Repoinit validation results:");

        if (!hasConflicts()) {
            messages.add("No issues found");
        } else {
            for (Map.Entry<Feature, List<CreatePath[]>> entry : conflicts.entrySet()) {
                Feature feature = entry.getKey();
                List<CreatePath[]> featureConflicts = entry.getValue();

                messages.add("Repoinit validation conflict summary feature=" + feature + " conflicts="
                        + featureConflicts.size());

                for (CreatePath[] conflict : featureConflicts) {
                    messages.add("Conflicting statement :\n  "
                            + StringUtils.trim(conflict[0].asRepoInitString())
                            + "\n"
                            + StringUtils.trim(conflict[1].asRepoInitString()));
                }
            }
        }

        return messages;
    }
}
