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
package de.tudarmstadt.ukp.inception.support.uima;

import static de.tudarmstadt.ukp.inception.support.uima.SegmentationUtils.splitSentences;
import static de.tudarmstadt.ukp.inception.support.uima.SegmentationUtils.tokenize;
import static org.apache.uima.fit.factory.JCasFactory.createText;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Div;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Heading;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Paragraph;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class SegmentationUtilsTest
{
    @Test
    public void testSplitSentences() throws Exception
    {
        var jcas = createText("I am one. I am two.", "en");

        splitSentences(jcas.getCas());

        assertThat(select(jcas, Sentence.class)) //
                .extracting(Sentence::getCoveredText) //
                .containsExactly("I am one.", "I am two.");
    }

    @Test
    public void testSplitSentencesWithZones() throws Exception
    {
        var jcas = createText("Heading I am two.", "en");
        new Heading(jcas, 0, 7).addToIndexes();
        new Paragraph(jcas, 8, 17).addToIndexes();

        splitSentences(jcas.getCas(), 0, jcas.getDocumentText().length(), jcas.select(Div.class));

        assertThat(select(jcas, Sentence.class)) //
                .extracting(Sentence::getCoveredText) //
                .containsExactly("Heading", "I am two.");
    }

    @Test
    public void testSplitSentencesWithLimitAndZones() throws Exception
    {
        var jcas = createText("Heading I am two.", "en");
        new Heading(jcas, 0, 7).addToIndexes();
        new Paragraph(jcas, 8, 17).addToIndexes();

        splitSentences(jcas.getCas(), 4, 12, jcas.select(Div.class));

        assertThat(select(jcas, Sentence.class)) //
                .extracting(Sentence::getCoveredText) //
                .containsExactly("ing", "I am");
    }

    @Test
    public void testTokenize() throws Exception
    {
        var jcas = createText("i am one.i am two.", "en");
        new Sentence(jcas, 0, 9).addToIndexes();
        new Sentence(jcas, 9, 18).addToIndexes();

        tokenize(jcas.getCas());

        assertThat(select(jcas, Sentence.class)) //
                .extracting(Sentence::getCoveredText) //
                .containsExactly("i am one.", "i am two.");

        assertThat(select(jcas, Token.class)) //
                .extracting(Token::getCoveredText) //
                .containsExactly( //
                        "i", "am", "one", ".", //
                        "i", "am", "two", ".");
    }

    @Test
    public void testTokenizeWithZones() throws Exception
    {
        var jcas = createText("i am one.i am two.", "en");
        new Sentence(jcas, 0, 9).addToIndexes();
        new Sentence(jcas, 9, 18).addToIndexes();
        new Div(jcas, 3, 3).addToIndexes();
        new Div(jcas, 12, 15).addToIndexes();

        tokenize(jcas.getCas(), 0, jcas.getDocumentText().length(), jcas.select(Div.class));

        assertThat(select(jcas, Sentence.class)) //
                .extracting(Sentence::getCoveredText) //
                .containsExactly("i am one.", "i am two.");

        assertThat(select(jcas, Token.class)) //
                .extracting(Token::getCoveredText) //
                .containsExactly( //
                        "i", "a", "m", "one", ".", //
                        "i", "a", "m", "t", "wo", ".");
    }

    @Test
    public void testTokenizeWithLimitAndZones() throws Exception
    {
        var jcas = createText("i am one.i am two.", "en");
        new Sentence(jcas, 0, 9).addToIndexes();
        new Sentence(jcas, 9, 18).addToIndexes();
        new Div(jcas, 3, 3).addToIndexes();
        new Div(jcas, 12, 15).addToIndexes();

        tokenize(jcas.getCas(), 5, 13, jcas.select(Div.class));

        assertThat(select(jcas, Sentence.class)) //
                .extracting(Sentence::getCoveredText) //
                .containsExactly("i am one.", "i am two.");

        assertThat(select(jcas, Token.class)) //
                .extracting(Token::getCoveredText) //
                .containsExactly( //
                        "one", ".", //
                        "i", "a", "m");
    }

    @Test
    public void testTokenizeWithZonesAndNonDefaultSentence() throws Exception
    {
        var jcas = createText("    this isa test.    ", "en");
        new Sentence(jcas, 4, 18).addToIndexes();
        new Div(jcas, 11, 11).addToIndexes();

        tokenize(jcas.getCas(), 0, jcas.getDocumentText().length(), jcas.select(Div.class));

        assertThat(select(jcas, Sentence.class)) //
                .extracting(Sentence::getCoveredText) //
                .containsExactly("this isa test.");

        assertThat(select(jcas, Token.class)) //
                .extracting(Token::getCoveredText) //
                .containsExactly( //
                        "this", "is", "a", "test", ".");
    }

    @Test
    public void testTokenizeWithZonesInBetweenSentences() throws Exception
    {
        var jcas = createText("0123456789", "en");
        new Sentence(jcas, 1, 3).addToIndexes();
        new Sentence(jcas, 4, 6).addToIndexes();
        new Sentence(jcas, 7, 9).addToIndexes();
        new Div(jcas, 2, 2).addToIndexes();
        new Div(jcas, 5, 5).addToIndexes();
        new Div(jcas, 8, 8).addToIndexes();

        tokenize(jcas.getCas(), 0, jcas.getDocumentText().length(), jcas.select(Div.class));

        assertThat(select(jcas, Sentence.class)) //
                .extracting(Sentence::getCoveredText) //
                .containsExactly("12", "45", "78");

        assertThat(select(jcas, Token.class)) //
                .extracting(Token::getCoveredText) //
                .containsExactly( //
                        "1", "2", "4", "5", "7", "8");
    }
}
