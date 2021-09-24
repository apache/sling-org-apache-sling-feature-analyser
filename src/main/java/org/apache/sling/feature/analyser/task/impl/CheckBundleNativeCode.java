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

import java.util.jar.Manifest;

import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.osgi.framework.Constants;

public class CheckBundleNativeCode implements AnalyserTask {

    @Override
    public String getName() {
        return "Bundle Native Code Check";
    }

    @Override
    public String getId() {
        return "bundle-nativecode";
    }

    @Override
    public void execute(final AnalyserTaskContext ctx) {
        for(final BundleDescriptor descriptor : ctx.getFeatureDescriptor().getBundleDescriptors()) {
            final Manifest mf = descriptor.getManifest();

            final String nativeCode = mf.getMainAttributes().getValue(Constants.BUNDLE_NATIVECODE);
            if ( nativeCode != null ) {
                ctx.reportArtifactError(descriptor.getArtifact().getId(), "Found native code instruction in bundle: ".concat(nativeCode) );
            }
        }
    }
}
