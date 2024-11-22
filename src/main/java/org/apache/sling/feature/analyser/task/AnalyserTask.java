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

import org.osgi.annotation.versioning.ConsumerType;

/**
 * A analyser task analyses a specific part of the assembled
 * application. It can report errors and warnings.
 */
@ConsumerType
public interface AnalyserTask {

    /** A unique (short) ID.
     * @return the task ID.
     */
    default String getId() {
        return getClass().getName();
    }
    ;

    /** A human readable name to identify the task.
     * @return the task name.
     */
    default String getName() {
        return getClass().getSimpleName();
    }
    ;

    /** Execute the task.
     * @param ctx the task context.
     * @throws Exception when the task throws an exception.
     */
    void execute(AnalyserTaskContext ctx) throws Exception;
}
