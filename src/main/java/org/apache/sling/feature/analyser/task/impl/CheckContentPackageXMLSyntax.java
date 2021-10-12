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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.scanner.ContentPackageDescriptor;
import org.xml.sax.SAXException;

/**
 * This analyser checks for bundles and configurations in packages
 */
public class CheckContentPackageXMLSyntax implements AnalyserTask {

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
            checkPackageSyntax(ctx, cp);
        }
    }

    private void checkPackageSyntax(final AnalyserTaskContext ctx, final ContentPackageDescriptor cp) {
        URL artifactFile = cp.getArtifactFile();
        if (artifactFile ==  null) {
            ctx.reportArtifactError(cp.getArtifact().getId(), "Content package " + cp.getName() + " is not resolved and can not be checked.");
            return;
        }
        
        try {
            URLConnection conn = artifactFile.openConnection();
            InputStream is = conn.getInputStream();
            byte[] buffer = new byte[1024*1024];
            ZipInputStream zis = new ZipInputStream(is);
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                if (zipEntry.getName().endsWith(".xml")) {
                    ByteArrayInputStream fis = createStreamFromZipEntry(buffer, zis);
                    try {
                        checkXmlFile(fis);
                    } catch (SAXException e) {
                        String msg = String.format("Parse error in file %s : %s", zipEntry.getName(), e.getMessage());
                        ctx.reportArtifactError(cp.getArtifact().getId(), msg);
                    }
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    private ByteArrayInputStream createStreamFromZipEntry(byte[] buffer, ZipInputStream zis) throws IOException {
        int offset = 0;
        int len = 0;
        while (len > -1) {
            len = zis.read(buffer, offset, 1024);
            if (len > 0) {
                offset += len;
            }
        }
        ByteArrayInputStream fis = new ByteArrayInputStream(buffer, 0, offset);
        return fis;
    }

    private void checkXmlFile(InputStream fis) throws SAXException, IOException {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder;
            try {
                dBuilder = dbFactory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new IOException(e);
            }
            dBuilder.parse(fis);
    }
}
