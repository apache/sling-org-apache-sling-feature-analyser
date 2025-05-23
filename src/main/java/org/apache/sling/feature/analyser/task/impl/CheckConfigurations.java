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

import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.osgi.framework.Constants;

public class CheckConfigurations implements AnalyserTask {

    @Override
    public String getId() {
        return "configurations-basic";
    }

    @Override
    public String getName() {
        return "Basic checks for Configurations";
    }

    @Override
    public void execute(AnalyserTaskContext ctx) throws Exception {
        for (final Configuration cfg : ctx.getFeature().getConfigurations()) {
            // check that service ranking is of type integer
            final Object val = cfg.getProperties().get(Constants.SERVICE_RANKING);
            if (val != null && !(val instanceof Integer)) {
                ctx.reportConfigurationError(
                        cfg, "Service.ranking is not of type Integer. Use 'service.ranking:Integer' as the key.");
            }
        }
    }
}
