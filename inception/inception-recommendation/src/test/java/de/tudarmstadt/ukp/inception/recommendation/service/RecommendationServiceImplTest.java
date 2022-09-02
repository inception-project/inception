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
package de.tudarmstadt.ukp.inception.recommendation.service;

import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType.REJECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType.SKIPPED;
import static de.tudarmstadt.ukp.inception.recommendation.service.RecommendationServiceImpl.hideSuggestionsRejectedOrSkipped;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;

class RecommendationServiceImplTest
{
    private SourceDocument doc1;
    private SourceDocument doc2;
    private AnnotationLayer layer1;
    private AnnotationLayer layer2;
    private AnnotationFeature feature1;
    private AnnotationFeature feature2;

    @BeforeEach
    void setup()
    {
        doc1 = new SourceDocument("doc1", null, null);
        doc1.setId(1l);
        doc2 = new SourceDocument("doc2", null, null);
        doc2.setId(2l);
        layer1 = AnnotationLayer.builder().withId(1l).withName("layer1").build();
        layer2 = AnnotationLayer.builder().withId(2l).withName("layer2").build();
        feature1 = AnnotationFeature.builder().withName("feat1").build();
        feature2 = AnnotationFeature.builder().withName("feat2").build();
    }

    @Test
    void thatRejectedSuggestionIsHidden()
    {
        var records = asList(LearningRecord.builder() //
                .withSourceDocument(doc1) //
                .withLayer(layer1) //
                .withAnnotationFeature(feature1) //
                .withOffsetBegin(0) //
                .withOffsetEnd(10) //
                .withAnnotation("x") //
                .withUserAction(REJECTED).build());

        var doc1Suggestion = makeSuggestion(0, 10, "x", doc1, layer1, feature1);
        assertThat(doc1Suggestion.isVisible()).isTrue();
        hideSuggestionsRejectedOrSkipped(doc1Suggestion, records);
        assertThat(doc1Suggestion.isVisible()) //
                .as("Suggestion in same document/layer/feature should be hidden") //
                .isFalse();
        assertThat(doc1Suggestion.getReasonForHiding().trim()).isEqualTo("rejected");

        var doc2Suggestion = makeSuggestion(0, 10, "x", doc2, layer1, feature1);
        assertThat(doc2Suggestion.isVisible()).isTrue();
        hideSuggestionsRejectedOrSkipped(doc2Suggestion, records);
        assertThat(doc2Suggestion.isVisible()) //
                .as("Suggestion in other document should not be hidden") //
                .isTrue();

        var doc3Suggestion = makeSuggestion(0, 10, "x", doc1, layer2, feature1);
        assertThat(doc3Suggestion.isVisible()).isTrue();
        hideSuggestionsRejectedOrSkipped(doc3Suggestion, records);
        assertThat(doc3Suggestion.isVisible()) //
                .as("Suggestion in other layer should not be hidden") //
                .isTrue();

        var doc4Suggestion = makeSuggestion(0, 10, "x", doc1, layer1, feature2);
        assertThat(doc4Suggestion.isVisible()).isTrue();
        hideSuggestionsRejectedOrSkipped(doc3Suggestion, records);
        assertThat(doc4Suggestion.isVisible()) //
                .as("Suggestion in other feature should not be hidden") //
                .isTrue();
    }

    @Test
    void thatSkippedSuggestionIsHidden()
    {
        var records = asList(LearningRecord.builder() //
                .withSourceDocument(doc1) //
                .withLayer(layer1) //
                .withAnnotationFeature(feature1) //
                .withOffsetBegin(0) //
                .withOffsetEnd(10) //
                .withAnnotation("x") //
                .withUserAction(SKIPPED).build());

        var doc1Suggestion = makeSuggestion(0, 10, "x", doc1, layer1, feature1);
        assertThat(doc1Suggestion.isVisible()).isTrue();
        hideSuggestionsRejectedOrSkipped(doc1Suggestion, records);
        assertThat(doc1Suggestion.isVisible()) //
                .as("Suggestion in same document/layer/feature should be hidden") //
                .isFalse();
        assertThat(doc1Suggestion.getReasonForHiding().trim()).isEqualTo("skipped");

        var doc2Suggestion = makeSuggestion(0, 10, "x", doc2, layer1, feature1);
        assertThat(doc2Suggestion.isVisible()).isTrue();
        hideSuggestionsRejectedOrSkipped(doc2Suggestion, records);
        assertThat(doc2Suggestion.isVisible()) //
                .as("Suggestion in other document should not be hidden") //
                .isTrue();

        var doc3Suggestion = makeSuggestion(0, 10, "x", doc1, layer2, feature1);
        assertThat(doc3Suggestion.isVisible()).isTrue();
        hideSuggestionsRejectedOrSkipped(doc3Suggestion, records);
        assertThat(doc3Suggestion.isVisible()) //
                .as("Suggestion in other layer should not be hidden") //
                .isTrue();

        var doc4Suggestion = makeSuggestion(0, 10, "x", doc1, layer1, feature2);
        assertThat(doc4Suggestion.isVisible()).isTrue();
        hideSuggestionsRejectedOrSkipped(doc3Suggestion, records);
        assertThat(doc4Suggestion.isVisible()) //
                .as("Suggestion in other feature should not be hidden") //
                .isTrue();
    }

    private SpanSuggestion makeSuggestion(int aBegin, int aEnd, String aLabel, SourceDocument aDoc,
            AnnotationLayer aLayer, AnnotationFeature aFeature)
    {
        return new SpanSuggestion(0, // aId,
                0, // aRecommenderId,
                "", // aRecommenderName
                aLayer.getId(), // aLayerId,
                aFeature.getName(), // aFeature,
                aDoc.getName(), // aDocumentName
                aBegin, // aBegin
                aEnd, // aEnd
                "", // aCoveredText,
                aLabel, // aLabel
                aLabel, // aUiLabel
                0.0, // aScore
                "" // aScoreExplanation
        );
    }
}
