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
package de.tudarmstadt.ukp.inception.recommendation.imls.ollama.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class JsonExtractorTest
{
    private MentionsFromJsonExtractor sut = new MentionsFromJsonExtractor();

    @Test
    void testExtractMentionFromJson_variant1()
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
    void testExtractMentionFromJson_variant2()
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
    void testExtractMentionFromJson_variant3()
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
    void testExtractMentionFromJson_variant4()
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

    @Disabled("Cannot really tell this one apart from variant 4")
    @Test
    void testExtractMentionFromJson_variant5()
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
