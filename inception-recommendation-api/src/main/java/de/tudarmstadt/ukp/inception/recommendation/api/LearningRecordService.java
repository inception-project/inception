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

import org.springframework.security.access.prepost.PreAuthorize;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction;


public interface LearningRecordService {
    String SERVICE_NAME = "LearningRecordService";

    List<LearningRecord> getRecordByDocument(SourceDocument sourceDocument);

    List<LearningRecord> getRecordByDocumentAndUser(SourceDocument sourceDocument, User user);

    public List<LearningRecord> getAllRecordsByDocumentAndUserAndLayer(
            SourceDocument sourceDocument, String user, AnnotationLayer layer);

    public List<LearningRecord> getRecordsByDocumentAndUserAndLayer(
            SourceDocument sourceDocument, String user, AnnotationLayer layer, int aLimit);

    public void deleteRecordByDocumentAndUser(SourceDocument document, String user);

    LearningRecord getRecordById(long recordId);

    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void create(LearningRecord learningRecord);

    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void update(LearningRecord learningRecord);

    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void delete(LearningRecord learningRecord);

    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void deleteById(long id);

    void logLearningRecord(SourceDocument aDocument, String aUsername,
            AnnotationSuggestion aPrediction, AnnotationLayer aLayer, AnnotationFeature aFeature,
            LearningRecordUserAction aUserAction);

    void logLearningRecord(SourceDocument aDocument, String aUsername,
            AnnotationSuggestion aPrediction, String aAlternativeLabel, AnnotationLayer aLayer,
            AnnotationFeature aFeature, LearningRecordUserAction aUserAction);
}
