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
package de.tudarmstadt.ukp.inception.support.text;

import org.apache.uima.jcas.tcas.Annotation;

public class TrimUtils
{
    /**
     * Trim the offsets of the given annotation to remove leading/trailing whitespace.
     * <p>
     * <b>Note:</b> use this method if the document text of the CAS has not been set yet but you
     * have it available in a buffer.
     * <p>
     * <b>Note:</b> best use this method before adding the annotation to the indexes.
     * 
     * @param aText
     *            the document text (available so far).
     * @param aAnnotation
     *            the annotation to trim. Offsets are updated.
     */
    public static void trim(CharSequence aText, Annotation aAnnotation)
    {
        int[] offsets = { aAnnotation.getBegin(), aAnnotation.getEnd() };
        trim(aText, offsets);
        aAnnotation.setBegin(offsets[0]);
        aAnnotation.setEnd(offsets[1]);
    }

    /**
     * Remove trailing or leading whitespace from the annotation.
     * 
     * @param aText
     *            the text.
     * @param aSpan
     *            the offsets.
     */
    public static void trim(CharSequence aText, int[] aSpan)
    {
        if (aSpan[0] == aSpan[1]) {
            // Nothing to do on empty spans
            return;
        }

        int begin = aSpan[0];
        int end = aSpan[1];

        // First we trim at the end. If a trimmed span is empty, we want to return the original
        // begin as the begin/end of the trimmed span
        while ((end > 0) && end > begin && trimChar(aText.charAt(end - 1))) {
            end--;
        }

        // Then, trim at the start
        while ((begin < (aText.length() - 1)) && begin < end && trimChar(aText.charAt(begin))) {
            begin++;
        }

        aSpan[0] = begin;
        aSpan[1] = end;
    }

    private static boolean trimChar(final char aChar)
    {
        switch (aChar) {
        case '\n': // Line break
        case '\r': // Carriage return
        case '\t': // Tab
        case '\u00A0': // NBSP
        case '\u2000': // EN QUAD
        case '\u2001': // EM QUAD
        case '\u2002': // EN SPACE
        case '\u2003': // EM SPACE
        case '\u2004': // THREE-PER-EM SPACE
        case '\u2005': // FOUR-PER-EM SPACE
        case '\u2006': // SIX-PER-EM SPACE
        case '\u2007': // FIGURE SPACE
        case '\u2008': // PUNCTUATION SPACE
        case '\u2009': // THIN SPACE
        case '\u200A': // HAIR SPACE
        case '\u200B': // ZERO WIDTH SPACE
        case '\u200C': // ZERO WIDTH NON-JOINER
        case '\u200D': // ZERO WIDTH JOINER
        case '\u200E': // LEFT-TO-RIGHT MARK
        case '\u200F': // RIGHT-TO-LEFT MARK
        case '\u2028': // LINE SEPARATOR
        case '\u2029': // PARAGRAPH SEPARATOR
        case '\u202A': // LEFT-TO-RIGHT EMBEDDING
        case '\u202B': // RIGHT-TO-LEFT EMBEDDING
        case '\u202C': // POP DIRECTIONAL FORMATTING
        case '\u202D': // LEFT-TO-RIGHT OVERRIDE
        case '\u202E': // RIGHT-TO-LEFT OVERRIDE
        case '\u202F': // NARROW NO-BREAK SPACE
        case '\u2060': // WORD JOINER
        case '\u2061': // FUNCTION APPLICATION
        case '\u2062': // INVISIBLE TIMES
        case '\u2063': // INVISIBLE SEPARATOR
        case '\u2064': // INVISIBLE PLUS
        case '\u2065': // <unassigned>
        case '\u2066': // LEFT-TO-RIGHT ISOLATE
        case '\u2067': // RIGHT-TO-LEFT ISOLATE
        case '\u2068': // FIRST STRONG ISOLATE
        case '\u2069': // POP DIRECTIONAL ISOLATE
        case '\u206A': // INHIBIT SYMMETRIC SWAPPING
        case '\u206B': // ACTIVATE SYMMETRIC SWAPPING
        case '\u206C': // INHIBIT ARABIC FORM SHAPING
        case '\u206D': // ACTIVATE ARABIC FORM SHAPING
        case '\u206E': // NATIONAL DIGIT SHAPES
        case '\u206F': // NOMINAL DIGIT SHAPES
            return true;
        default:
            return Character.isWhitespace(aChar);
        }
    }
}
