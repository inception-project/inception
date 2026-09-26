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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TrieTest
{
    private Trie<String> sut = new Trie<>();

    @BeforeEach
    public void setup()
    {
        sut = new Trie<String>();
    }

    @Test
    public void thatElementsCanBeAddedAndRetrieved()
    {
        List<String> keys = asList("1", "asf", "asf sadf", "dsjkla sfasd kj92");

        for (String key : keys) {
            sut.put(key, key);
        }

        assertThat(sut.size()).isEqualTo(keys.size());
        assertThat(sut.keys()).containsExactlyInAnyOrderElementsOf(keys);
        assertThat(sut.keyIterator()).toIterable().containsExactlyInAnyOrderElementsOf(keys);

        for (String key : keys) {
            assertThat(sut.getNode(key)).isNotNull();
        }

        assertThat(sut.getNode("asf sadf", 0)) //
                .extracting($ -> $.matchLength) //
                .isEqualTo(8);
    }

    @Test
    public void thatExactMatchesCanBeFound()
    {
        sut.put("in", "in");

        assertThat(sut.get("initially")).isNull();
    }

    @Test
    public void thatMatchLengthEndsAtLongestMatchingKey()
    {
        sut = new Trie<>(WhitespaceNormalizingSanitizer.factory());

        sut.put("Washington", "Location");
        sut.put("Washington State University", "Organization");

        var text = "In Washington is the capital.";
        var match = sut.getNode(text, 3);

        assertThat(match).isNotNull();
        assertThat(match.node.value).isEqualTo("Location");
        assertThat(match.matchLength).isEqualTo("Washington".length());

        match = sut.getNode("Washington State University.", 0);

        assertThat(match).isNotNull();
        assertThat(match.node.value).isEqualTo("Organization");
        assertThat(match.matchLength).isEqualTo("Washington State University".length());
    }

    @Test
    public void thatAllMatchingKeysAreReturnedFromShortestToLongest()
    {
        sut = new Trie<>(WhitespaceNormalizingSanitizer.factory());

        sut.put("Washington", "Location");
        sut.put("Washington State", "Organization");
        sut.put("Washington State University", "Organization");

        assertThat(sut.getNodes("Washington  Statesman spoke.", 0)) //
                .extracting($ -> $.node.value, $ -> $.matchLength) //
                .containsExactly( //
                        tuple("Location", "Washington".length()), //
                        tuple("Organization", "Washington  State".length()));

        assertThat(sut.getNodes("Wash", 0)).isEmpty();
        assertThat(sut.getNodes(" Washington", 0)).isEmpty();
    }

    @Test
    public void thatCaseFoldingKeepsMatchLengthAlignedWithText()
    {
        sut = new Trie<>(WhitespaceNormalizingSanitizer.factory(true));

        sut.put("istanbul", "LOC");
        sut.put("ΟΔΟΣ", "street");

        // "İ".toLowerCase() yields two characters
        assertThat(sut.getNode("İstanbul is big", 0)) //
                .extracting($ -> $.node.value, $ -> $.matchLength) //
                .containsExactly("LOC", "İstanbul".length());

        // Final sigma matches upper case sigma
        assertThat(sut.getNode("οδος", 0)) //
                .extracting($ -> $.node.value, $ -> $.matchLength) //
                .containsExactly("street", 4);

        sut = new Trie<>(WhitespaceNormalizingSanitizer.factory());
        sut.put("istanbul", "LOC");

        assertThat(sut.getNode("İstanbul", 0)).isNull();
    }

    @Test
    public void testThatKeySanitizerWorks()
    {
        sut = new Trie<>(WhitespaceNormalizingSanitizer.factory());

        sut.put("  this is\ta test\n  .", "exists");

        System.out.println(sut.keys());

        assertThat(sut.getNode("this is a test .")).isNotNull();
        assertThat(sut.getNode("this is a test .").node.level).isEqualTo(16);
        assertThat(sut.getNode("this is a test .").matchLength).isEqualTo(16);
        assertThat(sut.getNode("  this is\ta test  .")).isNotNull();
        assertThat(sut.getNode("  this is\ta test  .").node.level).isEqualTo(16);
        assertThat(sut.getNode("  this is\ta test  .").matchLength).isEqualTo(19);
    }

}
