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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import org.apache.jackrabbit.vault.validation.ValidationViolation;
import org.apache.jackrabbit.vault.validation.spi.ValidationMessageSeverity;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.analyser.task.impl.contentpackage.PackageValidator;
import org.apache.sling.feature.scanner.ContentPackageDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This analyser checks for bundles and configurations in packages
 */
public class CheckContentPackages implements AnalyserTask {
    Logger log = LoggerFactory.getLogger(this.getClass());
    
    @Override
    public String getName() {
        return "Content Package xml syntax check";
    }

    @Override
    public String getId() {
        return "content-packages-syntax";
    }

    @Override
    public void execute(final AnalyserTaskContext ctx)
            throws Exception {

        for (final ContentPackageDescriptor cp : ctx.getFeatureDescriptor().getDescriptors(ContentPackageDescriptor.class)) {
            URL artifactFile = cp.getArtifactFile();
            if (artifactFile ==  null) {
                ctx.reportArtifactError(cp.getArtifact().getId(), "Content package " + cp.getName() + " is not resolved and can not be checked.");
            } else {
                checkPackageSyntax(ctx, cp, artifactFile);
            }
        }
    }

    private void checkPackageSyntax(final AnalyserTaskContext ctx, final ContentPackageDescriptor cp, URL artifactFile) {
        try {
            URI artifactURI = artifactFile.toURI();
            Path artifactPath = Paths.get(artifactURI);
            PackageValidator validator = new PackageValidator(artifactURI);
            Collection<ValidationViolation> violations = validator.validate();
            printMessages(violations, artifactPath);
            reportViolations(ctx, cp, violations, artifactPath);
        } catch (URISyntaxException e1) {
            throw new RuntimeException(e1);
        }
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
        if (violation.getSeverity() == ValidationMessageSeverity.WARN) {
            ctx.reportArtifactWarning(id, msg);
        }
        if (violation.getSeverity() == ValidationMessageSeverity.ERROR) {
            ctx.reportArtifactError(id, msg);
        }
    }

    /**
     * 
     * @param violations
     * @param log
     * @param buildContext
     * @param baseDirectory the directory to which all absolute paths should be made relative (i.e. the Maven basedir)
     * @throws IOException 
     */
    public void printMessages(Collection<ValidationViolation> violations, Path baseDirectory) {
        for (ValidationViolation violation : violations) {
            switch (violation.getSeverity()) {
            case ERROR:
                log.error(getDetailMessage(violation, baseDirectory));
                if (violation.getThrowable() != null) {
                    log.debug("", violation.getThrowable());
                }
                break;
            case WARN:
                log.warn(getDetailMessage(violation, baseDirectory));
                if (violation.getThrowable() != null) {
                    log.debug("", violation.getThrowable());
                }
                break;
            case INFO:
                log.info(getDetailMessage(violation, baseDirectory));
                break;
            default:
                log.debug(getDetailMessage(violation, baseDirectory));
                break;
            }
        }
    }
    
    private static String getDetailMessage(ValidationViolation violation, Path baseDirectory) {
        StringBuilder message = new StringBuilder("ValidationViolation: ");
        message.append("\"").append(getMessage(violation)).append("\"");
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
