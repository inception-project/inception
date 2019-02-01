/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.sanitizers.WhitespaceNormalizingSanitizer;

public class TrieTest
{
    private Trie<String> sut;

    @Before
    public void setup()
    {
        sut = new Trie<String>();
    }

    @Test
    public void thatElementsCanBeAddedAndRetrieved()
    {
        List<String> keys = asList("1", "asf", "asf sadf", "dsjkla sfasd kj92");

        int i = 0;
        for (String key : keys) {
            sut.put(key, String.valueOf(i));
            i++;
        }

        assertThat(sut.keys()).containsExactlyInAnyOrderElementsOf(keys);
        assertThat(sut.size()).isEqualTo(keys.size());
        
        for (String key : keys) {
            assertThat(sut.getNode(key)).isNotNull();
        }
        
        assertThat(sut.getNode("029332")).isNull();
    }
    
    @Test
    public void testThatKeySanitizerWorks()
    {
        sut = new Trie<String>(WhitespaceNormalizingSanitizer.factory());
        
        sut.put("  this is\ta test  .", "exists");
        
        System.out.println(sut.keys());
        
        assertThat(sut.getNode("this is a test .")).isNotNull();
        assertThat(sut.getNode("  this is\ta test  .")).isNotNull();
    }
}
