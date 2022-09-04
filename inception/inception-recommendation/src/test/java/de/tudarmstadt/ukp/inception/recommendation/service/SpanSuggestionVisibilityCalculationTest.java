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
package de.tudarmstadt.ukp.inception.recommendation.service;

import static de.tudarmstadt.ukp.inception.recommendation.service.Fixtures.getInvisibleSuggestions;
import static de.tudarmstadt.ukp.inception.recommendation.service.Fixtures.getVisibleSuggestions;
import static de.tudarmstadt.ukp.inception.recommendation.service.Fixtures.makeSpanSuggestionGroup;
import static java.util.stream.Collectors.toList;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionDocumentGroup;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

@ExtendWith(MockitoExtension.class)
public class SpanSuggestionVisibilityCalculationTest
{
    private @Mock LearningRecordService recordService;
    private @Mock AnnotationSchemaService annoService;

    private Project project;
    private SourceDocument doc;
    private AnnotationLayer layer;
    private AnnotationFeature feature;
    private String user;

    private RecommendationServiceImpl sut;

    @BeforeEach
    public void setUp() throws Exception
    {
        user = "Testuser";

        layer = new AnnotationLayer();
        layer.setName(NamedEntity._TypeName);
        layer.setId(42l);

        feature = AnnotationFeature.builder().withId(2l).withLayer(layer)
                .withName(NamedEntity._FeatName_value).build();

        project = new Project();
        project.setName("Test Project");

        doc = SourceDocument.builder().withId(12l).withName("doc").withProject(project).build();

        List<AnnotationFeature> featureList = new ArrayList<AnnotationFeature>();
        featureList.add(new AnnotationFeature(NamedEntity._FeatName_value, TYPE_NAME_STRING));
        when(annoService.listSupportedFeatures(layer)).thenReturn(featureList);

        sut = new RecommendationServiceImpl(null, null, null, null, null, annoService, null,
                recordService, (EntityManager) null);
    }

    @Test
    public void testCalculateVisibilityNoRecordsAllHidden() throws Exception
    {
        when(recordService.listRecords(user, layer)).thenReturn(new ArrayList<>());

        CAS cas = getTestCas();
        SuggestionDocumentGroup<SpanSuggestion> suggestions = makeSpanSuggestionGroup(doc, feature,
                new int[][] { { 1, 0, 3 }, { 2, 13, 20 } });
        sut.calculateSpanSuggestionVisibility(doc, cas, user, layer, suggestions, 0, 25);

        List<SpanSuggestion> invisibleSuggestions = getInvisibleSuggestions(suggestions);
        List<SpanSuggestion> visibleSuggestions = getVisibleSuggestions(suggestions);

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
        when(recordService.listRecords(user, layer)).thenReturn(new ArrayList<>());

        CAS cas = getTestCas();
        SuggestionDocumentGroup<SpanSuggestion> suggestions = makeSpanSuggestionGroup(doc, feature,
                new int[][] { { 1, 5, 10 } });
        sut.calculateSpanSuggestionVisibility(doc, cas, user, layer, suggestions, 0, 25);

        List<SpanSuggestion> invisibleSuggestions = getInvisibleSuggestions(suggestions);
        List<SpanSuggestion> visibleSuggestions = getVisibleSuggestions(suggestions);

        // check the invisible suggestions' states
        assertThat(visibleSuggestions).isNotEmpty();
        assertThat(invisibleSuggestions).isEmpty();
    }

    @Test
    public void testCalculateVisibilityRejected() throws Exception
    {
        List<LearningRecord> records = new ArrayList<>();
        LearningRecord rejectedRecord = new LearningRecord();
        rejectedRecord.setSourceDocument(doc);
        rejectedRecord.setUserAction(LearningRecordType.REJECTED);
        rejectedRecord.setLayer(layer);
        rejectedRecord.setAnnotationFeature(feature);
        rejectedRecord.setOffsetBegin(5);
        rejectedRecord.setOffsetEnd(10);
        records.add(rejectedRecord);
        when(recordService.listRecords(user, layer)).thenReturn(records);

        CAS cas = getTestCas();
        SuggestionDocumentGroup<SpanSuggestion> suggestions = makeSpanSuggestionGroup(doc, feature,
                new int[][] { { 1, 5, 10 } });
        sut.calculateSpanSuggestionVisibility(doc, cas, user, layer, suggestions, 0, 25);

        List<SpanSuggestion> invisibleSuggestions = getInvisibleSuggestions(suggestions);
        List<SpanSuggestion> visibleSuggestions = getVisibleSuggestions(suggestions);

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
        when(recordService.listRecords(user, layer)).thenReturn(new ArrayList<>());

        CAS cas = getTestCas();
        SuggestionDocumentGroup<SpanSuggestion> suggestions = makeSpanSuggestionGroup(doc, feature,
                new int[][] { { 1, 0, 3 }, { 2, 13, 20 } });
        sut.calculateSpanSuggestionVisibility(doc, cas, user, layer, suggestions, 0, 25);

        assertThat(getVisibleSuggestions(suggestions)) //
                .as("No suggestions are visible as they overlap with annotations") //
                .isEmpty();
        assertThat(getInvisibleSuggestions(suggestions)) //
                .as("All suggestions are hidden as the overlap with annotations") //
                .isNotEmpty();

        cas.select(NamedEntity.class).forEach(NamedEntity::removeFromIndexes);

        sut.calculateSpanSuggestionVisibility(doc, cas, user, layer, suggestions, 0, 25);

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
        String documentText = "Dies ist ein Testtext, ach ist der schoen, der schoenste von allen"
                + " Testtexten.";
        JCas jcas = JCasFactory.createText(documentText, "de");

        NamedEntity neLabel = new NamedEntity(jcas, 0, 3);
        neLabel.setValue("LOC");
        neLabel.addToIndexes();

        // the annotation's feature value is initialized as null
        NamedEntity neNoLabel = new NamedEntity(jcas, 13, 20);
        neNoLabel.addToIndexes();

        return jcas.getCas();
    }
}
