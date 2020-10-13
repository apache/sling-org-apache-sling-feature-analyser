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

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.PackageInfo;

public class CheckUnusedBundles implements AnalyserTask {

    @Override
    public String getName() {
        return "Unused Bundle Check";
    }

    @Override
    public String getId() {
        return "check-unused-bundles";
    }

    @Override
    public void execute(final AnalyserTaskContext ctx) throws IOException {
        // iterate over all bundles
        for(final BundleDescriptor info : ctx.getFeatureDescriptor().getBundleDescriptors()) {

            if ( !info.getExportedPackages().isEmpty() ) {

                final Set<PackageInfo> exports = new HashSet<>(info.getExportedPackages());
                
                // find importing bundle
                for(final BundleDescriptor inner : ctx.getFeatureDescriptor().getBundleDescriptors()) {
                    if ( inner == info ) {
                        continue;
                    }

                    final Iterator<PackageInfo> iter = exports.iterator();
                    while ( iter.hasNext() ) {
                        final PackageInfo expPck = iter.next();

                        boolean found = false;
                        for(final PackageInfo impPck : inner.getImportedPackages()) {
                            
                            if ( expPck.getName().equals(impPck.getName())) {
                                if ( impPck.getVersion() == null || impPck.getPackageVersionRange().includes(expPck.getPackageVersion()) ) {
                                    found = true;
                                    break;
                                }
                            }
                        }

                        if ( found ) {
                            iter.remove();
                        }
     
                    }

                    if ( exports.isEmpty() ) {
                        break;
                    }
                }

                if ( exports.size() == info.getExportedPackages().size() ) {
                    ctx.reportWarning("Exports from bundle ".concat(info.getArtifact().getId().toMvnId()).concat(" are not imported by any other bundle."));
                }

            }
        }
    }
}