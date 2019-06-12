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

import java.io.IOException;
import java.util.Collections;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.MatchingRequirement;
import org.apache.sling.feature.builder.ArtifactProvider;
import org.apache.sling.feature.scanner.ContainerDescriptor;
import org.apache.sling.feature.scanner.spi.ExtensionScanner;

public class RepoInitScanner implements ExtensionScanner {
    private static final MatchingRequirement REQUIREMENT_REPOINIT = new MatchingRequirementImpl(null,
            "osgi.implementation",
            Collections.singletonMap("filter", "(&(osgi.implementation=org.apache.sling.jcr.repoinit)(version>=1.0)(!(version>=2.0)))"),
                    null);

    @Override
    public String getId() {
        return Extension.EXTENSION_NAME_REPOINIT;
    }

    @Override
    public String getName() {
        return "Apache Sling Repoinit Scanner";
    }

    @Override
    public ContainerDescriptor scan(final Feature feature,
            final Extension extension,
            final ArtifactProvider provider)
    throws IOException {
        if (!Extension.EXTENSION_NAME_REPOINIT.equals(extension.getName())) {
            return null;
        }
        if ( extension.getType() != ExtensionType.TEXT ) {
            return null;
        }

        final ContainerDescriptor cd = new ContainerDescriptor(feature.getId().toMvnId() + "(" + getId() + ")") {};

        cd.getRequirements().add(REQUIREMENT_REPOINIT);

        cd.lock();

        return cd;
    }
}
