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
import org.junit.Before;
import org.junit.Test;

import static org.apache.sling.feature.analyser.task.impl.CheckServiceUserMapping.CFG_WARN_ONLY_FOR_DEPRECATED_MAPPINGS;
import static org.apache.sling.feature.analyser.task.impl.CheckServiceUserMapping.CFG_WARN_ONLY_FOR_DEPRECATED_MAPPINGS_DEFAULT;
import static org.apache.sling.feature.analyser.task.impl.CheckServiceUserMapping.USER_MAPPING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CheckServiceUserMappingTest {

    public static final String[] DEPRECATED_STATEMENTS = {
            "org.apache.sling.test_service:sub_service=sling-reader-service", 
            "org.apache.sling.test_service=sling-reader-service"};
    public static final String[] VALID_STATEMENTS = {
            "org.apache.sling.test_service:sub_service=[sling-reader-service]", 
            "org.apache.sling.test_service=[sling-reader-service, sling-writer-service],\n" +
                    "org.apache.sling.test_service:sub_service=[sling-reader-service]"};
    public static final String[] INVALID_STATEMENTS = {
            ":subservice=[sling-reader-service]",
            "=[sling-reader-service]", 
            "org.apache.sling.test_service:sub_service=", 
            "org.apache.sling.test_service:=[sling-reader-service]", 
            "org.apache.sling.test_service", 
            ":=[sling-reader-service]"};

    private AnalyserTaskContextImpl ctx;
    private AnalyserTask task;

    @Before
    public void setUp() throws Exception {
        ctx = new AnalyserTaskContextImpl();
        task = new CheckServiceUserMapping();
        task.execute(ctx);
        assertTrue(ctx.getErrors().isEmpty());
    }
    
    @Test
    public void testId() {
        assertEquals("serviceusermapping", task.getId());
    }
    
    @Test
    public void testName() {
        assertEquals("Service User Mapping Check", task.getName());
    }
    
    @Test
    public void testValidConfiguration() throws Exception {
        // create valid configuration
        for (String statement : VALID_STATEMENTS) {
            final Configuration cfg = new Configuration(CheckServiceUserMapping.SERVICE_USER_MAPPING_PID);
            cfg.getProperties().put(USER_MAPPING, statement);
            ctx.getFeature().getConfigurations().add(cfg);

            task.execute(ctx);
            assertTrue(ctx.getErrors().isEmpty());
        }
    }

    @Test
    public void testValidFactoryConfiguration() throws Exception {
        // create valid configuration
        for (String statement : VALID_STATEMENTS) {
            final Configuration cfg = new Configuration(CheckServiceUserMapping.FACTORY_PID.concat("~name"));
            cfg.getProperties().put(USER_MAPPING, statement);
            ctx.getFeature().getConfigurations().add(cfg);

            task.execute(ctx);
            assertTrue(ctx.getErrors().isEmpty());
        }
    }

    @Test
    public void testEmptyConfiguration() throws Exception {
        final Configuration cfg = new Configuration(CheckServiceUserMapping.FACTORY_PID.concat("~name"));
        cfg.getProperties().put(USER_MAPPING, "");
        ctx.getFeature().getConfigurations().add(cfg);

        task.execute(ctx);
        assertTrue(ctx.getErrors().isEmpty());
    }
    
    @Test 
    public void testInvalidConfiguration() throws Exception {
        for (String statement : INVALID_STATEMENTS) {
            final Configuration cfg = new Configuration(CheckServiceUserMapping.FACTORY_PID.concat("~name"));
            cfg.getProperties().put(USER_MAPPING, statement);
            ctx.getFeature().getConfigurations().add(cfg);
        }

        task.execute(ctx);
        assertEquals(6, ctx.getErrors().size());
    }

    @Test
    public void testDeprecatedMappingInvalied() throws Exception {
        assertFalse(Boolean.parseBoolean(ctx.getConfiguration().getOrDefault(CFG_WARN_ONLY_FOR_DEPRECATED_MAPPINGS, CFG_WARN_ONLY_FOR_DEPRECATED_MAPPINGS_DEFAULT)));
        
        for (String statement : DEPRECATED_STATEMENTS) {
            final Configuration cfg = new Configuration(CheckServiceUserMapping.FACTORY_PID.concat("~name"));
            cfg.getProperties().put(USER_MAPPING, statement);
            ctx.getFeature().getConfigurations().add(cfg);
        }

        task.execute(ctx);
        assertEquals(2, ctx.getErrors().size());
    }

    @Test 
    public void testDeprecatedMappingWarnOnly() throws Exception {
        ctx.putConfigurationValue(CFG_WARN_ONLY_FOR_DEPRECATED_MAPPINGS, Boolean.TRUE.toString());

        assertTrue(Boolean.parseBoolean(ctx.getConfiguration().getOrDefault(CFG_WARN_ONLY_FOR_DEPRECATED_MAPPINGS, CFG_WARN_ONLY_FOR_DEPRECATED_MAPPINGS_DEFAULT)));
        for (String statement : DEPRECATED_STATEMENTS) {
            final Configuration cfg = new Configuration(CheckServiceUserMapping.FACTORY_PID.concat("~name"));
            cfg.getProperties().put(USER_MAPPING, statement);
            ctx.getFeature().getConfigurations().add(cfg);
        }

        task.execute(ctx);
        assertTrue(ctx.getErrors().isEmpty());
    }
}
