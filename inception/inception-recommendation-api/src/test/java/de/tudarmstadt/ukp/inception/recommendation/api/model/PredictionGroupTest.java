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
package de.tudarmstadt.ukp.inception.recommendation.api.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class PredictionGroupTest
{
    private SourceDocument doc;
    private AnnotationLayer layer;
    private AnnotationFeature feature;
    private Recommender rec1;
    private Recommender rec2;

    @BeforeEach
    void setup()
    {
        doc = SourceDocument.builder().withId(123l).withName("doc1").build();
        layer = AnnotationLayer.builder().withId(1337l).withName("layer").build();
        feature = AnnotationFeature.builder().withId(1338l).withName("value").withLayer(layer)
                .build();
        rec1 = Recommender.builder().withId(1l).withName("rec1").withLayer(layer)
                .withFeature(feature).build();
        rec2 = Recommender.builder().withId(2l).withName("rec2").withLayer(layer)
                .withFeature(feature).build();
    }

    @Test
    public void thatAddingElementsToGroupWorks()
    {
        var builder = SpanSuggestion.builder().withDocument(doc).withPosition(0, 1);

        builder.withRecommender(rec1);
        var rec1Sug1 = builder.withId(1).withCoveredText("a").withLabel("A").withUiLabel("#A")
                .withScore(0.1).withScoreExplanation("E1").build();
        var rec1Sug2 = builder.withId(2).withCoveredText("b").withLabel("B").withUiLabel("#B")
                .withScore(0.2).withScoreExplanation("E2").build();

        builder.withRecommender(rec2);
        var rec2Sug1 = builder.withId(3).withCoveredText("c").withLabel("C").withUiLabel("#C")
                .withScore(0.1).withScoreExplanation("E1").build();
        var rec2Sug2 = builder.withId(4).withCoveredText("d").withLabel("D").withUiLabel("#D")
                .withScore(0.3).withScoreExplanation("E3").build();

        // Ensure that group grows and that all elements are added properly
        var sut = new SuggestionGroup<>();
        sut.add(rec1Sug1);
        assertThat(sut).hasSize(1).contains(rec1Sug1);
        sut.add(rec1Sug2);
        assertThat(sut).hasSize(2).contains(rec1Sug2);
        sut.add(rec2Sug1);
        assertThat(sut).hasSize(3).contains(rec2Sug1);
        sut.add(rec2Sug2);
        assertThat(sut).hasSize(4).contains(rec2Sug2);
    }

    @Test
    public void thatSortingWorks()
    {
        var builder = SpanSuggestion.builder().withDocument(doc).withPosition(0, 1);

        builder.withRecommender(rec1);
        var rec1Sug1 = builder.withId(1).withCoveredText("a").withLabel("A").withUiLabel("#A")
                .withScore(0.1).withScoreExplanation("E1").build();
        var rec1Sug2 = builder.withId(2).withCoveredText("b").withLabel("B").withUiLabel("#B")
                .withScore(0.2).withScoreExplanation("E2").build();

        builder.withRecommender(rec2);
        var rec2Sug1 = builder.withId(3).withCoveredText("c").withLabel("C").withUiLabel("#C")
                .withScore(0.1).withScoreExplanation("E1").build();
        var rec2Sug2 = builder.withId(4).withCoveredText("d").withLabel("D").withUiLabel("#D")
                .withScore(0.3).withScoreExplanation("E3").build();

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
        var builder = SpanSuggestion.builder().withDocument(doc).withPosition(0, 1);

        builder.withRecommender(rec1);
        var rec1Sug1 = builder.withId(1).withCoveredText("a").withLabel("A").withUiLabel("#A")
                .withScore(0.1).withScoreExplanation("E1").build();
        var rec1Sug2 = builder.withId(2).withCoveredText("b").withLabel("B").withUiLabel("#B")
                .withScore(0.2).withScoreExplanation("E2").build();

        builder.withRecommender(rec2);
        var rec2Sug1 = builder.withId(3).withCoveredText("c").withLabel("C").withUiLabel("#C")
                .withScore(0.1).withScoreExplanation("E1").build();
        var rec2Sug2 = builder.withId(4).withCoveredText("d").withLabel("D").withUiLabel("#D")
                .withScore(0.3).withScoreExplanation("E3").build();

        var sut = new SuggestionGroup<>(rec1Sug1, rec1Sug2, rec2Sug1, rec2Sug2);

        // Check that the deltas are ok
        var topDeltas = sut.getTopDeltas(new Preferences());
        assertThat(topDeltas).hasSize(2);
        assertThat(topDeltas.get(1L).getDelta()).isCloseTo(0.1, within(0.00001));
        assertThat(topDeltas.get(2L).getDelta()).isCloseTo(0.2, within(0.00001));
    }
}
