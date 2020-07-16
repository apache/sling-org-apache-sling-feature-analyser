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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;

/**
 * This analyser validates that the entries related to Apis Jar are valid.
 *
 * Current checks:
 *
 * <ol>
 *   <li>The {@code sourceId} property is a CSV list of valid artifact ids.</li>
 * </ol>
 *
 */
public class CheckApisJarsProperties implements AnalyserTask {

    /** Alternative SCM location. */
    private static final String SCM_LOCATION = "scm-location";

    /** Alternative IDs for artifact dependencies. */
    private static final String API_IDS = "api-ids";

    /** Alternative IDS to a source artifact. */
    private static final String SCM_IDS = "source-ids";

    /** Alternative classifier for the source artifact. */
    private static final String SCM_CLASSIFIER = "source-classifier";


    /** Links for javadocs. */
    private static final String JAVADOC_LINKS = "javadoc-links";

    @Override
    public String getId() {
        return "apis-jar";
    }

    @Override
    public String getName() {
        return "APIs jar properties check";
    }

    @Override
    public void execute(final AnalyserTaskContext ctx) throws Exception {
        for(final Artifact artifact : ctx.getFeature().getBundles()) {
            validateSourceInfo(ctx, artifact);
            checkIdValidity(ctx, artifact, SCM_IDS);
            checkIdValidity(ctx, artifact, API_IDS);
            checkJavadocLinks(ctx, artifact);
        }
    }

    private void checkIdValidity(final AnalyserTaskContext ctx, final Artifact a, final String propName) {
        final String sourceId = a.getMetadata().get(propName);
        if  ( sourceId != null ) {
            Arrays.stream(sourceId.split(","))
                .map( String::trim )
                .filter( el -> el.length() > 0)
                .forEach( el -> {
                    try {
                        // at the moment we can not validate the availability of the artifact since there is no access to Maven APIs
                        ArtifactId.parse(el);
                    } catch ( IllegalArgumentException e) {
                        ctx.reportError("Bundle " + a.getId().toMvnId() + " has invalid " + propName + " entry '" + el + "' : " + e.getMessage());
                    }
                });
        }
    }

    private void checkJavadocLinks(final AnalyserTaskContext ctx, final Artifact a) {
        final String value = a.getMetadata().get(JAVADOC_LINKS);
        if ( value != null ) {
            for(String v : value.split(",") ) {
                if ( v.endsWith("/") ) {
                    v = v.substring(0, v.length() - 1);
                }
                try {
                    new URL(v);
                } catch ( final MalformedURLException mue) {
                    ctx.reportError("Bundle " + a.getId().toMvnId() + " has invalid javadoc links URL : " + v);
                }
            }
        }
    }

    /**
     * Validate that only one source metadata is set
     */
    private void validateSourceInfo(final AnalyserTaskContext ctx, final Artifact artifact) {
        int count = 0;
        if ( artifact.getMetadata().get(SCM_LOCATION) != null ) {
            count++;
        }
        if ( artifact.getMetadata().get(SCM_CLASSIFIER) != null ) {
            count++;
        }
        if ( artifact.getMetadata().get(SCM_IDS) != null ) {
            count++;
        }
        if ( count > 1 ) {
            ctx.reportError("Bundle ".concat(artifact.getId().toMvnId())
                    .concat(" should either define ")
                    .concat(SCM_LOCATION).concat(", ")
                    .concat(SCM_CLASSIFIER).concat(", or")
                    .concat(SCM_IDS).concat(" - but only one of them."));
        }
    }
}
