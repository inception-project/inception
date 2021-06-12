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
package de.tudarmstadt.ukp.clarin.webanno.text;

import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class PretokenizedLineOrientedTextReaderTest
{
    @Test
    public void test() throws Exception
    {
        JCas doc = JCasFactory.createJCas();

        CollectionReader reader = createReader(PretokenizedLineOrientedTextReader.class,
                PretokenizedLineOrientedTextReader.PARAM_SOURCE_LOCATION, "LICENSE.txt");

        reader.getNext(doc.getCas());

        assertEquals(169, select(doc, Sentence.class).size());
        assertEquals(1581, select(doc, Token.class).size());
    }

    @Test
    public void testSpaceSeparatedNoFinalLineBreak() throws Exception
    {
        JCas doc = JCasFactory.createJCas();

        CollectionReader reader = createReader(PretokenizedLineOrientedTextReader.class,
                PretokenizedLineOrientedTextReader.PARAM_SOURCE_LOCATION,
                "src/test/resources/text/space-separated-no-final-line-break.txt");

        reader.getNext(doc.getCas());

        assertThat(select(doc, Sentence.class)) //
                .extracting(s -> s.getCoveredText()) //
                .containsExactly("1 2 3", "4 5 6", "7 8 9");

        assertThat(select(doc, Token.class)) //
                .extracting(t -> t.getCoveredText()) //
                .containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9");
    }

    @Test
    public void testTextWithPunctuation() throws Exception
    {
        JCas doc = JCasFactory.createJCas();

        CollectionReader reader = createReader(PretokenizedLineOrientedTextReader.class,
                PretokenizedLineOrientedTextReader.PARAM_SOURCE_LOCATION,
                "src/test/resources/text/text-with-punctuation.txt");

        reader.getNext(doc.getCas());

        assertThat(select(doc, Sentence.class)) //
                .extracting(s -> s.getCoveredText()) //
                .containsExactly("Yesterday, I left the house.");

        assertThat(select(doc, Token.class)) //
                .extracting(t -> t.getCoveredText()) //
                .containsExactly("Yesterday,", "I", "left", "the", "house.");
    }
}
