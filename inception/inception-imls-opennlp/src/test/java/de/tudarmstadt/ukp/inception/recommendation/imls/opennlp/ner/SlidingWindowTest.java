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
package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.ner;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;

import org.apache.uima.fit.factory.JCasBuilder;
import org.apache.uima.fit.factory.JCasFactory;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

class SlidingWindowTest
{

    @Test
    void test() throws Exception
    {
        var sentences = 10;
        var sentenceLength = 10;

        var cas = JCasFactory.createJCas();
        var casBuilder = new JCasBuilder(cas);

        var expectedTokens = new HashSet<String>();

        for (int s = 0; s < sentences; s++) {
            int sBegin = casBuilder.getPosition();

            for (int t = 0; t < sentenceLength; t++) {
                var token = String.format("%02d", s * sentenceLength + t);
                expectedTokens.add(token);
                casBuilder.add(token, Token.class);
                casBuilder.add(" ");
            }

            casBuilder.add(sBegin, Sentence.class);
            casBuilder.add(".\n");
        }

        casBuilder.close();

        var sut = new SlidingWindow<>(cas.getCas(), Token.class, 20, 10);

        var actualTokens = new HashSet<String>();
        int base = 0;
        for (var unit : sut) {
            unit.stream().map(Token::getCoveredText).forEach(actualTokens::add);

            var expected = new ArrayList<String>();
            for (int n = 0; n < sentenceLength; n++) {
                expected.add(String.format("%02d", base + n));
            }

            assertThat(unit.stream().map(Token::getCoveredText).toList())
                    .containsExactlyElementsOf(expected);
            base += sentenceLength / 2;
        }

        assertThat(actualTokens).containsExactlyInAnyOrderElementsOf(expectedTokens);
    }
}
