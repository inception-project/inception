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
package de.tudarmstadt.ukp.inception.conceptlinking;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.EXCLUSIVE_WRITE_ACCESS;
import static de.tudarmstadt.ukp.inception.support.test.recommendation.RecommenderTestHelper.getPredictions;
import static de.tudarmstadt.ukp.inception.support.uima.AnnotationBuilder.buildAnnotation;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.CasFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasSet;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.conceptlinking.recommender.NamedEntityLinker;
import de.tudarmstadt.ukp.inception.conceptlinking.recommender.NamedEntityLinkerTraits;
import de.tudarmstadt.ukp.inception.conceptlinking.service.ConceptLinkingService;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.IriConstants;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.PredictionContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.support.test.recommendation.RecommenderTestHelper;
import de.tudarmstadt.ukp.inception.support.uima.SegmentationUtils;

@ExtendWith(MockitoExtension.class)
public class NamedEntityLinkerTest
{
    private @Mock ConceptLinkingService clService;
    private @Mock KnowledgeBaseService kbService;
    private @Mock FeatureSupportRegistry fsRegistry;

    private RecommenderContext context;
    private Recommender recommender;
    private AnnotationLayer layer;
    private AnnotationFeature feature;
    private CasStorageSession casStorageSession;
    private CAS cas;
    private ConceptFeatureTraits conceptFeatureTraits;
    private NamedEntityLinkerTraits traits;
    private NamedEntityLinker sut;

    @BeforeEach
    public void setUp() throws Exception
    {
        casStorageSession = CasStorageSession.open();
        context = new RecommenderContext();

        layer = AnnotationLayer.builder() //
                .forJCasClass(NamedEntity.class) //
                .build();

        feature = AnnotationFeature.builder() //
                .withLayer(layer) //
                .withName(NamedEntity._FeatName_identifier) //
                .build();

        recommender = Recommender.builder() //
                .withLayer(layer) //
                .withFeature(feature) //
                .withMaxRecommendations(3) //
                .build();

        var mockResult = asList(
                KBHandle.builder().withIdentifier("https://www.wikidata.org/wiki/Q76") //
                        .withName("Barack Obama") //
                        .withDescription("44th President of the United States of America") //
                        .build(),
                KBHandle.builder().withIdentifier("https://www.wikidata.org/wiki/Q26446735") //
                        .withName("Obama") //
                        .withDescription("Japanese Family Name") //
                        .build(),
                KBHandle.builder().withIdentifier("https://www.wikidata.org/wiki/Q18355807") //
                        .withName("Obama") //
                        .withDescription("genus of worms") //
                        .build(),
                KBHandle.builder().withIdentifier("https://www.wikidata.org/wiki/Q41773") //
                        .withName("Obama") //
                        .withDescription("city in Fukui prefecture, Japan") //
                        .build());

        var kb = new KnowledgeBase();
        kb.setFullTextSearchIri(IriConstants.FTS_RDF4J_LUCENE.stringValue());
        lenient().when(kbService.getEnabledKnowledgeBases(any())).thenReturn(asList(kb));
        lenient().when(kbService.read(any(), any())).thenReturn(mockResult);

        conceptFeatureTraits = new ConceptFeatureTraits();
        lenient().when(fsRegistry.readTraits(any(), any())).thenReturn(conceptFeatureTraits);

        traits = new NamedEntityLinkerTraits();
        sut = new NamedEntityLinker(recommender, traits, kbService, clService, fsRegistry,
                conceptFeatureTraits);

        cas = CasFactory.createCas();
        casStorageSession.add(CasSet.forTest("cas"), EXCLUSIVE_WRITE_ACCESS, cas);
        cas.setDocumentText(
                "It was Barack Obama who became the 44th President of the United States of America.");
        buildAnnotation(cas, NamedEntity.class).on("Barack Obama").buildAllAndAddToIndexes();
        SegmentationUtils.splitSentences(cas);
        SegmentationUtils.tokenize(cas);
        RecommenderTestHelper.addPredictionFeatures(cas, NamedEntity.class,
                NamedEntity._FeatName_value);
        RecommenderTestHelper.addPredictionFeatures(cas, NamedEntity.class,
                NamedEntity._FeatName_identifier);
    }

    @AfterEach
    public void tearDown()
    {
        CasStorageSession.get().close();
    }

    @Test
    public void thatPredictionWorks() throws Exception
    {
        sut.predict(new PredictionContext(context), cas);

        var predictions = getPredictions(cas, NamedEntity.class);

        assertThat(predictions) //
                .extracting(NamedEntity::getCoveredText, NamedEntity::getIdentifier) //
                .containsExactlyInAnyOrder( //
                        tuple("Barack Obama", "https://www.wikidata.org/wiki/Q76"), //
                        tuple("Barack Obama", "https://www.wikidata.org/wiki/Q26446735"), //
                        tuple("Barack Obama", "https://www.wikidata.org/wiki/Q18355807"));
    }

    @Test
    public void thatPredictionIsSkippedIfThereIsNoEmptyFeature() throws Exception
    {
        layer.setOverlapMode(OverlapMode.ANY_OVERLAP);
        traits.setEmptyCandidateFeatureRequired(true);

        cas.select(NamedEntity.class).forEach(ne -> ne.setIdentifier("non-empty"));

        sut.predict(new PredictionContext(context), cas);

        var predictions = getPredictions(cas, NamedEntity.class);

        assertThat(predictions) //
                .extracting(NamedEntity::getCoveredText, NamedEntity::getIdentifier) //
                .isEmpty();
    }

    @Test
    public void thatAdditionalPredictionsOnStackableLayerAreGenerated() throws Exception
    {
        layer.setOverlapMode(OverlapMode.ANY_OVERLAP);
        traits.setEmptyCandidateFeatureRequired(false);

        cas.select(NamedEntity.class).forEach(ne -> ne.setIdentifier("non-empty"));

        sut.predict(new PredictionContext(context), cas);

        var predictions = getPredictions(cas, NamedEntity.class);

        assertThat(predictions) //
                .extracting(NamedEntity::getCoveredText, NamedEntity::getIdentifier) //
                .containsExactlyInAnyOrder( //
                        tuple("Barack Obama", "https://www.wikidata.org/wiki/Q76"), //
                        tuple("Barack Obama", "https://www.wikidata.org/wiki/Q26446735"), //
                        tuple("Barack Obama", "https://www.wikidata.org/wiki/Q18355807"));
    }
}
