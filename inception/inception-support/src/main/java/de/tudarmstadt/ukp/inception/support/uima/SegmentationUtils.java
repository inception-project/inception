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
import static java.text.BreakIterator.DONE;
import static java.util.Locale.US;

import java.text.BreakIterator;
import java.util.Locale;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;

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
        splitSentences(aCas);
        tokenize(aCas);
    }

    public static void splitSentences(CAS aCas)
    {
        splitSentences(aCas, 0, aCas.getDocumentText().length(), null);
    }

    public static void splitSentences(CAS aCas, int aBegin, int aEnd)
    {
        var bi = BreakIterator.getSentenceInstance(Locale.US);
        bi.setText(aCas.getDocumentText().substring(aBegin, aEnd));
        var last = bi.first();
        var cur = bi.next();
        while (cur != DONE) {
            var span = new int[] { last + aBegin, cur + aBegin };
            TrimUtils.trim(aCas.getDocumentText(), span);
            if (!isEmpty(span[0], span[1])) {
                aCas.addFsToIndexes(createSentence(aCas, span[0], span[1]));
            }
            last = cur;
            cur = bi.next();
        }
    }

    public static void splitSentences(CAS aCas, int aBegin, int aEnd,
            Iterable<? extends AnnotationFS> aZones)
    {
        if (aCas.getDocumentText() == null) {
            return;
        }

        int[] sortedZoneBoundaries = sortedZoneBoundaries(aCas, aBegin, aEnd, aZones);

        for (int i = 1; i < sortedZoneBoundaries.length; i++) {
            var begin = sortedZoneBoundaries[i - 1];
            var end = sortedZoneBoundaries[i];

            splitSentences(aCas, begin, end);
        }
    }

    public static void tokenize(CAS aCas)
    {
        tokenize(aCas, 0, aCas.getDocumentText().length(), null);
    }

    public static void tokenize(CAS aCas, int aBegin, int aEnd,
            Iterable<? extends AnnotationFS> aZones)
    {
        if (aCas.getDocumentText() == null) {
            return;
        }

        var sortedZoneBoundaries = sortedZoneBoundaries(aCas, aBegin, aEnd, aZones);
        var zbi = 0;

        for (var s : selectSentences(aCas)) {
            var innerZoneBoundariesBuffer = new IntArrayList();
            innerZoneBoundariesBuffer.add(s.getBegin());
            innerZoneBoundariesBuffer.add(s.getEnd());

            while (zbi < sortedZoneBoundaries.length && sortedZoneBoundaries[zbi] < s.getEnd()) {
                if (sortedZoneBoundaries[zbi] >= s.getBegin()) {
                    innerZoneBoundariesBuffer.add(sortedZoneBoundaries[zbi]);
                }
                zbi++;
            }

            var innerZoneBoundaries = innerZoneBoundariesBuffer.intStream()
                    .filter(i -> aBegin <= i && i <= aEnd) //
                    .distinct().sorted().toArray();

            for (int i = 1; i < innerZoneBoundaries.length; i++) {
                var begin = innerZoneBoundaries[i - 1];
                var end = innerZoneBoundaries[i];
                tokenize(aCas, begin, end);
            }
        }
    }

    private static void tokenize(CAS aCas, int aBegin, int aEnd)
    {
        var bi = BreakIterator.getWordInstance(US);
        bi.setText(aCas.getDocumentText().substring(aBegin, aEnd));
        var last = bi.first();
        var cur = bi.next();
        while (cur != DONE) {
            var span = new int[] { last + aBegin, cur + aBegin };
            TrimUtils.trim(aCas.getDocumentText(), span);
            if (!isEmpty(span[0], span[1])) {
                aCas.addFsToIndexes(createToken(aCas, span[0], span[1]));
            }
            last = cur;
            cur = bi.next();
        }
    }

    public static boolean isEmpty(int aBegin, int aEnd)
    {
        return aBegin >= aEnd;
    }

    private static int[] sortedZoneBoundaries(CAS aCas, int aBegin, int aEnd,
            Iterable<? extends AnnotationFS> aZones)
    {
        var zoneBoundaries = new IntArrayList();
        zoneBoundaries.add(aBegin);
        zoneBoundaries.add(aEnd);

        if (aZones != null) {
            for (var zone : aZones) {
                zoneBoundaries.add(zone.getBegin());
                zoneBoundaries.add(zone.getEnd());
            }
        }

        return zoneBoundaries.intStream() //
                .filter(i -> aBegin <= i && i <= aEnd) //
                .distinct().sorted().toArray();
    }
}
