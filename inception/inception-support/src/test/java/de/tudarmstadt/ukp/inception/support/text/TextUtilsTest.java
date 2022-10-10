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
}
