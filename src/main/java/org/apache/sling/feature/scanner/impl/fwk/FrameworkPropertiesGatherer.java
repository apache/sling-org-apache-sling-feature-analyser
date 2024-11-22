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
package org.apache.sling.feature.scanner.impl.fwk;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

/**
 * This class launches an OSGi framework and obtains the system packages and capabilities provided by it.
 * It takes 2 arguments: <ul>
 *
 *  <li>1st argument: the file name of a properties file containing the properties to use to initialise the framework
 *  <li>2nd argument: the file name to write the result into. This will also be written in the form of a properties file.
 *
 * </ul>After obtaining the values, the launched framework is stopped.
 */
public class FrameworkPropertiesGatherer {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.print("usage: FrameworkPropertiesGatherer <in props filename> <out props filename>");
            System.exit(1);
        }

        Properties inProps = new Properties();
        try (Reader reader = new FileReader(args[0])) {
            inProps.load(reader);
        }

        ServiceLoader<FrameworkFactory> ldr = ServiceLoader.load(FrameworkFactory.class);
        FrameworkFactory ff = ldr.iterator().next();

        @SuppressWarnings({"unchecked", "rawtypes"})
        Framework fwk = ff.newFramework((Map) inProps);

        fwk.init();
        fwk.start();

        // The headers will contain the computed export packages and framework capabilities.
        Dictionary<String, String> headers = fwk.getHeaders();

        Properties outProps = new Properties();
        outProps.put(Constants.FRAMEWORK_SYSTEMPACKAGES, headers.get(Constants.EXPORT_PACKAGE));
        outProps.put(Constants.FRAMEWORK_SYSTEMCAPABILITIES, headers.get(Constants.PROVIDE_CAPABILITY));

        fwk.stop();

        try (Writer writer = new FileWriter(args[1])) {
            outProps.store(writer, "Framework exports and capabilities");
        }

        fwk.waitForStop(1000);
        System.exit(0); // To be sure
    }
}
