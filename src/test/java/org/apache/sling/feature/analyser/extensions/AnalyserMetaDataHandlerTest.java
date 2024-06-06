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
package org.apache.sling.feature.analyser.extensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.sling.feature.Feature;
import org.apache.sling.feature.builder.ArtifactProvider;
import org.apache.sling.feature.builder.HandlerContext;
import org.apache.sling.feature.io.json.FeatureJSONReader;
import org.apache.sling.feature.io.json.FeatureJSONWriter;
import org.junit.Test;
import org.mockito.Mockito;

public class AnalyserMetaDataHandlerTest {

    @Test
    public void testMetaDataHandler() throws Exception {
        URL url = getClass().getResource("/analyse-metadata/feature-input.json");

        Feature feature;
        try (Reader r = new InputStreamReader(url.openStream())) {
            feature = FeatureJSONReader.read(r, url.toString());
        }

        assertNotNull(feature);

        HandlerContext ctx = Mockito.mock(HandlerContext.class);
        ArtifactProvider provider = artifactId -> {
            try {
                return new URL("https://repo1.maven.org/maven2/" + artifactId.toMvnPath());
            } catch (MalformedURLException e) {
                throw new UncheckedIOException(e);
            }
        };

        Mockito.when(ctx.getArtifactProvider()).thenReturn(provider);

        new AnalyserMetaDataHandler().
            postProcess(ctx, feature, feature.getExtensions().getByName("analyser-metadata"));

        URL expectedURL = getClass().getResource("/analyse-metadata/feature-expected-output.json");

        Feature expected;
        try (Reader r = new InputStreamReader(expectedURL.openStream())) {
            expected = FeatureJSONReader.read(r, expectedURL.toString());
        }

        StringWriter featureWriter = new StringWriter();
        StringWriter expectedWriter = new StringWriter();

        FeatureJSONWriter.write(featureWriter, feature);
        FeatureJSONWriter.write(expectedWriter, expected);

        assertEquals(expectedWriter.toString(), featureWriter.toString());
    }

}
