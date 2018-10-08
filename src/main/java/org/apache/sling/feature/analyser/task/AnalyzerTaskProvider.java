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
package org.apache.sling.feature.analyser.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import org.apache.sling.feature.analyser.Analyser;

public final class AnalyzerTaskProvider {

    private AnalyzerTaskProvider() {
        // this class must not be instantiated directly
    }

    public static AnalyserTask[] getTasks() {
        return getTasksByIds(null, null);
    }

    // includes can be null, means "include everything"
    // excludes can be null, means "do not exclude anything"
    // if both includes and excludes are null, method mehaves like getTasks()
    public static AnalyserTask[] getTasksByIds(Set<String> includes, Set<String> excludes) {
        final ServiceLoader<AnalyserTask> loader = ServiceLoader.load(AnalyserTask.class);
        final List<AnalyserTask> list = new ArrayList<>();
        for(final AnalyserTask task : loader) {
            boolean included = includes != null ? includes.contains(task.getId()) : true;
            boolean excluded = excludes != null ? excludes.contains(task.getId()) : false;

            if (included && !excluded) {
                list.add(task);
            }
        }
        return list.toArray(new AnalyserTask[list.size()]);
    }

    // Get tasks from class names
    public static AnalyserTask[] getTasksByClassName(String...taskClassNames) throws IOException {
        if (taskClassNames == null) {
            throw new IOException("Impossible to load Tasks from a null string array");
        }

        List<AnalyserTask> list = new ArrayList<>();
        for (String cls : taskClassNames) {
            try {
                AnalyserTask task = (AnalyserTask) Analyser.class.getClassLoader().loadClass(cls).newInstance();
                list.add(task);
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
        return list.toArray(new AnalyserTask[list.size()]);
    }

}
