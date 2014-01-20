/*******************************************************************************
 * Copyright 2011
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
 ******************************************************************************/
package de.tudarmstadt.ukp.dkpro.core.io.tei;

import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.junit.Assert.assertEquals;

import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.JCasIterable;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Ignore;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class TeiReaderTest
{

    @Test
    @Ignore("No TEI yet to opensource ")
    public void testTeiReader()
        throws Exception
    {
        CollectionReaderDescription reader = createReaderDescription(TeiReader.class,
                TeiReader.PARAM_LANGUAGE, "en", TeiReader.PARAM_SOURCE_LOCATION, "classpath:/tei/",
                TeiReader.PARAM_PATTERNS, new String[] { "[+]*.xml" });

        String firstSentence = "\"På Facebook handler det egentlig bare om at blive set. "
                + "I informationssamfundet er den største knaphedsressource andres opmærksomhed."
                + " Vi ser det også på den måde, vi bruger sms og e-mail. Vældig mange af"
                + " beskederne handler sådan set bare om at sige: Hallo hallo, glem ikke, "
                + "at jeg findes. Det samme med Facebook. Og det at have 2.000 såkaldte "
                + "Facebookvenner giver indtryk af, at  man er en person, der sidder "
                + "som en edderkop i et stort netværk. Og i et netværkssamfund er det en stor "
                + "ressource for vældig mange mennesker\", siger Thomas Hylland Eriksen.";

        for (JCas jcas : new JCasIterable(reader)) {
            DocumentMetaData meta = DocumentMetaData.get(jcas);
            String text = jcas.getDocumentText();
            System.out.printf("%s - %d%n", meta.getDocumentId(), text.length());
            System.out.println(jcas.getDocumentLanguage());

            assertEquals(876, JCasUtil.select(jcas, Token.class).size());
            assertEquals(292, JCasUtil.select(jcas, POS.class).size());
            assertEquals(292, JCasUtil.select(jcas, Lemma.class).size());
            assertEquals(220, JCasUtil.select(jcas, NamedEntity.class).size());
            assertEquals(5, JCasUtil.select(jcas, Sentence.class).size());

            assertEquals(firstSentence, JCasUtil.select(jcas, Sentence.class).iterator().next()
                    .getCoveredText());
        }

    }
}
