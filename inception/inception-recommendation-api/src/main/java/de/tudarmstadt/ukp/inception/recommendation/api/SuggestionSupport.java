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
package de.tudarmstadt.ukp.inception.recommendation.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.AnnotationBaseFS;
import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.ExtractionContext;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.support.extensionpoint.Extension;

public interface SuggestionSupport
    extends Extension<SuggestionSupportQuery>
{
    /**
     * @param aCtx
     *            the extraction context containing all important information.
     * @return the suggestions extracted from the prediction CAS provided in the context.
     */
    List<AnnotationSuggestion> extractSuggestions(ExtractionContext aCtx);

    /**
     * @return the renderer used to render suggestions provided by this suggestion support.
     */
    Optional<SuggestionRenderer> getRenderer();

    /**
     * Calculate the visibility for the given suggestions. The suggestions must have been produced
     * by this suggestion support. Also, they must all be from the same layer - be sure to process
     * one layer at a time. The suggestions may come from different recommenders though.
     */
    <T extends AnnotationSuggestion> void calculateSuggestionVisibility(String aSessionOwner,
            SourceDocument aDocument, CAS aCas, String aDataOwner, AnnotationLayer aLayer,
            Collection<SuggestionGroup<T>> aRecommendations, int aWindowBegin, int aWindowEnd);

    /**
     * Uses the given annotation suggestion to create a new annotation or to update a feature in an
     * existing annotation.
     * 
     * @param aSessionOwner
     *            the user currently logged in
     * @param aDocument
     *            the source document to which the annotations belong
     * @param aDataOwner
     *            the annotator user to whom the annotations belong
     * @param aCas
     *            the CAS containing the annotations
     * @param aAdapter
     *            an adapter for the layer to upsert
     * @param aFeature
     *            the feature on the layer that should be upserted
     * @param aSuggestion
     *            the suggestion
     * @param aLocation
     *            the location from where the change was triggered
     * @param aAction
     *            whether the annotation was accepted or corrected
     * @return an {@link Optional} containing created/updated annotation
     * @throws AnnotationException
     *             if there was an annotation-level problem
     */
    Optional<AnnotationBaseFS> acceptSuggestion(String aSessionOwner, SourceDocument aDocument,
            String aDataOwner, CAS aCas, TypeAdapter aAdapter, AnnotationFeature aFeature,
            Predictions aPredictions, AnnotationSuggestion aSuggestion,
            LearningRecordChangeLocation aLocation, LearningRecordUserAction aAction)
        throws AnnotationException;

    /**
     * Reject the given suggestion.
     * 
     * @param aSessionOwner
     *            the user currently logged in
     * @param aDocument
     *            the source document to which the annotations belong
     * @param aDataOwner
     *            the annotator user to whom the annotations belong
     * @param aSuggestion
     *            the suggestion to reject.
     * @throws AnnotationException
     *             if there was a problem rejecting the annotation.
     */
    void rejectSuggestion(String aSessionOwner, SourceDocument aDocument, String aDataOwner,
            AnnotationSuggestion aSuggestion, LearningRecordChangeLocation aLocation)
        throws AnnotationException;

    /**
     * Skip the given suggestion.
     * 
     * @param aSessionOwner
     *            the user currently logged in
     * @param aDocument
     *            the source document to which the annotations belong
     * @param aDataOwner
     *            the annotator user to whom the annotations belong
     * @param aSuggestion
     *            the suggestion to skip.
     * @throws AnnotationException
     *             if there was a problem skipping the annotation.
     */
    void skipSuggestion(String aSessionOwner, SourceDocument aDocument, String aDataOwner,
            AnnotationSuggestion aSuggestion, LearningRecordChangeLocation aAction)
        throws AnnotationException;

    /**
     * Create a learning record from the given suggestion.
     * 
     * @param aDocument
     *            the source document to which the annotations belong
     * @param aDataOwner
     *            the annotator user to whom the annotations belong
     */
    LearningRecord toLearningRecord(SourceDocument aDocument, String aDataOwner,
            AnnotationSuggestion aSuggestion, AnnotationFeature aFeature,
            LearningRecordUserAction aUserAction, LearningRecordChangeLocation aLocation);
}
