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
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

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

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.feature.multistring.MultiValueStringFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureSupport;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionDocumentGroup;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistryImpl;

@ExtendWith(MockitoExtension.class)
public class SpanSuggestionMultiValueFeatureSuggestionVisibilityCalculationTest
{
    private static final String TEST_USER = "Testuser";

    private @Mock AnnotationSchemaService annoService;
    private @Mock LearningRecordService learningRecordService;

    private Project project;
    private SourceDocument doc;

    private TypeSystemDescription tsd;
    private TypeDescription typeDesc;
    private FeatureDescription featureDesc;
    private AnnotationLayer layer;
    private AnnotationFeature feature;
    private Recommender rec;
    private CAS cas;
    private SpanSuggestion.Builder suggestionTemplate;

    private SpanSuggestionSupport sut;

    @BeforeEach
    public void setUp() throws Exception
    {
        project = Project.builder().withName("Test Project").build();

        doc = SourceDocument.builder().withId(12l).withName("doc").withProject(project).build();

        var featureSupportRegistry = new FeatureSupportRegistryImpl(
                asList(new StringFeatureSupport(), new MultiValueStringFeatureSupport()));
        featureSupportRegistry.init();

        sut = new SpanSuggestionSupport(null, learningRecordService, null, annoService,
                featureSupportRegistry, null);

        tsd = new TypeSystemDescription_impl();
        typeDesc = tsd.addType("Span", null, CAS.TYPE_NAME_ANNOTATION);
        featureDesc = typeDesc.addFeature("values", null, CAS.TYPE_NAME_STRING_ARRAY,
                CAS.TYPE_NAME_STRING, false);
        layer = AnnotationLayer.builder().withId(44l).withName(typeDesc.getName()).build();
        feature = AnnotationFeature.builder().withId(4l) //
                .withLayer(layer) //
                .withName(featureDesc.getName()) //
                .withMultiValueMode(MultiValueMode.ARRAY) //
                .withType(CAS.TYPE_NAME_STRING_ARRAY) //
                .build();

        rec = Recommender.builder().withId(123l).withName("rec").withLayer(layer)
                .withFeature(feature).build();

        cas = CasFactory.createCas(tsd);

        doReturn(emptyList()).when(learningRecordService).listLearningRecords(TEST_USER, TEST_USER,
                layer);
        when(annoService.listSupportedFeatures(layer)).thenReturn(asList(feature));

        suggestionTemplate = SpanSuggestion.builder() //
                .withDocument(doc) //
                .withRecommender(rec) //
                .withId(1) //
                .withPosition(0, 1);
    }

    @Test
    public void collocatedWithAnnotationWithoutLabelAreNotHidden() throws Exception
    {
        var suggestion = suggestionTemplate.withLabel("blah").build();

        // Add annotation without label at same location as suggestion
        var ann = cas.createAnnotation(cas.getTypeSystem().getType(typeDesc.getName()),
                suggestion.getBegin(), suggestion.getEnd());
        cas.addFsToIndexes(ann);

        var documentSuggestions = SuggestionDocumentGroup.groupsOfType(SpanSuggestion.class,
                asList(suggestion));
        sut.calculateSuggestionVisibility(TEST_USER, doc, cas, TEST_USER, layer,
                documentSuggestions, 0, 2);

        assertThat(getInvisibleSuggestions(documentSuggestions)) //
                .as("Hidden suggestions") //
                .isEmpty();
    }

    @Test
    public void collocatedWithAnnotationWithSameLabelAreHidden() throws Exception
    {
        var suggestion = suggestionTemplate.withLabel("blah").build();

        // Add annotation with label at same location as suggestion
        var ann = cas.createAnnotation(cas.getTypeSystem().getType(typeDesc.getName()),
                suggestion.getBegin(), suggestion.getEnd());
        FSUtil.setFeature(ann, "values", asList(suggestion.getLabel()));
        cas.addFsToIndexes(ann);

        var documentSuggestions = calculateVisibility(suggestion);

        assertThat(getInvisibleSuggestions(documentSuggestions)) //
                .as("Hidden suggestions") //
                .containsExactly(suggestion);
    }

    @Test
    public void collocatedWithAnnotationWithDifferentLabelAreNotHidden() throws Exception
    {
        var suggestion = suggestionTemplate.withLabel("blah").build();

        // Add annotation with alternative label at same location as suggestion
        var ann = cas.createAnnotation(cas.getTypeSystem().getType(typeDesc.getName()),
                suggestion.getBegin(), suggestion.getEnd());
        FSUtil.setFeature(ann, "values", asList(suggestion.getLabel() + "_other"));
        cas.addFsToIndexes(ann);

        var documentSuggestions = calculateVisibility(suggestion);

        assertThat(getInvisibleSuggestions(documentSuggestions)) //
                .as("Hidden suggestions") //
                .isEmpty();
    }

    @Test
    public void withoutLabelCollocatedWithAnnotationWithLabelAreHidden() throws Exception
    {
        var suggestion = suggestionTemplate.withLabel(null).build();

        // Add annotation with alternative label at same location as suggestion
        var ann = cas.createAnnotation(cas.getTypeSystem().getType(typeDesc.getName()),
                suggestion.getBegin(), suggestion.getEnd());
        FSUtil.setFeature(ann, "values", asList("blah"));
        cas.addFsToIndexes(ann);

        var documentSuggestions = calculateVisibility(suggestion);

        assertThat(getInvisibleSuggestions(documentSuggestions)) //
                .as("Hidden suggestions") //
                .containsExactly(suggestion);
    }

    @Test
    public void withoutLabelOverlappingWithAnnotationWithLabelAreNotHidden() throws Exception
    {
        var suggestion = suggestionTemplate.withLabel(null).build();

        // Add annotation with alternative label at overlapping location as suggestion
        var ann = cas.createAnnotation(cas.getTypeSystem().getType(typeDesc.getName()),
                suggestion.getBegin(), suggestion.getEnd() - 1);
        FSUtil.setFeature(ann, "values", asList("blah"));
        cas.addFsToIndexes(ann);

        var documentSuggestions = calculateVisibility(suggestion);

        assertThat(getInvisibleSuggestions(documentSuggestions)) //
                .as("Hidden suggestions") //
                .isEmpty();
    }

    private SuggestionDocumentGroup<SpanSuggestion> calculateVisibility(SpanSuggestion suggestion)
    {
        var documentSuggestions = SuggestionDocumentGroup.groupsOfType(SpanSuggestion.class,
                asList(suggestion));
        sut.calculateSuggestionVisibility(TEST_USER, doc, cas, TEST_USER, layer,
                documentSuggestions, 0, 2);
        return documentSuggestions;
    }
}
