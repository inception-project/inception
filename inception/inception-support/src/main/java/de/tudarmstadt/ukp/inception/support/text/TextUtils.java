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

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.function.Predicate;

public class TextUtils
{
    public static final char SHY = '\u00AD';
    public static final char NBSP = '\u00A0';

    public static String sortAndRemoveDuplicateCharacters(String... aCharacterSets)
    {
        if (aCharacterSets.length == 0) {
            return "";
        }

        var characters = new HashSet<Character>();
        for (var characterSet : aCharacterSets) {
            for (int i = 0; i < characterSet.length(); i++) {
                characters.add(characterSet.charAt(i));
            }
        }

        var sortedCharacters = new ArrayList<>(characters);
        sortedCharacters.sort(Character::compare);

        StringBuilder result = new StringBuilder();
        for (char c : sortedCharacters) {
            result.append(c);
        }

        return result.toString();
    }

    public static boolean startsWithMatching(String aValue, Predicate<Character> aPredicate)
    {
        if (aValue == null || aValue.isEmpty()) {
            return false;
        }

        return aPredicate.test(aValue.charAt(0));
    }

    public static boolean endsWithMatching(String aValue, Predicate<Character> aPredicate)
    {
        if (aValue == null || aValue.isEmpty()) {
            return false;
        }

        return aPredicate.test(aValue.charAt(aValue.length() - 1));
    }

    public static boolean containsNoCharacterMatching(String aValue,
            Predicate<Character> aPredicate)
    {
        return !containsAnyCharacterMatching(aValue, aPredicate);
    }

    public static boolean containsAnyCharacterMatching(String aValue,
            Predicate<Character> aPredicate)
    {
        var iter = new StringCharacterIterator(aValue);
        for (char c = iter.first(); c != CharacterIterator.DONE; c = iter.next()) {
            if (aPredicate.test(c)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isControlCharacter(char aChar)
    {
        return aChar < 32 || aChar == 127 || (aChar >= 128 && aChar <= 159);
    }

    public static String sanitizeVisibleText(String aText, char aReplacementCharacter)
    {
        char[] chars = aText.toCharArray();

        sanitizeVisibleText(chars, aReplacementCharacter, 0, chars.length);

        return new String(chars);
    }

    public static void sanitizeVisibleText(char[] aText, char aReplacementCharacter, int aStart,
            int aLength)
    {
        // NBSP is recognized by Firefox as a proper addressable character in
        // SVGText.getNumberOfChars()
        char whitespaceReplacementChar = NBSP; // NBSP
        if (aReplacementCharacter > 0) {
            whitespaceReplacementChar = aReplacementCharacter;
        }

        for (int i = aStart; i < aLength; i++) {
            switch (aText[i]) {
            // Replace newline characters before sending to the client to avoid
            // rendering glitches in the client-side brat rendering code
            case '\n':
            case '\r':
                aText[i] = ' ';
                break;
            // Some browsers (e.g. Firefox) do not count invisible chars in some functions
            // (e.g. SVGText.getNumberOfChars()) and this causes trouble. See:
            //
            // - https://github.com/webanno/webanno/issues/307
            // - https://github.com/inception-project/inception/issues/1849
            //
            // To avoid this, we replace the chars with a visible whitespace character before
            // sending the data to the browser. Hopefully this makes sense.
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
                aText[i] = whitespaceReplacementChar;
                break;
            default:
                // Nothing to do
            }
        }
    }

    public static void sanitizeIllegalXmlCharacters(char[] aText, char aReplacementCharacter,
            int aStart, int aLength)
    {
        char whiteplaceReplacementChar = ' '; // SPACE
        if (aReplacementCharacter > 0) {
            whiteplaceReplacementChar = aReplacementCharacter;
        }

        for (int i = aStart; i < aLength; i++) {
            switch (aText[i]) {
            case '\u0000': // NULL
            case '\u0001': // START OF HEADING
            case '\u0002': // START OF TEXT
            case '\u0003': // END OF TEXT
            case '\u0004': // END OF TRANSMISSION
            case '\u0005': // ENQUIRY
            case '\u0006': // ACKNOWLEDGE
            case '\u0007': // BELL
            case '\b': // BACKSPACE '\u0008'
            case '\u000B': // VERTICAL TAB
            case '\f': // FORM FEED '\u000C'
            case '\u000E': // SHIFT OUT
            case '\u000F': // SHIFT IN
            case '\u0010': // DATA LINK ESCAPE
            case '\u0011': // DEVICE CONTROL ONE
            case '\u0012': // DEVICE CONTROL TWO
            case '\u0013': // DEVICE CONTROL THREE
            case '\u0014': // DEVICE CONTROL FOUR
            case '\u0015': // NEGATIVE ACKNOWLEDGE
            case '\u0016': // SYNCHRONOUS IDLE
            case '\u0017': // END OF TRANSMISSION BLOCK
            case '\u0018': // CANCEL
            case '\u0019': // END OF MEDIUM
            case '\u001A': // SUBSTITUTE
            case '\u001B': // ESCAPE
            case '\u001C': // FILE SEPARATOR
            case '\u001D': // GROUP SEPARATOR
            case '\u001E': // RECORD SEPARATOR
            case '\u001F': // UNIT SEPARATOR
            case '\uFFFE': // NONCHARACTER U+FFFE
            case '\uFFFF': // NONCHARACTER U+FFFF
                aText[i] = whiteplaceReplacementChar;
                break;
            default:
                // Nothing to do
            }
        }
    }
}
