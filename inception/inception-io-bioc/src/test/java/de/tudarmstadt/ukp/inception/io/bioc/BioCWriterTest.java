/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.io.bioc;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.apache.uima.fit.factory.CasFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;

class BioCWriterTest
{
    @Test
    void thatMetadataIsGeneratedToOutput(@TempDir File aTmp) throws Exception
    {
        var cas = CasFactory.createCas();
        cas.setDocumentText("This is a test");

        var dmd = DocumentMetaData.create(cas);
        dmd.setCollectionId("collectionId");
        dmd.setDocumentId("documentId");

        var writer = createEngine( //
                BioCWriter.class, //
                BioCWriter.PARAM_TARGET_LOCATION, aTmp);

        writer.process(cas);

        var out = new File(aTmp, "documentId.xml");
        assertThat(out).exists() //
                .content() //
                .contains("<source>collectionId</source>") //
                .contains("<date>") //
                .contains("<key>documentId</key>");
    }
}
