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

import java.io.StringReader;

import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.repoinit.parser.RepoInitParser;
import org.apache.sling.repoinit.parser.RepoInitParsingException;
import org.apache.sling.repoinit.parser.impl.RepoInitParserService;
import org.osgi.util.converter.Converters;

public class CheckRepoinit implements AnalyserTask {

    public static final String PID = "org.apache.sling.jcr.repoinit.impl.RepositoryInitializer";

    public static final String FACTORY_PID = "org.apache.sling.jcr.repoinit.RepositoryInitializer";

    @Override
    public String getName() {
        return "Repoinit Check";
    }

    @Override
    public String getId() {
        return "repoinit";
    }

    @Override
    public void execute(final AnalyserTaskContext ctx) {
        // check extension
        final Extension ext = ctx.getFeature().getExtensions().getByName(Extension.EXTENSION_NAME_REPOINIT);
        if (ext != null) {
            if (ext.getType() != ExtensionType.TEXT) {
                ctx.reportExtensionError(Extension.EXTENSION_NAME_REPOINIT, "Repoinit extension must be of type TEXT");
            } else {
                check(ctx, "extension", ext.getText());
            }
        }

        // configuration
        final Configuration cfg = ctx.getFeature().getConfigurations().getConfiguration(PID);
        if (cfg != null) {
            check(ctx, cfg, false);
        }
        for (final Configuration c : ctx.getFeature().getConfigurations()) {
            if (FACTORY_PID.equals(c.getFactoryPid())) {
                check(ctx, c, true);
            }
        }
    }

    private void check(final AnalyserTaskContext ctx, final Configuration cfg, final boolean supportsScripts) {
        // for now we only check scripts
        if (supportsScripts) {
            final Object val = cfg.getProperties().get("scripts");
            if (val != null) {
                final String[] scripts =
                        Converters.standardConverter().convert(val).to(String[].class);
                for (final String contents : scripts) {
                    check(ctx, "configuration ".concat(cfg.getPid()), contents);
                }
            }
        }
    }

    private void check(final AnalyserTaskContext ctx, final String id, final String contents) {
        final RepoInitParser parser = new RepoInitParserService();

        try {
            parser.parse(new StringReader(contents));
        } catch (RepoInitParsingException e) {
            ctx.reportExtensionError(
                    Extension.EXTENSION_NAME_REPOINIT,
                    "Parsing error in repoinit from ".concat(id).concat(" : ").concat(e.getMessage()));
        }
    }
}
