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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import org.apache.jackrabbit.vault.validation.ValidationViolation;
import org.apache.jackrabbit.vault.validation.impl.util.ValidatorSettingsImpl;
import org.apache.jackrabbit.vault.validation.spi.ValidationMessageSeverity;
import org.apache.jackrabbit.vault.validation.spi.ValidatorFactory;
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
    // Comma separated list of validator ids to enable
    static final String ENABLED_VALIDATORS = "enabled-validators";
    static final String MAX_REPORT_LEVEL = "max-report-level";
    private Logger log = LoggerFactory.getLogger(this.getClass());
    
    @Override
    public String getName() {
        return "Content Package validation";
    }

    @Override
    public String getId() {
        return "content-packages-validation";
    }

    @Override
    public void execute(final AnalyserTaskContext ctx) throws Exception {
        String enabledValidators = ctx.getConfiguration().get(ENABLED_VALIDATORS);
        Map<String, ValidatorSettings> validatorSettings = enableValidators(enabledValidators);
        String maxReportLevelSt = ctx.getConfiguration().get(MAX_REPORT_LEVEL);
        ValidationMessageSeverity maxReportLevel = maxReportLevelSt == null ? ValidationMessageSeverity.WARN : ValidationMessageSeverity.valueOf(maxReportLevelSt); 
        for (final ContentPackageDescriptor cp : ctx.getFeatureDescriptor().getDescriptors(ContentPackageDescriptor.class)) {
            URL artifactFile = cp.getArtifactFile();
            if (artifactFile ==  null) {
                ctx.reportArtifactError(cp.getArtifact().getId(), "Content package " + cp.getName() + " is not resolved and can not be checked.");
            } else {
                validatePackage(ctx, cp, artifactFile, validatorSettings, maxReportLevel);
            }
        }
    }

    private Map<String, ValidatorSettings> enableValidators(String enabledValidators) {
        ServiceLoader<ValidatorFactory> validatorFactories = ServiceLoader.load(ValidatorFactory.class, PackageValidator.class.getClassLoader());

        Map<String, ValidatorSettings> validatorSettings = new HashMap<>();
        Set<String> enabledValidatorsSet = split(enabledValidators);
        for (ValidatorFactory validatorFactory : validatorFactories) {
            String validatorId = validatorFactory.getId();
            boolean enabled = enabledValidatorsSet.contains(validatorId);
            validatorSettings.put(validatorId, new ValidatorSettingsImpl(!enabled));
        }
        return validatorSettings;
    }

    private Set<String> split(String enabledValidators) {
        HashSet<String> enabled = new HashSet<String>();
        if (enabledValidators != null) {
            List<String> enabledList = Arrays.asList(enabledValidators.split(","));
            enabled.addAll(enabledList);
        }
        return enabled;
    }

    private void validatePackage(final AnalyserTaskContext ctx, 
            final ContentPackageDescriptor cp,
            URL artifactFile, Map<String, ValidatorSettings> validatorSettings, 
            ValidationMessageSeverity maxReportLevel) throws URISyntaxException, IOException {
        URI artifactURI = artifactFile.toURI();
        Path artifactPath = Paths.get(artifactURI);
        PackageValidator validator = new PackageValidator(artifactURI, validatorSettings);
        Collection<ValidationViolation> violations = validator.validate();
        reportViolations(ctx, cp, violations, artifactPath, maxReportLevel);
    }

    private void reportViolations(AnalyserTaskContext ctx, ContentPackageDescriptor cp,
            Collection<ValidationViolation> violations, Path artifactPath, ValidationMessageSeverity maxReportLevel) {
        for (ValidationViolation violation : violations) {
            ValidationMessageSeverity severity = getMin(maxReportLevel, violation.getSeverity());
            reportViolation(ctx, cp, violation, artifactPath, severity);
        }
    }

    private ValidationMessageSeverity getMin(ValidationMessageSeverity a, ValidationMessageSeverity b) {
        return a.compareTo(b) < 0 ? a : b;
    }

    private void reportViolation(AnalyserTaskContext ctx, ContentPackageDescriptor cp, ValidationViolation violation, 
            Path artifactPath, ValidationMessageSeverity severity) {
        String msg = getDetailMessage(violation, artifactPath);
        ArtifactId id = cp.getArtifact().getId();
        switch (severity) {
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
