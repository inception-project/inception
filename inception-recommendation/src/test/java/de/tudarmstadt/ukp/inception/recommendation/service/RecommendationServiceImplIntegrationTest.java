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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SpringConfig.class)
@Transactional
@DataJpaTest
public class RecommendationServiceImplIntegrationTest
{
    private static final String PROJECT_NAME = "Test project";

    @Autowired
    private TestEntityManager testEntityManager;

    private RecommendationServiceImpl sut;
    private Project project;
    private AnnotationLayer layer;
    private AnnotationFeature feature;
    private Recommender rec;

    @Before
    public void setUp() throws Exception
    {
        sut = new RecommendationServiceImpl(testEntityManager.getEntityManager());
        project = createProject(PROJECT_NAME);
        layer = createAnnotationLayer();
        layer.setProject(project);
        feature = createAnnotationFeature(layer, "strFeat");
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
        rec = buildRecommender(project, feature);
        rec.setEnabled(true);

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
        rec = buildRecommender(project, feature);
        rec.setEnabled(true);

        sut.createOrUpdateRecommender(rec);

        Optional<Recommender> enabledRecommenders = sut.getEnabledRecommender(rec.getId());

        assertThat(enabledRecommenders)
                .as("Check that only the previously created recommender is found")
                .isPresent()
                .contains(rec);
    }

    @Test
    public void getRecommenders_WithDisabledRecommender_ShouldReturnEmptyList()
    {
        rec = buildRecommender(project, feature);
        rec.setEnabled(false);

        sut.createOrUpdateRecommender(rec);

        Optional<Recommender> enabledRecommenders = sut.getEnabledRecommender(rec.getId());

        assertThat(enabledRecommenders).as("Check that no recommender is found").isEmpty();;
    }

    @Test
    public void getRecommenders_WithOtherRecommenderId_ShouldReturnEmptyList()
    {
        rec = buildRecommender(project, feature);
        rec.setEnabled(false);

        sut.createOrUpdateRecommender(rec);

        long otherId = 9999L;
        Optional<Recommender> enabledRecommenders = sut.getEnabledRecommender(otherId );

        assertThat(enabledRecommenders)
                .as("Check that no recommender is found")
                .isEmpty();;
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

    // Helper
    private Project createProject(String aName)
    {
        Project project = new Project();
        project.setName(aName);
        project.setMode(WebAnnoConst.PROJECT_TYPE_ANNOTATION);
        return testEntityManager.persist(project);
    }

    public AnnotationLayer createAnnotationLayer()
    {
        AnnotationLayer layer = new AnnotationLayer();
        layer.setEnabled(true);
        layer.setName("annotation type name");
        layer.setReadonly(false);
        layer.setType("test type");
        layer.setUiName("test ui name");
        layer.setAnchoringMode(false, false);
       
        return testEntityManager.persist(layer);
    }

    public AnnotationFeature createAnnotationFeature(AnnotationLayer aLayer, String aName)
    {
        AnnotationFeature feature = new AnnotationFeature();
        feature.setLayer(aLayer);
        feature.setName(aName);
        feature.setUiName(aName);
        feature.setType(CAS.TYPE_NAME_STRING);
               
        return testEntityManager.persist(feature);
    }
}
