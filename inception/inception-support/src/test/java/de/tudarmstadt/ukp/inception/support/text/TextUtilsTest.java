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

import static de.tudarmstadt.ukp.inception.support.text.TextUtils.containsAnyCharacterMatching;
import static de.tudarmstadt.ukp.inception.support.text.TextUtils.endsWithMatching;
import static de.tudarmstadt.ukp.inception.support.text.TextUtils.sortAndRemoveDuplicateCharacters;
import static de.tudarmstadt.ukp.inception.support.text.TextUtils.startsWithMatching;
import static java.lang.Character.CONNECTOR_PUNCTUATION;
import static java.lang.Character.DASH_PUNCTUATION;
import static java.lang.Character.END_PUNCTUATION;
import static java.lang.Character.FINAL_QUOTE_PUNCTUATION;
import static java.lang.Character.INITIAL_QUOTE_PUNCTUATION;
import static java.lang.Character.OTHER_PUNCTUATION;
import static java.lang.Character.START_PUNCTUATION;
import static java.lang.Character.charCount;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextUtilsTest
{
    @Test
    void testSortAndRemoveDuplicateCharacters()
    {
        assertThat(sortAndRemoveDuplicateCharacters("eeddccbbaa\0")).isEqualTo("\0abcde");
        assertThat(sortAndRemoveDuplicateCharacters("")).isEqualTo("");
    }

    @Test
    void testStartsWithMatching()
    {
        assertThat(startsWithMatching(" lala", Character::isWhitespace)).isTrue();
        assertThat(startsWithMatching("\tlala", Character::isWhitespace)).isTrue();
        assertThat(startsWithMatching("\nlala", Character::isWhitespace)).isTrue();
        assertThat(startsWithMatching("lala", Character::isWhitespace)).isFalse();
    }

    @Test
    void testEndsWithMatching()
    {
        assertThat(endsWithMatching("lala ", Character::isWhitespace)).isTrue();
        assertThat(endsWithMatching("lala\t", Character::isWhitespace)).isTrue();
        assertThat(endsWithMatching("lala\n", Character::isWhitespace)).isTrue();
        assertThat(endsWithMatching("lala", Character::isWhitespace)).isFalse();
    }

    @Test
    void testContainsAnyCharacterMatching()
    {
        assertThat(containsAnyCharacterMatching("la la", Character::isWhitespace)).isTrue();
        assertThat(containsAnyCharacterMatching("la\tla", Character::isWhitespace)).isTrue();
        assertThat(containsAnyCharacterMatching("la\nla", Character::isWhitespace)).isTrue();
        assertThat(containsAnyCharacterMatching("lala", Character::isWhitespace)).isFalse();

        assertThat(containsAnyCharacterMatching("la\0la", TextUtils::isControlCharacter)).isTrue();
        assertThat(containsAnyCharacterMatching("lala", TextUtils::isControlCharacter)).isFalse();
        assertThat(containsAnyCharacterMatching("la\u0016la", TextUtils::isControlCharacter))
                .isTrue();
    }

    @Test
    void testIsControlCharacter()
    {
        for (char c = 0; c < 255; c++) {
            if (c < 32) {
                assertThat(TextUtils.isControlCharacter(c)).isTrue();
            }
            else if (c == 127) {
                assertThat(TextUtils.isControlCharacter(c)).isTrue();
            }
            else if (c >= 128 && c <= 159) {
                assertThat(TextUtils.isControlCharacter(c)).isTrue();
            }
            else {
                assertThat(TextUtils.isControlCharacter(c)).isFalse();
            }
        }
    }

    @Test
    void asciiObfuscation_preservesWhitespacePunctuationAndLengths()
    {
        var input = "My number is 555-1234.";
        var ob = TextUtils.obfuscate(input);

        assertThat(ob).isNotNull();
        assertThat(ob.length()).as("UTF-16 length must match").isEqualTo(input.length());
        assertThat(ob.codePointCount(0, ob.length())).as("Code point count must match")
                .isEqualTo(input.codePointCount(0, input.length()));
        assertThat(ob.getBytes(UTF_8).length).as("UTF-8 byte length must match")
                .isEqualTo(input.getBytes(UTF_8).length);

        // Whitespace and punctuation must be preserved exactly at the same positions
        int i1 = 0, i2 = 0;
        while (i1 < input.length() && i2 < ob.length()) {
            int cp1 = input.codePointAt(i1);
            int cp2 = ob.codePointAt(i2);

            if (Character.isWhitespace(cp1) || isPunctuationType(Character.getType(cp1))) {
                assertThat(cp2).as("Whitespace/punctuation must be preserved at offset " + i1)
                        .isEqualTo(cp1);
            }

            i1 += charCount(cp1);
            i2 += charCount(cp2);
        }
    }

    @Test
    void multilingualObfuscation_preservesEncodingsAndCounts()
    {
        // mixture: Arabic words + Arabic comma, ASCII exclamation, Arabic-Indic digits, emoji, CJK
        var input = "مرحبا، عالم! ١٢٣ 😃 中";
        var ob = TextUtils.obfuscate(input);

        assertThat(ob).isNotNull();
        assertThat(ob.length()).as("UTF-16 length must match").isEqualTo(input.length());
        assertThat(ob.codePointCount(0, ob.length())).as("Code point count must match")
                .isEqualTo(input.codePointCount(0, input.length()));
        assertThat(ob.getBytes(UTF_8).length).as("UTF-8 byte length must match")
                .isEqualTo(input.getBytes(UTF_8).length);

        int i1 = 0, i2 = 0;
        while (i1 < input.length() && i2 < ob.length()) {
            int cp1 = input.codePointAt(i1);
            int cp2 = ob.codePointAt(i2);
            if (Character.isWhitespace(cp1) || isPunctuationType(Character.getType(cp1))) {
                assertThat(cp2).as("Whitespace/punctuation must be preserved at offset " + i1)
                        .isEqualTo(cp1);
            }
            i1 += charCount(cp1);
            i2 += charCount(cp2);
        }
    }

    @Test
    void digitsAreReplacedWithZeroInSameDigitBlock()
    {
        var asciiDigits = "0123456789";
        var obAscii = TextUtils.obfuscate(asciiDigits);
        assertThat(obAscii.length()).isEqualTo(asciiDigits.length());
        for (int i = 0; i < asciiDigits.length(); i++) {
            // all ASCII digits must be replaced by ASCII '0'
            assertThat(obAscii.charAt(i)).isEqualTo('0');
        }

        var arabicIndic = "٠١٢٣٤٥٦٧٨٩"; // U+0660..U+0669
        var obArabic = TextUtils.obfuscate(arabicIndic);
        // ensure code point counts and UTF-8 lengths are preserved
        assertThat(arabicIndic.codePointCount(0, arabicIndic.length()))
                .isEqualTo(obArabic.codePointCount(0, obArabic.length()));
        assertThat(arabicIndic.getBytes(UTF_8).length).isEqualTo(obArabic.getBytes(UTF_8).length);

        // all resulting digits must have numeric value 0 (i.e., be the zero in that digit block)
        int i = 0;
        while (i < obArabic.length()) {
            int cp = obArabic.codePointAt(i);
            int val = Character.getNumericValue(cp);
            assertThat(val).as("Digit must have numeric value 0").isEqualTo(0);
            i += charCount(cp);
        }
    }

    static boolean isPunctuationType(int aType)
    {
        switch (aType) {
        case CONNECTOR_PUNCTUATION:
        case DASH_PUNCTUATION:
        case START_PUNCTUATION:
        case END_PUNCTUATION:
        case OTHER_PUNCTUATION:
        case INITIAL_QUOTE_PUNCTUATION:
        case FINAL_QUOTE_PUNCTUATION:
            return true;
        default:
            return false;
        }
    }
}
