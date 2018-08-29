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

import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.model.IModel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureType;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.datasets.Dataset;
import de.tudarmstadt.ukp.dkpro.core.api.datasets.DatasetFactory;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.io.conll.Conll2002Reader;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;
import de.tudarmstadt.ukp.inception.conceptlinking.recommender.NamedEntityLinker;
import de.tudarmstadt.ukp.inception.conceptlinking.service.ConceptLinkingService;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.type.PredictedSpan;
import de.tudarmstadt.ukp.inception.recommendation.api.v2.RecommenderContext;

public class NamedEntityLinkerTest
{
    private static File cache = DkproTestContext.getCacheFolder();
    private static DatasetFactory loader = new DatasetFactory(cache);

    private RecommenderContext context;
    private Recommender recommender;

    private @Mock NamedEntityLinker nwefewel;

    @Before
    public void setUp() {
        context = new RecommenderContext();
        recommender = buildRecommender();
    }

    @Test
    public void thatTrainingWorks() throws Exception
    {
        NamedEntityLinker sut = new NamedEntityLinker(recommender, mock(KnowledgeBaseService.class),
            mock(ConceptLinkingService.class), mock(AnnotationSchemaService.class),
            mock(FeatureSupportRegistry.class));

        List<CAS> casList = loadDevelopmentData();

        sut.train(context, casList);

        assertThat(context.get(NamedEntityLinker.KEY_MODEL))
            .as("Model has been set")
            .isNotNull();
    }
    @Test
    public void thatPredictionWorks() throws Exception
    {
        List<KBHandle> mockResult = new ArrayList<>();
        mockResult.add(new KBHandle("https://www.wikidata.org/wiki/Q76", "Barack Obama",
            "44th President of the United States of America"));
        mockResult.add(new KBHandle("https://www.wikidata.org/wiki/Q26446735", "Obama",
            "Japanese Family Name"));
        mockResult.add(new KBHandle("https://www.wikidata.org/wiki/Q18355807", "Obama",
            "genus of worms"));
        mockResult.add(new KBHandle("https://www.wikidata.org/wiki/Q41773", "Obama",
            "city in Fukui prefecture, Japan"));

        KnowledgeBaseService kbService = mock(KnowledgeBaseService.class);
        KnowledgeBase kb = new KnowledgeBase();
        kb.setSupportConceptLinking(true);
        when(kbService.getKnowledgeBaseById(any(), anyString())).thenReturn(Optional.of(kb));
        when(kbService.getEnabledKnowledgeBases(any())).thenReturn(Collections.singletonList(kb));
        when(kbService.read(any(), any())).thenReturn(mockResult);

        ConceptLinkingService clService = mock(ConceptLinkingService.class);
        when(clService.disambiguate(any(), anyString(), anyString(), anyInt(), any()))
            .thenReturn(mockResult);

        AnnotationSchemaService annoSchemaService = mock(AnnotationSchemaService.class);
        AnnotationFeature mockAnnoFeature = mock(AnnotationFeature.class);
        when(annoSchemaService.getFeature(recommender.getFeature(), recommender.getLayer()))
            .thenReturn(mockAnnoFeature);

        FeatureSupportRegistry fsRegistry = mock(FeatureSupportRegistry.class);
        FeatureSupport fs = mock(MockFeatureSupport.class);
        when(fsRegistry.getFeatureSupport(mockAnnoFeature)).thenReturn(fs);
        when(fs.readTraits(mockAnnoFeature)).thenReturn(new ConceptFeatureTraits());

        NamedEntityLinker sut = new NamedEntityLinker(recommender, kbService, clService,
            annoSchemaService, fsRegistry);

        List<CAS> casList = loadDevelopmentData();
        CAS cas = casList.get(0);
        
        sut.train(context, Collections.singletonList(cas));
        sut.predict(context, cas);

        Collection<PredictedSpan> predictions = JCasUtil.select(cas.getJCas(), PredictedSpan.class);

        assertThat(predictions).as("Predictions have been written to CAS")
            .isNotEmpty();
    }

    private List<CAS> loadAllData() throws IOException, UIMAException
    {
        Dataset ds = loader.load("germeval2014-de");
        return loadData(ds, ds.getDataFiles());
    }

    private List<CAS> loadDevelopmentData() throws IOException, UIMAException
    {
        Dataset ds = loader.load("germeval2014-de");
        return loadData(ds, ds.getDefaultSplit().getDevelopmentFiles());
    }

    private List<CAS> loadData(Dataset ds, File ... files) throws UIMAException, IOException
    {
        CollectionReader reader = createReader(Conll2002Reader.class,
            Conll2002Reader.PARAM_PATTERNS, files, 
            Conll2002Reader.PARAM_LANGUAGE, ds.getLanguage(), 
            Conll2002Reader.PARAM_COLUMN_SEPARATOR, Conll2002Reader.ColumnSeparators.TAB.getName(),
            Conll2002Reader.PARAM_HAS_TOKEN_NUMBER, true, 
            Conll2002Reader.PARAM_HAS_HEADER, true, 
            Conll2002Reader.PARAM_HAS_EMBEDDED_NAMED_ENTITY, true);

        List<CAS> casList = new ArrayList<>();
        while (reader.hasNext()) {
            JCas cas = JCasFactory.createJCas();
            reader.getNext(cas.getCas());
            casList.add(cas.getCas());
        }
        return casList;
    }

    private static Recommender buildRecommender()
    {
        AnnotationLayer layer = new AnnotationLayer();
        layer.setName(NamedEntity.class.getName());

        Recommender recommender = new Recommender();
        recommender.setLayer(layer);
        recommender.setFeature("identifier");

        return recommender;
    }

    private class MockFeatureSupport
        implements FeatureSupport<ConceptFeatureTraits>
    {
        @Override
        public String getId()
        {
            return null;
        }

        @Override
        public boolean accepts(AnnotationFeature annotationFeature)
        {
            return false;
        }

        @Override
        public List<FeatureType> getSupportedFeatureTypes(AnnotationLayer annotationLayer)
        {
            return null;
        }

        @Override
        public void generateFeature(TypeSystemDescription typeSystemDescription,
            TypeDescription typeDescription, AnnotationFeature annotationFeature)
        {

        }

        @Override
        public FeatureEditor createEditor(String s, MarkupContainer markupContainer,
            AnnotationActionHandler annotationActionHandler, IModel<AnnotatorState> iModel,
            IModel<FeatureState> iModel1)
        {
            return null;
        }

        @Override
        public <V> V unwrapFeatureValue(AnnotationFeature annotationFeature, CAS cas,
            Object o)
        {
            return null;
        }

        @Override
        public Object wrapFeatureValue(AnnotationFeature annotationFeature, CAS cas,
            Object o)
        {
            return null;
        }

        @Override
        public void setBeanName(String s)
        {

        }
    }
}

