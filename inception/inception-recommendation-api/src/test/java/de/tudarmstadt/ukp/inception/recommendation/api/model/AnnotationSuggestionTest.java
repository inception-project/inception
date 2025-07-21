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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class AnnotationSuggestionTest
{
    @Test
    public void thatEqualsAndHashCodeAndCompareToWorkCorrectly()
    {
        var doc = SourceDocument.builder() //
                .withId(1L) //
                .withName("doc1") //
                .build();
        var layer = AnnotationLayer.builder() //
                .withId(2l) //
                .build();
        var feature = AnnotationFeature.builder().withLayer(layer).withName("value").build();
        var rec1 = Recommender.builder().withId(1l).withLayer(layer).withFeature(feature).build();
        var rec2 = Recommender.builder().withId(2l).withLayer(layer).withFeature(feature).build();

        var builder = SpanSuggestion.builder() //
                .withLayerId(1) //
                .withFeature("value") //
                .withDocument(doc) //
                .withPosition(0, 1);

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

        var all = asList(rec1Sug1, rec1Sug2, rec2Sug1, rec2Sug2);
        for (var x : all) {
            for (var y : all) {
                if (x == y) {
                    assertThat(x).isEqualTo(y);
                    assertThat(y).isEqualTo(x);
                    assertThat(x.hashCode()).isEqualTo(y.hashCode());
                }
                else {
                    assertThat(x).isNotEqualTo(y);
                    assertThat(y).isNotEqualTo(x);
                    assertThat(x.hashCode()).isNotEqualTo(y.hashCode());
                }
            }
        }
    }
}
