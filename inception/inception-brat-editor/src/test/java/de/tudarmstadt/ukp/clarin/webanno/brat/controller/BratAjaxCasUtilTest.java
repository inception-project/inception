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
package de.tudarmstadt.ukp.clarin.webanno.brat.controller;

import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.isBeginEndInSameSentence;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.isBeginInSameSentence;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.JCasBuilder;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

public class BratAjaxCasUtilTest
{
    @Test
    public void testIsBeginInSameSentence() throws Exception
    {
        JCas jcas = JCasFactory.createJCas();

        JCasBuilder jb = new JCasBuilder(jcas);
        Sentence s1 = jb.add("Sentence 1.", Sentence.class);
        jb.add(" ");
        Sentence s2 = jb.add("Sentence 2.", Sentence.class);
        jb.add(" ");
        Sentence s3 = jb.add(".", Sentence.class);
        Sentence s4 = jb.add(".", Sentence.class);
        jb.close();

        CAS cas = jcas.getCas();

        assertFalse(isBeginInSameSentence(cas, s2.getBegin(), s2.getEnd()));
        assertFalse(isBeginInSameSentence(cas, s2.getEnd(), s2.getBegin()));

        assertTrue(isBeginInSameSentence(cas, s1.getBegin() + 1, s1.getEnd() - 1));
        assertTrue(isBeginInSameSentence(cas, s1.getEnd() - 1, s1.getBegin() + 1));

        assertFalse(isBeginInSameSentence(cas, s1.getBegin(), s1.getEnd()));
        assertFalse(isBeginInSameSentence(cas, s1.getEnd(), s1.getBegin()));

        assertFalse(isBeginInSameSentence(cas, s2.getBegin(), s1.getBegin()));
        assertFalse(isBeginInSameSentence(cas, s1.getBegin(), s2.getBegin()));

        assertFalse(isBeginInSameSentence(cas, s3.getBegin(), s4.getBegin()));

        assertTrue(isBeginInSameSentence(cas, 0, 0));
    }

    @Test
    public void testIsBeginEndInSameSentence() throws Exception
    {
        JCas jcas = JCasFactory.createJCas();

        JCasBuilder jb = new JCasBuilder(jcas);
        Sentence s1 = jb.add("Sentence 1.", Sentence.class);
        jb.add(" ");
        Sentence s2 = jb.add("Sentence 2.", Sentence.class);
        jb.add(" ");
        Sentence s3 = jb.add(".", Sentence.class);
        Sentence s4 = jb.add(".", Sentence.class);
        jb.close();

        CAS cas = jcas.getCas();

        assertTrue(isBeginEndInSameSentence(cas, s2.getBegin(), s2.getEnd()));

        assertTrue(isBeginEndInSameSentence(cas, s1.getBegin() + 1, s1.getEnd() - 1));
        // Invalid because begin > end
        assertTrue(isBeginEndInSameSentence(cas, s1.getEnd() - 1, s1.getBegin() + 1));

        assertTrue(isBeginEndInSameSentence(cas, s1.getBegin(), s1.getEnd()));

        // Invalid because begin > end
        assertFalse(isBeginEndInSameSentence(cas, s2.getBegin(), s1.getBegin()));
        assertFalse(isBeginEndInSameSentence(cas, s1.getBegin(), s2.getBegin()));

        assertTrue(isBeginEndInSameSentence(cas, 0, 0));

        // Note that this is an invalid use of isBeginEndInSameSentence because two begin offsets
        // are compared with each other
        assertTrue(isBeginEndInSameSentence(cas, s3.getBegin(), s4.getBegin()));

        // Note that these are invalid uses of isBeginEndInSameSentence because the first offset
        // must be a begin offset
        // Invalid because begin > end
        assertFalse(isBeginEndInSameSentence(cas, s1.getEnd(), s1.getBegin()));
        // Invalid because begin > end
        assertFalse(isBeginEndInSameSentence(cas, s2.getEnd(), s2.getBegin()));
    }
}
