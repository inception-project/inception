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
package de.tudarmstadt.ukp.inception.recommendation.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
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
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;

public class PredictionTaskTest
{
    private @Mock LearningRecordService recordService;
    private @Mock AnnotationSchemaService annoService;
    
    private Project project;
    private AnnotationLayer layer;
    private String user;
    private String neName;
    long layerId;
    
    // AnnotationSuggestion constants
    private long recommenderId = 1;
    private String feature = "value";
    private String documentName = "TestDocument";
    private String uiLabel = "TestUiLabel";
    private double confidence = 0.2;
    private String recommenderName = "TestEntityRecommender";
    private String coveredText = "TestText";
   


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
     
    }

    @Test
    public void testCalculateVisibilityNoRecordsAllHidden() throws Exception
    {
        when(recordService.listRecords(user, layer)).thenReturn(new ArrayList<LearningRecord>());

        CAS cas = getTestCas();
        Collection<SuggestionGroup> suggestions = getASuggestionGroup(
                new int[][] { { 1, 0, 3 }, { 2, 13, 20 } });
        PredictionTask.calculateVisibility(recordService, annoService, cas, user, layer,
                suggestions, 0, 25);

        List<AnnotationSuggestion> invisibleSuggestions = getInVisibleSuggestions(suggestions);
        List<AnnotationSuggestion> visibleSuggestions = getVisibleSuggestions(suggestions);

        // check the invisible suggestions' states
        assertTrue(invisibleSuggestions.size() > 0);
        assertThat(invisibleSuggestions)
                .as("All invisible suggestions are hidden because of overlapping")
                .allMatch(s -> s.getReasonForHiding().equals("overlapping "));
        
        // check no visible suggestions
        assertTrue(visibleSuggestions.size() == 0);
    }

    @Test
    public void testCalculateVisibilityNoRecordsNotHidden() throws Exception
    {
        when(recordService.listRecords(user, layer)).thenReturn(new ArrayList<LearningRecord>());

        CAS cas = getTestCas();
        Collection<SuggestionGroup> suggestions = getASuggestionGroup(
                new int[][] { { 1, 5, 10 }});
        PredictionTask.calculateVisibility(recordService, annoService, cas, user, layer,
                suggestions, 0, 25);

        List<AnnotationSuggestion> invisibleSuggestions = getInVisibleSuggestions(suggestions);
        List<AnnotationSuggestion> visibleSuggestions = getVisibleSuggestions(suggestions);

        // check the invisible suggestions' states
        assertTrue(visibleSuggestions.size() > 0);
        assertThat(invisibleSuggestions.size() == 0);
    }
    
    @Test
    public void testCalculateVisibilityRejected() throws Exception
    {
        List<LearningRecord> records = new ArrayList<LearningRecord>();
        LearningRecord rejectedRecord = new LearningRecord();
        rejectedRecord.setUserAction(LearningRecordType.REJECTED);
        rejectedRecord.setOffsetCharacterBegin(5);
        rejectedRecord.setOffsetCharacterEnd(10);
        records.add(rejectedRecord);
        when(recordService.listRecords(user, layer)).thenReturn(records);

        CAS cas = getTestCas();
        Collection<SuggestionGroup> suggestions = getASuggestionGroup(
                new int[][] { { 1, 5, 10 }});
        PredictionTask.calculateVisibility(recordService, annoService, cas, user, layer,
                suggestions, 0, 25);

        List<AnnotationSuggestion> invisibleSuggestions = getInVisibleSuggestions(suggestions);
        List<AnnotationSuggestion> visibleSuggestions = getVisibleSuggestions(suggestions);

        // check the invisible suggestions' states
        assertTrue(visibleSuggestions.size() == 0);
        assertThat(invisibleSuggestions).as("Invisible suggestions are hidden because of rejection")
        .allMatch(s -> s.getReasonForHiding().equals("rejected "));
    }

    private List<AnnotationSuggestion> getInVisibleSuggestions(
            Collection<SuggestionGroup> aSuggestions)
    {
        return aSuggestions.stream()
                .flatMap(SuggestionGroup::stream).filter(s -> !s.isVisible())
                .collect(Collectors.toList());
    }

    private List<AnnotationSuggestion> getVisibleSuggestions(
            Collection<SuggestionGroup> aSuggestions)
    {
        return aSuggestions.stream()
                .flatMap(SuggestionGroup::stream).filter(s -> s.isVisible())
                .collect(Collectors.toList());
    }    

    private Collection<SuggestionGroup> getASuggestionGroup(int[][] vals)
    {

        List<AnnotationSuggestion> suggestions = new ArrayList<AnnotationSuggestion>();
        for (int[] val : vals) {
            suggestions.add(new AnnotationSuggestion(val[0], recommenderId, recommenderName,
                    layerId, feature, documentName, val[1], val[2], coveredText, null, uiLabel,
                    confidence));
            suggestions.add(new AnnotationSuggestion(val[0], recommenderId, recommenderName,
                    layerId, feature, documentName, val[1], val[2], coveredText, null, uiLabel,
                    confidence));
        }

        return SuggestionGroup.group(suggestions);
    }

    private CAS getTestCas() throws Exception
    {
        JCas jcas = JCasFactory.createJCas();
        jcas.setDocumentLanguage("de");
        DocumentMetaData meta = new DocumentMetaData(jcas);
        meta.setDocumentTitle("Filename");
        meta.addToIndexes();

        jcas.setDocumentText("Dies ist ein Testtext, ach ist der schoen, der schoenste von"
                + "allen Testtexten.");

        NamedEntity neLabel = new NamedEntity(jcas);
        neLabel.setBegin(0);
        neLabel.setEnd(3);
        neLabel.setValue("LOC");
        neLabel.addToIndexes();

        NamedEntity neNoLabel = new NamedEntity(jcas);
        neNoLabel.setBegin(13);
        neNoLabel.setEnd(20);
        neNoLabel.setValue(null);
        neNoLabel.addToIndexes();

        return jcas.getCas();
    }
}
