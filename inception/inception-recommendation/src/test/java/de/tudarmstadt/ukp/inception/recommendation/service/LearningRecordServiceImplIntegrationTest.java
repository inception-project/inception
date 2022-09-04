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

import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation.DETAIL_EDITOR;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation.MAIN_EDITOR;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RelationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionType;

@Transactional
@DataJpaTest(excludeAutoConfiguration = LiquibaseAutoConfiguration.class)
public class LearningRecordServiceImplIntegrationTest
{
    private static final String USER_NAME = "testUser";
    private static final String FEATURE_NAME = "testFeature";

    private @Autowired TestEntityManager testEntityManager;

    private LearningRecordService sut;

    @BeforeEach
    public void setUp() throws Exception
    {
        sut = new LearningRecordServiceImpl(testEntityManager.getEntityManager());
    }

    @AfterEach
    public void tearDown()
    {
        testEntityManager.clear();
    }

    @Test
    public void thatApplicationContextStarts()
    {
    }

    @Test
    public void thatSpanSuggestionsCanBeRecorded()
    {
        SourceDocument sourceDoc = createSourceDocument("doc");
        AnnotationLayer layer = createAnnotationLayer("layer");
        AnnotationFeature feature = createAnnotationFeature(layer, FEATURE_NAME);

        SpanSuggestion suggestion = new SpanSuggestion(42, 1337, "testRecommender", layer.getId(),
                feature.getName(), sourceDoc.getName(), 7, 14, "aCoveredText", "testLabel",
                "testUiLabel", 0.42, "Test confidence");

        sut.logSpanRecord(sourceDoc, USER_NAME, suggestion, layer, feature,
                LearningRecordType.ACCEPTED, MAIN_EDITOR);

        List<LearningRecord> records = sut.listRecords(USER_NAME, layer);
        assertThat(records).hasSize(1);

        LearningRecord record = records.get(0);
        assertThat(record).hasFieldOrProperty("id") //
                .hasFieldOrPropertyWithValue("sourceDocument", sourceDoc) //
                .hasFieldOrPropertyWithValue("user", USER_NAME) //
                .hasFieldOrPropertyWithValue("layer", layer) //
                .hasFieldOrPropertyWithValue("annotationFeature", feature) //
                .hasFieldOrPropertyWithValue("offsetBegin", 7) //
                .hasFieldOrPropertyWithValue("offsetEnd", 14) //
                .hasFieldOrPropertyWithValue("offsetBegin2", -1) //
                .hasFieldOrPropertyWithValue("offsetEnd2", -1) //
                .hasFieldOrPropertyWithValue("tokenText", "aCoveredText") //
                .hasFieldOrPropertyWithValue("annotation", "testLabel") //
                .hasFieldOrPropertyWithValue("changeLocation", MAIN_EDITOR) //
                .hasFieldOrPropertyWithValue("userAction", LearningRecordType.ACCEPTED) //
                .hasFieldOrPropertyWithValue("suggestionType", SuggestionType.SPAN);
    }

    @Test
    public void thatRelationSuggestionsCanBeRecorded()
    {
        SourceDocument sourceDoc = createSourceDocument("doc");
        AnnotationLayer layer = createAnnotationLayer("layer");
        AnnotationFeature feature = createAnnotationFeature(layer, FEATURE_NAME);

        RelationSuggestion suggestion = new RelationSuggestion(42, 1337, "testRecommender",
                layer.getId(), feature.getName(), sourceDoc.getName(), 7, 14, 21, 28, "testLabel",
                "testUiLabel", 0.42, "Test confidence");

        sut.logRelationRecord(sourceDoc, USER_NAME, suggestion, layer, feature,
                LearningRecordType.REJECTED, DETAIL_EDITOR);

        List<LearningRecord> records = sut.listRecords(USER_NAME, layer);
        assertThat(records).hasSize(1);

        LearningRecord record = records.get(0);
        assertThat(record).hasFieldOrProperty("id") //
                .hasFieldOrPropertyWithValue("sourceDocument", sourceDoc) //
                .hasFieldOrPropertyWithValue("user", USER_NAME) //
                .hasFieldOrPropertyWithValue("layer", layer) //
                .hasFieldOrPropertyWithValue("annotationFeature", feature) //
                .hasFieldOrPropertyWithValue("offsetBegin", 7) //
                .hasFieldOrPropertyWithValue("offsetEnd", 14) //
                .hasFieldOrPropertyWithValue("offsetBegin2", 21) //
                .hasFieldOrPropertyWithValue("offsetEnd2", 28) //
                .hasFieldOrPropertyWithValue("tokenText", "") //
                .hasFieldOrPropertyWithValue("annotation", "testLabel") //
                .hasFieldOrPropertyWithValue("changeLocation", DETAIL_EDITOR) //
                .hasFieldOrPropertyWithValue("userAction", LearningRecordType.REJECTED) //
                .hasFieldOrPropertyWithValue("suggestionType", SuggestionType.RELATION);
    }

    @Test
    void thatListingRecordsForRendering()
    {
        SourceDocument sourceDoc1 = createSourceDocument("doc1");
        SourceDocument sourceDoc2 = createSourceDocument("doc2");
        AnnotationLayer layer1 = createAnnotationLayer("layer1");
        AnnotationLayer layer2 = createAnnotationLayer("layer2");
        AnnotationFeature feature1 = createAnnotationFeature(layer1, "feat1");
        AnnotationFeature feature2 = createAnnotationFeature(layer2, "feat1");

        sut.logSpanRecord(sourceDoc1, USER_NAME,
                new SpanSuggestion(42, 1337, "testRecommender", layer1.getId(), feature1.getName(),
                        sourceDoc1.getName(), 7, 14, "aCoveredText", "testLabel", "testUiLabel",
                        0.42, "Test confidence"),
                layer1, feature1, LearningRecordType.ACCEPTED, MAIN_EDITOR);

        sut.logSpanRecord(sourceDoc1, USER_NAME,
                new SpanSuggestion(42, 1337, "testRecommender2", layer2.getId(), feature2.getName(),
                        sourceDoc1.getName(), 7, 14, "aCoveredText", "testLabel", "testUiLabel",
                        0.42, "Test confidence"),
                layer2, feature2, LearningRecordType.ACCEPTED, MAIN_EDITOR);

        sut.logSpanRecord(sourceDoc2, USER_NAME,
                new SpanSuggestion(42, 1337, "testRecommender", layer1.getId(), feature1.getName(),
                        sourceDoc2.getName(), 7, 14, "aCoveredText", "testLabel", "testUiLabel",
                        0.42, "Test confidence"),
                layer1, feature1, LearningRecordType.ACCEPTED, MAIN_EDITOR);

        sut.logSpanRecord(sourceDoc2, USER_NAME,
                new SpanSuggestion(42, 1337, "testRecommender2", layer2.getId(), feature2.getName(),
                        sourceDoc2.getName(), 7, 14, "aCoveredText", "testLabel", "testUiLabel",
                        0.42, "Test confidence"),
                layer2, feature2, LearningRecordType.ACCEPTED, MAIN_EDITOR);

        assertThat(sut.listRecords(sourceDoc1, USER_NAME, feature1)).hasSize(1);
        assertThat(sut.listRecords(sourceDoc1, USER_NAME, feature2)).hasSize(1);
        assertThat(sut.listRecords(sourceDoc2, USER_NAME, feature1)).hasSize(1);
        assertThat(sut.listRecords(sourceDoc2, USER_NAME, feature2)).hasSize(1);
        assertThat(sut.listRecords(sourceDoc1, "otherUser", feature1)).hasSize(0);
        assertThat(sut.listRecords(sourceDoc1, "otherUser", feature2)).hasSize(0);
    }

    // Helper

    private SourceDocument createSourceDocument(String aName)
    {
        SourceDocument doc = new SourceDocument();
        doc.setName(aName);
        return testEntityManager.persist(doc);
    }

    private AnnotationLayer createAnnotationLayer(String aType)
    {
        AnnotationLayer layer = new AnnotationLayer();
        layer.setEnabled(true);
        layer.setName(aType);
        layer.setReadonly(false);
        layer.setType(aType);
        layer.setUiName(aType);
        layer.setAnchoringMode(false, false);

        return testEntityManager.persist(layer);
    }

    private AnnotationFeature createAnnotationFeature(AnnotationLayer aLayer, String aName)
    {
        AnnotationFeature feature = new AnnotationFeature();
        feature.setLayer(aLayer);
        feature.setName(aName);
        feature.setUiName(aName);
        feature.setType(CAS.TYPE_NAME_STRING);

        return testEntityManager.persist(feature);
    }
}
