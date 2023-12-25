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

import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

class SpanSuggestionTest
{
    @Test
    void thatCloningRetainsAllInformation()
    {
        var layer = AnnotationLayer.builder().withId(5l).withName("layer").build();
        var feature = AnnotationFeature.builder().withId(6l).withName("feature").withLayer(layer)
                .build();
        var rec = Recommender.builder().withId(4l).withName("rec").withLayer(layer)
                .withFeature(feature).build();
        var doc = SourceDocument.builder().withId(8l).withName("doc").build();

        var s = SpanSuggestion.builder() //
                .withId(1) //
                .withGeneration(2) //
                .withAge(3) //
                .withRecommender(rec) //
                .withDocument(doc) //
                .withLabel("label") //
                .withUiLabel("uiLabel") //
                .withScore(6.0) //
                .withScoreExplanation("scoreExplanation") //
                .withPosition(1, 2) //
                .withCoveredText("coveredText") //
                .withAutoAcceptMode(AutoAcceptMode.ON_FIRST_ACCESS) //
                .withHidingFlags(7) //
                .build();

        assertThat(s.toBuilder().build()).usingRecursiveComparison().isEqualTo(s);
    }
}
