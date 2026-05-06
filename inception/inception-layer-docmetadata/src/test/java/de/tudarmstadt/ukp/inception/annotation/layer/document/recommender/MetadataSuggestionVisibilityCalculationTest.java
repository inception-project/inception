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
package de.tudarmstadt.ukp.inception.annotation.layer.document.recommender;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.feature.multistring.MultiValueStringFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerTraits;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction;
import de.tudarmstadt.ukp.inception.recommendation.api.model.MetadataSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionDocumentGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistryImpl;

@ExtendWith(MockitoExtension.class)
class MetadataSuggestionVisibilityCalculationTest
{
    private static final String TEST_USER = "Testuser";

    private @Mock RecommendationService recommendationService;
    private @Mock LearningRecordService learningRecordService;
    private @Mock ApplicationEventPublisher applicationEventPublisher;
    private @Mock AnnotationSchemaService schemaService;
    private @Mock DocumentMetadataLayerAdapter adapter;

    private Project project;
    private SourceDocument doc;
    private TypeSystemDescription tsd;
    private TypeDescription typeDesc;
    private AnnotationLayer layer;
    private AnnotationFeature singleValueFeature;
    private AnnotationFeature multiValueFeature;
    private Recommender singleValueRecommender;
    private Recommender multiValueRecommender;
    private DocumentMetadataLayerTraits traits;
    private CAS cas;

    private MetadataSuggestionSupport sut;

    @BeforeEach
    void setUp() throws Exception
    {
        project = Project.builder().withId(1l).withName("Test").build();
        doc = SourceDocument.builder().withId(12l).withName("doc").withProject(project).build();

        tsd = new TypeSystemDescription_impl();
        typeDesc = tsd.addType("custom.Metadata", "", CAS.TYPE_NAME_ANNOTATION_BASE);
        FeatureDescription singleFeatDesc = typeDesc.addFeature("value", "", CAS.TYPE_NAME_STRING);
        FeatureDescription multiFeatDesc = typeDesc.addFeature("values", "",
                CAS.TYPE_NAME_STRING_ARRAY, CAS.TYPE_NAME_STRING, false);

        layer = AnnotationLayer.builder().withId(44l).withName(typeDesc.getName()).build();

        singleValueFeature = AnnotationFeature.builder().withId(1l) //
                .withLayer(layer) //
                .withName(singleFeatDesc.getName()) //
                .withType(CAS.TYPE_NAME_STRING) //
                .withMultiValueMode(MultiValueMode.NONE) //
                .build();

        multiValueFeature = AnnotationFeature.builder().withId(2l) //
                .withLayer(layer) //
                .withName(multiFeatDesc.getName()) //
                .withType(CAS.TYPE_NAME_STRING_ARRAY) //
                .withMultiValueMode(MultiValueMode.ARRAY) //
                .build();

        singleValueRecommender = Recommender.builder().withId(101l).withName("rec-single") //
                .withProject(project).withLayer(layer).withFeature(singleValueFeature).build();
        multiValueRecommender = Recommender.builder().withId(102l).withName("rec-multi") //
                .withProject(project).withLayer(layer).withFeature(multiValueFeature).build();

        traits = new DocumentMetadataLayerTraits();

        cas = CasFactory.createCas(tsd);

        when(schemaService.getAdapter(layer)).thenReturn(adapter);
        when(adapter.getTraits(DocumentMetadataLayerTraits.class))
                .thenReturn(Optional.of(traits));

        var featureSupportRegistry = new FeatureSupportRegistryImpl(
                asList(new StringFeatureSupport(), new MultiValueStringFeatureSupport()));
        featureSupportRegistry.init();

        sut = new MetadataSuggestionSupport(recommendationService, learningRecordService,
                applicationEventPublisher, schemaService, featureSupportRegistry);
    }

    @Test
    void testWithoutExistingAnnotationSuggestionStaysVisible() throws Exception
    {
        when(schemaService.listSupportedFeatures(layer)).thenReturn(asList(singleValueFeature));
        doReturn(emptyList()).when(learningRecordService).listLearningRecords(TEST_USER, TEST_USER,
                layer);

        var suggestion = newSuggestion(singleValueRecommender, "happy", 1);
        var groups = groups(suggestion);

        sut.calculateSuggestionVisibility(TEST_USER, doc, cas, TEST_USER, layer, groups, 0,
                cas.getDocumentText() == null ? 0 : cas.getDocumentText().length());

        assertThat(visible(groups)).containsExactly(suggestion);
    }

    @Test
    void testSingleValuedFeatureWithMatchingLabelHidesSuggestion() throws Exception
    {
        when(schemaService.listSupportedFeatures(layer)).thenReturn(asList(singleValueFeature));
        doReturn(emptyList()).when(learningRecordService).listLearningRecords(TEST_USER, TEST_USER,
                layer);

        var ann = cas.createFS(cas.getTypeSystem().getType(typeDesc.getName()));
        FSUtil.setFeature(ann, "value", "happy");
        cas.addFsToIndexes(ann);

        var suggestion = newSuggestion(singleValueRecommender, "happy", 1);
        var groups = groups(suggestion);

        sut.calculateSuggestionVisibility(TEST_USER, doc, cas, TEST_USER, layer, groups, 0, 0);

        assertThat(invisible(groups)).containsExactly(suggestion);
    }

    @Test
    void testSingleValuedFeatureWithDifferentLabelKeepsSuggestionVisible() throws Exception
    {
        when(schemaService.listSupportedFeatures(layer)).thenReturn(asList(singleValueFeature));
        doReturn(emptyList()).when(learningRecordService).listLearningRecords(TEST_USER, TEST_USER,
                layer);

        var ann = cas.createFS(cas.getTypeSystem().getType(typeDesc.getName()));
        FSUtil.setFeature(ann, "value", "sad");
        cas.addFsToIndexes(ann);

        var suggestion = newSuggestion(singleValueRecommender, "happy", 1);
        var groups = groups(suggestion);

        sut.calculateSuggestionVisibility(TEST_USER, doc, cas, TEST_USER, layer, groups, 0, 0);

        assertThat(visible(groups)).containsExactly(suggestion);
    }

    @Test
    void testSingletonLayerHidesAllSuggestionsWhenAnnotationExists() throws Exception
    {
        traits.setSingleton(true);
        when(schemaService.listSupportedFeatures(layer)).thenReturn(asList(singleValueFeature));
        doReturn(emptyList()).when(learningRecordService).listLearningRecords(TEST_USER, TEST_USER,
                layer);

        var ann = cas.createFS(cas.getTypeSystem().getType(typeDesc.getName()));
        FSUtil.setFeature(ann, "value", "anything");
        cas.addFsToIndexes(ann);

        var suggestionA = newSuggestion(singleValueRecommender, "happy", 1);
        var suggestionB = newSuggestion(singleValueRecommender, "sad", 2);
        var groups = groups(suggestionA, suggestionB);

        sut.calculateSuggestionVisibility(TEST_USER, doc, cas, TEST_USER, layer, groups, 0, 0);

        assertThat(invisible(groups)).containsExactlyInAnyOrder(suggestionA, suggestionB);
    }

    @Test
    void testRejectedSuggestionIsHidden() throws Exception
    {
        when(schemaService.listSupportedFeatures(layer)).thenReturn(asList(singleValueFeature));

        var record = new LearningRecord();
        record.setUser(TEST_USER);
        record.setSourceDocument(doc);
        record.setLayer(layer);
        record.setAnnotationFeature(singleValueFeature);
        record.setAnnotation("happy");
        record.setUserAction(LearningRecordUserAction.REJECTED);
        doReturn(asList(record)).when(learningRecordService).listLearningRecords(TEST_USER,
                TEST_USER, layer);

        var suggestion = newSuggestion(singleValueRecommender, "happy", 1);
        var groups = groups(suggestion);

        sut.calculateSuggestionVisibility(TEST_USER, doc, cas, TEST_USER, layer, groups, 0, 0);

        assertThat(invisible(groups)).containsExactly(suggestion);
    }

    @Test
    void testMultiValuedFeatureWithMatchingLabelHidesSuggestion() throws Exception
    {
        when(schemaService.listSupportedFeatures(layer)).thenReturn(asList(multiValueFeature));
        doReturn(emptyList()).when(learningRecordService).listLearningRecords(TEST_USER, TEST_USER,
                layer);

        var ann = cas.createFS(cas.getTypeSystem().getType(typeDesc.getName()));
        FSUtil.setFeature(ann, "values", asList("happy", "calm"));
        cas.addFsToIndexes(ann);

        var suggestion = newSuggestion(multiValueRecommender, "happy", 1);
        var groups = groups(suggestion);

        sut.calculateSuggestionVisibility(TEST_USER, doc, cas, TEST_USER, layer, groups, 0, 0);

        assertThat(invisible(groups)).containsExactly(suggestion);
    }

    @Test
    void testMultiValuedFeatureWithDifferentLabelsKeepsSuggestionVisible() throws Exception
    {
        when(schemaService.listSupportedFeatures(layer)).thenReturn(asList(multiValueFeature));
        doReturn(emptyList()).when(learningRecordService).listLearningRecords(TEST_USER, TEST_USER,
                layer);

        var ann = cas.createFS(cas.getTypeSystem().getType(typeDesc.getName()));
        FSUtil.setFeature(ann, "values", asList("sad", "angry"));
        cas.addFsToIndexes(ann);

        var suggestion = newSuggestion(multiValueRecommender, "happy", 1);
        var groups = groups(suggestion);

        sut.calculateSuggestionVisibility(TEST_USER, doc, cas, TEST_USER, layer, groups, 0, 0);

        assertThat(visible(groups)).containsExactly(suggestion);
    }

    private MetadataSuggestion newSuggestion(Recommender aRecommender, String aLabel, int aId)
    {
        return MetadataSuggestion.builder() //
                .withId(aId) //
                .withRecommender(aRecommender) //
                .withDocument(doc) //
                .withLabel(aLabel) //
                .build();
    }

    private SuggestionDocumentGroup<MetadataSuggestion> groups(MetadataSuggestion... aSuggestions)
    {
        return SuggestionDocumentGroup.groupsOfType(MetadataSuggestion.class, asList(aSuggestions));
    }

    private static List<MetadataSuggestion> visible(
            Collection<SuggestionGroup<MetadataSuggestion>> aGroups)
    {
        return aGroups.stream() //
                .flatMap(SuggestionGroup::stream) //
                .filter(AnnotationSuggestion::isVisible) //
                .toList();
    }

    private static List<MetadataSuggestion> invisible(
            Collection<SuggestionGroup<MetadataSuggestion>> aGroups)
    {
        return aGroups.stream() //
                .flatMap(SuggestionGroup::stream) //
                .filter(s -> !s.isVisible()) //
                .toList();
    }
}
