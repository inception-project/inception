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

import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.ANY_OVERLAP;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation.DETAIL_EDITOR;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation.MAIN_EDITOR;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.ACCEPTED;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.JCasFactory.createJCas;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.uima.cas.CAS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.recommender.RelationSuggestionSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanAdapterImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.recommender.SpanSuggestionSupport;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommenderFactoryRegistry;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Offset;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RelationPosition;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RelationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.service.AnnotationSchemaServiceImpl;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = SpringConfig.class)
@Transactional
@DataJpaTest( //
        showSql = false, //
        properties = { //
                "spring.main.banner-mode=off" }, //
        excludeAutoConfiguration = LiquibaseAutoConfiguration.class)
public class RecommendationServiceImplIntegrationTest
{
    private static final String PROJECT_NAME = "Test project";
    private static final String USER_NAME = "testUser";
    private static final String FEATURE_NAME = "testFeature";

    private @Autowired TestEntityManager testEntityManager;

    private @Mock ConstraintsService constraintsService;
    private @Mock RecommenderFactoryRegistry recommenderFactoryRegistry;
    private @Mock AnnotationSchemaServiceImpl schemaService;
    private @Mock LayerSupportRegistry layerSupportRegistry;
    private @Mock LearningRecordService learningRecordService;

    private RecommendationServiceImpl sut;

    private FeatureSupportRegistryImpl featureSupportRegistry;
    private SuggestionSupportRegistryImpl suggestionSupportRegistry;
    private Project project;
    private AnnotationLayer spanLayer;
    private Recommender spanLayerRecommender;
    private AnnotationFeature spanLayerFeature;

    @BeforeEach
    public void setUp() throws Exception
    {
        suggestionSupportRegistry = new SuggestionSupportRegistryImpl(asList( //
                new SpanSuggestionSupport(null, learningRecordService, null, schemaService, null,
                        null),
                new RelationSuggestionSupport(null, learningRecordService, null, schemaService,
                        null)));

        sut = new RecommendationServiceImpl(null, null, null, recommenderFactoryRegistry, null,
                schemaService, suggestionSupportRegistry, testEntityManager.getEntityManager());

        suggestionSupportRegistry.init();

        featureSupportRegistry = new FeatureSupportRegistryImpl(asList(new StringFeatureSupport()));
        featureSupportRegistry.init();

        project = createProject(PROJECT_NAME);
        spanLayer = createSpanLayer(NamedEntity._TypeName);
        spanLayerFeature = createAnnotationFeature(spanLayer, "value");

        spanLayerRecommender = buildRecommender(spanLayerFeature);
        sut.createOrUpdateRecommender(spanLayerRecommender);
    }

    @AfterEach
    public void tearDown() throws Exception
    {
        testEntityManager.clear();
    }

    @Test
    public void thatApplicationContextStarts()
    {
    }

    @Test
    public void listRecommenders_WithOneEnabledRecommender_ShouldReturnStoredRecommender()
    {
        sut.createOrUpdateRecommender(spanLayerRecommender);

        var enabledRecommenders = sut.listEnabledRecommenders(spanLayerRecommender.getLayer());

        assertThat(enabledRecommenders) //
                .as("Check that the previously created recommender is found") //
                .containsExactly(spanLayerRecommender);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getNumOfEnabledRecommenders_WithOneEnabledRecommender()
    {
        var recFactory = mock(RecommendationEngineFactory.class);
        when(recommenderFactoryRegistry.getFactory(any(String.class))) //
                .thenReturn(recFactory);

        assertThat(recommenderFactoryRegistry.getFactory("nummy")).isNotNull();

        sut.createOrUpdateRecommender(spanLayerRecommender);

        assertThat(sut.countEnabledRecommenders()).isEqualTo(1);
    }

    @Test
    public void getNumOfEnabledRecommenders_WithNoEnabledRecommender()
    {
        spanLayerRecommender.setEnabled(false);
        testEntityManager.persist(spanLayerRecommender);

        assertThat(sut.countEnabledRecommenders()).isEqualTo(0);
    }

    @Test
    public void getRecommenders_WithOneEnabledRecommender_ShouldReturnStoredRecommender()
    {
        assertThat(sut.getEnabledRecommender(spanLayerRecommender.getId()))
                .as("Check that only the previously created recommender is found").isPresent()
                .contains(spanLayerRecommender);
    }

    @Test
    public void getRecommenders_WithOnlyDisabledRecommender_ShouldReturnEmptyList()
    {
        spanLayerRecommender.setEnabled(false);
        testEntityManager.persist(spanLayerRecommender);

        assertThat(sut.getEnabledRecommender(spanLayerRecommender.getId())) //
                .as("Check that no recommender is found") //
                .isEmpty();
    }

    @Test
    public void getRecommenders_WithOtherRecommenderId_ShouldReturnEmptyList()
    {
        var otherId = 9999L;
        var enabledRecommenders = sut.getEnabledRecommender(otherId);

        assertThat(enabledRecommenders).as("Check that no recommender is found").isEmpty();
    }

    @Test
    void testUpsertSpanFeature() throws Exception
    {
        var docOwner = "dummy";
        var doc = SourceDocument.builder() //
                .withId(1l) //
                .withProject(project) //
                .build();
        var adapter = new SpanAdapterImpl(layerSupportRegistry, featureSupportRegistry, null,
                spanLayer, () -> asList(), asList(), constraintsService);

        when(schemaService.getLayer(anyLong())).thenReturn(spanLayer);
        when(schemaService.getAdapter(any())).thenReturn(adapter);
        when(schemaService.getFeature(any(), any())).thenReturn(spanLayerFeature);

        spanLayer.setOverlapMode(NO_OVERLAP);
        var cas = createJCas();
        var targetFS = new NamedEntity(cas, 0, 10);
        targetFS.addToIndexes();
        assertThat(targetFS.getValue()).isNull();

        var s1 = SpanSuggestion.builder().withLabel("V1").withRecommender(spanLayerRecommender)
                .withPosition(new Offset(targetFS)).build();
        sut.acceptSuggestion(USER_NAME, doc, docOwner, cas.getCas(), null, s1, MAIN_EDITOR);

        assertThat(targetFS.getValue()) //
                .as("Label was merged into existing annotation replacing unset label") //
                .isEqualTo("V1");

        var s2 = SpanSuggestion.builder().withLabel("V2").withRecommender(spanLayerRecommender)
                .withPosition(new Offset(targetFS)).build();
        sut.acceptSuggestion(USER_NAME, doc, docOwner, cas.getCas(), null, s2, MAIN_EDITOR);

        assertThat(targetFS.getValue()) //
                .as("Label was merged into existing annotation replacing previous label") //
                .isEqualTo("V2");

        var s3 = SpanSuggestion.builder().withLabel("V3").withRecommender(spanLayerRecommender)
                .withPosition(new Offset(10, 20)).build();
        sut.acceptSuggestion(USER_NAME, doc, docOwner, cas.getCas(), null, s3, MAIN_EDITOR);

        assertThat(cas.select(NamedEntity.class).asList()) //
                .as("Label was merged as new annotation") //
                .extracting(NamedEntity::getBegin, NamedEntity::getEnd, NamedEntity::getValue)
                .containsExactlyInAnyOrder( //
                        tuple(0, 10, "V2"), //
                        tuple(10, 20, "V3"));

        spanLayer.setOverlapMode(ANY_OVERLAP);
        cas.reset();
        targetFS = new NamedEntity(cas, 0, 10);
        targetFS.addToIndexes();
        assertThat(targetFS.getValue()).isNull();

        var s4 = SpanSuggestion.builder().withLabel("V1").withRecommender(spanLayerRecommender)
                .withPosition(new Offset(targetFS)).build();
        sut.acceptSuggestion(USER_NAME, doc, docOwner, cas.getCas(), null, s4, MAIN_EDITOR);

        assertThat(targetFS.getValue()) //
                .as("Label was merged into existing annotation replacing unset label") //
                .isEqualTo("V1");

        var s5 = SpanSuggestion.builder().withLabel("V2").withRecommender(spanLayerRecommender)
                .withPosition(new Offset(targetFS)).build();
        sut.acceptSuggestion(USER_NAME, doc, docOwner, cas.getCas(), null, s5, MAIN_EDITOR);

        assertThat(cas.select(NamedEntity.class).asList()) //
                .as("Label was merged as new annotation") //
                .extracting(NamedEntity::getBegin, NamedEntity::getEnd, NamedEntity::getValue)
                .containsExactlyInAnyOrder( //
                        tuple(0, 10, "V1"), //
                        tuple(0, 10, "V2"));

        var s6 = SpanSuggestion.builder().withLabel("V3").withRecommender(spanLayerRecommender)
                .withPosition(new Offset(10, 20)).build();
        sut.acceptSuggestion(USER_NAME, doc, docOwner, cas.getCas(), null, s6, MAIN_EDITOR);

        assertThat(cas.select(NamedEntity.class).asList()) //
                .as("Label was merged as new annotation") //
                .extracting(NamedEntity::getBegin, NamedEntity::getEnd, NamedEntity::getValue)
                .containsExactlyInAnyOrder( //
                        tuple(0, 10, "V1"), //
                        tuple(0, 10, "V2"), //
                        tuple(10, 20, "V3"));

        new NamedEntity(cas, 0, 10).addToIndexes();
        new NamedEntity(cas, 0, 10).addToIndexes();

        var s7 = SpanSuggestion.builder().withLabel("V4").withRecommender(spanLayerRecommender)
                .withPosition(new Offset(0, 10)).build();
        sut.acceptSuggestion(USER_NAME, doc, docOwner, cas.getCas(), null, s7, MAIN_EDITOR);

        assertThat(cas.select(NamedEntity.class).asList()) //
                .as("Label was merged again into one of the entities without a label") //
                .extracting(NamedEntity::getBegin, NamedEntity::getEnd, NamedEntity::getValue)
                .containsExactlyInAnyOrder( //
                        tuple(0, 10, "V1"), //
                        tuple(0, 10, "V2"), //
                        tuple(0, 10, "V4"), //
                        tuple(0, 10, null), //
                        tuple(10, 20, "V3"));
    }

    @Test
    public void thatSpanSuggestionsCanBeRecorded()
    {
        var sourceDoc = createSourceDocument("doc");

        var suggestion = SpanSuggestion.builder() //
                .withId(42) //
                .withRecommender(spanLayerRecommender) //
                .withDocument(sourceDoc) //
                .withPosition(7, 14) //
                .withCoveredText("aCoveredText") //
                .withLabel("testLabel") //
                .withUiLabel("testUiLabel") //
                .withScore(0.42) //
                .withScoreExplanation("Test confidence") //
                .build();

        sut.logRecord(USER_NAME, sourceDoc, USER_NAME, suggestion, spanLayerFeature, ACCEPTED,
                MAIN_EDITOR);

        var records = sut.listLearningRecords(USER_NAME, USER_NAME, spanLayer);
        assertThat(records).hasSize(1);

        LearningRecord record = records.get(0);
        assertThat(record).hasFieldOrProperty("id") //
                .hasFieldOrPropertyWithValue("sourceDocument", sourceDoc) //
                .hasFieldOrPropertyWithValue("user", USER_NAME) //
                .hasFieldOrPropertyWithValue("layer", spanLayer) //
                .hasFieldOrPropertyWithValue("annotationFeature", spanLayerFeature) //
                .hasFieldOrPropertyWithValue("offsetBegin", 7) //
                .hasFieldOrPropertyWithValue("offsetEnd", 14) //
                .hasFieldOrPropertyWithValue("offsetBegin2", -1) //
                .hasFieldOrPropertyWithValue("offsetEnd2", -1) //
                .hasFieldOrPropertyWithValue("tokenText", "aCoveredText") //
                .hasFieldOrPropertyWithValue("annotation", "testLabel") //
                .hasFieldOrPropertyWithValue("changeLocation", MAIN_EDITOR) //
                .hasFieldOrPropertyWithValue("userAction", ACCEPTED) //
                .hasFieldOrPropertyWithValue("suggestionType", SpanSuggestionSupport.TYPE);
    }

    @Test
    public void thatRelationSuggestionsCanBeRecorded()
    {
        var sourceDoc = createSourceDocument("doc");
        var layer = createRelationLayer("layer");
        var feature = createAnnotationFeature(layer, FEATURE_NAME);
        var rec = buildRecommender(feature);

        var suggestion = RelationSuggestion.builder().withId(42).withRecommender(rec)
                .withDocument(sourceDoc).withPosition(new RelationPosition(7, 14, 21, 28))
                .withLabel("testLabel").withUiLabel("testUiLabel").withScore(0.42)
                .withScoreExplanation("Test confidence").build();

        sut.logRecord(USER_NAME, sourceDoc, USER_NAME, suggestion, feature,
                LearningRecordUserAction.REJECTED, DETAIL_EDITOR);

        var records = sut.listLearningRecords(USER_NAME, USER_NAME, layer);
        assertThat(records).hasSize(1);

        var record = records.get(0);
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
                .hasFieldOrPropertyWithValue("userAction", LearningRecordUserAction.REJECTED) //
                .hasFieldOrPropertyWithValue("suggestionType", RelationSuggestionSupport.TYPE);
    }

    @Test
    void thatListingRecordsForRendering()
    {
        var sourceDoc1 = createSourceDocument("doc1");
        var sourceDoc2 = createSourceDocument("doc2");
        var layer1 = createSpanLayer("layer1");
        var layer2 = createSpanLayer("layer2");
        var feature1 = createAnnotationFeature(layer1, "feat1");
        var feature2 = createAnnotationFeature(layer2, "feat1");
        var rec1 = buildRecommender(feature1);
        var rec2 = buildRecommender(feature2);

        Offset position = new Offset(7, 14);
        sut.logRecord(USER_NAME, sourceDoc1, USER_NAME,
                SpanSuggestion.builder().withRecommender(rec1).withDocument(sourceDoc1)
                        .withPosition(position).withLabel("testLabel")
                        .withCoveredText("aCoveredText").build(),
                feature1, ACCEPTED, MAIN_EDITOR);

        sut.logRecord(USER_NAME, sourceDoc1, USER_NAME,
                SpanSuggestion.builder().withRecommender(rec2).withDocument(sourceDoc1)
                        .withPosition(position).withLabel("testLabel")
                        .withCoveredText("aCoveredText").build(),
                feature2, ACCEPTED, MAIN_EDITOR);

        sut.logRecord(USER_NAME, sourceDoc2, USER_NAME,
                SpanSuggestion.builder().withRecommender(rec1).withDocument(sourceDoc1)
                        .withPosition(position).withLabel("testLabel")
                        .withCoveredText("aCoveredText").build(),
                feature1, ACCEPTED, MAIN_EDITOR);

        sut.logRecord(USER_NAME, sourceDoc2, USER_NAME,
                SpanSuggestion.builder().withRecommender(rec2).withDocument(sourceDoc1)
                        .withPosition(position).withLabel("testLabel")
                        .withCoveredText("aCoveredText").build(),
                feature2, ACCEPTED, MAIN_EDITOR);

        assertThat(sut.listLearningRecords(USER_NAME, sourceDoc1, USER_NAME, feature1)).hasSize(1);
        assertThat(sut.listLearningRecords(USER_NAME, sourceDoc1, USER_NAME, feature2)).hasSize(1);
        assertThat(sut.listLearningRecords(USER_NAME, sourceDoc2, USER_NAME, feature1)).hasSize(1);
        assertThat(sut.listLearningRecords(USER_NAME, sourceDoc2, USER_NAME, feature2)).hasSize(1);
        assertThat(sut.listLearningRecords(USER_NAME, sourceDoc1, "otherUser", feature1)).isEmpty();
        assertThat(sut.listLearningRecords(USER_NAME, sourceDoc1, "otherUser", feature2)).isEmpty();
    }

    // Helper

    private SourceDocument createSourceDocument(String aName)
    {
        var doc = SourceDocument.builder().withProject(project).withName(aName).build();
        return testEntityManager.persist(doc);
    }

    private AnnotationLayer createSpanLayer(String aType)
    {
        var l = new AnnotationLayer();
        l.setProject(project);
        l.setEnabled(true);
        l.setName(aType);
        l.setReadonly(false);
        l.setType(SpanLayerSupport.TYPE);
        l.setUiName(aType);
        l.setAnchoringMode(false, false);

        return testEntityManager.persist(l);
    }

    private AnnotationLayer createRelationLayer(String aType)
    {
        var l = new AnnotationLayer();
        l.setProject(project);
        l.setEnabled(true);
        l.setName(aType);
        l.setReadonly(false);
        l.setType(RelationLayerSupport.TYPE);
        l.setUiName(aType);
        l.setAnchoringMode(false, false);

        return testEntityManager.persist(l);
    }

    private AnnotationFeature createAnnotationFeature(AnnotationLayer aLayer, String aName)
    {
        var f = new AnnotationFeature();
        f.setProject(project);
        f.setLayer(aLayer);
        f.setName(aName);
        f.setUiName(aName);
        f.setType(CAS.TYPE_NAME_STRING);

        return testEntityManager.persist(f);
    }

    private Project createProject(String aName)
    {
        var l = new Project();
        l.setName(aName);
        return testEntityManager.persist(l);
    }

    private Recommender buildRecommender(AnnotationFeature aFeature)
    {
        var r = new Recommender();
        r.setLayer(aFeature.getLayer());
        r.setFeature(aFeature);
        r.setProject(aFeature.getProject());
        r.setAlwaysSelected(true);
        r.setSkipEvaluation(false);
        r.setMaxRecommendations(3);
        r.setTool("dummyRecommenderTool");

        return testEntityManager.persist(r);
    }
}
