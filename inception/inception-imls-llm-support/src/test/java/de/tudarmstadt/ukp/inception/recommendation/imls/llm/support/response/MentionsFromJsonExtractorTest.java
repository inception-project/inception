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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class MentionsFromJsonExtractorTest
{
    private SpanJsonAnnotationTaskCodec sut = new SpanJsonAnnotationTaskCodec();

    @Test
    void testExtractMentionFromJson_categorizedNumbers()
    {
        var json = """
                {
                    "even_numbers": [2, 4, 6]
                }
                """;
        assertThat(sut.extractMentionFromJson(json)) //
                .containsExactly( //
                        Pair.of("2", "even_numbers"), //
                        Pair.of("4", "even_numbers"), //
                        Pair.of("6", "even_numbers"));
    }

    @Test
    void testExtractMentionFromJson_nullLabels()
    {
        var json = """
                {
                    "Honolulu": null,
                    "Columbia University": null,
                    "Harvard Law Review": null
                }
                """;
        assertThat(sut.extractMentionFromJson(json)) //
                .containsExactly( //
                        Pair.of("Honolulu", null), //
                        Pair.of("Columbia University", null), //
                        Pair.of("Harvard Law Review", null));
    }

    @Test
    void testExtractMentionFromJson_categorizedStrings()
    {
        var json = """
                {
                    "Person": ["John"],
                    "Location": ["diner", "Starbucks"]
                }
                """;
        assertThat(sut.extractMentionFromJson(json)) //
                .containsExactly( //
                        Pair.of("John", "Person"), //
                        Pair.of("diner", "Location"), //
                        Pair.of("Starbucks", "Location"));
    }

    @Test
    void testExtractMentionFromJson_categorizedObjects()
    {
        var json = """
                {
                    "politicians": [
                         { "name": "President Livingston" },
                         { "name": "John" },
                         { "name": "Don Horny" }
                     ]
                }
                """;
        assertThat(sut.extractMentionFromJson(json)) //
                .containsExactly( //
                        Pair.of("President Livingston", "politicians"), //
                        Pair.of("John", "politicians"), //
                        Pair.of("Don Horny", "politicians"));
    }

    @Test
    void testExtractMentionFromJson_structuredObjects()
    {
        var json = """
                {
                     "entities": [
                         {
                             "type": "politicians",
                             "text": "President Livingston"
                         },
                         {
                             "type": "politicians",
                             "text": "John"
                         },
                         {
                             "type": "politicians",
                             "text": "Don Horny"
                         }
                     ]
                 }
                """;
        assertThat(sut.extractMentionFromJson(json)) //
                .containsExactly( //
                        Pair.of("President Livingston", "politicians"), //
                        Pair.of("John", "politicians"), //
                        Pair.of("Don Horny", "politicians"));
    }

    @Test
    void testExtractMentionFromJson_namedObjects()
    {
        var json = """
                {
                    "John": {"type": "PERSON"},
                    "diner": {"type": "LOCATION"},
                    "Starbucks": {"type": "LOCATION"}
                }
                """;
        assertThat(sut.extractMentionFromJson(json)) //
                .containsExactly( //
                        Pair.of("John", null), //
                        Pair.of("diner", null), //
                        Pair.of("Starbucks", null));
    }

    @Test
    void testExtractMentionFromJson_keyValue()
    {
        var json = """
                {
                    "John": "politician",
                    "President Livingston": "politician",
                    "minister of foreign affairs": "politician",
                    "Don Horny": "politician"
                }
                """;
        assertThat(sut.extractMentionFromJson(json)) //
                .containsExactly( //
                        Pair.of("John", "politician"), //
                        Pair.of("President Livingston", "politician"), //
                        Pair.of("minister of foreign affairs", "politician"), //
                        Pair.of("Don Horny", "politician"));
    }

    @Disabled("Cannot really tell this one apart from keyValue")
    @Test
    void testExtractMentionFromJson_valueKey()
    {
        // We assume that the first item is the most relevant one (the
        // mention) so we do not get a bad mention in cases like this:
        var json = """
                {
                    "name": "Don Horny",
                    "affiliation": "Lord of Darkness"
                }
                """;
        assertThat(sut.extractMentionFromJson(json)) //
                .containsExactly( //
                        Pair.of("Don Horny", null));
    }
}
