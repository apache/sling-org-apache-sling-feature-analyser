/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.feature.scanner.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DeleteDirectoryHook extends Thread {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final List<File> toBeDeletedList = new ArrayList<>();

    public void markToBeDeleted(File toBeDeleted) {
        if (toBeDeleted != null && toBeDeleted.exists()) {
            toBeDeletedList.add(toBeDeleted);
        }
    }

    @Override
    public void run() {
        for (File toBeDeleted : toBeDeletedList) {
            logger.debug("Deleting recursively " + toBeDeleted + "...");

            if (!deleteRecursive(toBeDeleted)) {
                logger.warn("Somethign went wrong when deleting directory "
                            + toBeDeleted
                            + " please verify the current user has enough rights to delete such dir.");
            } else {
                logger.debug(toBeDeleted + " successfully deleted");
            }
        }
    }

    private static boolean deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] childs = file.listFiles();
            if (childs != null) {
                for (File child : childs) {
                    if (!deleteRecursive(child)) {
                        return false;
                    }
                }
                return file.delete();
            }
            else {
                return false;
            }
        }
        else {
            return file.delete();
        }
    }

}
