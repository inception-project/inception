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
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;

public class PredictionTaskTest
{
    private @Mock LearningRecordService recordService;
    private @Mock AnnotationSchemaService annoService;
    private Project project;
    private AnnotationLayer layer;
    private String user;


    @Before
    public void setUp() throws Exception
    {
        initMocks(this);

        user = "Testuser";
        
        layer = new AnnotationLayer();
        String neName = "de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity";
        layer.setName(neName);

        project = new Project();
        project.setName("Test Project");
        project.setMode(WebAnnoConst.PROJECT_TYPE_ANNOTATION);
        
        List<AnnotationFeature> featureList = new ArrayList<AnnotationFeature>();
        featureList.add(new AnnotationFeature("NamedEntity", neName));
        when(annoService.listAnnotationFeature(layer)).thenReturn(featureList);
     
    }

    @Test
    public void testCalculateVisibilityNoRecords() throws Exception
    {
        List<AnnotationSuggestion> expectedSuggestions = null; // TODO fill
        when(recordService.listRecords(user, layer)).thenReturn(null);

        CAS cas = getTestCas();
        Collection<SuggestionGroup> suggestions = getTestSuggestionGroup();
        PredictionTask.calculateVisibility(recordService, annoService, cas, user, layer,
                suggestions, 0, 25);

        List<AnnotationSuggestion> visibleSuggestions = suggestions.stream()
                .flatMap(SuggestionGroup::stream).filter(AnnotationSuggestion::isVisible)
                .collect(Collectors.toList());
        
        assertThat(visibleSuggestions).as("Visible suggestions are visible.")
                .allMatch(s -> expectedSuggestions.contains(s));
    }

    private Collection<SuggestionGroup> getTestSuggestionGroup()
    {
        // TODO Auto-generated method stub
        return null;
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
