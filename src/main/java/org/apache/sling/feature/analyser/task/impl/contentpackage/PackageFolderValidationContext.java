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
package org.apache.sling.feature.analyser.task.impl.contentpackage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;

import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.config.ConfigurationException;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.packaging.PackageInfo;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.apache.jackrabbit.vault.packaging.impl.DefaultPackageProperties;
import org.apache.jackrabbit.vault.util.Constants;
import org.apache.jackrabbit.vault.validation.spi.ValidationContext;

public class PackageFolderValidationContext implements ValidationContext {

    private final Path rootFolder;
    private final DefaultWorkspaceFilter filter;
    private final PackageProperties properties;

    public PackageFolderValidationContext(Path rootFolder) throws IOException, ConfigurationException {
        this.rootFolder = rootFolder;
        filter = new DefaultWorkspaceFilter();
        try (InputStream input = Files.newInputStream(rootFolder.resolve(Paths.get(Constants.META_DIR,Constants.FILTER_XML)))) {
            filter.load(input);
        }
        properties = DefaultPackageProperties.fromFile(rootFolder.resolve(Paths.get(Constants.META_DIR,Constants.PROPERTIES_XML)));
    }

    @Override
    public WorkspaceFilter getFilter() {
        return filter;
    }

    @Override
    public PackageProperties getProperties() {
        return properties;
    }

    @Override
    public ValidationContext getContainerValidationContext() {
        return null;
    }

    @Override
    public Path getPackageRootPath() {
        return rootFolder;
    }

    @Override
    public Collection<PackageInfo> getDependenciesPackageInfo() {
        return Collections.emptyList();
    }

}
