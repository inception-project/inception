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
package de.tudarmstadt.ukp.inception.io.xml.dkprocore;

import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.uima.fit.factory.JCasFactory;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

class XmlDocumentReaderTest
{
    @Test
    void testBlockElementsAsSentences() throws Exception
    {
        var reader = createReader( //
                XmlDocumentReader.class, //
                XmlDocumentReader.PARAM_SOURCE_LOCATION, "classpath:/xml/block-elements.xml", //
                XmlDocumentReader.PARAM_BLOCK_ELEMENTS, "p", //
                XmlDocumentReader.PARAM_SPLIT_SENTENCES_IN_BLOCK_ELEMENTS, false);

        var jcas = JCasFactory.createJCas();
        reader.getNext(jcas.getCas());

        assertThat(jcas.select(Sentence.class).asList()) //
                .extracting(Sentence::getCoveredText) //
                .containsExactly("Sentence 1.", "Sentence 2.\n    Sentence 3.", "Sentence 4.");
    }

    @Test
    void testSentencesRespectingBlockElements() throws Exception
    {
        var reader = createReader( //
                XmlDocumentReader.class, //
                XmlDocumentReader.PARAM_SOURCE_LOCATION, "classpath:/xml/block-elements.xml", //
                XmlDocumentReader.PARAM_BLOCK_ELEMENTS, "p", //
                XmlDocumentReader.PARAM_SPLIT_SENTENCES_IN_BLOCK_ELEMENTS, true);

        var jcas = JCasFactory.createJCas();
        reader.getNext(jcas.getCas());

        assertThat(jcas.select(Sentence.class).asList()) //
                .extracting(Sentence::getCoveredText) //
                .containsExactly("Sentence 1.", "Sentence 2.", "Sentence 3.", "Sentence 4.");
    }
}
