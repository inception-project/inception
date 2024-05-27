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

import java.text.BreakIterator;
import java.util.Locale;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;

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
            BreakIterator bi = BreakIterator.getSentenceInstance(Locale.US);
            bi.setText(aCas.getDocumentText().substring(begin, end));
            int last = bi.first();
            int cur = bi.next();
            while (cur != BreakIterator.DONE) {
                int[] span = new int[] { last + begin, cur + begin };
                trim(aCas.getDocumentText(), span);
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
                trim(s.getCoveredText(), span);
                if (!isEmpty(span[0], span[1])) {
                    aCas.addFsToIndexes(
                            createToken(aCas, span[0] + s.getBegin(), span[1] + s.getBegin()));
                }
                last = cur;
                cur = bi.next();
            }
        }
    }

    /**
     * Remove trailing or leading whitespace from the annotation.
     * 
     * @param aText
     *            the text.
     * @param aSpan
     *            the offsets.
     */
    public static void trim(String aText, int[] aSpan)
    {
        String data = aText;

        int begin = aSpan[0];
        int end = aSpan[1] - 1;

        // Remove whitespace at end
        while ((end > 0) && trimChar(data.charAt(end))) {
            end--;
        }
        end++;

        // Remove whitespace at start
        while ((begin < end) && trimChar(data.charAt(begin))) {
            begin++;
        }

        aSpan[0] = begin;
        aSpan[1] = end;
    }

    public static boolean isEmpty(int aBegin, int aEnd)
    {
        return aBegin >= aEnd;
    }

    public static boolean trimChar(final char aChar)
    {
        switch (aChar) {
        case '\n':
            return true; // Line break
        case '\r':
            return true; // Carriage return
        case '\t':
            return true; // Tab
        case '\u200E':
            return true; // LEFT-TO-RIGHT MARK
        case '\u200F':
            return true; // RIGHT-TO-LEFT MARK
        case '\u2028':
            return true; // LINE SEPARATOR
        case '\u2029':
            return true; // PARAGRAPH SEPARATOR
        default:
            return Character.isWhitespace(aChar);
        }
    }

}
