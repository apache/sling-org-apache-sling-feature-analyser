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
package org.apache.sling.feature.analyser.task.impl;

import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.PackageInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;

public class ListExportedPackages implements AnalyserTask {

    @Override
    public void execute(AnalyserTaskContext ctx) throws Exception {
        SortedSet<String> packages = new TreeSet<>();

        for (BundleDescriptor bd : ctx.getDescriptor().getBundleDescriptors()) {
            for (PackageInfo p : bd.getExportedPackages()) {
                packages.add(p.getName());
            }
        }

        File f = new File("packages.txt");
        if (f.exists()) {
            throw new IOException("File " + f.getAbsolutePath() +
                    " already exists. This plugin does not overwrite an existing file");
        }

        try (FileWriter fw = new FileWriter(f)) {
            for (String p : packages) {
                fw.write(p);
                fw.write(System.getProperty("line.separator"));
            }
        }
        ctx.reportWarning("Finished writing exported packages to " + f.getAbsolutePath());;
    }
}
