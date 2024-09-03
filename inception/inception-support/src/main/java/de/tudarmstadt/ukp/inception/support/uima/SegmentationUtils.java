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

import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.createSentence;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.createToken;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.selectSentences;
import static org.apache.uima.fit.util.CasUtil.getType;

import java.text.BreakIterator;
import java.util.Locale;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.support.text.TrimUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public abstract class SegmentationUtils
{
    private SegmentationUtils()
    {
        // No instances
    }

    public static void segment(CAS aCas)
    {
        splitSentences(aCas, null);
        tokenize(aCas);
    }

    public static void splitSentences(CAS aCas)
    {
        splitSentences(aCas, null);
    }

    public static void splitSentences(CAS aCas, int aBegin, int aEnd)
    {
        var bi = BreakIterator.getSentenceInstance(Locale.US);
        bi.setText(aCas.getDocumentText().substring(aBegin, aEnd));
        var last = bi.first();
        var cur = bi.next();
        while (cur != BreakIterator.DONE) {
            var sentence = aCas.createAnnotation(getType(aCas, Sentence.class), last + aBegin,
                    cur + aBegin);
            sentence.trim();
            if (sentence.getBegin() != sentence.getEnd()) {
                aCas.addFsToIndexes(sentence);
            }
            last = cur;
            cur = bi.next();
        }
    }

    public static void splitSentences(CAS aCas, Iterable<? extends AnnotationFS> aZones)
    {
        if (aCas.getDocumentText() == null) {
            return;
        }

        int[] sortedZoneBoundaries = null;

        if (aZones != null) {
            var zoneBoundaries = new IntArrayList();
            for (var zone : aZones) {
                zoneBoundaries.add(zone.getBegin());
                zoneBoundaries.add(zone.getEnd());
            }

            sortedZoneBoundaries = zoneBoundaries.intStream().distinct().sorted().toArray();
        }

        if (sortedZoneBoundaries == null || sortedZoneBoundaries.length < 2) {
            sortedZoneBoundaries = new int[] { 0, aCas.getDocumentText().length() };
        }

        for (int i = 1; i < sortedZoneBoundaries.length; i++) {
            var begin = sortedZoneBoundaries[i - 1];
            var end = sortedZoneBoundaries[i];
            var bi = BreakIterator.getSentenceInstance(Locale.US);
            bi.setText(aCas.getDocumentText().substring(begin, end));
            var last = bi.first();
            var cur = bi.next();
            while (cur != BreakIterator.DONE) {
                var span = new int[] { last + begin, cur + begin };
                TrimUtils.trim(aCas.getDocumentText(), span);
                if (!isEmpty(span[0], span[1])) {
                    aCas.addFsToIndexes(createSentence(aCas, span[0], span[1]));
                }
                last = cur;
                cur = bi.next();
            }
        }
    }

    public static void tokenize(CAS aCas)
    {
        if (aCas.getDocumentText() == null) {
            return;
        }

        BreakIterator bi = BreakIterator.getWordInstance(Locale.US);
        for (AnnotationFS s : selectSentences(aCas)) {
            bi.setText(s.getCoveredText());
            int last = bi.first();
            int cur = bi.next();
            while (cur != BreakIterator.DONE) {
                int[] span = new int[] { last, cur };
                TrimUtils.trim(s.getCoveredText(), span);
                if (!isEmpty(span[0], span[1])) {
                    aCas.addFsToIndexes(
                            createToken(aCas, span[0] + s.getBegin(), span[1] + s.getBegin()));
                }
                last = cur;
                cur = bi.next();
            }
        }
    }

    public static boolean isEmpty(int aBegin, int aEnd)
    {
        return aBegin >= aEnd;
    }
}
