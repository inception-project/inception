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
package de.tudarmstadt.ukp.inception.export;

import static org.apache.uima.fit.util.CasUtil.toText;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.uima.fit.factory.JCasFactory;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Div;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Heading;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Paragraph;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.support.uima.SegmentationUtils;

public class SegmentationUtilsTest
{
    @Test
    public void testSplitSentences() throws Exception
    {
        var jcas = JCasFactory.createText("I am one. I am two.", "en");

        SegmentationUtils.splitSentences(jcas.getCas());

        assertThat(toText(select(jcas, Sentence.class))) //
                .containsExactly("I am one.", "I am two.");
    }

    @Test
    public void testSplitSentencesWithZones() throws Exception
    {
        var jcas = JCasFactory.createText("Heading I am two.", "en");
        new Heading(jcas, 0, 7).addToIndexes();
        new Paragraph(jcas, 8, 17).addToIndexes();

        SegmentationUtils.splitSentences(jcas.getCas(), jcas.select(Div.class));

        assertThat(toText(select(jcas, Sentence.class))) //
                .containsExactly("Heading", "I am two.");
    }

    @Test
    public void testTokenize() throws Exception
    {
        var jcas = JCasFactory.createText("i am one.i am two.", "en");
        new Sentence(jcas, 0, 9).addToIndexes();
        new Sentence(jcas, 9, 18).addToIndexes();

        SegmentationUtils.tokenize(jcas.getCas());

        assertThat(toText(select(jcas, Sentence.class))) //
                .containsExactly("i am one.", "i am two.");

        assertThat(toText(select(jcas, Token.class))) //
                .containsExactly("i", "am", "one", ".", "i", "am", "two", ".");
    }
}
