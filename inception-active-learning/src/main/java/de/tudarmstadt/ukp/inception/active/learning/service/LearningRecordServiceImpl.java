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
package de.tudarmstadt.ukp.inception.active.learning.service;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.active.learning.model.LearningRecord;


@Component(LearningRecordService.SERVICE_NAME)
public class LearningRecordServiceImpl implements LearningRecordService, InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<LearningRecord> getRecordByDocument(SourceDocument sourceDocument) {
        String sql = "FROM LearningRecord l where l.sourceDocument =:sourceDocument";
        List<LearningRecord> learningRecords = entityManager.createQuery(sql, LearningRecord.class)
                .setParameter("sourceDocument", sourceDocument).getResultList();
        return learningRecords;
    }

    @Override
    public List<LearningRecord> getRecordByDocumentAndUser(SourceDocument sourceDocument, User
            user) {
        String sql = "FROM LearningRecord l where l.user=:user and l.sourceDocument=:sourceDocument";
        List<LearningRecord> learningRecords = entityManager.createQuery(sql, LearningRecord
                .class).setParameter("user", user).setParameter("sourceDocument",sourceDocument)
                .getResultList();
        return learningRecords;
    }

    @Override
    public List<LearningRecord> getAllRecordsByDocumentAndUserAndLayer(
        SourceDocument sourceDocument, String user, AnnotationLayer layer)
    {
        String sql = "FROM LearningRecord l where l.user=:user and l.sourceDocument.project " +
            "=:project and l.layer=:layer order by l.id desc";
        List<LearningRecord> learningRecords = entityManager.createQuery(sql, LearningRecord
            .class).setParameter("user", user).setParameter("project", sourceDocument.getProject())
            .setParameter("layer", layer).setMaxResults(50).getResultList();
        return learningRecords;
    }

    @Override
    public LearningRecord getRecordById(Long recordId) {
        String sql = "FROM LearningRecord l where l.id =:id";
        LearningRecord learningRecord = entityManager.createQuery(sql, LearningRecord.class)
                .setParameter("id",recordId).getSingleResult();
        return learningRecord;
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
        entityManager.remove(learningRecord);
    }

    @Override
    @Transactional
    public void deleteById(Long recordId) {
        LearningRecord learningRecord = this.getRecordById(recordId);
        if (learningRecord != null) {
            this.delete(learningRecord);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }
}
