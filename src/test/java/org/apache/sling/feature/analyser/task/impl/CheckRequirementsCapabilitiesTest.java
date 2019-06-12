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

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

import org.apache.felix.utils.resource.CapabilityImpl;
import org.apache.felix.utils.resource.RequirementImpl;
import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.MatchingRequirement;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.FeatureDescriptor;
import org.apache.sling.feature.scanner.impl.BundleDescriptorImpl;
import org.apache.sling.feature.scanner.impl.FeatureDescriptorImpl;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;


public class CheckRequirementsCapabilitiesTest {
    @Test
    public void testCheckRequirementsCapabilitiesAllOk() throws Exception {
        File f = new File(getClass().getResource("/test-bundle5.jar").getFile());

        BundleDescriptor bd1 = new BundleDescriptorImpl(
                new Artifact(ArtifactId.fromMvnId("g:b1:1.2.0")),
                f.toURI().toURL(), 7);

        Feature feature = new Feature(ArtifactId.fromMvnId("a:b:1"));

        Capability cap1 = new CapabilityImpl(null,
                "org.foo.blah", Collections.emptyMap(),
                Collections.singletonMap("abc", "def"));
        Capability cap2 = new CapabilityImpl(null,
                "org.foo.bar", Collections.emptyMap(),
                Collections.singletonMap("abc", "def"));
        MatchingRequirement req = new MatchingRequirementImpl(null,
                "org.zzz", "(&(zzz=aaa)(qqq=123))");

        feature.getCapabilities().addAll(Arrays.asList(cap1, cap2));
        feature.getRequirements().add(req);

        FeatureDescriptor fd = new FeatureDescriptorImpl(feature);
        fd.getBundleDescriptors().add(bd1);

        AnalyserTaskContext ctx = Mockito.mock(AnalyserTaskContext.class);
        Mockito.when(ctx.getFeature()).thenReturn(feature);
        Mockito.when(ctx.getFeatureDescriptor()).thenReturn(fd);

        CheckRequirementsCapabilities crc = new CheckRequirementsCapabilities();
        crc.execute(ctx);

        Mockito.verify(ctx, Mockito.never()).reportError(Mockito.anyString());
    }

    @Test
    public void testCheckRequirementsCapabilitiesMissingFromBundle() throws Exception {
        File f = new File(getClass().getResource("/test-bundle5.jar").getFile());

        BundleDescriptor bd1 = new BundleDescriptorImpl(
                new Artifact(ArtifactId.fromMvnId("g:b1:1.2.0")),
                f.toURI().toURL(), 7);

        Feature feature = new Feature(ArtifactId.fromMvnId("a:b:1"));
        FeatureDescriptor fd = new FeatureDescriptorImpl(feature);
        fd.getBundleDescriptors().add(bd1);

        AnalyserTaskContext ctx = Mockito.mock(AnalyserTaskContext.class);
        Mockito.when(ctx.getFeature()).thenReturn(feature);
        Mockito.when(ctx.getFeatureDescriptor()).thenReturn(fd);

        CheckRequirementsCapabilities crc = new CheckRequirementsCapabilities();
        crc.execute(ctx);

        Mockito.verify(ctx).reportError(Mockito.contains("org.foo.bar"));
    }

    @Test
    public void testCheckRequirementsCapabilitiesMissingFromFeature() throws Exception {
        Feature feature = new Feature(ArtifactId.fromMvnId("a:b:1"));

        Capability cap1 = new CapabilityImpl(null,
                "org.foo.blah", Collections.emptyMap(),
                Collections.singletonMap("abc", "def"));
        Capability cap2 = new CapabilityImpl(null,
                "org.foo.bar", Collections.emptyMap(),
                Collections.singletonMap("abc", "def"));
        MatchingRequirement req = new MatchingRequirementImpl(null,
                "org.zzz", "(&(zzz=aaa)(qqq=123))");

        feature.getCapabilities().addAll(Arrays.asList(cap1, cap2));
        feature.getRequirements().add(req);

        FeatureDescriptor fd = new FeatureDescriptorImpl(feature);

        AnalyserTaskContext ctx = Mockito.mock(AnalyserTaskContext.class);
        Mockito.when(ctx.getFeature()).thenReturn(feature);
        Mockito.when(ctx.getFeatureDescriptor()).thenReturn(fd);

        CheckRequirementsCapabilities crc = new CheckRequirementsCapabilities();
        crc.execute(ctx);

        Mockito.verify(ctx).reportError(Mockito.contains("org.zzz"));
    }

    private static class MatchingRequirementImpl extends RequirementImpl implements MatchingRequirement {

        public MatchingRequirementImpl(Resource res, String ns, String filter) {
            super(res, ns, filter);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || !(o instanceof RequirementImpl)) {
                return false;
            }
            final RequirementImpl that = (RequirementImpl) o;
            return Objects.equals(resource, that.getResource()) && Objects.equals(namespace, that.getNamespace())
                    && Objects.equals(attributes, that.getAttributes())
                    && Objects.equals(directives, that.getDirectives());
        }
    }
}
