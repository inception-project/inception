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

import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.apache.uima.fit.pipeline.SimplePipeline.iteratePipeline;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

import org.apache.uima.fit.factory.CasFactory;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

/**
 * @deprecated Experimental code that was deprecated.
 */
@Deprecated
public class BioCXmlDocumentReaderTest
{
    @Test
    void testRead() throws Exception
    {
        var reader = createReader( //
                BioCXmlDocumentReader.class, //
                BioCXmlDocumentReader.PARAM_SOURCE_LOCATION,
                "src/test/resources/bioc/example-with-sentences.xml");

        assertThat(reader.hasNext()).isTrue();

        var cas = CasFactory.createCas();
        reader.getNext(cas);

        assertThat(cas.getDocumentText()) //
                .contains("Sentence 1.") //
                .contains("Sentence 2.");

        assertThat(cas.select(Sentence.class).asList()) //
                .extracting(Sentence::getCoveredText) //
                .containsExactly("Sentence 1.", "Sentence 2.");
    }

    @Test
    void testReadMultipleFromOneFile() throws Exception
    {
        var reader = createReaderDescription( //
                BioCXmlDocumentReader.class, //
                BioCXmlDocumentReader.PARAM_SOURCE_LOCATION,
                "src/test/resources/bioc/example-with-multiple-documents.xml");

        var texts = new ArrayList<String>();
        iteratePipeline(reader).forEach(cas -> texts.add(cas.getDocumentText().trim()));

        assertThat(texts) //
                .containsExactly("Document 1 text.", "Document 2 text.", "Document 3 text.");
    }
}
