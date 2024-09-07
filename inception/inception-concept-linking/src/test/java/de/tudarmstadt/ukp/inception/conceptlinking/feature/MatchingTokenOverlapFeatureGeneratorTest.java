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
package de.tudarmstadt.ukp.inception.conceptlinking.feature;

import static java.lang.Integer.MAX_VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;

class MatchingTokenOverlapFeatureGeneratorTest
{
    private MatchingTokenOverlapFeatureGenerator sut;

    @BeforeEach
    void setup()
    {
        sut = new MatchingTokenOverlapFeatureGenerator();
    }

    // Method to provide test cases
    static Stream<Arguments> data()
    {
        return Stream.of( //
                arguments("archery domain", "archery domain", 0), //
                arguments("domain archery", "archery domain", 0), //
                arguments("arch dom", "archery domain", 6), //
                arguments("dom arch", "archery domain", 6), //
                arguments("dom dom arch", "archery domain", 6), //
                arguments("dom dom", "archery domain", 10), //
                arguments("arch dom doma", "archery domain dominance", 11), //
                arguments("arch dom dom", "archery domain dominance", 12), //
                arguments("doma dom arch", "archery domain", MAX_VALUE), //
                arguments("", "archery domain", MAX_VALUE), //
                arguments("archery domain", "", MAX_VALUE), //
                arguments("bull dia", "bulla", MAX_VALUE));
    }

    @ParameterizedTest
    @MethodSource("data")
    void test(String s1, String s2, int expected)
    {
        assertDistance(s1, s2, expected);
    }

    private void assertDistance(String query, String label, int distance)
    {
        var kbHandle = new KBHandle("id", label, "desc", "en");
        var candidateEntity = new CandidateEntity(kbHandle) //
                .withMention(null) //
                .withQuery(query);

        sut.apply(candidateEntity);

        assertThat(candidateEntity.get(CandidateEntity.SCORE_TOKEN_OVERLAP_QUERY)) //
                .get().isEqualTo(distance);
    }
}
