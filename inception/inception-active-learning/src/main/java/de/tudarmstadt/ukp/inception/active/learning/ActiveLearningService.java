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
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.active.learning.ActiveLearningServiceImpl.ActiveLearningUserState;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup.Delta;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;

public interface ActiveLearningService
{
    /**
     * @param aDataOwner
     *            annotator user to get suggestions for
     * @param aLayer
     *            layer to get suggestions for
     * @return all suggestions for the given layer and user as a flat list (i.e. not grouped by
     *         documents, but grouped by alternatives).
     */
    List<SuggestionGroup<SpanSuggestion>> getSuggestions(User aDataOwner, AnnotationLayer aLayer);

    /**
     * @return if the are any records of type {@link LearningRecordUserAction#SKIPPED} in the
     *         history of the given layer for the given user.
     * 
     * @param aDataOwner
     *            annotator user to check suggestions for
     * @param aLayer
     *            layer to check suggestions for
     */
    boolean hasSkippedSuggestions(String aSessionOwner, User aDataOwner, AnnotationLayer aLayer);

    void hideRejectedOrSkippedAnnotations(String aSessionOwner, User aDataOwner,
            AnnotationLayer aLayer, boolean aFilterSkippedRecommendation,
            List<SuggestionGroup<SpanSuggestion>> aSuggestionGroups);

    Optional<Delta<SpanSuggestion>> generateNextSuggestion(String aSessionOwner, User aDataOwner,
            ActiveLearningUserState aAlState);

    void acceptSpanSuggestion(SourceDocument aDocument, User aDataOwner, Predictions aPredictions,
            SpanSuggestion aSuggestion, Object aValue)
        throws IOException, AnnotationException;

    void rejectSpanSuggestion(String aSessionOwner, User aDataOwner, AnnotationLayer aLayer,
            SpanSuggestion aSuggestion)
        throws AnnotationException;

    void skipSpanSuggestion(String aSessionOwner, User aDataOwner, AnnotationLayer aLayer,
            SpanSuggestion aSuggestion)
        throws AnnotationException;
}
