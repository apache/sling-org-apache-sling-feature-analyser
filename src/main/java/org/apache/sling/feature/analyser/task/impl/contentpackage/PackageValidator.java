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
package org.apache.sling.feature.analyser.task.impl.contentpackage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.fs.io.ZipArchive;
import org.apache.jackrabbit.vault.util.Constants;
import org.apache.jackrabbit.vault.validation.ValidationExecutor;
import org.apache.jackrabbit.vault.validation.ValidationExecutorFactory;
import org.apache.jackrabbit.vault.validation.ValidationViolation;
import org.apache.jackrabbit.vault.validation.spi.ValidationMessageSeverity;
import org.apache.jackrabbit.vault.validation.spi.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validate a package using filevault validation API.
 * This class contains code from filevault package maven plugin. 
 */
public class PackageValidator {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    
    private final ValidationExecutorFactory validationExecutorFactory;

    private final URI artifactURI;

    private final Collection<ValidationViolation> messages;

    private ArchiveValidationContextImpl context;

    private ValidationExecutor executor;
    
    public PackageValidator(URI artifactURI) {
        this.artifactURI = artifactURI;
        validationExecutorFactory = new ValidationExecutorFactory(
                this.getClass().getClassLoader());
        messages = new LinkedList<>();
    }

    public Collection<ValidationViolation> validate() {
        Path artifactPath = Paths.get(artifactURI);
        try (Archive archive = new ZipArchive(new File(artifactURI))) {
            archive.open(true);
            context = new ArchiveValidationContextImpl(archive, artifactPath);
            executor = validationExecutorFactory.createValidationExecutor(context, false, false,
                    null);
            if (executor != null) {
                printUsedValidators(true);
                validateArchive(archive, artifactPath);
            } else {
                throw new RuntimeException("No registered validators found!");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return messages;
    }
    
    private void validateArchive(Archive archive, Path path) throws IOException {
        validateEntry(archive, archive.getRoot(), Paths.get(""), path);
        messages.addAll(executor.done());
    }
    
    private void validateEntry(Archive archive, Archive.Entry entry, Path entryPath, Path packagePath) throws IOException {
        // sort children to make sure that .content.xml comes first!
        List<Archive.Entry> sortedEntryList = new ArrayList<>(entry.getChildren());
        sortedEntryList.sort(Comparator.comparing(Archive.Entry::getName, new DotContentXmlFirstComparator()));
        
        for (Archive.Entry childEntry : sortedEntryList) {
            if (childEntry.isDirectory()) {
                validateInputStream(null, entryPath.resolve(childEntry.getName()), packagePath);
                validateEntry(archive, childEntry, entryPath.resolve(childEntry.getName()), packagePath);
            } else {
                try (InputStream input = archive.openInputStream(childEntry)) {
                    validateInputStream(input, entryPath.resolve(childEntry.getName()), packagePath);
                }
            }
        }
    }
    
    private void validateInputStream(InputStream inputStream, Path entryPath, Path packagePath) throws IOException {
        if (entryPath.startsWith(Constants.META_INF)) {
            messages.addAll(executor.validateMetaInf(inputStream, Paths.get(Constants.META_INF).relativize(entryPath), packagePath.resolve(Constants.META_INF)));
        } else if (entryPath.startsWith(Constants.ROOT_DIR)) {
            // strip off jcr_root
            Path relativeJcrPath = Paths.get(Constants.ROOT_DIR).relativize(entryPath);
            messages.addAll(executor.validateJcrRoot(inputStream, relativeJcrPath, packagePath.resolve(Constants.ROOT_DIR)));
        } else {
            messages.add(new ValidationViolation(ValidationMessageSeverity.WARN, "Found unexpected file outside of " + Constants.ROOT_DIR + " and " + Constants.META_INF, entryPath, packagePath, null, 0,0, null));
        }
    }
    
    private void printUsedValidators(boolean printUnusedValidators) {
        String packageType = context.getProperties().getPackageType() != null ? context.getProperties().getPackageType().toString() : "unknown";
        int numValidators = executor.getAllValidatorsById().entrySet().size();
        log.info("Using {} validators for package of type {}: {}", numValidators, packageType, getValidatorNames());
        if (printUnusedValidators) {
            Map<String, Validator> unusedValidatorsById = executor.getUnusedValidatorsById();
            if (!unusedValidatorsById.isEmpty()) {
                String validatorNames = StringUtils.join(unusedValidatorsById.keySet(), ".");
                log.warn("There are unused validators among those which are not executed: {}", validatorNames);
            }
        }
    }
    
    private String getValidatorNames() {
        return executor.getAllValidatorsById().entrySet().stream()
                .map(this::describeValidator)
                .collect(Collectors.joining(", "));
    }
    
    private String describeValidator(Map.Entry<String, Validator> validatorById) {
        String validatorClassName = validatorById.getValue().getClass().getName();
        return validatorById.getKey() + " (" + validatorClassName + ")";
    }
    
    /** 
     * Comparator on file names (excluding paths) which makes sure that the files named {@code .content.xml} come first. Other file names are ordered lexicographically. 
     */
    static final class DotContentXmlFirstComparator implements Comparator<String> {
        @Override
        public int compare(String s1, String s2) {
            if (Constants.DOT_CONTENT_XML.equals(s1) && Constants.DOT_CONTENT_XML.equals(s2)) {
                return 0;
            } else if (Constants.DOT_CONTENT_XML.equals(s1)) {
                return -1;
            } else if (Constants.DOT_CONTENT_XML.equals(s2)) {
                return 1;
            }
            return s1.compareTo(s2);
        }
    }
}
