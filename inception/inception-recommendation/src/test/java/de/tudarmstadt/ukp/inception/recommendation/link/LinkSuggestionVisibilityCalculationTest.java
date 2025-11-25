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

import static de.tudarmstadt.ukp.inception.recommendation.service.Fixtures.getInvisibleSuggestions;
import static de.tudarmstadt.ukp.inception.recommendation.service.Fixtures.getVisibleSuggestions;
import static de.tudarmstadt.ukp.inception.support.uima.AnnotationBuilder.buildAnnotation;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.link.recommender.LinkSuggestionSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.LayerBehaviorRegistry;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanLayerSupportImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LinkPosition;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LinkSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionDocumentGroup;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.schema.api.feature.LinkWithRoleModel;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.support.uima.SegmentationUtils;

@ExtendWith(MockitoExtension.class)
public class LinkSuggestionVisibilityCalculationTest
{
    private final static String TEST_USER = "Testuser";
    private final static long RECOMMENDER_ID = 1;
    private final static String RECOMMENDER_NAME = "TestEntityRecommender";
    private final static double CONFIDENCE = 0.2;
    private final static String CONFIDENCE_EXPLANATION = "Predictor A: 0.05 | Predictor B: 0.15";

    private @Mock ConstraintsService constraintsService;
    private @Mock LearningRecordService learningRecordService;
    private @Mock AnnotationSchemaService schemaService;
    private @Mock LayerBehaviorRegistry layerBehaviorRegistry;

    private LinkSuggestionSupport sut;

    private LayerSupportRegistryImpl layerSupportRegistry;
    private FeatureSupportRegistryImpl featureSupportRegistry;

    private LinkFeatureSupport linkFeatureSupport;

    private Project project;
    private SourceDocument document;
    private AnnotationLayer linkHostLayer;
    private AnnotationLayer slotFillerLayer;
    private AnnotationFeature linkFeature;

    private CAS cas;
    private Recommender recommender;
    private TypeAdapter linkHostLayerAdapter;

    @BeforeEach
    public void setUp() throws Exception
    {
        linkFeatureSupport = new LinkFeatureSupport(schemaService);
        featureSupportRegistry = new FeatureSupportRegistryImpl(asList(linkFeatureSupport));
        featureSupportRegistry.init();
        layerSupportRegistry = new LayerSupportRegistryImpl(asList(new SpanLayerSupportImpl(
                featureSupportRegistry, null, layerBehaviorRegistry, constraintsService)));
        layerSupportRegistry.init();

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
                .withId(RECOMMENDER_ID) //
                .withName(RECOMMENDER_NAME) //
                .withLayer(linkFeature.getLayer()) //
                .withFeature(linkFeature) //
                .build();

        cas = createCas(asList(linkHostLayer, slotFillerLayer), asList(linkFeature));
        cas.setDocumentText("This is a test.");

        SegmentationUtils.splitSentences(cas);
        SegmentationUtils.tokenize(cas);

        when(schemaService.listSupportedFeatures(linkHostLayer)).thenReturn(asList(linkFeature));

        sut = new LinkSuggestionSupport(null, learningRecordService, null, schemaService, null);

        linkHostLayerAdapter = layerSupportRegistry.getLayerSupport(linkHostLayer)
                .createAdapter(linkHostLayer, () -> asList(linkFeature));
        when(schemaService.getAdapter(linkHostLayer)).thenReturn(linkHostLayerAdapter);
    }

    @Test
    public void testCalculateVisibilityNoRecordsAllHidden() throws Exception
    {
        doReturn(emptyList()).when(learningRecordService).listLearningRecords(TEST_USER, TEST_USER,
                linkHostLayer);

        var slotFiller = buildAnnotation(cas, slotFillerLayer.getName()) //
                .onMatch("\\btest\\b") //
                .buildAndAddToIndexes();

        var linkHost = buildAnnotation(cas, linkHostLayer.getName()) //
                .onMatch("\\bis\\b") //
                .buildAndAddToIndexes();
        linkHostLayerAdapter.setFeatureValue(document, TEST_USER, linkHost, linkFeature,
                asList(new LinkWithRoleModel("role", slotFiller)));

        var suggestions = makeLinkSuggestionGroup(document, linkFeature,
                new int[][] { { 1, linkHost.getBegin(), linkHost.getEnd(), slotFiller.getBegin(),
                        slotFiller.getEnd() } });

        sut.calculateSuggestionVisibility(TEST_USER, document, cas, TEST_USER, linkHostLayer,
                suggestions, 0, cas.getDocumentText().length());

        assertThat(getVisibleSuggestions(suggestions)) //
                .as("No suggestions are visible as they overlap with annotations") //
                .isEmpty();

        assertThat(getInvisibleSuggestions(suggestions)) //
                .as("Invisible suggestions are hidden because of overlapping") //
                .extracting(AnnotationSuggestion::getReasonForHiding) //
                .extracting(String::trim) //
                .containsExactly("overlapping");
    }

    @Test
    public void thatVisibilityIsRestoredWhenOverlappingAnnotationIsRemoved() throws Exception
    {
        doReturn(emptyList()).when(learningRecordService).listLearningRecords(TEST_USER, TEST_USER,
                linkHostLayer);

        var slotFiller = buildAnnotation(cas, slotFillerLayer.getName()) //
                .onMatch("\\btest\\b") //
                .buildAndAddToIndexes();

        var linkHost = buildAnnotation(cas, linkHostLayer.getName()) //
                .onMatch("\\bis\\b") //
                .buildAndAddToIndexes();
        linkHostLayerAdapter.setFeatureValue(document, TEST_USER, linkHost, linkFeature,
                asList(new LinkWithRoleModel("role", slotFiller)));

        var suggestions = makeLinkSuggestionGroup(document, linkFeature,
                new int[][] { { 1, linkHost.getBegin(), linkHost.getEnd(), slotFiller.getBegin(),
                        slotFiller.getEnd() } });

        sut.calculateSuggestionVisibility(TEST_USER, document, cas, TEST_USER, linkHostLayer,
                suggestions, 0, 25);

        assertThat(getVisibleSuggestions(suggestions)) //
                .as("No suggestions are visible as they overlap with annotations") //
                .isEmpty();
        assertThat(getInvisibleSuggestions(suggestions)) //
                .as("All suggestions are hidden as the overlap with annotations") //
                .isNotEmpty();

        linkHostLayerAdapter.setFeatureValue(document, TEST_USER, linkHost, linkFeature, asList());

        sut.calculateSuggestionVisibility(TEST_USER, document, cas, TEST_USER, linkHostLayer,
                suggestions, 0, 25);

        assertThat(getInvisibleSuggestions(suggestions)) //
                .as("No suggestions are hidden as they no longer overlap with annotations") //
                .containsExactly();
        assertThat(getVisibleSuggestions(suggestions)) //
                .as("All suggestions are visible as they no longer overlap with annotations") //
                .containsExactlyInAnyOrderElementsOf(
                        suggestions.stream().flatMap(g -> g.stream()).toList());
    }

    private CAS createCas(List<AnnotationLayer> aLayers, List<AnnotationFeature> aFeatures)
        throws Exception
    {
        var globalTypes = TypeSystemDescriptionFactory.createTypeSystemDescription();
        var localTypes = new TypeSystemDescription_impl();

        for (var layer : aLayers) {
            layerSupportRegistry.getLayerSupport(layer).generateTypes(localTypes, layer, aFeatures);
        }

        return CasFactory.createCas(mergeTypeSystems(asList(globalTypes, localTypes)));
    }

    SuggestionDocumentGroup<LinkSuggestion> makeLinkSuggestionGroup(SourceDocument doc,
            AnnotationFeature aFeat, int[][] vals)
    {
        var suggestions = new ArrayList<LinkSuggestion>();
        for (int[] val : vals) {
            var suggestion = LinkSuggestion.builder() //
                    .withId(val[0]) //
                    .withRecommender(recommender) //
                    .withDocument(doc) //
                    .withPosition(new LinkPosition(aFeat.getName(), val[1], val[2], val[3], val[4])) //
                    .withScore(CONFIDENCE) //
                    .withScoreExplanation(CONFIDENCE_EXPLANATION) //
                    .build();
            suggestions.add(suggestion);
        }

        return SuggestionDocumentGroup.groupsOfType(LinkSuggestion.class, suggestions);
    }
}
