/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
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
package de.tudarmstadt.ukp.inception.recommendation.api.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

public class PredictionGroupTest
{
    @Test
    public void thatAddingElementsToGroupWorks()
    {
        var rec1Sug1 = new SpanSuggestion(1, 1, "rec1", 1, "value", "doc1", 0, 1, "a", "A", "#A",
                0.1, "E1");
        var rec1Sug2 = new SpanSuggestion(2, 1, "rec1", 1, "value", "doc1", 0, 1, "b", "B", "#B",
                0.2, "E2");
        var rec2Sug1 = new SpanSuggestion(3, 2, "rec2", 1, "value", "doc1", 0, 1, "c", "C", "#C",
                0.1, "E1");
        var rec2Sug2 = new SpanSuggestion(4, 2, "rec2", 1, "value", "doc1", 0, 1, "d", "D", "#D",
                0.3, "E3");

        // Ensure that group grows and that all elements are added properly
        var sut = new SuggestionGroup<>();
        sut.add(rec1Sug1);
        assertThat(sut).hasSize(1);
        assertThat(sut).contains(rec1Sug1);
        sut.add(rec1Sug2);
        assertThat(sut).hasSize(2);
        assertThat(sut).contains(rec1Sug2);
        sut.add(rec2Sug1);
        assertThat(sut).hasSize(3);
        assertThat(sut).contains(rec2Sug1);
        sut.add(rec2Sug2);
        assertThat(sut).hasSize(4);
        assertThat(sut).contains(rec2Sug2);
    }

    @Test
    public void thatSortingWorks()
    {
        var rec1Sug1 = new SpanSuggestion(1, 1, "rec1", 1, "value", "doc1", 0, 1, "a", "A", "#A",
                0.1, "E1");
        var rec1Sug2 = new SpanSuggestion(2, 1, "rec1", 1, "value", "doc1", 0, 1, "b", "B", "#B",
                0.2, "E2");
        var rec2Sug1 = new SpanSuggestion(3, 2, "rec2", 1, "value", "doc1", 0, 1, "c", "C", "#C",
                0.1, "E1");
        var rec2Sug2 = new SpanSuggestion(4, 2, "rec2", 1, "value", "doc1", 0, 1, "d", "D", "#D",
                0.3, "E3");

        var sut = new SuggestionGroup<>(rec1Sug1, rec1Sug2, rec2Sug1, rec2Sug2);

        assertThat(sut) //
                .as("Sorted by score (decreasing) but retain insertion order on tie")
                .containsExactly(rec2Sug2, rec1Sug2, rec1Sug1, rec2Sug1);

        assertThat(sut.stream())
                .as("Sorted by score (decreasing) but retain insertion order on tie")
                .containsExactly(rec2Sug2, rec1Sug2, rec1Sug1, rec2Sug1);

        assertThat(sut.iterator()).toIterable()
                .as("Sorted by score (decreasing) but retain insertion order on tie")
                .containsExactly(rec2Sug2, rec1Sug2, rec1Sug1, rec2Sug1);
    }

    @Test
    public void thatTopDeltasAreCorrect()
    {
        var rec1Sug1 = new SpanSuggestion(1, 1, "rec1", 1, "value", "doc1", 0, 1, "a", "A", "#A",
                0.1, "E1");
        var rec1Sug2 = new SpanSuggestion(2, 1, "rec1", 1, "value", "doc1", 0, 1, "b", "B", "#B",
                0.2, "E2");
        var rec2Sug1 = new SpanSuggestion(3, 2, "rec2", 1, "value", "doc1", 0, 1, "c", "C", "#C",
                0.1, "E1");
        var rec2Sug2 = new SpanSuggestion(4, 2, "rec2", 1, "value", "doc1", 0, 1, "d", "D", "#D",
                0.3, "E3");

        var sut = new SuggestionGroup<>(rec1Sug1, rec1Sug2, rec2Sug1, rec2Sug2);

        // Check that the deltas are ok
        var topDeltas = sut.getTopDeltas(new Preferences());
        assertThat(topDeltas).hasSize(2);
        assertThat(topDeltas.get(1L).getDelta()).isCloseTo(0.1, within(0.00001));
        assertThat(topDeltas.get(2L).getDelta()).isCloseTo(0.2, within(0.00001));
    }
}
