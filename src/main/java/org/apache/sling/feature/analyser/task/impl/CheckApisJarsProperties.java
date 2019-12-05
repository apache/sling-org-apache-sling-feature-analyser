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

import java.util.Arrays;
import java.util.List;

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
 *  <li>The <tt>sourceId</tt> property is a CSV list of valid artifact ids.</li>
 * </ol>
 *
 */
public class CheckApisJarsProperties implements AnalyserTask {
    
    // TODO - also defined in ApisJarMojo
    private static final String SOURCE_ID = "sourceId";

    @Override
    public String getId() {
        return "apis-jar";
    }
    
    @Override
    public String getName() {
        return "APIs jar properties check";
    }
    
    @Override
    public void execute(AnalyserTaskContext ctx) throws Exception {

        ctx.getFeature().getBundles().getBundlesByStartOrder().values().stream()
            .flatMap( List::stream )
            .filter ( artifact -> artifact.getMetadata().containsKey(SOURCE_ID) )
            .forEach( artifact -> checkSourceIdValidity(artifact, ctx));
    }
    
    private void checkSourceIdValidity(Artifact a, AnalyserTaskContext ctx) {
        String sourceId = a.getMetadata().get(SOURCE_ID);
        Arrays.stream(sourceId.split(","))
            .map( String::trim )
            .filter( el -> el.length() > 0)
            .forEach( el -> {
                try {
                    // at the moment we can not validate the availability of the artifact since there is no access to Maven APIs
                    ArtifactId.parse(el);
                } catch ( IllegalArgumentException e) {
                    ctx.reportError("Bundle " + a.getId() + " has invalid sourceId entry '" + el + "' : " + e.getMessage());
                }
            });
        
    }

}
