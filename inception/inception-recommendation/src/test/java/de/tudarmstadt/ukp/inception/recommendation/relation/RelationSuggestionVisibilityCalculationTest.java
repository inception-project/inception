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
package de.tudarmstadt.ukp.inception.recommendation.relation;

import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.REJECTED;
import static de.tudarmstadt.ukp.inception.recommendation.service.Fixtures.getInvisibleSuggestions;
import static de.tudarmstadt.ukp.inception.recommendation.service.Fixtures.getVisibleSuggestions;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.JCasFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.LayerBehaviorRegistry;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationLayerSupportImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.recommender.RelationSuggestionSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanLayerSupportImpl;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RelationPosition;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RelationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionDocumentGroup;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistryImpl;

@ExtendWith(MockitoExtension.class)
public class RelationSuggestionVisibilityCalculationTest
{
    private static final String TEST_USER = "Testuser";

    private final static long RECOMMENDER_ID = 1;
    private final static String RECOMMENDER_NAME = "TestEntityRecommender";
    private final static double CONFIDENCE = 0.2;
    private final static String CONFIDENCE_EXPLANATION = "Predictor A: 0.05 | Predictor B: 0.15";

    private @Mock ConstraintsService constraintsService;
    private @Mock AnnotationSchemaService schemaService;
    private @Mock LearningRecordService learningRecordService;
    private @Mock LayerBehaviorRegistry layerBehaviorRegistry;
    private @Mock FeatureSupportRegistry featureSupportRegistry;

    private Project project;
    private SourceDocument doc;
    private AnnotationLayer layer;
    private AnnotationFeature feature;

    private RelationSuggestionSupport sut;

    private LayerSupportRegistryImpl layerSupportRegistry;

    private TypeAdapter relationAdapter;

    @BeforeEach
    public void setUp() throws Exception
    {
        layerSupportRegistry = new LayerSupportRegistryImpl(asList(
                new SpanLayerSupportImpl(featureSupportRegistry, null, layerBehaviorRegistry,
                        constraintsService),
                new RelationLayerSupportImpl(featureSupportRegistry, null, layerBehaviorRegistry,
                        constraintsService)));
        layerSupportRegistry.init();

        layer = AnnotationLayer.builder() //
                .withId(42l) //
                .forJCasClass(Dependency.class) //
                .withType(RelationLayerSupport.TYPE) //
                .build();

        feature = AnnotationFeature.builder() //
                .withId(2l) //
                .withLayer(layer) //
                .withName(Dependency._FeatName_DependencyType) //
                .withType(TYPE_NAME_STRING) //
                .build();

        project = Project.builder() //
                .withName("Test Project") //
                .build();

        doc = SourceDocument.builder() //
                .withId(12l) //
                .withName("doc") //
                .withProject(project) //
                .build();

        when(schemaService.listSupportedFeatures(layer)).thenReturn(asList(feature));

        sut = new RelationSuggestionSupport(null, learningRecordService, null, schemaService, null);

        relationAdapter = layerSupportRegistry.getLayerSupport(layer).createAdapter(layer,
                () -> asList());
        when(schemaService.getAdapter(layer)).thenReturn(relationAdapter);
    }

    @Test
    public void testCalculateVisibilityNoRecordsAllHidden() throws Exception
    {
        doReturn(emptyList()).when(learningRecordService).listLearningRecords(TEST_USER, TEST_USER,
                layer);

        var cas = getTestCas();
        var suggestions = makeRelationSuggestionGroup(doc, feature,
                new int[][] { { 1, 0, 3, 13, 20 } });
        sut.calculateSuggestionVisibility(TEST_USER, doc, cas, TEST_USER, layer, suggestions, 0,
                25);

        assertThat(getVisibleSuggestions(suggestions)) //
                .as("No suggestions are visible as they overlap with annotations") //
                .isEmpty();
        // FIXME find out why suggestions are repeated/doubled
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
                layer);

        var cas = getTestCas();
        var suggestions = makeRelationSuggestionGroup(doc, feature,
                new int[][] { { 1, 0, 3, 13, 20 } });
        sut.calculateSuggestionVisibility(TEST_USER, doc, cas, TEST_USER, layer, suggestions, 0,
                25);

        assertThat(getVisibleSuggestions(suggestions)) //
                .as("No suggestions are visible as they overlap with annotations") //
                .isEmpty();
        assertThat(getInvisibleSuggestions(suggestions)) //
                .as("All suggestions are hidden as the overlap with annotations") //
                .isNotEmpty();

        cas.select(Dependency.class).forEach(Dependency::removeFromIndexes);

        sut.calculateSuggestionVisibility(TEST_USER, doc, cas, TEST_USER, layer, suggestions, 0,
                25);

        assertThat(getInvisibleSuggestions(suggestions)) //
                .as("No suggestions are hidden as they no longer overlap with annotations") //
                .containsExactly();
        assertThat(getVisibleSuggestions(suggestions)) //
                .as("All suggestions are visible as they no longer overlap with annotations") //
                .containsExactlyInAnyOrderElementsOf(
                        suggestions.stream().flatMap(g -> g.stream()).collect(toList()));
    }

    private CAS getTestCas() throws Exception
    {
        var jcas = JCasFactory.createText("Dies ist ein Testtext, ach ist der schoen, "
                + "der schoenste von allen Testtexten.", "de");

        var governor = new Token(jcas, 0, 3);
        governor.addToIndexes();

        // the annotation's feature value is initialized as null
        var dependent = new Token(jcas, 13, 20);
        dependent.addToIndexes();

        var dep = new Dependency(jcas, dependent.getBegin(), dependent.getEnd());
        dep.setDependent(dependent);
        dep.setGovernor(governor);
        dep.setDependencyType("DEP");
        dep.addToIndexes();

        return jcas.getCas();
    }

    static SuggestionDocumentGroup<RelationSuggestion> makeRelationSuggestionGroup(
            SourceDocument doc, AnnotationFeature aFeat, int[][] vals)
    {
        return makeRelationSuggestionGroup(doc, aFeat, vals, null);
    }

    static SuggestionDocumentGroup<RelationSuggestion> makeRelationSuggestionGroup(
            SourceDocument doc, AnnotationFeature aFeat, int[][] vals, String[] labels)
    {
        var rec = Recommender.builder().withId(RECOMMENDER_ID).withName(RECOMMENDER_NAME)
                .withLayer(aFeat.getLayer()).withFeature(aFeat).build();

        var suggestions = new ArrayList<RelationSuggestion>();
        for (int i = 0; i < vals.length; i++) {
            var val = vals[i];
            var builder = RelationSuggestion.builder() //
                    .withId(val[0]) //
                    .withRecommender(rec) //
                    .withDocument(doc) //
                    .withPosition(new RelationPosition(val[1], val[2], val[3], val[4]))
                    .withScore(CONFIDENCE).withScoreExplanation(CONFIDENCE_EXPLANATION);
            if (labels != null && labels[i] != null) {
                builder.withLabel(labels[i]);
            }
            suggestions.add(builder.build());
        }

        return SuggestionDocumentGroup.groupsOfType(RelationSuggestion.class, suggestions);
    }

    @Test
    public void thatSuggestionAtFreePositionIsVisible() throws Exception
    {
        doReturn(emptyList()).when(learningRecordService).listLearningRecords(TEST_USER, TEST_USER,
                layer);

        // Existing relation in CAS spans (0,3) -> (13,20). Suggestion endpoints differ.
        var cas = getTestCas();
        var suggestions = makeRelationSuggestionGroup(doc, feature,
                new int[][] { { 1, 26, 30, 36, 41 } }, new String[] { "DEP" });

        sut.calculateSuggestionVisibility(TEST_USER, doc, cas, TEST_USER, layer, suggestions, 0,
                100);

        assertThat(getVisibleSuggestions(suggestions)) //
                .as("Suggestion at a free position should be visible") //
                .hasSize(1);
        assertThat(getInvisibleSuggestions(suggestions)) //
                .as("No suggestions hidden when no overlap exists") //
                .isEmpty();
    }

    @Test
    public void thatSuggestionWithSameLabelAsExistingRelationIsHidden() throws Exception
    {
        doReturn(emptyList()).when(learningRecordService).listLearningRecords(TEST_USER, TEST_USER,
                layer);

        // Existing relation has label "DEP". Suggestion duplicates it.
        var cas = getTestCas();
        var suggestions = makeRelationSuggestionGroup(doc, feature,
                new int[][] { { 1, 0, 3, 13, 20 } }, new String[] { "DEP" });

        sut.calculateSuggestionVisibility(TEST_USER, doc, cas, TEST_USER, layer, suggestions, 0,
                25);

        assertThat(getVisibleSuggestions(suggestions)) //
                .as("Duplicate suggestion should be hidden") //
                .isEmpty();
        assertThat(getInvisibleSuggestions(suggestions)) //
                .extracting(AnnotationSuggestion::getReasonForHiding) //
                .extracting(String::trim) //
                .containsExactly("overlapping");
    }

    @Test
    public void thatSuggestionWithDifferentLabelHiddenWhenStackingDisabled() throws Exception
    {
        layer.setOverlapMode(OverlapMode.NO_OVERLAP);
        doReturn(emptyList()).when(learningRecordService).listLearningRecords(TEST_USER, TEST_USER,
                layer);

        // Existing relation has label "DEP". Suggestion has different label, but stacking off.
        var cas = getTestCas();
        var suggestions = makeRelationSuggestionGroup(doc, feature,
                new int[][] { { 1, 0, 3, 13, 20 } }, new String[] { "OTHER" });

        sut.calculateSuggestionVisibility(TEST_USER, doc, cas, TEST_USER, layer, suggestions, 0,
                25);

        assertThat(getVisibleSuggestions(suggestions)) //
                .as("Different-label suggestion must be hidden when stacking is disabled") //
                .isEmpty();
        assertThat(getInvisibleSuggestions(suggestions)) //
                .as("Hidden because no stacking is allowed") //
                .isNotEmpty();
    }

    @Test
    public void thatSuggestionWithDifferentLabelVisibleWhenStackingEnabled() throws Exception
    {
        layer.setOverlapMode(OverlapMode.ANY_OVERLAP);
        doReturn(emptyList()).when(learningRecordService).listLearningRecords(TEST_USER, TEST_USER,
                layer);

        // Existing relation has label "DEP". Suggestion has different label, stacking on.
        var cas = getTestCas();
        var suggestions = makeRelationSuggestionGroup(doc, feature,
                new int[][] { { 1, 0, 3, 13, 20 } }, new String[] { "OTHER" });

        sut.calculateSuggestionVisibility(TEST_USER, doc, cas, TEST_USER, layer, suggestions, 0,
                25);

        assertThat(getVisibleSuggestions(suggestions)) //
                .as("Different-label suggestion must remain visible when stacking is enabled") //
                .hasSize(1);
        assertThat(getInvisibleSuggestions(suggestions)) //
                .as("Nothing should be hidden when stacking permits a new differently-labeled "
                        + "relation at the same endpoints") //
                .isEmpty();
    }

    @Test
    public void thatSuggestionWithSameLabelHiddenEvenWhenStackingEnabled() throws Exception
    {
        layer.setOverlapMode(OverlapMode.ANY_OVERLAP);
        doReturn(emptyList()).when(learningRecordService).listLearningRecords(TEST_USER, TEST_USER,
                layer);

        var cas = getTestCas();
        var suggestions = makeRelationSuggestionGroup(doc, feature,
                new int[][] { { 1, 0, 3, 13, 20 } }, new String[] { "DEP" });

        sut.calculateSuggestionVisibility(TEST_USER, doc, cas, TEST_USER, layer, suggestions, 0,
                25);

        assertThat(getVisibleSuggestions(suggestions)) //
                .as("Duplicate-label suggestion must be hidden even with stacking enabled") //
                .isEmpty();
    }

    @Test
    public void thatRejectedSuggestionWithMatchingLabelIsHidden() throws Exception
    {
        var rejected = LearningRecord.builder() //
                .withSourceDocument(doc) //
                .withLayer(layer) //
                .withAnnotationFeature(feature) //
                .withOffsetBegin(0).withOffsetEnd(3) // source
                .withOffsetBegin2(13).withOffsetEnd2(20) // target
                .withAnnotation("DEP") //
                .withUserAction(REJECTED) //
                .build();
        doReturn(asList(rejected)).when(learningRecordService).listLearningRecords(TEST_USER,
                TEST_USER, layer);

        // Use endpoints with no existing relation in the CAS so the only reason to hide is the
        // learning record.
        var cas = getEmptyCasWithTokens();
        var suggestions = makeRelationSuggestionGroup(doc, feature,
                new int[][] { { 1, 0, 3, 13, 20 } }, new String[] { "DEP" });

        sut.calculateSuggestionVisibility(TEST_USER, doc, cas, TEST_USER, layer, suggestions, 0,
                25);

        assertThat(getVisibleSuggestions(suggestions)) //
                .as("Suggestion matching a rejected learning record must be hidden") //
                .isEmpty();
        assertThat(getInvisibleSuggestions(suggestions)).isNotEmpty();
    }

    @Test
    public void thatRejectedSuggestionWithDifferentLabelStaysVisible() throws Exception
    {
        var rejected = LearningRecord.builder() //
                .withSourceDocument(doc) //
                .withLayer(layer) //
                .withAnnotationFeature(feature) //
                .withOffsetBegin(0).withOffsetEnd(3) //
                .withOffsetBegin2(13).withOffsetEnd2(20) //
                .withAnnotation("OTHER") //
                .withUserAction(REJECTED) //
                .build();
        doReturn(asList(rejected)).when(learningRecordService).listLearningRecords(TEST_USER,
                TEST_USER, layer);

        var cas = getEmptyCasWithTokens();
        var suggestions = makeRelationSuggestionGroup(doc, feature,
                new int[][] { { 1, 0, 3, 13, 20 } }, new String[] { "DEP" });

        sut.calculateSuggestionVisibility(TEST_USER, doc, cas, TEST_USER, layer, suggestions, 0,
                25);

        assertThat(getVisibleSuggestions(suggestions)) //
                .as("Suggestion with different label than rejected record must remain visible") //
                .hasSize(1);
    }

    private CAS getEmptyCasWithTokens() throws Exception
    {
        var jcas = JCasFactory.createText("Dies ist ein Testtext, ach ist der schoen, "
                + "der schoenste von allen Testtexten.", "de");
        new Token(jcas, 0, 3).addToIndexes();
        new Token(jcas, 13, 20).addToIndexes();
        new Token(jcas, 26, 30).addToIndexes();
        new Token(jcas, 36, 41).addToIndexes();
        return jcas.getCas();
    }
}
