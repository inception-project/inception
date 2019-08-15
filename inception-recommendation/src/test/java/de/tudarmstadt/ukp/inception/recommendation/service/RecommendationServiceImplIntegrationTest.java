/*
 * Copyright 2018
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

import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_IS_PREDICTION;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_SCORE_EXPLANATION_SUFFIX;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_SCORE_SUFFIX;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.TypeSystemUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.AnnotationSchemaServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommenderFactoryRegistry;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringConfig.class)
@Transactional
@DataJpaTest
public class RecommendationServiceImplIntegrationTest
{
    private static final String PROJECT_NAME = "Test project";

    private @Autowired TestEntityManager testEntityManager;

    private RecommendationServiceImpl sut;
    private SessionRegistry sessionRegistry;
    private UserDao userRepository;
    private RecommenderFactoryRegistry recommenderFactoryRegistry;
    private SchedulingService schedulingService;
    private @Mock AnnotationSchemaServiceImpl annoService;
    private DocumentService documentService;
    private LearningRecordService learningRecordService;

    private Project project;
    private AnnotationLayer layer;
    private User user;
    private Recommender rec;
    private AnnotationFeature feature;

    @Before
    public void setUp() throws Exception
    {
        sut = new RecommendationServiceImpl(sessionRegistry, userRepository,
                recommenderFactoryRegistry, schedulingService, annoService, documentService,
                learningRecordService, testEntityManager.getEntityManager());

        project = createProject(PROJECT_NAME);
        layer = createAnnotationLayer();
        layer.setProject(project);
        user = createUser();
        feature = createAnnotationFeature(layer, "value");

        rec = buildRecommender(project, feature);
        sut.createOrUpdateRecommender(rec);
    }

    @After
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

        assertThat(enabledRecommenders)
        .as("Check that the previously created recommender is found")
                .hasSize(1)
                .contains(rec);
    }
    
    @Test
    public void getRecommenders_WithOneEnabledRecommender_ShouldReturnStoredRecommender()
    {
        Optional<Recommender> enabledRecommenders = sut.getEnabledRecommender(rec.getId());

        assertThat(enabledRecommenders)
                .as("Check that only the previously created recommender is found")
                .isPresent()
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
        Optional<Recommender> enabledRecommenders = sut.getEnabledRecommender(otherId );

        assertThat(enabledRecommenders)
                .as("Check that no recommender is found")
                .isEmpty();
    }

    @Test
    public void monkeyPatchTypeSystem_WithNer_CreatesScoreFeatures() throws Exception
    {
        JCas jCas = JCasFactory.createText("I am text CAS", "de");
        when(annoService.getFullProjectTypeSystem(project))
                .thenReturn(TypeSystemUtil.typeSystem2TypeSystemDescription(jCas.getTypeSystem()));
        when(annoService.listAnnotationLayer(project))
                .thenReturn(asList(layer));
        doCallRealMethod().when(annoService)
                .upgradeCas(any(CAS.class), any(TypeSystemDescription.class));
        doCallRealMethod().when(annoService)
                .upgradeCas(any(CAS.class), any(CAS.class), any(TypeSystemDescription.class));

        sut.cloneAndMonkeyPatchCAS(project, jCas.getCas(), jCas.getCas());

        Type type = CasUtil.getType(jCas.getCas(), layer.getName());

        assertThat(type.getFeatures())
                .extracting(Feature::getShortName)
                .contains(feature.getName() + FEATURE_NAME_SCORE_SUFFIX)
                .contains(feature.getName() + FEATURE_NAME_SCORE_EXPLANATION_SUFFIX)
                .contains(FEATURE_NAME_IS_PREDICTION);
    }

    // Helper

    private Project createProject(String aName)
    {
        Project project = new Project();
        project.setName(aName);
        project.setMode(WebAnnoConst.PROJECT_TYPE_ANNOTATION);
        return testEntityManager.persist(project);
    }

    private AnnotationLayer createAnnotationLayer()
    {
        AnnotationLayer layer = new AnnotationLayer();
        layer.setEnabled(true);
        layer.setName(NamedEntity.class.getName());
        layer.setReadonly(false);
        layer.setType(NamedEntity.class.getName());
        layer.setUiName("test ui name");
        layer.setAnchoringMode(false, false);
       
        return testEntityManager.persist(layer);
    }

    private User createUser()
    {
        User user = new User();

        return user;
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

        return recommender;
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
