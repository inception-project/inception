/*
 * Copyright 2016
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import static org.junit.Assert.assertEquals;

import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class LineOrientedTextReaderTest
{
    @Test
    public void test() throws Exception
    {
        JCas doc = JCasFactory.createJCas();

        CollectionReader reader = createReader(LineOrientedTextReader.class,
                LineOrientedTextReader.PARAM_SOURCE_LOCATION, "LICENSE.txt");

        reader.getNext(doc.getCas());

        // select(doc, Sentence.class).forEach(s -> System.out.println(s.getCoveredText()));

        assertEquals(169, select(doc, Sentence.class).size());
        assertEquals(0, select(doc, Token.class).size());
    }
}
