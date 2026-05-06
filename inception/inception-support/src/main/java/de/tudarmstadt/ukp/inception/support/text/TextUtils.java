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

import static java.lang.Character.charCount;
import static java.lang.Character.getNumericValue;
import static java.lang.Character.getType;
import static java.lang.Character.isDigit;
import static java.lang.Character.isLetter;
import static java.lang.Character.isLowerCase;
import static java.lang.Character.isUpperCase;
import static java.lang.Character.isWhitespace;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.function.Predicate;

public class TextUtils
{
    private static final char DIGIT_1_BYTE = '0';

    // 'x' - ASCII small x, replacement for ASCII lower-case letters
    private static final char LETTER_LC_1_BYTE = 'x';

    // 'X' - ASCII capital X, replacement for ASCII upper-case letters
    private static final char LETTER_UC_1_BYTE = 'X';

    // 0x0101: 'ā' (LATIN SMALL LETTER A WITH MACRON)
    // Used as a 2-byte UTF-8 placeholder for lower-case two-byte letters
    private static final int LETTER_LC_2_BYTE = 0x0101;

    // 0x0100: 'Ā' (LATIN CAPITAL LETTER A WITH MACRON)
    // Used as a 2-byte UTF-8 placeholder for upper-case two-byte letters
    private static final int LETTER_UC_2_BYTE = 0x0100;

    // 0x4E2D: '中' (CJK UNIFIED IDEOGRAPH-4E2D)
    // Used as a 3-byte UTF-8 placeholder for CJK or other 3-byte characters
    private static final int LETTER_3_BYTE = 0x4E2D;

    // ASCII 's' (0x73) - single-byte non-punctuation placeholder for ASCII symbols.
    // Note: ASCII contains no non-punctuation symbol characters, so we choose a
    // simple non-punctuation ASCII letter to avoid introducing punctuation.
    private static final char SYMBOL_1_BYTE = 's';

    // 0x00A9: '©' (COPYRIGHT SIGN)
    // 2-byte UTF-8 placeholder for two-byte symbols (not punctuation)
    private static final int SYMBOL_2_BYTE = 0x00A9;

    // 0x20AC: '€' (EURO SIGN)
    // 3-byte UTF-8 placeholder for three-byte symbols (not punctuation)
    private static final int SYMBOL_3_BYTE = 0x20AC;

    // 0x1F600: '😀' (GRINNING FACE)
    // Used as a placeholder for 4-byte Unicode emoji/code points
    private static final int SYMBOL_4_BYTE = 0x1F600;

    // '\u00AD' SOFT HYPHEN (SHY) - often invisible but used in sanitization
    public static final char SHY = '\u00AD';

    // '\u00A0' NO-BREAK SPACE (NBSP)
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

    public static String obfuscate(String aSource)
    {
        if (aSource == null) {
            return null;
        }

        var sb = new StringBuilder(aSource.length());

        for (var i = 0; i < aSource.length();) {
            var cp = aSource.codePointAt(i);
            var charCount = charCount(cp);
            var type = getType(cp);

            // Preserve whitespace and punctuation
            if (isWhitespace(cp) || isPunctuationType(type)) {
                sb.appendCodePoint(cp);
                i += charCount;
                continue;
            }

            // Digits -> zero in same digit block if possible
            if (isDigit(cp)) {
                int val = getNumericValue(cp);
                if (val >= 0 && val <= 9) {
                    int zeroCp = cp - val;
                    sb.appendCodePoint(zeroCp);
                }
                else {
                    sb.append(DIGIT_1_BYTE);
                }
                i += charCount;
                continue;
            }

            int utf8len = utf8LengthForCodePoint(cp);

            // Letters: preserve case where relevant, choose placeholder by UTF-8 length
            if (isLetter(cp)) {
                if (charCount == 1) {
                    if (utf8len == 1) {
                        if (isUpperCase(cp)) {
                            sb.append(LETTER_UC_1_BYTE);
                        }
                        else if (isLowerCase(cp)) {
                            sb.append(LETTER_LC_1_BYTE);
                        }
                        else {
                            sb.append(LETTER_LC_1_BYTE);
                        }
                    }
                    else if (utf8len == 2) {
                        sb.appendCodePoint(isUpperCase(cp) ? LETTER_UC_2_BYTE : LETTER_LC_2_BYTE);
                    }
                    else {
                        sb.appendCodePoint(LETTER_3_BYTE); // CJK placeholder (3-byte UTF-8)
                    }
                }
                else {
                    sb.appendCodePoint(SYMBOL_4_BYTE); // emoji placeholder (4-byte UTF-8)
                }
                i += charCount;
                continue;
            }

            // Other characters (symbols, marks, etc.) — use dedicated symbol placeholders
            if (charCount == 1) {
                if (utf8len == 1) {
                    // ASCII symbol: use single-byte non-punctuation placeholder
                    sb.append(SYMBOL_1_BYTE);
                }
                else if (utf8len == 2) {
                    sb.appendCodePoint(SYMBOL_2_BYTE);
                }
                else {
                    sb.appendCodePoint(SYMBOL_3_BYTE);
                }
            }
            else {
                sb.appendCodePoint(SYMBOL_4_BYTE);
            }
            i += charCount;
        }

        return sb.toString();
    }

    private static int utf8LengthForCodePoint(int aCp)
    {
        if (aCp <= 0x7F) {
            return 1;
        }
        else if (aCp <= 0x7FF) {
            return 2;
        }
        else if (aCp <= 0xFFFF) {
            return 3;
        }
        else {
            return 4;
        }
    }

    private static boolean isPunctuationType(int aType)
    {
        switch (aType) {
        case Character.CONNECTOR_PUNCTUATION:
        case Character.DASH_PUNCTUATION:
        case Character.START_PUNCTUATION:
        case Character.END_PUNCTUATION:
        case Character.OTHER_PUNCTUATION:
        case Character.INITIAL_QUOTE_PUNCTUATION:
        case Character.FINAL_QUOTE_PUNCTUATION:
            return true;
        default:
            return false;
        }
    }
}
