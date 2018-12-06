/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.active.learning;

import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;

public interface ActiveLearningService
{
    /**
     * Get all suggestions for the given layer and user as a flat list (i.e. not grouped by
     * documents, but grouped by alternatives).
     */
    List<SuggestionGroup> getRecommendationFromRecommendationModel(User aUser,
            AnnotationLayer aLayer);

    /**
     * Check if the suggestions from which the given record was created (or an equivalent one)
     * is visible to the user. This is useful to check if the suggestion can be highlighted when
     * clicking on a history record.
     */
    boolean isSuggestionVisible(LearningRecord aRecord);

    /**
     * Checks if the are any records of type {@link LearningRecordType#SKIPPED} in the history of
     * the given layer for the given user.
     */
    boolean hasSkippedSuggestions(User aUser, AnnotationLayer aLayer);

    void hideRejectedOrSkippedAnnotations(SourceDocument aDocument, User aUser,
            AnnotationLayer aLayer, boolean aFilterSkippedRecommendation,
            List<SuggestionGroup> aSuggestionGroups);
}
