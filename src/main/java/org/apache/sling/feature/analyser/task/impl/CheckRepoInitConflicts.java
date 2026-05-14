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

import java.util.List;

import org.apache.sling.feature.Feature;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.analyser.task.impl.repoinitconflicts.RepoInitValidationReport;
import org.apache.sling.feature.analyser.task.impl.repoinitconflicts.RepoInitValidator;

public class CheckRepoInitConflicts implements AnalyserTask {

    @Override
    public String getId() {
        return "repoinit-conflict-validation";
    }

    @Override
    public String getName() {
        return "Repoinit Conflict Validation";
    }

    /**
     * Executes the Repoinit conflict validation.
     * <p>
     * This method retrieves the feature from the context, validates it using
     * {@link RepoInitValidator}, and reports a warning if any conflicts are found.
     *
     * @param context analyser task context containing the feature to validate
     */
    @Override
    public void execute(final AnalyserTaskContext context) {
        Feature feature = context.getFeature();

        RepoInitValidationReport report = RepoInitValidator.validateRepoinit(feature);

        if (!report.hasConflicts()) {
            return;
        }

        List<String> messages = report.generate();
        messages.forEach(context::reportWarning);
    }
}
