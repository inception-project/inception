/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.inception.recommendation.api;

import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType;

public interface LearningRecordService
{
    String SERVICE_NAME = "LearningRecordService";

    List<LearningRecord> listRecords(String user, AnnotationLayer layer);

    /**
     * Fetches the learning records for the given document, user and layer. An optional limit can be
     * used, e.g. for loading only a reduced part of the history in the active learning sidebar.
     * Learning records with the action {@link LearningRecordType#SHOWN} are <b>not</b> returned by
     * this method.
     */
    List<LearningRecord> listRecords(String user, AnnotationLayer layer, int aLimit);

    void deleteRecords(SourceDocument document, String user);

    LearningRecord getRecordById(long recordId);

    void create(LearningRecord learningRecord);

    void update(LearningRecord learningRecord);

    void delete(LearningRecord learningRecord);

    void deleteById(long id);

    void logRecord(SourceDocument aDocument, String aUsername,
            AnnotationSuggestion aPrediction, AnnotationLayer aLayer, AnnotationFeature aFeature,
            LearningRecordType aUserAction, LearningRecordChangeLocation aLocation);

    /**
     * Updates the learning log with an entry for the given suggestion. Any entries which are 
     * duplicates of the new action are removed as part of this action. Note that the actual
     * action the user performed is not taken into account to determine duplicateness.
     */
    void logRecord(SourceDocument aDocument, String aUsername,
            AnnotationSuggestion aSuggestion, String aAlternativeLabel, AnnotationLayer aLayer,
            AnnotationFeature aFeature, LearningRecordType aUserAction,
            LearningRecordChangeLocation aLocation);

    /**
     * Checks if the are any records of type {@link LearningRecordType#SKIPPED} in the history of
     * the given layer for the given user.
     */
    boolean hasSkippedSuggestions(User aUser, AnnotationLayer aLayer);

    /**
     * Removes all records of type {@link LearningRecordType#SKIPPED} in the history of the given
     * layer for the given user.
     */
    void deleteSkippedSuggestions(User aUser, AnnotationLayer aLayer);
}
