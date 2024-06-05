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
import org.apache.sling.feature.Configurations;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.util.converter.Converters;

import java.util.Set;

public class CheckServiceUserMapping implements AnalyserTask {

    static final String SERVICE_USER_MAPPING_PID = "org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl";

    static final String FACTORY_PID = "org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended";

    static final String USER_MAPPING = "user.mapping";

    static final String CFG_WARN_ONLY_FOR_DEPRECATED_MAPPINGS = "warnOnlyForDeprecatedMappings";
    static final String CFG_WARN_ONLY_FOR_DEPRECATED_MAPPINGS_DEFAULT = Boolean.FALSE.toString();
    
    @Override
    public String getName() {
        return "Service User Mapping Check";
    }

    @Override
    public String getId() {
        return SERVICE_USER_MAPPING_PID;
    }

    @Override
    public void execute(final AnalyserTaskContext ctx) {
        final boolean warnOnlyForDeprecation = Boolean.parseBoolean(ctx.getConfiguration().getOrDefault(CFG_WARN_ONLY_FOR_DEPRECATED_MAPPINGS, CFG_WARN_ONLY_FOR_DEPRECATED_MAPPINGS_DEFAULT));

        // configuration
        Configurations configurations = ctx.getFeature().getConfigurations();
        final Configuration cfg = configurations.getConfiguration(SERVICE_USER_MAPPING_PID);
        if (cfg != null) {
            check(ctx, cfg, warnOnlyForDeprecation);
        }
        for (final Configuration c : configurations.getFactoryConfigurations(FACTORY_PID)) {
            check(ctx, c, warnOnlyForDeprecation);
        }
    }

    private static void check(final AnalyserTaskContext ctx, final Configuration cfg, final boolean warnOnlyForDeprecation) {
        final Object val = cfg.getConfigurationProperties().get(USER_MAPPING);
        if (val != null) {
            final String[] mappings = Converters.standardConverter().convert(val).to(String[].class);
            for (final String spec : mappings) {
                check(ctx, cfg, spec, warnOnlyForDeprecation);
            }
        }
    }

    private static void check(final @NotNull AnalyserTaskContext ctx, final @NotNull Configuration cfg, final @Nullable String spec, final boolean warnOnlyForDeprecation) {
        final String id = cfg.getPid();
        if (spec == null || spec.trim().isEmpty()) {
            ctx.reportConfigurationWarning(cfg, "Ignoring empty mapping in " + id);
            return;
        }

        final Mapping mapping = Mapping.parse(spec, ctx, cfg);
        if (mapping == null) {
            ctx.reportConfigurationError(cfg, String.format("Invalid service user mapping '%s' from %s", spec, id));
        } else if (mapping.isDeprecated()) {
            String msg = String.format("Deprecated service user mapping '%s' from %s", spec, id);
            if (warnOnlyForDeprecation) {
                ctx.reportConfigurationWarning(cfg, msg);
            } else {
                ctx.reportConfigurationError(cfg, msg);
            }
        }
    }

    /**
     * Parsing copied from sling-org-apache-sling-serviceusermapper.Mapping
     */
    private static class Mapping {

        private final String userName;
        private final Set<String> principalNames;
        private final String serviceName;
        private final String subServiceName;

        private static @Nullable Mapping parse(@NotNull final String spec, @NotNull final AnalyserTaskContext ctx, @NotNull final Configuration cfg) {
            final int colon = spec.indexOf(':');
            final int equals = spec.indexOf('=');

            if (colon == 0 || equals <= 0) {
                ctx.reportConfigurationWarning(cfg, "Invalid service user mapping: serviceName is required");
                return null;
            } else if (equals == spec.length() - 1) {
                ctx.reportConfigurationWarning(cfg, "Invalid service user mapping: userName or principalNames is required");
                return null;
            } else if (colon + 1 == equals) {
                ctx.reportConfigurationWarning(cfg, "Invalid service user mapping: serviceInfo must not be empty");
                return null;
            }

            final String serviceName;
            final String subServiceName;
            if (colon < 0 || colon > equals) {
                serviceName = spec.substring(0, equals);
                subServiceName = null;
            } else {
                serviceName = spec.substring(0, colon);
                subServiceName = spec.substring(colon + 1, equals);
            }

            final String userName;
            final Set<String> principalNames;
            String s = spec.substring(equals + 1);
            if (s.charAt(0) == '[' && s.charAt(s.length() - 1) == ']') {
                userName = null;
                principalNames = org.apache.sling.serviceusermapping.Mapping.extractPrincipalNames(s);
            } else {
                userName = s;
                principalNames = null;
            }
            return new Mapping(userName, principalNames, serviceName, subServiceName);
        }

        private Mapping(@Nullable final String userName, @Nullable final Set<String> principalNames, @NotNull final String serviceName, @Nullable final String subServiceName) {
            this.userName = userName;
            this.principalNames = principalNames;
            this.serviceName = serviceName;
            this.subServiceName = subServiceName;
        }

        private boolean isDeprecated() {
            return userName != null;
        }
    }
}

