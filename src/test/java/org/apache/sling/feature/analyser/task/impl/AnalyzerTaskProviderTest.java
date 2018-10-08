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

import static org.junit.Assert.assertEquals;
import static org.apache.sling.feature.analyser.task.AnalyzerTaskProvider.*;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.ServiceLoader;

import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.junit.Test;

public final class AnalyzerTaskProviderTest {

    private static int allTasks() {
        int size = 0;
        Iterator<AnalyserTask> iterator = ServiceLoader.load(AnalyserTask.class).iterator();
        while (iterator.hasNext()) {
            iterator.next();
            size++;
        }
        return size;
    }

    @Test
    public void testLoadAll() {
        int expected = allTasks();
        int actual = getTasks().length;
        assertEquals(expected, actual);

        actual = getTasksByIds(null, null).length;
        assertEquals(expected, actual);
    }

    @Test(expected = IOException.class)
    public void failOnLoadingFromNull() throws Exception {
        getTasksByClassName((String[])null);
    }

    @Test
    public void loadCheckApiRegionsonly() throws Exception {
        int expected = 1;
        int actual = getTasksByClassName("org.apache.sling.feature.analyser.task.impl.CheckApiRegions").length;
        assertEquals(expected, actual);
    }

    @Test
    public void includeCheckApiRegionsonly() throws Exception {
        int expected = 1;
        int actual = getTasksByIds(Collections.singleton("api-regions"), null).length;
        assertEquals(expected, actual);
    }

    @Test
    public void excludeCheckApiRegionsonly() throws Exception {
        int expected = allTasks() - 1;
        int actual = getTasksByIds(null, Collections.singleton("api-regions")).length;
        assertEquals(expected, actual);
    }

    @Test
    public void doesNotincludeAnything() throws Exception {
        int expected = 0;
        int actual = getTasksByIds(Collections.singleton("api-regions"), Collections.singleton("api-regions")).length;
        assertEquals(expected, actual);
    }

}
