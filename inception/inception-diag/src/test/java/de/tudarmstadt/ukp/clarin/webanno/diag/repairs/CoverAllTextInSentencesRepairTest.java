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
package de.tudarmstadt.ukp.clarin.webanno.diag.repairs;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

class CoverAllTextInSentencesRepairTest
{
    CoverAllTextInSentencesRepair sut;
    SourceDocument document;
    String dataOwner;
    JCas jCas;

    @BeforeEach
    void setup() throws Exception
    {
        sut = new CoverAllTextInSentencesRepair();
        document = SourceDocument.builder().build();
        jCas = JCasFactory.createJCas();
    }

    @Test
    void thatNewSentenceIsAddedOnText()
    {
        jCas.setDocumentText("This is a test.");

        var annotations = asList( //
                new Sentence(jCas, 0, 4), //
                new Sentence(jCas, 10, 15));
        annotations.forEach(Annotation::addToIndexes);

        var messages = new ArrayList<LogMessage>();

        sut.repair(document, dataOwner, jCas.getCas(), messages);

        assertThat(jCas.select(Sentence.class).asList()) //
                .extracting(Annotation::getBegin, Annotation::getEnd)
                .containsExactly(tuple(0, 4), tuple(5, 9), tuple(10, 15));

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getMessage()).contains("new sentence at [5-9]");
    }

    @Test
    void thatNoNewSentenceIsAddedOnBlank()
    {
        jCas.setDocumentText("This      test.");

        var annotations = asList( //
                new Sentence(jCas, 0, 4), //
                new Sentence(jCas, 10, 15));
        annotations.forEach(Annotation::addToIndexes);

        var messages = new ArrayList<LogMessage>();

        sut.repair(document, dataOwner, jCas.getCas(), messages);

        assertThat(jCas.select(Sentence.class).asList()) //
                .extracting(Annotation::getBegin, Annotation::getEnd)
                .containsExactly(tuple(0, 4), tuple(10, 15));

        assertThat(messages).isEmpty();
    }

    @Test
    void thatNewSentenceIsAddedAtDocumentStart()
    {
        jCas.setDocumentText("  This is a test.");

        var annotations = asList( //
                new Sentence(jCas, 7, 17));
        annotations.forEach(Annotation::addToIndexes);

        var messages = new ArrayList<LogMessage>();

        sut.repair(document, dataOwner, jCas.getCas(), messages);

        assertThat(jCas.select(Sentence.class).asList()) //
                .extracting(Annotation::getBegin, Annotation::getEnd)
                .containsExactly(tuple(2, 6), tuple(7, 17));

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getMessage()).contains("new sentence at [2-6]");
    }

    @Test
    void thatNewSentenceIsAddedAtDocumentEnd()
    {
        jCas.setDocumentText("This is a test.  ");

        var annotations = asList( //
                new Sentence(jCas, 0, 4));
        annotations.forEach(Annotation::addToIndexes);

        var messages = new ArrayList<LogMessage>();

        sut.repair(document, dataOwner, jCas.getCas(), messages);

        assertThat(jCas.select(Sentence.class).asList()) //
                .extracting(Annotation::getBegin, Annotation::getEnd)
                .containsExactly(tuple(0, 4), tuple(5, 15));

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getMessage()).contains("new sentence at [5-15]");
    }
}
