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
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.trie;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.StringMatchingRecommender.DictEntry;

public class TrieTest
{
    private Trie<DictEntry> sut;

    @BeforeEach
    public void setup()
    {
        sut = new Trie<DictEntry>();
    }

    @Test
    public void thatElementsCanBeAddedAndRetrieved()
    {
        List<String> keys = asList("1", "asf", "asf sadf", "dsjkla sfasd kj92");

        for (String key : keys) {
            sut.put(key, new DictEntry(key));
        }

        assertThat(sut.size()).isEqualTo(keys.size());
        assertThat(sut.keys()).containsExactlyInAnyOrderElementsOf(keys);
        assertThat(sut.keyIterator()).toIterable().containsExactlyInAnyOrderElementsOf(keys);

        for (String key : keys) {
            assertThat(sut.getNode(key)).isNotNull();
        }

        assertThat(sut.getNode("029332")).isNull();
    }

    @Test
    public void thatExactMatchesCanBeFound()
    {
        sut.put("in", new DictEntry("in"));

        assertThat(sut.get("initially")).isNull();
    }

    @Test
    public void testThatKeySanitizerWorks()
    {
        sut = new Trie<DictEntry>(WhitespaceNormalizingSanitizer.factory());

        sut.put("  this is\ta test\n  .", new DictEntry("exists"));

        System.out.println(sut.keys());

        assertThat(sut.getNode("this is a test .")).isNotNull();
        assertThat(sut.getNode("this is a test .").node.level).isEqualTo(16);
        assertThat(sut.getNode("this is a test .").matchLength).isEqualTo(16);
        assertThat(sut.getNode("  this is\ta test  .")).isNotNull();
        assertThat(sut.getNode("  this is\ta test  .").node.level).isEqualTo(16);
        assertThat(sut.getNode("  this is\ta test  .").matchLength).isEqualTo(19);
    }
}
