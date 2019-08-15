/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.inception.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;

public class VisibilityCalculationTests
{
    private @Mock LearningRecordService recordService;
    private @Mock AnnotationSchemaService annoService;

    private Project project;
    private AnnotationLayer layer;
    private String user;
    private String neName;
    private long layerId;
    
    private RecommendationServiceImpl sut;

    // AnnotationSuggestion
    private final static long RECOMMENDER_ID = 1;
    private final static String RECOMMENDER_NAME = "TestEntityRecommender";
    private final static String FEATURE = "value";
    private final static String DOC_NAME = "TestDocument";
    private final static String UI_LABEL = "TestUiLabel";
    private final static double CONFIDENCE = 0.2;
    private final static String CONFIDENCE_EXPLANATION = "Predictor A: 0.05 | Predictor B: 0.15";
    private final static String COVERED_TEXT = "TestText";

    @Before
    public void setUp() throws Exception
    {
        initMocks(this);

        user = "Testuser";
        neName = "de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity";

        layer = new AnnotationLayer();
        layer.setName(neName);
        layer.setId(new Long(42));
        layerId = layer.getId();

        project = new Project();
        project.setName("Test Project");
        project.setMode(WebAnnoConst.PROJECT_TYPE_ANNOTATION);

        List<AnnotationFeature> featureList = new ArrayList<AnnotationFeature>();
        featureList.add(new AnnotationFeature("value", "uima.cas.String"));
        when(annoService.listAnnotationFeature(layer)).thenReturn(featureList);
        
        sut = new RecommendationServiceImpl(null, null, null, null, annoService, null,
                recordService, null);
    }

    @Test
    public void testCalculateVisibilityNoRecordsAllHidden() throws Exception
    {
        when(recordService.listRecords(user, layer)).thenReturn(new ArrayList<>());

        CAS cas = getTestCas();
        Collection<SuggestionGroup> suggestions = getSuggestionGroup(
                new int[][] { { 1, 0, 3 }, { 2, 13, 20 } });
        sut.calculateVisibility(cas, user, layer, suggestions, 0, 25);

        List<AnnotationSuggestion> invisibleSuggestions = getInvisibleSuggestions(suggestions);
        List<AnnotationSuggestion> visibleSuggestions = getVisibleSuggestions(suggestions);

        // check the invisible suggestions' states
        assertThat(invisibleSuggestions).isNotEmpty();
        //FIXME find out why suggestions are repeated/doubled
        assertThat(invisibleSuggestions)
                .as("Invisible suggestions are hidden because of overlapping")
                .extracting(AnnotationSuggestion::getReasonForHiding).extracting(String::trim)
                .containsExactly("overlapping", "overlapping");

        // check no visible suggestions
        assertThat(visibleSuggestions).isEmpty();
    }

    @Test
    public void testCalculateVisibilityNoRecordsNotHidden() throws Exception
    {
        when(recordService.listRecords(user, layer)).thenReturn(new ArrayList<>());

        CAS cas = getTestCas();
        Collection<SuggestionGroup> suggestions = getSuggestionGroup(new int[][] { { 1, 5, 10 } });
        sut.calculateVisibility(cas, user, layer, suggestions, 0, 25);

        List<AnnotationSuggestion> invisibleSuggestions = getInvisibleSuggestions(suggestions);
        List<AnnotationSuggestion> visibleSuggestions = getVisibleSuggestions(suggestions);

        // check the invisible suggestions' states
        assertThat(visibleSuggestions).isNotEmpty();
        assertThat(invisibleSuggestions).isEmpty();
    }

    @Test
    public void testCalculateVisibilityRejected() throws Exception
    {
        List<LearningRecord> records = new ArrayList<>();
        LearningRecord rejectedRecord = new LearningRecord();
        rejectedRecord.setUserAction(LearningRecordType.REJECTED);
        rejectedRecord.setOffsetCharacterBegin(5);
        rejectedRecord.setOffsetCharacterEnd(10);
        records.add(rejectedRecord);
        when(recordService.listRecords(user, layer)).thenReturn(records);

        CAS cas = getTestCas();
        Collection<SuggestionGroup> suggestions = getSuggestionGroup(new int[][] { { 1, 5, 10 } });
        sut.calculateVisibility(cas, user, layer, suggestions, 0, 25);

        List<AnnotationSuggestion> invisibleSuggestions = getInvisibleSuggestions(suggestions);
        List<AnnotationSuggestion> visibleSuggestions = getVisibleSuggestions(suggestions);

        // check the invisible suggestions' states
        assertThat(visibleSuggestions).isEmpty();
        assertThat(invisibleSuggestions).as("Invisible suggestions are hidden because of rejection")
                .extracting(AnnotationSuggestion::getReasonForHiding).extracting(String::trim)
                .containsExactly("rejected");
    }

    private List<AnnotationSuggestion> getInvisibleSuggestions(
            Collection<SuggestionGroup> aSuggestions)
    {
        return aSuggestions.stream().flatMap(SuggestionGroup::stream).filter(s -> !s.isVisible())
                .collect(Collectors.toList());
    }

    private List<AnnotationSuggestion> getVisibleSuggestions(
            Collection<SuggestionGroup> aSuggestions)
    {
        return aSuggestions.stream().flatMap(SuggestionGroup::stream).filter(s -> s.isVisible())
                .collect(Collectors.toList());
    }

    private Collection<SuggestionGroup> getSuggestionGroup(int[][] vals)
    {

        List<AnnotationSuggestion> suggestions = new ArrayList<>();
        for (int[] val : vals) {
            suggestions.add(new AnnotationSuggestion(val[0], RECOMMENDER_ID, RECOMMENDER_NAME,
                    layerId, FEATURE, DOC_NAME, val[1], val[2], COVERED_TEXT, null, UI_LABEL,
                    CONFIDENCE, CONFIDENCE_EXPLANATION));
        }

        return SuggestionGroup.group(suggestions);
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
