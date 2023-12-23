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
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_AUTO_ACCEPT_MODE_SUFFIX;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_IS_PREDICTION;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_SCORE_EXPLANATION_SUFFIX;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_SCORE_SUFFIX;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AutoAcceptMode.NEVER;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation.DETAIL_EDITOR;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation.MAIN_EDITOR;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.ACCEPTED;
import static de.tudarmstadt.ukp.inception.recommendation.service.SuggestionExtraction.getOffsetsAnchoredOnTokens;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.JCasFactory.createJCas;
import static org.apache.uima.fit.factory.JCasFactory.createText;
import static org.apache.uima.util.TypeSystemUtil.typeSystem2TypeSystemDescription;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.TypeSystemDescription;
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

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageSession;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommenderFactoryRegistry;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Offset;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RelationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.service.AnnotationSchemaServiceImpl;
import de.tudarmstadt.ukp.inception.schema.service.FeatureSupportRegistryImpl;

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

    private @Mock RecommenderFactoryRegistry recommenderFactoryRegistry;
    private @Mock AnnotationSchemaServiceImpl schemaService;
    private @Mock LayerSupportRegistry layerSupportRegistry;

    private RecommendationServiceImpl sut;

    private FeatureSupportRegistryImpl featureSupportRegistry;
    private LayerRecommendtionSupportRegistryImpl layerRecommendtionSupportRegistry;
    private Project project;
    private AnnotationLayer layer;
    private Recommender rec;
    private AnnotationFeature feature;

    @BeforeEach
    public void setUp() throws Exception
    {
        layerRecommendtionSupportRegistry = new LayerRecommendtionSupportRegistryImpl(asList( //
                new SpanSuggestionSupport(sut, sut, null, schemaService),
                new RelationSuggestionSupport(sut, sut, null, schemaService)));
        layerRecommendtionSupportRegistry.init();

        sut = new RecommendationServiceImpl(null, null, null, recommenderFactoryRegistry, null,
                schemaService, null, layerRecommendtionSupportRegistry,
                testEntityManager.getEntityManager());

        featureSupportRegistry = new FeatureSupportRegistryImpl(asList(new StringFeatureSupport()));
        featureSupportRegistry.init();

        project = createProject(PROJECT_NAME);
        layer = createAnnotationLayer();
        layer.setProject(project);
        feature = createAnnotationFeature(layer, "value");

        rec = buildRecommender(project, feature);
        sut.createOrUpdateRecommender(rec);
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
        sut.createOrUpdateRecommender(rec);

        var enabledRecommenders = sut.listEnabledRecommenders(rec.getLayer());

        assertThat(enabledRecommenders) //
                .as("Check that the previously created recommender is found") //
                .containsExactly(rec);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getNumOfEnabledRecommenders_WithOneEnabledRecommender()
    {
        var recFactory = mock(RecommendationEngineFactory.class);
        when(recommenderFactoryRegistry.getFactory(any(String.class))) //
                .thenReturn(recFactory);

        assertThat(recommenderFactoryRegistry.getFactory("nummy")).isNotNull();

        sut.createOrUpdateRecommender(rec);

        assertThat(sut.countEnabledRecommenders()).isEqualTo(1);
    }

    @Test
    public void getNumOfEnabledRecommenders_WithNoEnabledRecommender()
    {
        rec.setEnabled(false);
        testEntityManager.persist(rec);

        assertThat(sut.countEnabledRecommenders()).isEqualTo(0);
    }

    @Test
    public void getRecommenders_WithOneEnabledRecommender_ShouldReturnStoredRecommender()
    {
        assertThat(sut.getEnabledRecommender(rec.getId()))
                .as("Check that only the previously created recommender is found").isPresent()
                .contains(rec);
    }

    @Test
    public void getRecommenders_WithOnlyDisabledRecommender_ShouldReturnEmptyList()
    {
        rec.setEnabled(false);
        testEntityManager.persist(rec);

        assertThat(sut.getEnabledRecommender(rec.getId())) //
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
    public void monkeyPatchTypeSystem_WithNer_CreatesScoreFeatures() throws Exception
    {
        try (CasStorageSession session = CasStorageSession.open()) {
            JCas jCas = createText("I am text CAS", "de");
            session.add("jCas", CasAccessMode.EXCLUSIVE_WRITE_ACCESS, jCas.getCas());

            when(schemaService.getFullProjectTypeSystem(project))
                    .thenReturn(typeSystem2TypeSystemDescription(jCas.getTypeSystem()));
            when(schemaService.listAnnotationFeature(project)).thenReturn(asList(feature));
            doCallRealMethod().when(schemaService).upgradeCas(any(CAS.class), any(CAS.class),
                    any(TypeSystemDescription.class));

            sut.cloneAndMonkeyPatchCAS(project, jCas.getCas(), jCas.getCas());

            Type type = CasUtil.getType(jCas.getCas(), layer.getName());

            assertThat(type.getFeatures()) //
                    .extracting(Feature::getShortName) //
                    .containsExactlyInAnyOrder( //
                            "sofa", //
                            "begin", //
                            "end", //
                            "value", //
                            feature.getName() + FEATURE_NAME_SCORE_SUFFIX, //
                            feature.getName() + FEATURE_NAME_SCORE_EXPLANATION_SUFFIX, //
                            feature.getName() + FEATURE_NAME_AUTO_ACCEPT_MODE_SUFFIX, //
                            "identifier", //
                            FEATURE_NAME_IS_PREDICTION);
        }
    }

    @Test
    void thatZeroWithAnnotationsAreCorrectlyAnchoredOnTokens() throws Exception
    {
        var jCas = createJCas();
        TokenBuilder.create(Token.class, Sentence.class).buildTokens(jCas, "  This is  a test.  ");
        var textLength = jCas.getDocumentText().length();
        var tokens = jCas.select(Token.class).asList();
        var firstTokenBegin = tokens.get(0).getBegin();
        var lastTokenEnd = tokens.get(tokens.size() - 1).getEnd();

        assertThat(getOffsetsAnchoredOnTokens(jCas.getCas(), new Annotation(jCas, 0, 0))).get()
                .as("Zero-width annotation before first token snaps to first token start")
                .isEqualTo(new Offset(firstTokenBegin, firstTokenBegin));

        assertThat(getOffsetsAnchoredOnTokens(jCas.getCas(),
                new Annotation(jCas, textLength, textLength))).get()
                        .as("Zero-width annotation after last token snaps to last token end")
                        .isEqualTo(new Offset(lastTokenEnd, lastTokenEnd));

        assertThat(getOffsetsAnchoredOnTokens(jCas.getCas(), new Annotation(jCas, 4, 4))).get()
                .as("Zero-width annotation within token remains") //
                .isEqualTo(new Offset(4, 4));

        assertThat(getOffsetsAnchoredOnTokens(jCas.getCas(), new Annotation(jCas, 10, 10))).get()
                .as("Zero-width annotation between tokens snaps to end of previous") //
                .isEqualTo(new Offset(9, 9));
    }

    @Test
    void testUpsertSpanFeature() throws Exception
    {
        var docOwner = "dummy";
        var doc = SourceDocument.builder() //
                .withProject(project) //
                .build();
        var feature = AnnotationFeature.builder() //
                .withName(NamedEntity._FeatName_value) //
                .withType(CAS.TYPE_NAME_STRING) //
                .build();
        var layer = AnnotationLayer.builder() //
                .forJCasClass(NamedEntity.class) //
                .build();
        var adapter = new SpanAdapter(layerSupportRegistry, featureSupportRegistry, null, layer,
                () -> asList(), asList());

        when(schemaService.getLayer(anyLong())).thenReturn(layer);
        when(schemaService.getAdapter(any())).thenReturn(adapter);
        when(schemaService.getFeature(any(), any())).thenReturn(feature);

        layer.setOverlapMode(NO_OVERLAP);
        var cas = createJCas();
        var targetFS = new NamedEntity(cas, 0, 10);
        targetFS.addToIndexes();
        assertThat(targetFS.getValue()).isNull();

        var s1 = SpanSuggestion.builder().withLabel("V1").withPosition(new Offset(targetFS))
                .build();
        sut.acceptSuggestion(USER_NAME, doc, docOwner, cas.getCas(), s1, MAIN_EDITOR);

        assertThat(targetFS.getValue()) //
                .as("Label was merged into existing annotation replacing unset label") //
                .isEqualTo("V1");

        var s2 = SpanSuggestion.builder().withLabel("V2").withPosition(new Offset(targetFS))
                .build();
        sut.acceptSuggestion(USER_NAME, doc, docOwner, cas.getCas(), s2, MAIN_EDITOR);

        assertThat(targetFS.getValue()) //
                .as("Label was merged into existing annotation replacing previous label") //
                .isEqualTo("V2");

        var s3 = SpanSuggestion.builder().withLabel("V3").withPosition(new Offset(10, 20)).build();
        sut.acceptSuggestion(USER_NAME, doc, docOwner, cas.getCas(), s3, MAIN_EDITOR);

        assertThat(cas.select(NamedEntity.class).asList()) //
                .as("Label was merged as new annotation") //
                .extracting(NamedEntity::getBegin, NamedEntity::getEnd, NamedEntity::getValue)
                .containsExactlyInAnyOrder( //
                        tuple(0, 10, "V2"), //
                        tuple(10, 20, "V3"));

        layer.setOverlapMode(ANY_OVERLAP);
        cas.reset();
        targetFS = new NamedEntity(cas, 0, 10);
        targetFS.addToIndexes();
        assertThat(targetFS.getValue()).isNull();

        var s4 = SpanSuggestion.builder().withLabel("V1").withPosition(new Offset(targetFS))
                .build();
        sut.acceptSuggestion(USER_NAME, doc, docOwner, cas.getCas(), s4, MAIN_EDITOR);

        assertThat(targetFS.getValue()) //
                .as("Label was merged into existing annotation replacing unset label") //
                .isEqualTo("V1");

        var s5 = SpanSuggestion.builder().withLabel("V2").withPosition(new Offset(targetFS))
                .build();
        sut.acceptSuggestion(USER_NAME, doc, docOwner, cas.getCas(), s5, MAIN_EDITOR);

        assertThat(cas.select(NamedEntity.class).asList()) //
                .as("Label was merged as new annotation") //
                .extracting(NamedEntity::getBegin, NamedEntity::getEnd, NamedEntity::getValue)
                .containsExactlyInAnyOrder( //
                        tuple(0, 10, "V1"), //
                        tuple(0, 10, "V2"));

        var s6 = SpanSuggestion.builder().withLabel("V3").withPosition(new Offset(10, 20)).build();
        sut.acceptSuggestion(USER_NAME, doc, docOwner, cas.getCas(), s6, MAIN_EDITOR);

        assertThat(cas.select(NamedEntity.class).asList()) //
                .as("Label was merged as new annotation") //
                .extracting(NamedEntity::getBegin, NamedEntity::getEnd, NamedEntity::getValue)
                .containsExactlyInAnyOrder( //
                        tuple(0, 10, "V1"), //
                        tuple(0, 10, "V2"), //
                        tuple(10, 20, "V3"));

        new NamedEntity(cas, 0, 10).addToIndexes();
        new NamedEntity(cas, 0, 10).addToIndexes();

        var s7 = SpanSuggestion.builder().withLabel("V4").withPosition(new Offset(0, 10)).build();
        sut.acceptSuggestion(USER_NAME, doc, docOwner, cas.getCas(), s7, MAIN_EDITOR);

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
        var layer = createAnnotationLayer("layer");
        var feature = createAnnotationFeature(layer, FEATURE_NAME);

        var suggestion = SpanSuggestion.builder() //
                .withId(42) //
                .withRecommenderId(1337) //
                .withRecommenderName("testRecommender") //
                .withLayerId(layer.getId()) //
                .withFeature(feature.getName()) //
                .withDocumentName(sourceDoc.getName()) //
                .withPosition(new Offset(7, 14)) //
                .withCoveredText("aCoveredText") //
                .withLabel("testLabel") //
                .withUiLabel("testUiLabel") //
                .withScore(0.42) //
                .withScoreExplanation("Test confidence") //
                .withAutoAcceptMode(NEVER) //
                .build();

        sut.logRecord(USER_NAME, sourceDoc, USER_NAME, suggestion, feature, ACCEPTED, MAIN_EDITOR);

        var records = sut.listLearningRecords(USER_NAME, USER_NAME, layer);
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
                .hasFieldOrPropertyWithValue("userAction", ACCEPTED) //
                .hasFieldOrPropertyWithValue("suggestionType", SpanSuggestionSupport.TYPE);
    }

    @Test
    public void thatRelationSuggestionsCanBeRecorded()
    {
        var sourceDoc = createSourceDocument("doc");
        var layer = createAnnotationLayer("layer");
        var feature = createAnnotationFeature(layer, FEATURE_NAME);

        var suggestion = new RelationSuggestion(42, 1337, "testRecommender", layer.getId(),
                feature.getName(), sourceDoc.getName(), 7, 14, 21, 28, "testLabel", "testUiLabel",
                0.42, "Test confidence", NEVER);

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
        var layer1 = createAnnotationLayer("layer1");
        var layer2 = createAnnotationLayer("layer2");
        var feature1 = createAnnotationFeature(layer1, "feat1");
        var feature2 = createAnnotationFeature(layer2, "feat1");

        sut.logRecord(USER_NAME, sourceDoc1, USER_NAME,
                new SpanSuggestion(42, 1337, "testRecommender", layer1.getId(), feature1.getName(),
                        sourceDoc1.getName(), 7, 14, "aCoveredText", "testLabel", "testUiLabel",
                        0.42, "Test confidence", NEVER),
                feature1, ACCEPTED, MAIN_EDITOR);

        sut.logRecord(USER_NAME, sourceDoc1, USER_NAME,
                new SpanSuggestion(42, 1337, "testRecommender2", layer2.getId(), feature2.getName(),
                        sourceDoc1.getName(), 7, 14, "aCoveredText", "testLabel", "testUiLabel",
                        0.42, "Test confidence", NEVER),
                feature2, ACCEPTED, MAIN_EDITOR);

        sut.logRecord(USER_NAME, sourceDoc2, USER_NAME,
                new SpanSuggestion(42, 1337, "testRecommender", layer1.getId(), feature1.getName(),
                        sourceDoc2.getName(), 7, 14, "aCoveredText", "testLabel", "testUiLabel",
                        0.42, "Test confidence", NEVER),
                feature1, ACCEPTED, MAIN_EDITOR);

        sut.logRecord(USER_NAME, sourceDoc2, USER_NAME,
                new SpanSuggestion(42, 1337, "testRecommender2", layer2.getId(), feature2.getName(),
                        sourceDoc2.getName(), 7, 14, "aCoveredText", "testLabel", "testUiLabel",
                        0.42, "Test confidence", NEVER),
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
        var doc = new SourceDocument();
        doc.setProject(project);
        doc.setName(aName);
        return testEntityManager.persist(doc);
    }

    private AnnotationLayer createAnnotationLayer(String aType)
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

    private AnnotationLayer createAnnotationLayer()
    {
        return createAnnotationLayer(NamedEntity._TypeName);
    }

    private Recommender buildRecommender(Project aProject, AnnotationFeature aFeature)
    {
        var r = new Recommender();
        r.setLayer(aFeature.getLayer());
        r.setFeature(aFeature);
        r.setProject(aProject);
        r.setAlwaysSelected(true);
        r.setSkipEvaluation(false);
        r.setMaxRecommendations(3);
        r.setTool("dummyRecommenderTool");

        return r;
    }
}
