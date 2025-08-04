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
package de.tudarmstadt.ukp.inception.recommendation.link;

import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_IS_PREDICTION;
import static de.tudarmstadt.ukp.inception.support.uima.AnnotationBuilder.buildAnnotation;
import static java.util.Arrays.asList;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.link.recommender.LinkSuggestionSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.LayerBehaviorRegistry;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanLayerSupportImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommenderTypeSystemUtils;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LinkSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.ExtractionContext;
import de.tudarmstadt.ukp.inception.recommendation.config.RecommenderProperties;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.schema.api.feature.LinkWithRoleModel;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.support.uima.SegmentationUtils;

@ExtendWith(MockitoExtension.class)
class LinkSuggestionExtractionTest
{
    private @Mock ConstraintsService constraintsService;
    private @Mock RecommendationService recommendationService;
    private @Mock LearningRecordService learningRecordService;
    private @Mock ApplicationEventPublisher applicationEventPublisher;
    private @Mock AnnotationSchemaService schemaService;
    private @Mock RecommenderProperties recommenderProperties;
    private @Mock LayerBehaviorRegistry layerBehaviorRegistry;

    private TokenBuilder<Token, Sentence> tokenBuilder;
    private Project project;
    private SourceDocument document;
    private CAS originalCas;

    private LayerSupportRegistryImpl layerSupportRegistry;
    private FeatureSupportRegistryImpl featureSupportRegistry;

    private LinkSuggestionSupport sut;
    private LinkFeatureSupport linkFeatureSupport;

    private AnnotationLayer linkHostLayer;
    private AnnotationLayer slotFillerLayer;
    private AnnotationFeature linkFeature;

    private Recommender recommender;

    @BeforeEach
    void setup() throws Exception
    {
        linkFeatureSupport = new LinkFeatureSupport(schemaService);
        featureSupportRegistry = new FeatureSupportRegistryImpl(asList(linkFeatureSupport));
        featureSupportRegistry.init();
        layerSupportRegistry = new LayerSupportRegistryImpl(asList(new SpanLayerSupportImpl(
                featureSupportRegistry, null, layerBehaviorRegistry, constraintsService)));
        layerSupportRegistry.init();

        tokenBuilder = new TokenBuilder<>(Token.class, Sentence.class);

        project = Project.builder() //
                .withId(1l) //
                .withName("Test") //
                .build();
        document = SourceDocument.builder() //
                .withId(1l) //
                .withProject(project) //
                .withName("Doc") //
                .build();
        linkHostLayer = AnnotationLayer.builder() //
                .withId(1l) //
                .withName("custom.Span") //
                .withType(SpanLayerSupport.TYPE) //
                .build();
        slotFillerLayer = AnnotationLayer.builder() //
                .withId(1l) //
                .withName("custom.Arg") //
                .withType(SpanLayerSupport.TYPE) //
                .build();
        linkFeature = AnnotationFeature.builder() //
                .withLayer(linkHostLayer) //
                .withType(slotFillerLayer.getName()) //
                .withName("args") //
                .build();
        linkFeatureSupport.configureFeature(linkFeature);

        recommender = Recommender.builder() //
                .withId(1l) //
                .withName("recommender") //
                .withProject(project) //
                .withLayer(linkHostLayer) //
                .withFeature(linkFeature) //
                .build();

        originalCas = createCas(asList(linkHostLayer, slotFillerLayer), asList(linkFeature));
        originalCas.setDocumentText("This is a test.");

        SegmentationUtils.splitSentences(originalCas);
        SegmentationUtils.tokenize(originalCas);

        sut = new LinkSuggestionSupport(recommendationService, learningRecordService,
                applicationEventPublisher, schemaService, featureSupportRegistry);

        var linkHostLayerAdapter = layerSupportRegistry.getLayerSupport(linkHostLayer)
                .createAdapter(linkHostLayer, () -> asList(linkFeature));
        var slotFillerLayerAdapter = layerSupportRegistry.getLayerSupport(linkHostLayer)
                .createAdapter(slotFillerLayer, () -> asList());
        when(schemaService.getAdapter(linkHostLayer)).thenReturn(linkHostLayerAdapter);
        when(schemaService.findAdapter(any(), any())).thenReturn(slotFillerLayerAdapter);
    }

    @Test
    void testLinkExtraction() throws Exception
    {
        var slotFiller = buildAnnotation(originalCas, slotFillerLayer.getName()) //
                .onMatch("\\btest\\b") //
                .buildAndAddToIndexes();

        var linkHost = buildAnnotation(originalCas, linkHostLayer.getName()) //
                .onMatch("\\bis\\b") //
                .buildAndAddToIndexes();

        var predictionCas = RecommenderTypeSystemUtils.makePredictionCas(originalCas, linkFeature);

        var preSlotFiller = predictionCas.<Annotation> select(slotFillerLayer.getName()).get();
        var prediction = buildAnnotation(predictionCas, linkHostLayer.getName()) //
                .onMatch("\\bis\\b") //
                .withFeature(FEATURE_NAME_IS_PREDICTION, true) //
                .buildAndAddToIndexes();
        linkFeatureSupport.setFeatureValue(predictionCas, linkFeature, prediction.getAddress(),
                asList(new LinkWithRoleModel("role", "label", preSlotFiller.getAddress())));

        var ctx = new ExtractionContext(0, recommender, document, originalCas, predictionCas);
        var suggestions = sut.extractSuggestions(ctx);

        assertThat(suggestions) //
                .filteredOn(a -> a instanceof LinkSuggestion) //
                .map(a -> (LinkSuggestion) a) //
                .extracting( //
                        LinkSuggestion::getRecommenderName, //
                        LinkSuggestion::getLabel) //
                .containsExactly( //
                        tuple(recommender.getName(), "role"));
    }

    private CAS createCas(List<AnnotationLayer> aLayers, List<AnnotationFeature> aFeatures)
        throws ResourceInitializationException
    {
        var globalTypes = TypeSystemDescriptionFactory.createTypeSystemDescription();
        var localTypes = new TypeSystemDescription_impl();

        for (var layer : aLayers) {
            layerSupportRegistry.getLayerSupport(layer).generateTypes(localTypes, layer, aFeatures);
        }

        return CasFactory.createCas(mergeTypeSystems(asList(globalTypes, localTypes)));
    }
}
