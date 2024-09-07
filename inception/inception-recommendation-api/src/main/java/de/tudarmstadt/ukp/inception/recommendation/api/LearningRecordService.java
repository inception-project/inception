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

import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction;

public interface LearningRecordService
{
    List<LearningRecord> listLearningRecords(String aSessionOwner, String aDataOwner,
            AnnotationLayer layer);

    List<LearningRecord> listLearningRecords(String aSessionOwner, SourceDocument aDocument,
            String aDataOwner, AnnotationFeature aFeature);

    List<LearningRecord> listLearningRecords(Project aProject);

    /**
     * @return the learning records for the given document, user and layer. An optional limit can be
     *         used, e.g. for loading only a reduced part of the history in the active learning
     *         sidebar. Learning records with the action {@link LearningRecordUserAction#SHOWN} are
     *         <b>not</b> returned by this method.
     * @param aSessionOwner
     *            the user performing the action
     * @param aDataOwner
     *            the annotator user
     * @param aLayer
     *            the layer
     * @param aLimit
     *            the maximum number of records to retrieve
     */
    List<LearningRecord> listLearningRecords(String aSessionOwner, String aDataOwner,
            AnnotationLayer aLayer, int aLimit);

    void deleteLearningRecord(LearningRecord learningRecord);

    /**
     * Updates the learning log with an entry for the given suggestion. Any entries which are
     * duplicates of the new action are removed as part of this action. Note that the actual action
     * the user performed is not taken into account to determine duplicateness.
     * 
     * @param aSessionOwner
     *            the user performing the action
     * @param aDocument
     *            the document
     * @param aDataOwner
     *            the annotator user the annotations belong to
     * @param aSuggestion
     *            the suggestion
     * @param aFeature
     *            the feature on the given layer
     * @param aUserAction
     *            the annotators reaction to the suggestion
     * @param aLocation
     *            where the action on the suggestion was triggered
     * @deprecated Use {@link #logRecord(String, LearningRecord)} instead.
     */
    @Deprecated
    void logRecord(String aSessionOwner, SourceDocument aDocument, String aDataOwner,
            AnnotationSuggestion aSuggestion, AnnotationFeature aFeature,
            LearningRecordUserAction aUserAction, LearningRecordChangeLocation aLocation);

    /**
     * Updates the learning log with an entry for the given suggestion. Any entries which are
     * duplicates of the new action are removed as part of this action. Note that the actual action
     * the user performed is not taken into account to determine redundance.
     * 
     * @param aSessionOwner
     *            the user performing the action
     * @param aRecord
     *            the record
     */
    void logRecord(String aSessionOwner, LearningRecord aRecord);

    /**
     * @param aSessionOwner
     *            the user performing the action
     * @param aDataOwner
     *            the annotator user
     * @param aLayer
     *            the layer
     * @return if the are any records of type {@link LearningRecordUserAction#SKIPPED} in the
     *         history of the given layer for the given user.
     * 
     */
    boolean hasSkippedSuggestions(String aSessionOwner, User aDataOwner, AnnotationLayer aLayer);

    /**
     * Removes all records of type {@link LearningRecordUserAction#SKIPPED} in the history of the
     * given layer for the given user.
     * 
     * @param aSessionOwner
     *            the user performing the action
     * @param aDataOwner
     *            the annotator user
     * @param aLayer
     *            the layer
     */
    void deleteSkippedSuggestions(String aSessionOwner, User aDataOwner, AnnotationLayer aLayer);

    void createLearningRecord(LearningRecord aRecord);

    void createLearningRecords(LearningRecord... aRecords);
}
