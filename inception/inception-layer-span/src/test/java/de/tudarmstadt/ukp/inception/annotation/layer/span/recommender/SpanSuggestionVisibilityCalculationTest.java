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
package de.tudarmstadt.ukp.inception.annotation.layer.span.recommender;

import static de.tudarmstadt.ukp.inception.annotation.layer.span.recommender.Fixtures.getInvisibleSuggestions;
import static de.tudarmstadt.ukp.inception.annotation.layer.span.recommender.Fixtures.getVisibleSuggestions;
import static de.tudarmstadt.ukp.inception.annotation.layer.span.recommender.Fixtures.makeSpanSuggestionGroup;
import static de.tudarmstadt.ukp.inception.annotation.layer.span.recommender.SpanSuggestionSupport.hideSuggestionsRejectedOrSkipped;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.REJECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.SKIPPED;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;

import java.util.ArrayList;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.JCasFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.annotation.feature.multistring.MultiValueStringFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureSupport;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionDocumentGroup;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistryImpl;

@ExtendWith(MockitoExtension.class)
public class SpanSuggestionVisibilityCalculationTest
{
    private static final String TEST_USER = "Testuser";

    private @Mock AnnotationSchemaService annoService;
    private @Mock LearningRecordService learningRecordService;

    private Project project;
    private SourceDocument doc;
    private SourceDocument doc2;
    private AnnotationLayer layer;
    private AnnotationLayer layer2;
    private AnnotationFeature feature;
    private AnnotationFeature feature2;

    private SpanSuggestionSupport sut;

    @BeforeEach
    public void setUp() throws Exception
    {
        layer = AnnotationLayer.builder().withId(42l).forJCasClass(NamedEntity.class).build();
        layer2 = AnnotationLayer.builder().withId(43l).withName("custom.Layer2").build();

        feature = AnnotationFeature.builder().withId(2l).withLayer(layer)
                .withName(NamedEntity._FeatName_value).withType(TYPE_NAME_STRING).build();
        feature2 = AnnotationFeature.builder().withId(3l).withLayer(layer2).withName("value")
                .withType(TYPE_NAME_STRING).build();

        project = Project.builder().withName("Test Project").build();

        doc = SourceDocument.builder().withId(12l).withName("doc").withProject(project).build();
        doc2 = SourceDocument.builder().withId(13l).withName("doc2").withProject(project).build();

        lenient().when(annoService.listSupportedFeatures(layer)).thenReturn(asList(feature));
        lenient().when(annoService.listSupportedFeatures(layer2)).thenReturn(asList(feature2));

        var featureSupportRegistry = new FeatureSupportRegistryImpl(
                asList(new StringFeatureSupport(), new MultiValueStringFeatureSupport()));
        featureSupportRegistry.init();

        sut = new SpanSuggestionSupport(null, learningRecordService, null, annoService,
                featureSupportRegistry, null);
    }

    @Test
    public void testCalculateVisibilityNoRecordsAllHidden() throws Exception
    {
        doReturn(new ArrayList<>()).when(learningRecordService).listLearningRecords(TEST_USER,
                TEST_USER, layer);

        var cas = getTestCas();
        var suggestions = makeSpanSuggestionGroup(doc, feature,
                new int[][] { { 1, 0, 3 }, { 2, 13, 20 } });
        sut.calculateSuggestionVisibility(TEST_USER, doc, cas, TEST_USER, layer, suggestions, 0,
                25);

        var invisibleSuggestions = getInvisibleSuggestions(suggestions);
        var visibleSuggestions = getVisibleSuggestions(suggestions);

        // check the invisible suggestions' states
        assertThat(invisibleSuggestions).isNotEmpty();
        // FIXME find out why suggestions are repeated/doubled
        assertThat(invisibleSuggestions) //
                .as("Invisible suggestions are hidden because of overlapping") //
                .extracting(AnnotationSuggestion::getReasonForHiding) //
                .extracting(String::trim) //
                .containsExactly("overlapping", "overlapping");

        // check no visible suggestions
        assertThat(visibleSuggestions).isEmpty();
    }

    @Test
    public void testCalculateVisibilityNoRecordsNotHidden() throws Exception
    {
        doReturn(new ArrayList<>()).when(learningRecordService).listLearningRecords(TEST_USER,
                TEST_USER, layer);

        var cas = getTestCas();
        var suggestions = makeSpanSuggestionGroup(doc, feature, new int[][] { { 1, 5, 10 } });
        sut.calculateSuggestionVisibility(TEST_USER, doc, cas, TEST_USER, layer, suggestions, 0,
                25);

        var invisibleSuggestions = getInvisibleSuggestions(suggestions);
        var visibleSuggestions = getVisibleSuggestions(suggestions);

        // check the invisible suggestions' states
        assertThat(visibleSuggestions).isNotEmpty();
        assertThat(invisibleSuggestions).isEmpty();
    }

    @Test
    public void testCalculateVisibilityRejected() throws Exception
    {
        var records = new ArrayList<LearningRecord>();
        var rejectedRecord = new LearningRecord();
        rejectedRecord.setSourceDocument(doc);
        rejectedRecord.setUserAction(LearningRecordUserAction.REJECTED);
        rejectedRecord.setLayer(layer);
        rejectedRecord.setAnnotationFeature(feature);
        rejectedRecord.setOffsetBegin(5);
        rejectedRecord.setOffsetEnd(10);
        records.add(rejectedRecord);
        doReturn(records).when(learningRecordService).listLearningRecords(TEST_USER, TEST_USER,
                layer);

        var cas = getTestCas();
        var suggestions = makeSpanSuggestionGroup(doc, feature, new int[][] { { 1, 5, 10 } });
        sut.calculateSuggestionVisibility(TEST_USER, doc, cas, TEST_USER, layer, suggestions, 0,
                25);

        var invisibleSuggestions = getInvisibleSuggestions(suggestions);
        var visibleSuggestions = getVisibleSuggestions(suggestions);

        // check the invisible suggestions' states
        assertThat(visibleSuggestions).isEmpty();
        assertThat(invisibleSuggestions) //
                .as("Invisible suggestions are hidden because of rejection") //
                .extracting(AnnotationSuggestion::getReasonForHiding) //
                .extracting(String::trim) //
                .containsExactly("rejected");
    }

    @Test
    public void thatVisibilityIsRestoredWhenOverlappingAnnotationIsRemoved() throws Exception
    {
        doReturn(new ArrayList<>()).when(learningRecordService).listLearningRecords(TEST_USER,
                TEST_USER, layer);

        var cas = getTestCas();
        var suggestions = makeSpanSuggestionGroup(doc, feature,
                new int[][] { { 1, 0, 3 }, { 2, 13, 20 } });
        sut.calculateSuggestionVisibility(TEST_USER, doc, cas, TEST_USER, layer, suggestions, 0,
                25);

        assertThat(getVisibleSuggestions(suggestions)) //
                .as("No suggestions are visible as they overlap with annotations") //
                .isEmpty();
        assertThat(getInvisibleSuggestions(suggestions)) //
                .as("All suggestions are hidden as the overlap with annotations") //
                .isNotEmpty();

        cas.select(NamedEntity.class).forEach(NamedEntity::removeFromIndexes);

        sut.calculateSuggestionVisibility(TEST_USER, doc, cas, TEST_USER, layer, suggestions, 0,
                25);

        assertThat(getInvisibleSuggestions(suggestions)) //
                .as("No suggestions are hidden as they no longer overlap with annotations") //
                .isEmpty();
        assertThat(getVisibleSuggestions(suggestions)) //
                .as("All suggestions are visible as they no longer overlap with annotations") //
                .containsExactlyInAnyOrderElementsOf(
                        suggestions.stream().flatMap(g -> g.stream()).collect(toList()));
    }

    @Test
    public void thatOverlappingSuggestionsAreNotHiddenWhenStackingIsEnabled() throws Exception
    {
        doReturn(emptyList()).when(learningRecordService).listLearningRecords(TEST_USER, TEST_USER,
                layer);

        layer.setOverlapMode(OverlapMode.ANY_OVERLAP);
        var rec = Recommender.builder() //
                .withId(123l) //
                .withName("rec") //
                .withLayer(layer) //
                .withFeature(feature) //
                .build();

        var cas = JCasFactory.createText("a b", "de");

        var suggestionTemplate = SpanSuggestion.builder() //
                .withDocument(doc) //
                .withRecommender(rec) //
                .withLabel("blah");
        var suggestion1 = suggestionTemplate //
                .withId(1) //
                .withPosition(0, 1) //
                .build();
        var suggestion2 = suggestionTemplate //
                .withId(2) //
                .withPosition(1, 2) //
                .build();
        var suggestions = SuggestionDocumentGroup.groupsOfType(SpanSuggestion.class,
                asList(suggestion1, suggestion2));

        sut.calculateSuggestionVisibility(TEST_USER, doc, cas.getCas(), TEST_USER, layer,
                suggestions, 0, 2);

        assertThat(getInvisibleSuggestions(suggestions)) //
                .as("No suggestions are hidden as they do not overlap with annotations") //
                .containsExactly();
        assertThat(getVisibleSuggestions(suggestions)) //
                .as("All suggestions are visible as they do not overlap with annotations") //
                .containsExactlyInAnyOrderElementsOf(
                        suggestions.stream().flatMap(g -> g.stream()).collect(toList()));

        var ne1 = new NamedEntity(cas, 0, 1);
        ne1.addToIndexes();

        assertThat(getInvisibleSuggestions(suggestions)) //
                .as("First suggestion is still visible because as its label does not match the "
                        + "label of the annotation at the same position") //
                .isEmpty();

        ne1.setValue("blah");
        sut.calculateSuggestionVisibility(TEST_USER, doc, cas.getCas(), TEST_USER, layer,
                suggestions, 0, 2);

        assertThat(getInvisibleSuggestions(suggestions)) //
                .as("First suggestion is no longer visible because annotation with the same label exists") //
                .containsExactly(suggestion1);

        var ne2 = new NamedEntity(cas, 1, 2);
        ne2.setValue("blah");
        ne2.addToIndexes();
        sut.calculateSuggestionVisibility(TEST_USER, doc, cas.getCas(), TEST_USER, layer,
                suggestions, 0, 2);

        assertThat(getInvisibleSuggestions(suggestions)) //
                .as("Second suggestion is now also no longer visible because annotation with the same label exists") //
                .containsExactly(suggestion1, suggestion2);
    }

    @Test
    void thatRejectedSuggestionIsHidden()
    {
        var rec1 = Recommender.builder().withId(1l).withName("Rec1").withLayer(layer)
                .withFeature(feature).build();
        var rec2 = Recommender.builder().withId(2l).withName("Rec2").withLayer(layer2)
                .withFeature(feature).build();
        var rec3 = Recommender.builder().withId(3l).withName("Rec3").withLayer(layer)
                .withFeature(feature2).build();
        var label = "x";

        var records = asList(LearningRecord.builder() //
                .withSourceDocument(doc) //
                .withLayer(layer) //
                .withAnnotationFeature(feature) //
                .withOffsetBegin(0) //
                .withOffsetEnd(10) //
                .withAnnotation(label) //
                .withUserAction(REJECTED) //
                .build());

        var docSuggestion = SpanSuggestion.builder() //
                .withRecommender(rec1) //
                .withDocument(doc) //
                .withLabel(label) //
                .withPosition(0, 10) //
                .build();
        assertThat(docSuggestion.isVisible()).isTrue();
        hideSuggestionsRejectedOrSkipped(docSuggestion, records);
        assertThat(docSuggestion.isVisible()) //
                .as("Suggestion in same document/layer/feature should be hidden") //
                .isFalse();
        assertThat(docSuggestion.getReasonForHiding().trim()).isEqualTo("rejected");

        var doc2Suggestion = SpanSuggestion.builder() //
                .withRecommender(rec1) //
                .withDocument(doc2) //
                .withLabel(label) //
                .withPosition(0, 10) //
                .build();
        assertThat(doc2Suggestion.isVisible()).isTrue();
        hideSuggestionsRejectedOrSkipped(doc2Suggestion, records);
        assertThat(doc2Suggestion.isVisible()) //
                .as("Suggestion in other document should not be hidden") //
                .isTrue();

        var doc3Suggestion = SpanSuggestion.builder().withRecommender(rec2).withDocument(doc)
                .withLabel(label).withPosition(0, 10).build();
        assertThat(doc3Suggestion.isVisible()).isTrue();
        hideSuggestionsRejectedOrSkipped(doc3Suggestion, records);
        assertThat(doc3Suggestion.isVisible()) //
                .as("Suggestion in other layer should not be hidden") //
                .isTrue();

        var doc4Suggestion = SpanSuggestion.builder().withRecommender(rec3).withDocument(doc)
                .withLabel(label).withPosition(0, 10).build();
        assertThat(doc4Suggestion.isVisible()).isTrue();
        hideSuggestionsRejectedOrSkipped(doc3Suggestion, records);
        assertThat(doc4Suggestion.isVisible()) //
                .as("Suggestion in other feature should not be hidden") //
                .isTrue();
    }

    @Test
    void thatSkippedSuggestionIsHidden()
    {
        var rec1 = Recommender.builder().withId(1l).withName("Rec1").withLayer(layer)
                .withFeature(feature).build();
        var rec2 = Recommender.builder().withId(2l).withName("Rec2").withLayer(layer2)
                .withFeature(feature).build();
        var rec3 = Recommender.builder().withId(3l).withName("Rec3").withLayer(layer)
                .withFeature(feature2).build();
        var label = "x";

        var records = asList(LearningRecord.builder() //
                .withSourceDocument(doc) //
                .withLayer(layer) //
                .withAnnotationFeature(feature) //
                .withOffsetBegin(0) //
                .withOffsetEnd(10) //
                .withAnnotation(label) //
                .withUserAction(SKIPPED) //
                .build());

        var docSuggestion = SpanSuggestion.builder() //
                .withRecommender(rec1) //
                .withLayer(rec1.getLayer()) //
                .withFeature(rec1.getFeature()) //
                .withDocument(doc) //
                .withLabel(label) //
                .withPosition(0, 10) //
                .build();
        assertThat(docSuggestion.isVisible()).isTrue();
        hideSuggestionsRejectedOrSkipped(docSuggestion, records);
        assertThat(docSuggestion.isVisible()) //
                .as("Suggestion in same document/layer/feature should be hidden") //
                .isFalse();
        assertThat(docSuggestion.getReasonForHiding().trim()).isEqualTo("skipped");

        var doc2Suggestion = SpanSuggestion.builder() //
                .withRecommender(rec1) //
                .withLayer(rec1.getLayer()) //
                .withFeature(rec1.getFeature()) //
                .withDocument(doc2) //
                .withLabel(label) //
                .withPosition(0, 10) //
                .build();
        assertThat(doc2Suggestion.isVisible()).isTrue();
        hideSuggestionsRejectedOrSkipped(doc2Suggestion, records);
        assertThat(doc2Suggestion.isVisible()) //
                .as("Suggestion in other document should not be hidden") //
                .isTrue();

        var doc3Suggestion = SpanSuggestion.builder().withRecommender(rec2).withDocument(doc)
                .withLabel(label).withPosition(0, 10).build();
        hideSuggestionsRejectedOrSkipped(doc3Suggestion, records);
        assertThat(doc3Suggestion.isVisible()) //
                .as("Suggestion in other layer should not be hidden") //
                .isTrue();

        var doc4Suggestion = SpanSuggestion.builder().withRecommender(rec3).withDocument(doc)
                .withLabel(label).withPosition(0, 10).build();
        assertThat(doc4Suggestion.isVisible()).isTrue();
        hideSuggestionsRejectedOrSkipped(doc3Suggestion, records);
        assertThat(doc4Suggestion.isVisible()) //
                .as("Suggestion in other feature should not be hidden") //
                .isTrue();
    }

    @Test
    void thatNoLabelSuggestionsForDifferentFeaturesAtSamePositionAreConflated() throws Exception
    {
        // A multi-feature extractor targeting both features of the same layer produces a no-label
        // suggestion for each feature at the same position. Accepting any one creates the same
        // empty span, so only the highest-scoring suggestion from that recommender should be kept.
        var featureB = AnnotationFeature.builder() //
                .withId(99l) //
                .withLayer(layer) //
                .withName("anotherFeature") //
                .withType(TYPE_NAME_STRING) //
                .build();
        doReturn(asList(feature, featureB)).when(annoService).listSupportedFeatures(layer);
        doReturn(emptyList()).when(learningRecordService).listLearningRecords(TEST_USER, TEST_USER,
                layer);

        // One recommender (e.g. a multi-feature extractor) producing no-label for two features
        var rec1 = Recommender.builder().withId(1l).withName("Rec1").withLayer(layer)
                .withFeature(feature).build();

        var suggestion1 = SpanSuggestion.builder() //
                .withId(1) //
                .withRecommender(rec1) //
                .withDocument(doc) //
                .withPosition(5, 10) //
                .withScore(0.7) //
                .build();
        var suggestion2 = SpanSuggestion.builder() //
                .withId(2) //
                .withRecommender(rec1) //
                .withFeature(featureB) // same recommender, different feature
                .withDocument(doc) //
                .withPosition(5, 10) //
                .withScore(0.9) //
                .build();
        var suggestions = SuggestionDocumentGroup.groupsOfType(SpanSuggestion.class,
                asList(suggestion1, suggestion2));

        var cas = JCasFactory.createText("Hello World", "de").getCas();
        sut.calculateSuggestionVisibility(TEST_USER, doc, cas, TEST_USER, layer, suggestions, 0,
                11);

        assertThat(getVisibleSuggestions(suggestions)) //
                .as("Only the highest-scoring no-label suggestion from this recommender should be visible") //
                .hasSize(1) //
                .containsExactly(suggestion2);
        assertThat(getInvisibleSuggestions(suggestions)) //
                .as("The lower-scoring same-recommender duplicate should be hidden") //
                .hasSize(1) //
                .extracting(AnnotationSuggestion::getReasonForHiding) //
                .extracting(String::trim) //
                .containsExactly("duplicate");
    }

    @Test
    void thatNoLabelSuggestionsForSameFeatureFromDifferentRecommendersAreNotConflated()
        throws Exception
    {
        // Two recommenders both targeting the same feature - their no-label suggestions are in
        // the same SuggestionGroup and must both remain visible so the lazy detail can show
        // both recommenders and their scores.
        doReturn(asList(feature)).when(annoService).listSupportedFeatures(layer);
        doReturn(emptyList()).when(learningRecordService).listLearningRecords(TEST_USER, TEST_USER,
                layer);

        var rec1 = Recommender.builder().withId(1l).withName("Rec1").withLayer(layer)
                .withFeature(feature).build();
        var rec2 = Recommender.builder().withId(2l).withName("Rec2").withLayer(layer)
                .withFeature(feature).build();

        var suggestion1 = SpanSuggestion.builder() //
                .withId(1) //
                .withRecommender(rec1) //
                .withDocument(doc) //
                .withPosition(5, 10) //
                .withScore(1.0) //
                .build();
        var suggestion2 = SpanSuggestion.builder() //
                .withId(2) //
                .withRecommender(rec2) //
                .withDocument(doc) //
                .withPosition(5, 10) //
                .withScore(0.5) //
                .build();
        var suggestions = SuggestionDocumentGroup.groupsOfType(SpanSuggestion.class,
                asList(suggestion1, suggestion2));

        var cas = JCasFactory.createText("Hello World", "de").getCas();
        sut.calculateSuggestionVisibility(TEST_USER, doc, cas, TEST_USER, layer, suggestions, 0,
                11);

        assertThat(getInvisibleSuggestions(suggestions)) //
                .as("No suggestions should be hidden - same-feature duplicates are handled by "
                        + "SuggestionGroup, not by FLAG_DUPLICATE") //
                .isEmpty();
        assertThat(getVisibleSuggestions(suggestions)) //
                .as("Both suggestions should remain visible for lazy detail rendering") //
                .hasSize(2);
    }

    @Test
    void thatNoLabelSuggestionsForLosingFeatureAreHiddenEvenIfOtherRecommendersAlsoProduceSuggestions()
        throws Exception
    {
        // rec1 is a multi-feature recommender producing no-label for both featA (score 0.9) and
        // featB (score 0.7) at the same position. rec2 is a bound recommender targeting only
        // featA (score 0.5). Within rec1's group, featB loses to featA and is hidden.
        // rec2's suggestion is in a separate (offset, recommender) group and stays visible.
        var featureB = AnnotationFeature.builder() //
                .withId(99l) //
                .withLayer(layer) //
                .withName("anotherFeature") //
                .withType(TYPE_NAME_STRING) //
                .build();
        doReturn(asList(feature, featureB)).when(annoService).listSupportedFeatures(layer);
        doReturn(emptyList()).when(learningRecordService).listLearningRecords(TEST_USER, TEST_USER,
                layer);

        var rec1 = Recommender.builder().withId(1l).withName("Rec1").withLayer(layer)
                .withFeature(feature).build();
        var rec2 = Recommender.builder().withId(2l).withName("Rec2").withLayer(layer)
                .withFeature(feature).build();

        // rec1: multi-feature recommender, two features at same position
        var suggestion1 = SpanSuggestion.builder().withId(1).withRecommender(rec1).withDocument(doc)
                .withPosition(5, 10).withScore(0.9).build(); // featA
        var suggestion2 = SpanSuggestion.builder().withId(2).withRecommender(rec1)
                .withFeature(featureB) // same recommender, different feature
                .withDocument(doc).withPosition(5, 10).withScore(0.7).build(); // featB
        // rec2: bound recommender, just featA
        var suggestion3 = SpanSuggestion.builder().withId(3).withRecommender(rec2).withDocument(doc)
                .withPosition(5, 10).withScore(0.5).build(); // featA
        var suggestions = SuggestionDocumentGroup.groupsOfType(SpanSuggestion.class,
                asList(suggestion1, suggestion2, suggestion3));

        var cas = JCasFactory.createText("Hello World", "de").getCas();
        sut.calculateSuggestionVisibility(TEST_USER, doc, cas, TEST_USER, layer, suggestions, 0,
                11);

        assertThat(getVisibleSuggestions(suggestions)) //
                .as("rec1/featA and rec2/featA must remain visible") //
                .containsExactlyInAnyOrder(suggestion1, suggestion3);
        assertThat(getInvisibleSuggestions(suggestions)) //
                .as("rec1/featB should be hidden: same recommender as rec1/featA but lower score") //
                .containsExactly(suggestion2);
    }

    @Test
    void thatNoLabelSuggestionsAreHiddenWhenRecommenderAlsoProducesLabeledSuggestionAtSamePosition()
        throws Exception
    {
        // A multi-feature recommender produces both a labeled suggestion (featA="PER") and a
        // no-label
        // suggestion (featB=null) for the same span. The labeled suggestion is more specific,
        // so the no-label one should be hidden as a duplicate.
        var featureB = AnnotationFeature.builder() //
                .withId(99l) //
                .withLayer(layer) //
                .withName("anotherFeature") //
                .withType(TYPE_NAME_STRING) //
                .build();
        doReturn(asList(feature, featureB)).when(annoService).listSupportedFeatures(layer);
        doReturn(emptyList()).when(learningRecordService).listLearningRecords(TEST_USER, TEST_USER,
                layer);

        var rec1 = Recommender.builder() //
                .withId(1l) //
                .withName("Rec1") //
                .withLayer(layer) //
                .withFeature(feature) //
                .build();

        // Labeled suggestion for featA
        var labeledSuggestion = SpanSuggestion.builder() //
                .withId(1) //
                .withRecommender(rec1) //
                .withDocument(doc) //
                .withPosition(5, 10) //
                .withLabel("PER") //
                .withUiLabel("PER") //
                .withScore(0.9) //
                .build();
        // No-label suggestion for featB from the same recommender at the same position
        var noLabelSuggestion = SpanSuggestion.builder() //
                .withId(2) //
                .withRecommender(rec1) //
                .withFeature(featureB) //
                .withDocument(doc) //
                .withPosition(5, 10) //
                .withScore(0.8) //
                .build();
        var suggestions = SuggestionDocumentGroup.groupsOfType(SpanSuggestion.class,
                asList(labeledSuggestion, noLabelSuggestion));

        var cas = JCasFactory.createText("Hello World", "de").getCas();
        sut.calculateSuggestionVisibility(TEST_USER, doc, cas, TEST_USER, layer, suggestions, 0,
                11);

        assertThat(getVisibleSuggestions(suggestions)) //
                .as("Only the labeled suggestion should be visible") //
                .containsExactly(labeledSuggestion);
        assertThat(getInvisibleSuggestions(suggestions)) //
                .as("The no-label suggestion should be hidden: the recommender made a specific prediction") //
                .hasSize(1) //
                .extracting(AnnotationSuggestion::getReasonForHiding) //
                .extracting(String::trim) //
                .containsExactly("duplicate");
    }

    private CAS getTestCas() throws Exception
    {
        var text = "Dies ist ein Testtext, ach ist der schoen, der schoenste von allen  Testtexten.";
        var jcas = JCasFactory.createText(text, "de");

        var neLabel = new NamedEntity(jcas, 0, 3);
        neLabel.setValue("LOC");
        neLabel.addToIndexes();

        // the annotation's feature value is initialized as null
        var neNoLabel = new NamedEntity(jcas, 13, 20);
        neNoLabel.addToIndexes();

        return jcas.getCas();
    }
}
