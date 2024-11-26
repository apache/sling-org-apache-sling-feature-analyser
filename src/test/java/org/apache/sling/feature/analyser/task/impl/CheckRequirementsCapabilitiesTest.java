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
package org.apache.sling.feature.analyser.task.impl;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.felix.utils.resource.CapabilityImpl;
import org.apache.felix.utils.resource.RequirementImpl;
import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.MatchingRequirement;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Resource;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

public class CheckRequirementsCapabilitiesTest {
    @Test
    public void testCheckRequirementsCapabilitiesAllOk() throws Exception {
        AnalyserTaskContext ctx = Mockito.spy(createAnalyserTaskContext(
                requirement("org.zzz", "(&(zzz=aaa)(qqq=123))"),
                capability("org.foo.blah", Collections.singletonMap("abc", "def")),
                capability("org.foo.bar", Collections.singletonMap("abc", "def"))));

        BundleDescriptor bundleDescriptor = createTestBundleDescriptor();

        ctx.getFeatureDescriptor().getBundleDescriptors().add(bundleDescriptor);

        CheckRequirementsCapabilities crc = new CheckRequirementsCapabilities();
        crc.execute(ctx);

        Mockito.verify(ctx, Mockito.never()).reportArtifactError(Mockito.any(), Mockito.anyString());
        Mockito.verify(ctx, Mockito.never()).reportError(Mockito.anyString());
        Mockito.verify(ctx, Mockito.never()).reportArtifactWarning(Mockito.any(), Mockito.anyString());
        Mockito.verify(ctx, Mockito.never()).reportWarning(Mockito.anyString());
    }

    @Test
    public void testCheckRequirementsCapabilitiesMissingFromBundle() throws Exception {
        AnalyserTaskContextImpl ctx = Mockito.spy(new AnalyserTaskContextImpl());
        ctx.getFeatureDescriptor().getBundleDescriptors().add(createTestBundleDescriptor());

        CheckRequirementsCapabilities crc = new CheckRequirementsCapabilities();
        crc.execute(ctx);

        Mockito.verify(ctx, times(1)).reportArtifactError(Mockito.any(), Mockito.contains("org.foo.bar"));
        Mockito.verify(ctx, Mockito.never()).reportError(Mockito.anyString());
        Mockito.verify(ctx, Mockito.never()).reportArtifactWarning(Mockito.any(), Mockito.anyString());
        Mockito.verify(ctx, Mockito.never()).reportWarning(Mockito.anyString());
    }

    @Test
    public void testCheckRequirementsCapabilitiesMissingFromFeature() throws Exception {
        AnalyserTaskContext ctx = Mockito.spy(createAnalyserTaskContext(
                requirement("org.zzz", "(&(zzz=aaa)(qqq=123))"),
                capability("org.foo.blah", Collections.singletonMap("abc", "def")),
                capability("org.foo.bar", Collections.singletonMap("abc", "def"))));

        CheckRequirementsCapabilities crc = new CheckRequirementsCapabilities();
        crc.execute(ctx);

        Mockito.verify(ctx, times(1)).reportError(Mockito.contains("org.zzz"));
        Mockito.verify(ctx, never()).reportArtifactError(Mockito.any(), Mockito.anyString());
        Mockito.verify(ctx, never()).reportWarning(Mockito.anyString());
        Mockito.verify(ctx, never()).reportArtifactWarning(Mockito.any(), Mockito.anyString());
    }

    @Test
    public void testMissingOptionalRequirementCausesWarning() throws Exception {
        AnalyserTaskContext ctx = Mockito.spy(new AnalyserTaskContextImpl());

        BundleDescriptor consumer = createBundleDescriptor("requirement:optional:1.0.0");
        consumer.getRequirements()
                .add(requirement(
                        "osgi.serviceloader",
                        directives(
                                Namespace.REQUIREMENT_FILTER_DIRECTIVE,
                                "(osgi.serviceloader=org.example.Foo)",
                                Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE,
                                Namespace.RESOLUTION_OPTIONAL)));

        ctx.getFeatureDescriptor().getBundleDescriptors().add(consumer);

        CheckRequirementsCapabilities crc = new CheckRequirementsCapabilities();
        crc.execute(ctx);

        Mockito.verify(ctx, never()).reportArtifactError(Mockito.any(), Mockito.anyString());
        Mockito.verify(ctx, never()).reportError(Mockito.anyString());
        Mockito.verify(ctx, times(1))
                .reportArtifactWarning(
                        Mockito.any(),
                        Mockito.contains(
                                "while the requirement is optional no artifact is providing a matching capability in this start level."));
        Mockito.verify(ctx, never()).reportWarning(Mockito.anyString());
    }

    @Test
    public void testSingleCardinalityRequirementCausesWarningWhenSatisfiedTwice() throws Exception {
        AnalyserTaskContext ctx = Mockito.spy(new AnalyserTaskContextImpl());

        ctx.getFeatureDescriptor()
                .getBundleDescriptors()
                .addAll(asList(
                        serviceLoaderConsumer(Namespace.CARDINALITY_SINGLE),
                        serviceLoaderProvider("serviceloader:providerA:1.0.0", "org.acme.FooImpl"),
                        serviceLoaderProvider("serviceloader:providerB:1.0.0", "org.boo.FooImpl")));

        CheckRequirementsCapabilities crc = new CheckRequirementsCapabilities();
        crc.execute(ctx);

        Mockito.verify(ctx, never()).reportArtifactError(Mockito.any(), Mockito.anyString());
        Mockito.verify(ctx, never()).reportError(Mockito.anyString());
        Mockito.verify(ctx, times(1))
                .reportArtifactWarning(
                        Mockito.any(),
                        Mockito.contains("there is more than one matching capability in this start level"));
        Mockito.verify(ctx, never()).reportWarning(Mockito.anyString());
    }

    @Test
    public void testMultipleCardinalityRequirementCausesNoWarningWhenSatisfiedTwice() throws Exception {
        AnalyserTaskContext ctx = Mockito.spy(new AnalyserTaskContextImpl());

        ctx.getFeatureDescriptor()
                .getBundleDescriptors()
                .addAll(asList(
                        serviceLoaderConsumer(Namespace.CARDINALITY_MULTIPLE),
                        serviceLoaderProvider("serviceloader:providerA:1.0.0", "org.acme.FooImpl"),
                        serviceLoaderProvider("serviceloader:providerB:1.0.0", "org.boo.FooImpl")));

        CheckRequirementsCapabilities crc = new CheckRequirementsCapabilities();
        crc.execute(ctx);

        Mockito.verify(ctx, never()).reportArtifactError(Mockito.any(), Mockito.anyString());
        Mockito.verify(ctx, never()).reportError(Mockito.anyString());
        Mockito.verify(ctx, never()).reportArtifactWarning(Mockito.any(), Mockito.anyString());
        Mockito.verify(ctx, never()).reportWarning(Mockito.anyString());
    }

    private BundleDescriptor createBundleDescriptor(String mvnId) {
        ArtifactId artifactId = ArtifactId.fromMvnId(mvnId);
        return new BundleDescriptor(mvnId) {

            @Override
            public String getBundleSymbolicName() {
                return artifactId.getArtifactId();
            }

            @Override
            public String getBundleVersion() {
                return artifactId.getVersion();
            }

            @Override
            public Manifest getManifest() {
                Manifest manifest = new Manifest();
                Attributes mainAttributes = manifest.getMainAttributes();
                mainAttributes.putValue("Manifest-Version", "1.0");
                mainAttributes.putValue("Bundle-SymbolicName", getBundleSymbolicName());
                mainAttributes.putValue("Bundle-Version", getBundleVersion());
                return manifest;
            }

            @Override
            public URL getArtifactFile() {
                throw new UnsupportedOperationException("not implemented");
            }

            @Override
            public Artifact getArtifact() {
                return new Artifact(artifactId);
            }
        };
    }

    private @NotNull BundleDescriptor createTestBundleDescriptor() {
        BundleDescriptor bundleDescriptor = createBundleDescriptor("g:b1:1.2.0");
        bundleDescriptor
                .getCapabilities()
                .add(capability("org.zzz", attributes("qqq", "123", "vvv", "vvv", "zzz", "aaa")));
        bundleDescriptor.getRequirements().add(requirement("org.foo.bar", "(abc=def)"));
        return bundleDescriptor;
    }

    private Map<String, String> directives(String... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("keyValues must have an even number of elements");
        }
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; ) {
            map.put(keyValues[i++], keyValues[i++]);
        }
        return map;
    }

    private Map<String, Object> attributes(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("keyValues must have an even number of elements");
        }

        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; ) {
            Object key = keyValues[i++];
            if (!(key instanceof String)) {
                throw new IllegalArgumentException("all keys must be of type String");
            }
            map.put((String) key, keyValues[i++]);
        }
        return map;
    }

    private @NotNull BundleDescriptor serviceLoaderConsumer(String cardinality) {
        BundleDescriptor consumer = createBundleDescriptor("serviceloader:consumer:1.0.0");
        consumer.getRequirements()
                .add(requirement(
                        "osgi.serviceloader",
                        directives(
                                Namespace.REQUIREMENT_FILTER_DIRECTIVE,
                                "(osgi.serviceloader=org.example.Foo)",
                                Namespace.REQUIREMENT_CARDINALITY_DIRECTIVE,
                                cardinality)));
        return consumer;
    }

    private @NotNull BundleDescriptor serviceLoaderProvider(String mvnId, String implClass) {
        BundleDescriptor providerA = createBundleDescriptor(mvnId);
        providerA
                .getCapabilities()
                .add(capability(
                        "osgi.serviceloader",
                        attributes("osgi.serviceloader", "org.example.Foo", "register", implClass)));
        return providerA;
    }

    private static @NotNull AnalyserTaskContext createAnalyserTaskContext(
            MatchingRequirement requirement, Capability... capabilities) {
        AnalyserTaskContextImpl analyserTaskContext = new AnalyserTaskContextImpl("test:feature:1");
        Feature feature = analyserTaskContext.getFeature();
        feature.getCapabilities().addAll(asList(capabilities));
        feature.getRequirements().add(requirement);
        return analyserTaskContext;
    }

    private static @NotNull MatchingRequirement requirement(String namespace, String filter) {
        return new MatchingRequirementImpl(null, namespace, filter);
    }

    private static @NotNull MatchingRequirement requirement(String namespace, Map<String, String> directives) {
        return new MatchingRequirementImpl(null, namespace, directives, Collections.emptyMap());
    }

    private static @NotNull Capability capability(String namespace, Map<String, Object> attributes) {
        return capability(namespace, Collections.emptyMap(), attributes);
    }

    private static @NotNull Capability capability(
            String namespace, Map<String, String> directives, Map<String, Object> attributes) {
        return new CapabilityImpl(null, namespace, directives, attributes);
    }

    private static class MatchingRequirementImpl extends RequirementImpl implements MatchingRequirement {

        MatchingRequirementImpl(Resource res, String ns, String filter) {
            super(res, ns, filter);
        }

        MatchingRequirementImpl(
                final Resource res,
                final String ns,
                final Map<String, String> directives,
                final Map<String, Object> attributes) {
            super(res, ns, directives, attributes);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof RequirementImpl)) {
                return false;
            }
            final RequirementImpl that = (RequirementImpl) o;
            return Objects.equals(resource, that.getResource())
                    && Objects.equals(namespace, that.getNamespace())
                    && Objects.equals(attributes, that.getAttributes())
                    && Objects.equals(directives, that.getDirectives());
        }
    }
}
