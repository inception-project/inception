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
package de.tudarmstadt.ukp.inception.active.learning;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.active.learning.ActiveLearningServiceImpl.ActiveLearningUserState;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup.Delta;
import de.tudarmstadt.ukp.inception.schema.adapter.AnnotationException;

public interface ActiveLearningService
{
    /**
     * @param aUser
     *            annotator user to get suggestions for
     * @param aLayer
     *            layer to get suggestions for
     * @return all suggestions for the given layer and user as a flat list (i.e. not grouped by
     *         documents, but grouped by alternatives).
     */
    List<SuggestionGroup<SpanSuggestion>> getSuggestions(User aUser, AnnotationLayer aLayer);

    /**
     * @param aRecord
     *            record to check
     * @return if the suggestions from which the given record was created (or an equivalent one) is
     *         visible to the user. This is useful to check if the suggestion can be highlighted
     *         when clicking on a history record.
     */
    boolean isSuggestionVisible(LearningRecord aRecord);

    /**
     * @return if the are any records of type {@link LearningRecordType#SKIPPED} in the history of
     *         the given layer for the given user.
     * 
     * @param aUser
     *            annotator user to check suggestions for
     * @param aLayer
     *            layer to check suggestions for
     */
    boolean hasSkippedSuggestions(User aUser, AnnotationLayer aLayer);

    void hideRejectedOrSkippedAnnotations(User aUser, AnnotationLayer aLayer,
            boolean aFilterSkippedRecommendation,
            List<SuggestionGroup<SpanSuggestion>> aSuggestionGroups);

    Optional<Delta<SpanSuggestion>> generateNextSuggestion(User aUser,
            ActiveLearningUserState aAlState);

    void writeLearningRecordInDatabaseAndEventLog(User aUser, AnnotationLayer aLayer,
            SpanSuggestion aSuggestion, LearningRecordType aUserAction, String aAnnotationValue);

    void acceptSpanSuggestion(User aUser, AnnotationLayer aLayer, SpanSuggestion aSuggestion,
            Object aValue)
        throws IOException, AnnotationException;

    void rejectSpanSuggestion(User aUser, AnnotationLayer aLayer, SpanSuggestion aSuggestion);

    void skipSpanSuggestion(User aUser, AnnotationLayer aLayer, SpanSuggestion aSuggestion);
}
