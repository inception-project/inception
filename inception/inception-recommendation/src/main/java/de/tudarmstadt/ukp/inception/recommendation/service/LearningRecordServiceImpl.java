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

import static de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionType.RELATION;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionType.SPAN;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterDocumentResetEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RelationPosition;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RelationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionType;
import de.tudarmstadt.ukp.inception.recommendation.config.RecommenderServiceAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link RecommenderServiceAutoConfiguration#learningRecordService}.
 * </p>
 */
public class LearningRecordServiceImpl
    implements LearningRecordService
{
    private final EntityManager entityManager;

    public LearningRecordServiceImpl(EntityManager aEntityManager)
    {
        entityManager = aEntityManager;
    }

    @Transactional
    @EventListener
    public void afterDocumentReset(AfterDocumentResetEvent aEvent)
    {
        SourceDocument currentDocument = aEvent.getDocument().getDocument();
        String currentUser = aEvent.getDocument().getUser();
        deleteRecords(currentDocument, currentUser);
    }

    @Transactional
    @Override
    public void logSpanRecord(SourceDocument aDocument, String aUsername,
            SpanSuggestion aSuggestion, AnnotationLayer aLayer, AnnotationFeature aFeature,
            LearningRecordType aUserAction, LearningRecordChangeLocation aLocation)
    {
        logSpanRecord(aDocument, aUsername, aSuggestion, aSuggestion.getLabel(), aLayer, aFeature,
                aUserAction, aLocation);
    }

    @Transactional
    @Override
    public void logSpanRecord(SourceDocument aDocument, String aUsername,
            SpanSuggestion aSuggestion, String aAlternativeLabel, AnnotationLayer aLayer,
            AnnotationFeature aFeature, LearningRecordType aUserAction,
            LearningRecordChangeLocation aLocation)
    {
        logRecord(aDocument, aUsername, aSuggestion.getBegin(), aSuggestion.getEnd(), -1, -1, SPAN,
                aAlternativeLabel, aUserAction, aLayer, aFeature, aSuggestion.getCoveredText(),
                aLocation);
    }

    @Transactional
    @Override
    public void logRelationRecord(SourceDocument aDocument, String aUsername,
            RelationSuggestion aSuggestion, AnnotationLayer aLayer, AnnotationFeature aFeature,
            LearningRecordType aUserAction, LearningRecordChangeLocation aLocation)
    {
        logRelationRecord(aDocument, aUsername, aSuggestion, aSuggestion.getLabel(), aLayer,
                aFeature, aUserAction, aLocation);
    }

    @Transactional
    @Override
    public void logRelationRecord(SourceDocument aDocument, String aUsername,
            RelationSuggestion aSuggestion, String aAlternativeLabel, AnnotationLayer aLayer,
            AnnotationFeature aFeature, LearningRecordType aUserAction,
            LearningRecordChangeLocation aLocation)
    {
        RelationPosition pos = aSuggestion.getPosition();
        logRecord(aDocument, aUsername, pos.getSourceBegin(), pos.getSourceEnd(),
                pos.getTargetBegin(), pos.getTargetEnd(), RELATION, aAlternativeLabel, aUserAction,
                aLayer, aFeature, "", aLocation);
    }

    private void logRecord(SourceDocument aSourceDocument, String aUsername, int aOffsetBegin,
            int aOffsetEnd, int aOffset2Begin, int aOffset2End, SuggestionType aSuggestionType,
            String aLabel, LearningRecordType aUserAction, AnnotationLayer aLayer,
            AnnotationFeature aFeature, String aCoveredText, LearningRecordChangeLocation aLocation)
    {
        // It doesn't make any sense at all to have duplicate entries in the learning history,
        // so when adding a new entry, we dump any existing entries which basically are the
        // same as the one added. Mind that the actual action performed by the user does not
        // matter since there should basically be only one action in the log for any suggestion,
        // irrespective of what that action is.
        String query = String.join("\n", //
                "DELETE FROM LearningRecord WHERE", "user = :user AND", //
                "sourceDocument = :sourceDocument AND", //
                "offsetBegin = :offsetBegin AND", //
                "offsetEnd = :offsetEnd AND", //
                "offsetBegin2 = :offsetBegin2 AND", //
                "offsetEnd2 = :offsetEnd2 AND", //
                "layer = :layer AND", //
                "annotationFeature = :annotationFeature AND", //
                "suggestionType = :suggestionType AND", //
                "annotation = :annotation");
        entityManager.createQuery(query) //
                .setParameter("user", aUsername) //
                .setParameter("sourceDocument", aSourceDocument) //
                .setParameter("offsetBegin", aOffsetBegin) //
                .setParameter("offsetEnd", aOffsetEnd) //
                .setParameter("offsetBegin2", aOffset2Begin) //
                .setParameter("offsetEnd2", aOffset2End) //
                .setParameter("layer", aLayer) //
                .setParameter("annotationFeature", aFeature) //
                .setParameter("suggestionType", aSuggestionType) //
                .setParameter("annotation", aLabel) //
                .executeUpdate();

        LearningRecord record = new LearningRecord();
        record.setUser(aUsername);
        record.setSourceDocument(aSourceDocument);
        record.setUserAction(aUserAction);
        record.setOffsetBegin(aOffsetBegin);
        record.setOffsetEnd(aOffsetEnd);
        record.setOffsetBegin2(aOffset2Begin);
        record.setOffsetEnd2(aOffset2End);
        record.setTokenText(aCoveredText);
        record.setAnnotation(aLabel);
        record.setLayer(aLayer);
        record.setSuggestionType(aSuggestionType);
        record.setChangeLocation(aLocation);
        record.setAnnotationFeature(aFeature);

        create(record);
    }

    @Transactional
    @Override
    public List<LearningRecord> listRecords(SourceDocument aDocument, String aUsername,
            AnnotationFeature aFeature)
    {
        String sql = String.join("\n", //
                "FROM LearningRecord l WHERE", //
                "l.sourceDocument = :sourceDocument AND", //
                "l.user = :user AND", //
                "l.annotationFeature = :annotationFeature AND", //
                "l.userAction != :action", //
                "ORDER BY l.id desc");
        TypedQuery<LearningRecord> query = entityManager.createQuery(sql, LearningRecord.class) //
                .setParameter("sourceDocument", aDocument) //
                .setParameter("user", aUsername) //
                .setParameter("annotationFeature", aFeature) //
                .setParameter("action", LearningRecordType.SHOWN); // SHOWN records NOT returned
        return query.getResultList();
    }

    @Transactional
    @Override
    public List<LearningRecord> listRecords(String aUsername, AnnotationLayer aLayer, int aLimit)
    {
        String sql = String.join("\n", //
                "FROM LearningRecord l WHERE", //
                "l.user = :user AND", //
                "l.layer = :layer AND", //
                "l.userAction != :action", //
                "ORDER BY l.id desc");
        TypedQuery<LearningRecord> query = entityManager.createQuery(sql, LearningRecord.class) //
                .setParameter("user", aUsername) //
                .setParameter("layer", aLayer) //
                .setParameter("action", LearningRecordType.SHOWN); // SHOWN records NOT returned
        if (aLimit > 0) {
            query = query.setMaxResults(aLimit);
        }
        return query.getResultList();
    }

    @Transactional
    @Override
    public List<LearningRecord> listRecords(String aUsername, AnnotationLayer aLayer)
    {
        return listRecords(aUsername, aLayer, 0);
    }

    @Transactional
    @Override
    public LearningRecord getRecordById(long recordId)
    {
        String sql = "FROM LearningRecord l where l.id = :id";
        LearningRecord learningRecord = entityManager.createQuery(sql, LearningRecord.class)
                .setParameter("id", recordId) //
                .getSingleResult();
        return learningRecord;
    }

    @Transactional
    @Override
    public void deleteRecords(SourceDocument document, String user)
    {
        String sql = "DELETE FROM LearningRecord l where l.sourceDocument = :document and l.user "
                + "= :user";
        entityManager.createQuery(sql).setParameter("document", document).setParameter("user", user)
                .executeUpdate();
    }

    @Override
    @Transactional
    public void create(LearningRecord learningRecord)
    {
        entityManager.persist(learningRecord);
        entityManager.flush();
    }

    @Override
    @Transactional
    public void update(LearningRecord learningRecord)
    {
        entityManager.merge(learningRecord);
        entityManager.flush();
    }

    @Override
    @Transactional
    public void delete(LearningRecord learningRecord)
    {
        entityManager.remove(entityManager.contains(learningRecord) ? learningRecord
                : entityManager.merge(learningRecord));
    }

    @Override
    @Transactional
    public void deleteById(long recordId)
    {
        LearningRecord learningRecord = this.getRecordById(recordId);
        if (learningRecord != null) {
            this.delete(learningRecord);
        }
    }

    @Override
    @Transactional
    public boolean hasSkippedSuggestions(User aUser, AnnotationLayer aLayer)
    {
        String sql = String.join("\n", //
                "SELECT COUNT(*) FROM LearningRecord WHERE", //
                "user = :user AND", //
                "layer = :layer AND", //
                "userAction = :action");
        long count = entityManager.createQuery(sql, Long.class) //
                .setParameter("user", aUser.getUsername()) //
                .setParameter("layer", aLayer) //
                .setParameter("action", LearningRecordType.SKIPPED) //
                .getSingleResult();
        return count > 0;
    }

    @Override
    @Transactional
    public void deleteSkippedSuggestions(User aUser, AnnotationLayer aLayer)
    {
        String sql = String.join("\n", //
                "DELETE FROM LearningRecord WHERE", //
                "user = :user AND", //
                "layer = :layer AND", //
                "userAction = :action");
        entityManager.createQuery(sql) //
                .setParameter("user", aUser.getUsername()) //
                .setParameter("layer", aLayer) //
                .setParameter("action", LearningRecordType.SKIPPED) //
                .executeUpdate();
    }
}
