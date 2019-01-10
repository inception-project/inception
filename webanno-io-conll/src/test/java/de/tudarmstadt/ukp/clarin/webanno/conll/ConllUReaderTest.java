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

        String[] posMapped = { "O", "V", "CONJ", "V", "NN", "PUNC", "O", "V", "ADV", "ART", "NN",
                "PUNC" };

        String[] posOriginal = { "PRN", "VB", "CC", "VB", "NNS", ".", "PRN", "VB", "RB", "DT", "NN",
                "." };

        String[] morphologicalFeeatures = {
                "Case=Nom|Number=Plur",
                "Number=Plur|Person=3|Tense=Pres",
                "Number=Plur|Person=3|Tense=Pres",
                "Number=Plur",
                "Case=Nom|Number=Sing|Person=1",
                "Number=Sing|Person=1|Tense=Pres",
                "Negative=Neg",
                "Definite=Ind|PronType=Art",
                "Number=Sing"
        };
        
        assertSentence(sentences, select(jcas, Sentence.class));
        assertPOS(posMapped, posOriginal, select(jcas, POS.class));
        assertMorph(morphologicalFeeatures, select(jcas, MorphologicalFeatures.class));
    }

    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}
