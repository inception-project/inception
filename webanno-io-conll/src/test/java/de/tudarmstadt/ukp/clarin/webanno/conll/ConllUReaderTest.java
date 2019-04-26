/*
 * Copyright 2016
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.conll;

import static de.tudarmstadt.ukp.dkpro.core.testing.AssertAnnotations.assertMorph;
import static de.tudarmstadt.ukp.dkpro.core.testing.AssertAnnotations.assertPOS;
import static de.tudarmstadt.ukp.dkpro.core.testing.AssertAnnotations.assertSentence;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.apache.uima.fit.util.JCasUtil.select;

import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.JCasIterable;
import org.apache.uima.jcas.JCas;
import org.junit.Rule;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;

public class ConllUReaderTest
{
    @Test
    public void test()
        throws Exception
    {
        CollectionReaderDescription reader = createReaderDescription(
                ConllUReader.class, 
                ConllUReader.PARAM_LANGUAGE, "en",
                ConllUReader.PARAM_SOURCE_LOCATION, "src/test/resources/conll/u/", 
                ConllUReader.PARAM_PATTERNS, "conllu-en-orig.conll");
        
        JCas jcas = new JCasIterable(reader).iterator().next();

        String[] sentences = {
                "They buy and sell books.",
                "I have not a clue." };

        String[] posMapped = { "POS", "POS_VERB", "POS_CONJ", "POS_VERB", "POS_NOUN", "POS_PUNCT", "POS", "POS_VERB", "POS_ADV",
                "POS_DET", "POS_NOUN", "POS_PUNCT" };

        String[] posOriginal = { "PRN", "VB", "CC", "VB", "NNS", ".", "PRN", "VB", "RB", "DT", "NN",
                "." };

        String[] morphologicalFeeatures = {
                "[  0,  4]     -     -  Nom    -    -     -    -    -  Plur      -  -    -    -    -     -      -     - They (Case=Nom|Number=Plur)",
                "[  5,  8]     -     -    -    -    -     -    -    -  Plur      -  3    -    -    -  Pres      -     - buy (Number=Plur|Person=3|Tense=Pres)",
                "[ 13, 17]     -     -    -    -    -     -    -    -  Plur      -  3    -    -    -  Pres      -     - sell (Number=Plur|Person=3|Tense=Pres)",
                "[ 18, 23]     -     -    -    -    -     -    -    -  Plur      -  -    -    -    -     -      -     - books (Number=Plur)",
                "[ 25, 26]     -     -  Nom    -    -     -    -    -  Sing      -  1    -    -    -     -      -     - I (Case=Nom|Number=Sing|Person=1)",
                "[ 27, 31]     -     -    -    -    -     -    -    -  Sing      -  1    -    -    -  Pres      -     - have (Number=Sing|Person=1|Tense=Pres)",
                "[ 32, 35]     -     -    -    -    -     -    -  Neg     -      -  -    -    -    -     -      -     - not (Negative=Neg)",
                "[ 36, 37]     -     -    -    -    -     -    -    -     -      -  -    -  Art    -     -      -     - a (Definite=Ind|PronType=Art)",
                "[ 38, 42]     -     -    -    -    -     -    -    -  Sing      -  -    -    -    -     -      -     - clue (Number=Sing)"
        };
        
        assertSentence(sentences, select(jcas, Sentence.class));
        assertPOS(posMapped, posOriginal, select(jcas, POS.class));
        assertMorph(morphologicalFeeatures, select(jcas, MorphologicalFeatures.class));
    }

    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}
