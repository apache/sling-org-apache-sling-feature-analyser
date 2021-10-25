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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.jackrabbit.vault.validation.ValidationViolation;
import org.apache.jackrabbit.vault.validation.spi.ValidatorSettings;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.analyser.task.impl.contentpackage.PackageValidator;
import org.apache.sling.feature.scanner.ContentPackageDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This analyzer checks for bundles and configurations in packages
 */
public class CheckContentPackages implements AnalyserTask {
    Logger log = LoggerFactory.getLogger(this.getClass());
    
    @Override
    public String getName() {
        return "Content Package validation";
    }

    @Override
    public String getId() {
        return "content-packages";
    }

    @Override
    public void execute(final AnalyserTaskContext ctx) throws Exception {
        Map<String, ? extends ValidatorSettings> validatorSettings = new HashMap<>();
        for (final ContentPackageDescriptor cp : ctx.getFeatureDescriptor().getDescriptors(ContentPackageDescriptor.class)) {
            URL artifactFile = cp.getArtifactFile();
            if (artifactFile ==  null) {
                ctx.reportArtifactError(cp.getArtifact().getId(), "Content package " + cp.getName() + " is not resolved and can not be checked.");
            } else {
                validatePackage(ctx, cp, artifactFile, validatorSettings);
            }
        }
    }

    private void validatePackage(final AnalyserTaskContext ctx, final ContentPackageDescriptor cp, 
        URL artifactFile, Map<String, ? extends ValidatorSettings> validatorSettings) throws URISyntaxException {
        URI artifactURI = artifactFile.toURI();
        Path artifactPath = Paths.get(artifactURI);
        PackageValidator validator = new PackageValidator(artifactURI, validatorSettings);
        Collection<ValidationViolation> violations = validator.validate();
        reportViolations(ctx, cp, violations, artifactPath);
    }

    private void reportViolations(AnalyserTaskContext ctx, ContentPackageDescriptor cp,
            Collection<ValidationViolation> violations, Path artifactPath) {
        for (ValidationViolation violation : violations) {
            reportViolation(ctx, cp, violation, artifactPath);
        }
    }

    private void reportViolation(AnalyserTaskContext ctx, ContentPackageDescriptor cp, ValidationViolation violation, Path artifactPath) {
        String msg = getDetailMessage(violation, artifactPath);
        ArtifactId id = cp.getArtifact().getId();
        switch (violation.getSeverity()) {
        case ERROR:
            log.error(msg);
            ctx.reportArtifactError(id, msg);
            break;
        case WARN:
            log.warn(msg);
            ctx.reportArtifactWarning(id, msg);
            break;
        case INFO:
            log.info(msg);
            break;
        default:
            log.debug(msg);
            break;
        }
    }

    private static String getDetailMessage(ValidationViolation violation, Path baseDirectory) {
        StringBuilder message = new StringBuilder("ValidationViolation: \"" + getMessage(violation) + "\"");
        if (violation.getFilePath() != null) {
            message.append(", filePath=").append(baseDirectory.relativize(violation.getAbsoluteFilePath()));
        }
        if (violation.getNodePath() != null) {
            message.append(", nodePath=").append(violation.getNodePath());
        }
        if (violation.getLine() > 0) {
            message.append(", line=").append(violation.getLine());
        }
        if (violation.getColumn() > 0) {
            message.append(", column=").append(violation.getColumn());
        }
        return message.toString();
    }
    
    private static String getMessage(ValidationViolation violation) {
        StringBuilder message = new StringBuilder();
        if (violation.getValidatorId() != null) {
            message.append(violation.getValidatorId()).append(": ");
        }
        message.append(violation.getMessage());
        return message.toString();
    }
    
}
