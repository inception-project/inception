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
package de.tudarmstadt.ukp.inception.search.index.mtas;

import static de.tudarmstadt.ukp.inception.search.index.mtas.MtasDocumentIndex.preprocessQuery;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.search.model.AnnotationSearchState;

/**
 * Unit tests for {@link MtasDocumentIndex#preprocessQuery(String, AnnotationSearchState)} static
 * method.
 */
class MtasDocumentIndexPreprocessQueryTest
{
    @Test
    void thatPreprocessQuery_withCQLQuery_returnsUnchanged()
    {
        var prefs = new AnnotationSearchState();

        var queriesWithSpecialChars = List.of( //
                "[pos=\"NN\"]", //
                "<Token/>", //
                "\"quoted text\"", //
                "[word=\"test\"]", //
                "<Token>\"content\"</Token>" //
        );

        for (var query : queriesWithSpecialChars) {
            var result = preprocessQuery(query, prefs);
            assertThat(result) //
                    .as("CQL query should not be modified") //
                    .isEqualTo(query);
        }
    }

    @Test
    void thatPreprocessQuery_withSimpleText_wrapsInQuotes()
    {
        var prefs = new AnnotationSearchState();

        var result = preprocessQuery("test", prefs);

        assertThat(result).isEqualTo("\"test\"");
    }

    @Test
    void thatPreprocessQuery_withMultipleWords_wrapsEachWord()
    {
        var prefs = new AnnotationSearchState();

        var result = preprocessQuery("hello world", prefs);

        assertThat(result).isEqualTo("\"hello\" \"world\"");
    }

    @Test
    void thatPreprocessQuery_withCaseInsensitive_lowercasesWords()
    {
        var prefs = new AnnotationSearchState();
        prefs.setCaseSensitiveDocumentText(false);

        var result = preprocessQuery("Hello World", prefs);

        assertThat(result).isEqualTo("\"hello\" \"world\"");
    }

    @Test
    void thatPreprocessQuery_withCaseSensitive_preservesCase()
    {
        var prefs = new AnnotationSearchState();
        prefs.setCaseSensitiveDocumentText(true);

        var result = preprocessQuery("Hello World", prefs);

        assertThat(result).isEqualTo("\"Hello\" \"World\"");
    }

    @Test
    void thatPreprocessQuery_escapesSpecialCharacters()
    {
        var prefs = new AnnotationSearchState();

        // Test individual special characters that need escaping
        // Note: BreakIterator treats special characters as separate tokens
        assertThat(preprocessQuery("test&more", prefs)) //
                .isEqualTo("\"test\" \"\\&\" \"more\"");

        assertThat(preprocessQuery("test(more)", prefs)) //
                .isEqualTo("\"test\" \"\\(\" \"more\" \"\\)\"");

        assertThat(preprocessQuery("test#more", prefs)) //
                .isEqualTo("\"test\" \"\\#\" \"more\"");

        assertThat(preprocessQuery("test{more}", prefs)) //
                .isEqualTo("\"test\" \"\\{\" \"more\" \"\\}\"");
    }

    @Test
    void thatPreprocessQuery_withMultipleSpecialChars_escapesAll()
    {
        var prefs = new AnnotationSearchState();

        var result = preprocessQuery("test&(more)#data", prefs);

        // BreakIterator treats punctuation as separate tokens
        assertThat(result).isEqualTo("\"test\" \"\\&\" \"\\(\" \"more\" \"\\)\" \"\\#\" \"data\"");
    }

    @Test
    void thatPreprocessQuery_withWhitespace_handlesCorrectly()
    {
        var prefs = new AnnotationSearchState();

        // Multiple spaces between words
        var result = preprocessQuery("hello   world", prefs);
        assertThat(result).isEqualTo("\"hello\" \"world\"");

        // Leading/trailing whitespace is handled by word iterator
        result = preprocessQuery("  hello  ", prefs);
        assertThat(result).isEqualTo("\"hello\"");
    }

    @Test
    void thatPreprocessQuery_withPunctuation_treatsAsWords()
    {
        var prefs = new AnnotationSearchState();

        var result = preprocessQuery("hello, world!", prefs);

        // Punctuation is treated as separate tokens by BreakIterator
        assertThat(result).isEqualTo("\"hello\" \",\" \"world\" \"!\"");
    }

    @Test
    void thatPreprocessQuery_withEmptyString_returnsEmpty()
    {
        var prefs = new AnnotationSearchState();

        var result = preprocessQuery("", prefs);

        assertThat(result).isEmpty();
    }

    @Test
    void thatPreprocessQuery_withOnlyWhitespace_returnsEmpty()
    {
        var prefs = new AnnotationSearchState();

        var result = preprocessQuery("   ", prefs);

        assertThat(result).isEmpty();
    }

    @Test
    void thatPreprocessQuery_withMixedContent_handlesComplex()
    {
        var prefs = new AnnotationSearchState();
        prefs.setCaseSensitiveDocumentText(false);

        var result = preprocessQuery("Test&Data (2024)", prefs);

        // BreakIterator treats special characters and spaces as separate tokens
        assertThat(result).isEqualTo("\"test\" \"\\&\" \"data\" \"\\(\" \"2024\" \"\\)\"");
    }

    @Test
    void thatPreprocessQuery_doesNotEscapeWhenCQLDetected()
    {
        var prefs = new AnnotationSearchState();

        // If CQL syntax is detected (contains quotes, brackets, or angle brackets),
        // the query should be returned as-is without escaping
        var result = preprocessQuery("test & more \"quoted\"", prefs);

        // Because it contains quotes, it's treated as CQL and returned unchanged
        assertThat(result).isEqualTo("test & more \"quoted\"");
    }

    @Test
    void thatPreprocessQuery_escapesAllCharsToEscape()
    {
        var prefs = new AnnotationSearchState();

        // Verify that every character in CHARS_TO_ESCAPE is properly escaped
        for (var aChar : MtasDocumentIndex.CHARS_TO_ESCAPE) {
            // Skip characters that trigger CQL mode (they're returned unchanged)
            if (List.of(MtasDocumentIndex.CQL_TRIGGER_CHARS).contains(aChar)) {
                continue;
            }

            var input = "test" + aChar + "word";
            var result = preprocessQuery(input, prefs);

            // The character should appear escaped in the output
            // Note: BreakIterator may treat special chars as separate tokens
            assertThat(result).as("Character '%s' should be escaped", aChar).contains("\\" + aChar);
        }
    }
}
