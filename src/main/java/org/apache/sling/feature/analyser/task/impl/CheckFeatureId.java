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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;

public class CheckFeatureId implements AnalyserTask {

    static final String CONFIG_KEY_ACCEPTED_FEATURE_IDS = "accepted-feature-ids";

    @Override
    public String getId() {
        return "feature-id";
    }

    @Override
    public String getName() {
        return "Restrict feature id format";
    }

    @Override
    public void execute(AnalyserTaskContext ctx) throws Exception {
        Map<String, String> cfg = ctx.getConfiguration();
        String acceptedFeatureIds = cfg.get(CONFIG_KEY_ACCEPTED_FEATURE_IDS);
        if (acceptedFeatureIds == null) {
            // this is a comma-separated list of accepted 
            throw new IllegalArgumentException("Missing 'accepted-feature-ids' configuration for feature-id analyser.");
        }
        Collection<ArtifactId> acceptedArtifactIds = new ArrayList<>();
        for (String acceptedFeatureId : acceptedFeatureIds.split(",")) {
            try {
                acceptedArtifactIds.add(ArtifactId.fromMvnId(acceptedFeatureId));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid 'accepted-feature-ids' configuration for feature-id analyser, element '" + acceptedFeatureId + "' is not a valid maven coordinate string in format 'groupId:artifactId[:packaging[:classifier]]:version'", e);
            }
        }
        if (!matchesAnyOf(ctx.getFeature().getId(), acceptedArtifactIds)) {
            ctx.reportError("Feature " + ctx.getFeature().getId() + " does not match any of the accepted feature ids ");
        }
    }

    static boolean matchesAnyOf(ArtifactId artifactId, Collection<ArtifactId> expectedArtifactIds) {
        for (ArtifactId expectedArtifactId : expectedArtifactIds) {
            if (matches(artifactId, expectedArtifactId)) {
                return true;
            }
        }
        return false;
    }

    static boolean matches(ArtifactId artifactId, ArtifactId expectedArtifactId) {
        if (!expectedArtifactId.getGroupId().equals(artifactId.getGroupId()) && !expectedArtifactId.getGroupId().equals("*")) {
            return false;
        }
        if (!expectedArtifactId.getArtifactId().equals(artifactId.getArtifactId()) && !expectedArtifactId.getArtifactId().equals("*")) {
            return false;
        }
        if (!expectedArtifactId.getVersion().equals(artifactId.getVersion()) && !expectedArtifactId.getVersion().equals("*")) {
            return false;
        }
        if (!expectedArtifactId.getType().equals(artifactId.getType()) && !expectedArtifactId.getVersion().equals("*")) {
            return false;
        }
        // classifier is optional
        if (expectedArtifactId.getClassifier() != null && !expectedArtifactId.getClassifier().equals(artifactId.getClassifier()) && !expectedArtifactId.getClassifier().equals("*")) {
            return false;
        }
        return true;
    }

}
