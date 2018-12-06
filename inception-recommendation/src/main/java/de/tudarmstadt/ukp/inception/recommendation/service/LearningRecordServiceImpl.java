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
package de.tudarmstadt.ukp.inception.recommendation.service;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType;

@Component(LearningRecordService.SERVICE_NAME)
public class LearningRecordServiceImpl
    implements LearningRecordService
{
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    @Override
    public void logLearningRecord(SourceDocument aDocument, String aUsername,
            AnnotationSuggestion aSuggestion, AnnotationLayer aLayer, AnnotationFeature aFeature,
            LearningRecordType aUserAction, LearningRecordChangeLocation aLocation)
    {
        logLearningRecord(aDocument, aUsername, aSuggestion, aSuggestion.getLabel(), aLayer,
                aFeature, aUserAction, aLocation);
    }
    
    @Transactional
    @Override
    public void logLearningRecord(SourceDocument aDocument, String aUsername,
            AnnotationSuggestion aSuggestion, String aAlternativeLabel, AnnotationLayer aLayer,
            AnnotationFeature aFeature, LearningRecordType aUserAction, 
            LearningRecordChangeLocation aLocation)
    {
        LearningRecord record = new LearningRecord();
        record.setUser(aUsername);
        record.setSourceDocument(aDocument);
        record.setUserAction(aUserAction);
        record.setOffsetCharacterBegin(aSuggestion.getBegin());
        record.setOffsetCharacterEnd(aSuggestion.getEnd());
        record.setOffsetTokenBegin(-1);
        record.setOffsetTokenEnd(-1);
        record.setTokenText(aSuggestion.getCoveredText());
        record.setAnnotation(aAlternativeLabel);
        record.setLayer(aLayer);
        record.setChangeLocation(aLocation);
        record.setAnnotationFeature(aFeature);

        create(record);
    }

    @Transactional
    @Override
    public List<LearningRecord> getRecordByDocument(SourceDocument sourceDocument) {
        String sql = "FROM LearningRecord l where l.sourceDocument = :sourceDocument";
        List<LearningRecord> learningRecords = entityManager.createQuery(sql, LearningRecord.class)
                .setParameter("sourceDocument", sourceDocument).getResultList();
        return learningRecords;
    }

    @Transactional
    @Override
    public List<LearningRecord> getRecordByDocumentAndUser(SourceDocument sourceDocument, User
            user) {
        String sql = "FROM LearningRecord l where l.user = :user and l" +
            ".sourceDocument = :sourceDocument";
        List<LearningRecord> learningRecords = entityManager.createQuery(sql, LearningRecord.class)
                .setParameter("user", user)
                .setParameter("sourceDocument",sourceDocument)
                .getResultList();
        return learningRecords;
    }

    @Transactional
    @Override
    public List<LearningRecord> getRecordsByDocumentAndUserAndLayer(
            SourceDocument aDocument, String aUsername, AnnotationLayer aLayer, int aLimit)
    {
        String sql = String.join("\n",
                "FROM LearningRecord l WHERE",
                "l.user = :user AND",
                "l.sourceDocument.project = :project AND",
                "l.layer = :layer",
                "l.userAction != :action AND",
                "ORDER BY l.id desc");
        TypedQuery<LearningRecord> query = entityManager.createQuery(sql, LearningRecord.class)
                .setParameter("user", aUsername)
                .setParameter("project", aDocument.getProject())
                .setParameter("layer", aLayer)
                .setParameter("action", LearningRecordType.SHOWN); // SHOWN records NOT returned
        if (aLimit > 0) {
            query = query.setMaxResults(aLimit);
        }
        return query.getResultList();
    }

    @Transactional
    @Override
    public List<LearningRecord> getAllRecordsByDocumentAndUserAndLayer(
            SourceDocument aDocument, String aUsername, AnnotationLayer aLayer)
    {
        return getRecordsByDocumentAndUserAndLayer(aDocument, aUsername, aLayer, 0);
    }

    @Transactional
    @Override
    public LearningRecord getRecordById(long recordId) {
        String sql = "FROM LearningRecord l where l.id = :id";
        LearningRecord learningRecord = entityManager.createQuery(sql, LearningRecord.class)
                .setParameter("id",recordId)
                .getSingleResult();
        return learningRecord;
    }

    @Transactional
    @Override
    public void deleteRecordByDocumentAndUser(SourceDocument document, String user) {
        String sql = "DELETE FROM LearningRecord l where l.sourceDocument = :document and l.user " +
            "= :user";
        entityManager.createQuery(sql)
            .setParameter("document", document)
            .setParameter("user",user)
            .executeUpdate();
    }

    @Override
    @Transactional
    public void create(LearningRecord learningRecord) {
        entityManager.persist(learningRecord);
        entityManager.flush();
    }

    @Override
    @Transactional
    public void update(LearningRecord learningRecord) {
        entityManager.merge(learningRecord);
        entityManager.flush();
    }

    @Override
    @Transactional
    public void delete(LearningRecord learningRecord) {
        entityManager.remove(entityManager.contains(learningRecord) ? learningRecord :
            entityManager.merge(learningRecord));
    }

    @Override
    @Transactional
    public void deleteById(long recordId) {
        LearningRecord learningRecord = this.getRecordById(recordId);
        if (learningRecord != null) {
            this.delete(learningRecord);
        }
    }
}
