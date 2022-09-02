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

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RelationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionDocumentGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;

public class Fixtures
{
    // AnnotationSuggestion
    private final static long RECOMMENDER_ID = 1;
    private final static String RECOMMENDER_NAME = "TestEntityRecommender";
    private final static String UI_LABEL = "TestUiLabel";
    private final static double CONFIDENCE = 0.2;
    private final static String CONFIDENCE_EXPLANATION = "Predictor A: 0.05 | Predictor B: 0.15";
    private final static String COVERED_TEXT = "TestText";

    static <T extends AnnotationSuggestion> List<T> getInvisibleSuggestions(
            Collection<SuggestionGroup<T>> aSuggestions)
    {
        return aSuggestions.stream() //
                .flatMap(SuggestionGroup::stream) //
                .filter(s -> !s.isVisible()) //
                .collect(toList());
    }

    static <T extends AnnotationSuggestion> List<T> getVisibleSuggestions(
            Collection<SuggestionGroup<T>> aSuggestions)
    {
        return aSuggestions.stream() //
                .flatMap(SuggestionGroup::stream) //
                .filter(s -> s.isVisible()) //
                .collect(toList());
    }

    static SuggestionDocumentGroup<SpanSuggestion> makeSpanSuggestionGroup(SourceDocument doc,
            AnnotationFeature aFeat, int[][] vals)
    {
        List<SpanSuggestion> suggestions = new ArrayList<>();
        for (int[] val : vals) {
            suggestions.add(new SpanSuggestion(val[0], RECOMMENDER_ID, RECOMMENDER_NAME,
                    aFeat.getLayer().getId(), aFeat.getName(), doc.getName(), val[1], val[2],
                    COVERED_TEXT, null, UI_LABEL, CONFIDENCE, CONFIDENCE_EXPLANATION));
        }

        return new SuggestionDocumentGroup<>(suggestions);
    }

    static SuggestionDocumentGroup<RelationSuggestion> makeRelationSuggestionGroup(
            SourceDocument doc, AnnotationFeature aFeat, int[][] vals)
    {
        List<RelationSuggestion> suggestions = new ArrayList<>();
        for (int[] val : vals) {
            suggestions.add(new RelationSuggestion(val[0], RECOMMENDER_ID, RECOMMENDER_NAME,
                    aFeat.getLayer().getId(), aFeat.getName(), doc.getName(), val[1], val[2],
                    val[3], val[4], null, UI_LABEL, CONFIDENCE, CONFIDENCE_EXPLANATION));
        }

        return new SuggestionDocumentGroup<>(suggestions);
    }
}
