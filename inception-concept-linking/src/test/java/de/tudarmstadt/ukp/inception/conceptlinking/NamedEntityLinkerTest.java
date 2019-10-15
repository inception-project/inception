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
package de.tudarmstadt.ukp.inception.conceptlinking;

import static de.tudarmstadt.ukp.inception.support.test.recommendation.RecommenderTestHelper.getPredictions;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.assertj.core.api.Assertions.assertThat;
import static org.dkpro.core.api.datasets.DatasetValidationPolicy.CONTINUE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.dkpro.core.api.datasets.Dataset;
import org.dkpro.core.api.datasets.DatasetFactory;
import org.dkpro.core.io.conll.Conll2002Reader;
import org.dkpro.core.testing.DkproTestContext;
import org.junit.Before;
import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.conceptlinking.recommender.NamedEntityLinker;
import de.tudarmstadt.ukp.inception.conceptlinking.recommender.NamedEntityLinkerTraits;
import de.tudarmstadt.ukp.inception.conceptlinking.service.ConceptLinkingServiceImpl;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureValueType;
import de.tudarmstadt.ukp.inception.kb.IriConstants;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.support.test.recommendation.RecommenderTestHelper;

public class NamedEntityLinkerTest
{   
	/**
	 *this is rename method of NamedEntityLinkerTest.java
	 */
    private static File NELT_cache = DkproTestContext.getCacheFolder();
	/**
	 *this is rename method of NamedEntityLinkerTest.java
	 */
    private static DatasetFactory NELT_loader = new DatasetFactory(NELT_cache);
	/**
	 *this is rename method of NamedEntityLinkerTest.java
	 */
    private RecommenderContext NELT_context;
	/**
	 *this is rename method of NamedEntityLinkerTest.java
	 */
    private Recommender NELT_recommender;

    @Before
    public void setUp() {
        NELT_context = new RecommenderContext();
        NELT_recommender = buildRecommender();
    }

    @Test
    public void thatTrainingWorks() throws Exception
    {
		//this is rename varies of NamedEntityLinkerTest.java
        NamedEntityLinker sut = new NamedEntityLinker(NELT_recommender, new NamedEntityLinkerTraits(),
                mock(KnowledgeBaseService.class), mock(ConceptLinkingServiceImpl.class),
                mock(FeatureSupportRegistry.class), new ConceptFeatureTraits());

        List<CAS> NETL_casList = loadDevelopmentData();

        sut.train(NELT_context, NETL_casList);

        assertThat(NELT_context.get(NamedEntityLinker.KEY_MODEL))
            .as("Model has been set")
            .isNotNull();
    }
    @Test
    public void thatPredictionWorks() throws Exception
    {
        List<KBHandle> mockResult = asList(
            new KBHandle("https://www.wikidata.org/wiki/Q76", "Barack Obama",
                "44th President of the United States of America"),
            new KBHandle("https://www.wikidata.org/wiki/Q26446735", "Obama",
                "Japanese Family Name"),
            new KBHandle("https://www.wikidata.org/wiki/Q18355807", "Obama",
                "genus of worms"),
            new KBHandle("https://www.wikidata.org/wiki/Q41773", "Obama",
                "city in Fukui prefecture, Japan"));

        KnowledgeBaseService kbService = mock(KnowledgeBaseService.class);
        KnowledgeBase kb = new KnowledgeBase();
        kb.setFullTextSearchIri(IriConstants.FTS_VIRTUOSO);
        when(kbService.getKnowledgeBaseById(any(), anyString())).thenReturn(Optional.of(kb));
        when(kbService.getEnabledKnowledgeBases(any())).thenReturn(Collections.singletonList(kb));
        when(kbService.read(any(), any())).thenReturn(mockResult);

        ConceptLinkingServiceImpl clService = mock(ConceptLinkingServiceImpl.class);
        when(clService.disambiguate(any(), anyString(), any(ConceptFeatureValueType.class),
                anyString(), anyString(), anyInt(), any())).thenReturn(mockResult);

        FeatureSupportRegistry fsRegistry = mock(FeatureSupportRegistry.class);
        FeatureSupport fs = mock(FeatureSupport.class);
        when(fsRegistry.getFeatureSupport(NELT_recommender.getFeature())).thenReturn(fs);
        when(fs.readTraits(NELT_recommender.getFeature())).thenReturn(new ConceptFeatureTraits());

        NamedEntityLinker sut = new NamedEntityLinker(NELT_recommender, new NamedEntityLinkerTraits(),
                kbService, clService, fsRegistry, new ConceptFeatureTraits());

        List<CAS> NETL_casList = loadDevelopmentData();
        CAS NETL_cas = NETL_casList.get(0);
        
        sut.train(NELT_context, Collections.singletonList(NETL_cas));
        RecommenderTestHelper.addScoreFeature(NETL_cas, NamedEntity.class, "value");

        sut.predict(NELT_context, NETL_cas);

        List<NamedEntity> predictions = getPredictions(NETL_cas, NamedEntity.class);

        assertThat(predictions).as("Predictions have been written to CAS")
            .isNotEmpty();
    }

    private List<CAS> loadDevelopmentData() throws IOException, UIMAException
    {
        Dataset NETL_ds = NELT_loader.load("germeval2014-de", CONTINUE);
        return loadData(NETL_ds, NETL_ds.getDefaultSplit().getDevelopmentFiles());
    }

    private List<CAS> loadData(Dataset NETL_ds, File ... files) throws UIMAException, IOException
    {
        CollectionReader reader = createReader(Conll2002Reader.class,
            Conll2002Reader.PARAM_PATTERNS, files, 
            Conll2002Reader.PARAM_LANGUAGE, NETL_ds.getLanguage(), 
            Conll2002Reader.PARAM_COLUMN_SEPARATOR, Conll2002Reader.ColumnSeparators.TAB.getName(),
            Conll2002Reader.PARAM_HAS_TOKEN_NUMBER, true, 
            Conll2002Reader.PARAM_HAS_HEADER, true, 
            Conll2002Reader.PARAM_HAS_EMBEDDED_NAMED_ENTITY, true);

        List<CAS> NETL_casList = new ArrayList<>();
        while (reader.hasNext()) {
            JCas NETL_cas = JCasFactory.createJCas();
            reader.getNext(NETL_cas.getCas());
            NETL_casList.add(NETL_cas.getCas());
        }
        return NETL_casList;
    }

    private static Recommender buildRecommender()
    {
		//this is rename varies of NamedEntityLinkerTest.java
        AnnotationLayer NETL_layer = new AnnotationLayer();
        NETL_layer.setName(NamedEntity.class.getName());

        AnnotationFeature NETL_feature = new AnnotationFeature();
        NETL_feature.setName("identifier");
        
        Recommender NELT_recommender = new Recommender();
        NELT_recommender.setLayer(NETL_layer);
        NELT_recommender.setFeature(NETL_feature);
        NELT_recommender.setMaxRecommendations(3);

        return NELT_recommender;
    }
}

