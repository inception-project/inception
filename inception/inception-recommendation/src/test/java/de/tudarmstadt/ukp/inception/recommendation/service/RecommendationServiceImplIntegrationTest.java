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
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation.MAIN_EDITOR;
import static de.tudarmstadt.ukp.inception.recommendation.service.RecommendationServiceImpl.getOffsetsAnchoredOnTokens;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.JCasFactory.createJCas;
import static org.apache.uima.fit.factory.JCasFactory.createText;
import static org.apache.uima.util.TypeSystemUtil.typeSystem2TypeSystemDescription;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

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
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageSession;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommenderFactoryRegistry;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Offset;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.schema.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.service.AnnotationSchemaServiceImpl;
import de.tudarmstadt.ukp.inception.schema.service.FeatureSupportRegistryImpl;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = SpringConfig.class)
@Transactional
@DataJpaTest(excludeAutoConfiguration = LiquibaseAutoConfiguration.class)
public class RecommendationServiceImplIntegrationTest
{
    private static final String PROJECT_NAME = "Test project";

    private @Autowired TestEntityManager testEntityManager;

    private @Mock RecommenderFactoryRegistry recommenderFactoryRegistry;
    private @Mock AnnotationSchemaServiceImpl schemaService;
    private @Mock LayerSupportRegistry layerSupportRegistry;

    private RecommendationServiceImpl sut;

    private FeatureSupportRegistryImpl featureSupportRegistry;
    private Project project;
    private AnnotationLayer layer;
    private Recommender rec;
    private AnnotationFeature feature;

    @BeforeEach
    public void setUp() throws Exception
    {
        sut = new RecommendationServiceImpl(null, null, null, recommenderFactoryRegistry, null,
                schemaService, null, null, testEntityManager.getEntityManager());

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

        List<Recommender> enabledRecommenders = sut.listEnabledRecommenders(rec.getLayer());

        assertThat(enabledRecommenders).as("Check that the previously created recommender is found")
                .hasSize(1).contains(rec);
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

        long numOfRecommenders = sut.countEnabledRecommenders();
        assertThat(numOfRecommenders).isEqualTo(1);
    }

    @Test
    public void getNumOfEnabledRecommenders_WithNoEnabledRecommender()
    {
        rec.setEnabled(false);
        testEntityManager.persist(rec);

        long numOfRecommenders = sut.countEnabledRecommenders();
        assertThat(numOfRecommenders).isEqualTo(0);
    }

    @Test
    public void getRecommenders_WithOneEnabledRecommender_ShouldReturnStoredRecommender()
    {
        Optional<Recommender> enabledRecommenders = sut.getEnabledRecommender(rec.getId());

        assertThat(enabledRecommenders)
                .as("Check that only the previously created recommender is found").isPresent()
                .contains(rec);
    }

    @Test
    public void getRecommenders_WithOnlyDisabledRecommender_ShouldReturnEmptyList()
    {
        rec.setEnabled(false);
        testEntityManager.persist(rec);

        Optional<Recommender> enabledRecommenders = sut.getEnabledRecommender(rec.getId());

        assertThat(enabledRecommenders).as("Check that no recommender is found").isEmpty();
    }

    @Test
    public void getRecommenders_WithOtherRecommenderId_ShouldReturnEmptyList()
    {

        long otherId = 9999L;
        Optional<Recommender> enabledRecommenders = sut.getEnabledRecommender(otherId);

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
            when(schemaService.listAnnotationLayer(project)).thenReturn(asList(layer));
            doCallRealMethod().when(schemaService).upgradeCas(any(CAS.class), any(CAS.class),
                    any(TypeSystemDescription.class));

            sut.cloneAndMonkeyPatchCAS(project, jCas.getCas(), jCas.getCas());

            Type type = CasUtil.getType(jCas.getCas(), layer.getName());

            assertThat(type.getFeatures()).extracting(Feature::getShortName)
                    .contains(feature.getName() + FEATURE_NAME_SCORE_SUFFIX)
                    .contains(feature.getName() + FEATURE_NAME_SCORE_EXPLANATION_SUFFIX)
                    .contains(feature.getName() + FEATURE_NAME_AUTO_ACCEPT_MODE_SUFFIX)
                    .contains(FEATURE_NAME_IS_PREDICTION);
        }
    }

    @Test
    void thatZeroWithAnnotationsAreCorrectlyAnchoredOnTokens() throws Exception
    {
        JCas jCas = createJCas();
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

        layer.setOverlapMode(NO_OVERLAP);
        var cas = createJCas();
        var targetFS = new NamedEntity(cas, 0, 10);
        targetFS.addToIndexes();
        assertThat(targetFS.getValue()).isNull();

        var s1 = SpanSuggestion.builder().withLabel("V1").withPosition(new Offset(targetFS))
                .build();
        sut.acceptSuggestion(doc, docOwner, cas.getCas(), adapter, feature, s1, MAIN_EDITOR);

        assertThat(targetFS.getValue()) //
                .as("Label was merged into existing annotation replacing unset label") //
                .isEqualTo("V1");

        var s2 = SpanSuggestion.builder().withLabel("V2").withPosition(new Offset(targetFS))
                .build();
        sut.acceptSuggestion(doc, docOwner, cas.getCas(), adapter, feature, s2, MAIN_EDITOR);

        assertThat(targetFS.getValue()) //
                .as("Label was merged into existing annotation replacing previous label") //
                .isEqualTo("V2");

        var s3 = SpanSuggestion.builder().withLabel("V3").withPosition(new Offset(10, 20)).build();
        sut.acceptSuggestion(doc, docOwner, cas.getCas(), adapter, feature, s3, MAIN_EDITOR);

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
        sut.acceptSuggestion(doc, docOwner, cas.getCas(), adapter, feature, s4, MAIN_EDITOR);

        assertThat(targetFS.getValue()) //
                .as("Label was merged into existing annotation replacing unset label") //
                .isEqualTo("V1");

        var s5 = SpanSuggestion.builder().withLabel("V2").withPosition(new Offset(targetFS))
                .build();
        sut.acceptSuggestion(doc, docOwner, cas.getCas(), adapter, feature, s5, MAIN_EDITOR);

        assertThat(cas.select(NamedEntity.class).asList()) //
                .as("Label was merged as new annotation") //
                .extracting(NamedEntity::getBegin, NamedEntity::getEnd, NamedEntity::getValue)
                .containsExactlyInAnyOrder( //
                        tuple(0, 10, "V1"), //
                        tuple(0, 10, "V2"));

        var s6 = SpanSuggestion.builder().withLabel("V3").withPosition(new Offset(10, 20)).build();
        sut.acceptSuggestion(doc, docOwner, cas.getCas(), adapter, feature, s6, MAIN_EDITOR);

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
        sut.acceptSuggestion(doc, docOwner, cas.getCas(), adapter, feature, s7, MAIN_EDITOR);

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

    // Helper

    private Project createProject(String aName)
    {
        Project l = new Project();
        l.setName(aName);
        return testEntityManager.persist(l);
    }

    private AnnotationLayer createAnnotationLayer()
    {
        AnnotationLayer l = new AnnotationLayer();
        l.setEnabled(true);
        l.setName(NamedEntity.class.getName());
        l.setReadonly(false);
        l.setType(NamedEntity.class.getName());
        l.setUiName("test ui name");
        l.setAnchoringMode(false, false);

        return testEntityManager.persist(l);
    }

    private Recommender buildRecommender(Project aProject, AnnotationFeature aFeature)
    {
        Recommender recommender = new Recommender();
        recommender.setLayer(aFeature.getLayer());
        recommender.setFeature(aFeature);
        recommender.setProject(aProject);
        recommender.setAlwaysSelected(true);
        recommender.setSkipEvaluation(false);
        recommender.setMaxRecommendations(3);
        recommender.setTool("dummyRecommenderTool");

        return recommender;
    }

    private AnnotationFeature createAnnotationFeature(AnnotationLayer aLayer, String aName)
    {
        AnnotationFeature f = new AnnotationFeature();
        f.setLayer(aLayer);
        f.setName(aName);
        f.setUiName(aName);
        f.setType(CAS.TYPE_NAME_STRING);

        return testEntityManager.persist(f);
    }
}
