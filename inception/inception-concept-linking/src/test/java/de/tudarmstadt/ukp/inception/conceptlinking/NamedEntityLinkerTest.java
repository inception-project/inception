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
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.ANY_OVERLAP;
import static de.tudarmstadt.ukp.inception.support.test.recommendation.RecommenderTestHelper.addPredictionFeatures;
import static de.tudarmstadt.ukp.inception.support.test.recommendation.RecommenderTestHelper.getPredictions;
import static de.tudarmstadt.ukp.inception.support.uima.AnnotationBuilder.buildAnnotation;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.Strings.CI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.CasFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemArg;
import de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemArgLink;
import de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemPred;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanAdapter;
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
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.feature.LinkWithRoleModel;
import de.tudarmstadt.ukp.inception.support.uima.SegmentationUtils;

@ExtendWith(MockitoExtension.class)
public class NamedEntityLinkerTest
{
    private @Mock ConceptLinkingService clService;
    private @Mock KnowledgeBaseService kbService;
    private @Mock FeatureSupportRegistry fsRegistry;
    private @Mock AnnotationSchemaService schemaService;
    private @Mock SpanAdapter adapter;

    private RecommenderContext context;
    private Recommender recommender;
    private AnnotationLayer layer;
    private AnnotationFeature feature;
    private AnnotationFeature linkFeature;
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
                .forJCasClass(SemPred.class) //
                .build();

        feature = AnnotationFeature.builder() //
                .withLayer(layer) //
                .withName(SemPred._FeatName_category) //
                .build();

        linkFeature = AnnotationFeature.builder() //
                .withLayer(layer) //
                .withName(SemPred._FeatName_arguments) //
                .withLinkMode(LinkMode.WITH_ROLE) //
                .withLinkTypeName(SemArgLink.class.getName()) //
                .withLinkTypeRoleFeatureName(SemArgLink._FeatName_role) //
                .withLinkTypeTargetFeatureName(SemArgLink._FeatName_target) //
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
        lenient().when(clService.disambiguate(any(), any(), any(), any(), any(), anyInt(), any()))
                .thenAnswer(invocation -> {
                    var userQuery = invocation.getArgument(3, String.class);
                    if (userQuery != null && CI.containsAny(userQuery, "obama", "44th")) {
                        return mockResult;
                    }
                    return emptyList(); // or null, depending on your expected behavior
                });
        conceptFeatureTraits = new ConceptFeatureTraits();
        lenient().when(fsRegistry.readTraits(any(), any())).thenReturn(conceptFeatureTraits);
        lenient().when(schemaService.getAdapter(layer)).thenReturn(adapter);
        lenient().when(adapter.listFeatures()).thenReturn(asList(feature, linkFeature));

        traits = new NamedEntityLinkerTraits();
        sut = new NamedEntityLinker(recommender, traits, kbService, clService, fsRegistry,
                conceptFeatureTraits, schemaService);

        cas = CasFactory.createCas();
        casStorageSession.add(AnnotationSet.forTest("cas"), EXCLUSIVE_WRITE_ACCESS, cas);

        addPredictionFeatures(cas, SemPred.class, SemPred._FeatName_category);
    }

    @AfterEach
    public void tearDown()
    {
        CasStorageSession.get().close();
    }

    @Test
    public void thatPredictionWorks() throws Exception
    {
        cas.setDocumentText(
                "It was Barack Obama who became the 44th President of the United States of America.");
        SegmentationUtils.splitSentences(cas);
        SegmentationUtils.tokenize(cas);
        buildAnnotation(cas, SemPred.class).on("Barack Obama").buildAllAndAddToIndexes();

        sut.predict(new PredictionContext(context), cas);

        var predictions = getPredictions(cas, SemPred.class);

        assertThat(predictions) //
                .extracting(SemPred::getCoveredText, SemPred::getCategory) //
                .containsExactlyInAnyOrder( //
                        tuple("Barack Obama", "https://www.wikidata.org/wiki/Q76"), //
                        tuple("Barack Obama", "https://www.wikidata.org/wiki/Q26446735"), //
                        tuple("Barack Obama", "https://www.wikidata.org/wiki/Q18355807"));
    }

    @Test
    public void thatPredictionIsSkippedIfThereIsNoEmptyFeature() throws Exception
    {
        layer.setOverlapMode(ANY_OVERLAP);
        traits.setEmptyCandidateFeatureRequired(true);

        cas.setDocumentText(
                "It was Barack Obama who became the 44th President of the United States of America.");
        SegmentationUtils.splitSentences(cas);
        SegmentationUtils.tokenize(cas);
        buildAnnotation(cas, SemPred.class).on("Barack Obama").buildAllAndAddToIndexes();

        cas.select(SemPred.class).forEach(ne -> ne.setCategory("non-empty"));

        sut.predict(new PredictionContext(context), cas);

        var predictions = getPredictions(cas, SemPred.class);

        assertThat(predictions) //
                .extracting(SemPred::getCoveredText, SemPred::getCategory) //
                .isEmpty();
    }

    @Test
    public void thatAdditionalPredictionsOnStackableLayerAreGenerated() throws Exception
    {
        layer.setOverlapMode(ANY_OVERLAP);
        traits.setEmptyCandidateFeatureRequired(false);

        cas.setDocumentText(
                "It was Barack Obama who became the 44th President of the United States of America.");
        SegmentationUtils.splitSentences(cas);
        SegmentationUtils.tokenize(cas);
        buildAnnotation(cas, SemPred.class).on("Barack Obama").buildAllAndAddToIndexes();

        cas.select(SemPred.class).forEach(ne -> ne.setCategory("non-empty"));

        sut.predict(new PredictionContext(context), cas);

        var predictions = getPredictions(cas, SemPred.class);

        assertThat(predictions) //
                .extracting(SemPred::getCoveredText, SemPred::getCategory) //
                .containsExactlyInAnyOrder( //
                        tuple("Barack Obama", "https://www.wikidata.org/wiki/Q76"), //
                        tuple("Barack Obama", "https://www.wikidata.org/wiki/Q26446735"), //
                        tuple("Barack Obama", "https://www.wikidata.org/wiki/Q18355807"));
    }

    @Test
    public void thatCandidateNotRedundantlySuggestedOnStackedMentions() throws Exception
    {
        layer.setOverlapMode(ANY_OVERLAP);
        traits.setEmptyCandidateFeatureRequired(false);

        cas.setDocumentText(
                "It was Barack Obama who became the 44th President of the United States of America.");
        SegmentationUtils.splitSentences(cas);
        SegmentationUtils.tokenize(cas);
        buildAnnotation(cas, SemPred.class) //
                .on("Barack Obama") //
                .withFeature(SemPred._FeatName_category, "https://www.wikidata.org/wiki/Q76") //
                .buildAndAddToIndexes();

        sut.predict(new PredictionContext(context), cas);

        var predictions = getPredictions(cas, SemPred.class);

        assertThat(predictions) //
                .extracting(SemPred::getCoveredText, SemPred::getCategory) //
                .containsExactlyInAnyOrder( //
                        tuple("Barack Obama", "https://www.wikidata.org/wiki/Q26446735"), //
                        tuple("Barack Obama", "https://www.wikidata.org/wiki/Q18355807"));
    }

    @Test
    public void thatRedundancyCheckAppliesOnlyToStackedAnnotations() throws Exception
    {
        layer.setOverlapMode(ANY_OVERLAP);
        traits.setEmptyCandidateFeatureRequired(false);

        cas.setDocumentText(
                "It was Barack Obama who became the 44th President of the United States of America.");
        SegmentationUtils.splitSentences(cas);
        SegmentationUtils.tokenize(cas);
        buildAnnotation(cas, SemPred.class) //
                .on("Barack Obama") //
                .buildAndAddToIndexes();
        buildAnnotation(cas, SemPred.class) //
                .on("44th President of the United States of America") //
                .withFeature(SemPred._FeatName_category, "https://www.wikidata.org/wiki/Q76") //
                .buildAndAddToIndexes();

        sut.predict(new PredictionContext(context), cas);

        var predictions = getPredictions(cas, SemPred.class);

        // Q76 should be suppressed for the "44th President..." mention because it is already linked
        // there.
        // It should appear on the "Obama" mention though
        assertThat(predictions) //
                .extracting(SemPred::getCoveredText, SemPred::getCategory) //
                .containsExactlyInAnyOrder( //
                        tuple("44th President of the United States of America",
                                "https://www.wikidata.org/wiki/Q26446735"), //
                        tuple("44th President of the United States of America",
                                "https://www.wikidata.org/wiki/Q18355807"), //
                        tuple("Barack Obama", "https://www.wikidata.org/wiki/Q76"), //
                        tuple("Barack Obama", "https://www.wikidata.org/wiki/Q26446735"), //
                        tuple("Barack Obama", "https://www.wikidata.org/wiki/Q18355807"));
    }

    @Test
    public void thatLinkedAnnotationsUsedInLookup() throws Exception
    {
        layer.setOverlapMode(ANY_OVERLAP);
        traits.setEmptyCandidateFeatureRequired(false);
        traits.setIncludeLinkTargetsInQuery(false);

        cas.setDocumentText(
                "It was Barack Obama who became the 44th President of the United States of America.");
        SegmentationUtils.splitSentences(cas);
        SegmentationUtils.tokenize(cas);

        var arg = buildAnnotation(cas, SemArg.class) //
                .on("Obama") //
                .buildAndAddToIndexes();

        var argLink = new SemArgLink(cas.getJCasImpl());
        argLink.setTarget(arg);
        argLink.setRole("lastName");

        buildAnnotation(cas, SemPred.class) //
                .on("Barack") //
                .withFeature(SemPred._FeatName_arguments, asList(argLink)) //
                .buildAndAddToIndexes();

        lenient().when(adapter.getFeatureValue(any(), any())).thenReturn(
                asList(LinkWithRoleModel.builder().withRole("lastName").withTarget(arg).build()));

        // Without considering link targets
        traits.setIncludeLinkTargetsInQuery(false);
        sut.predict(new PredictionContext(context), cas);
        assertThat(getPredictions(cas, SemPred.class)).isEmpty();

        // With considering link targets
        traits.setIncludeLinkTargetsInQuery(true);
        sut.predict(new PredictionContext(context), cas);
        assertThat(getPredictions(cas, SemPred.class)) //
                .extracting(SemPred::getCoveredText, SemPred::getCategory) //
                .containsExactlyInAnyOrder( //
                        tuple("Barack", "https://www.wikidata.org/wiki/Q76"), //
                        tuple("Barack", "https://www.wikidata.org/wiki/Q26446735"), //
                        tuple("Barack", "https://www.wikidata.org/wiki/Q18355807"));
    }
}
