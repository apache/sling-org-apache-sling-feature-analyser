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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.scanner.BundleDescriptor;


public class CheckDuplicateSymbolicName implements AnalyserTask {

    @Override
    public String getName() {
        return "Duplicate Symbolic Name";
    }

    @Override
    public String getId() {
        return "duplicate-symbolic-names";
    }

    private SortedMap<String, Set<ArtifactId>> createBundleMap(final AnalyserTaskContext ctx) {
        // build a map of bundles by symbolic name

        final SortedMap<String, Set<ArtifactId>> bundleMap = new TreeMap<>();
        for(final BundleDescriptor desc : ctx.getFeatureDescriptor().getBundleDescriptors()) {
            final String key = desc.getBundleSymbolicName();
            final Set<ArtifactId> set = bundleMap.computeIfAbsent(key, id -> new HashSet<>());
            set.add(desc.getArtifact().getId());
        }
        return bundleMap;
    }

    @Override
    public void execute(final AnalyserTaskContext ctx) throws Exception {
        final SortedMap<String, Set<ArtifactId>> bundleMap = createBundleMap(ctx);

        for(final Map.Entry<String, Set<ArtifactId>> entry : bundleMap.entrySet()) {
            if ( entry.getValue().size() > 1 ) {
                final StringBuilder sb = new StringBuilder();
                sb.append("Duplicate symbolic name ");
                sb.append(entry.getKey());
                sb.append(" : ");
                boolean first = true;
                for(final ArtifactId id : entry.getValue()) {
                    if ( first ) first = false; else sb.append(", ");
                    sb.append(id.toMvnId());
                }
                ctx.reportError(sb.toString());
            }
        }
    }
}
