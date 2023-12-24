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

import static de.tudarmstadt.ukp.inception.recommendation.service.Fixtures.getInvisibleSuggestions;
import static de.tudarmstadt.ukp.inception.recommendation.service.Fixtures.getVisibleSuggestions;
import static de.tudarmstadt.ukp.inception.recommendation.service.Fixtures.makeRelationSuggestionGroup;
import static java.util.Arrays.asList;
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

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.relation.RelationSuggestionSupport;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

@ExtendWith(MockitoExtension.class)
public class RelationSuggestionVisibilityCalculationTest
{
    private static final String TEST_USER = "Testuser";

    private @Mock AnnotationSchemaService annoService;
    private @Mock LearningRecordService learningRecordService;

    private Project project;
    private SourceDocument doc;
    private AnnotationLayer layer;
    private AnnotationFeature feature;

    private RelationSuggestionSupport sut;

    @BeforeEach
    public void setUp() throws Exception
    {
        layer = AnnotationLayer.builder().withId(42l).forJCasClass(Dependency.class).build();

        feature = AnnotationFeature.builder().withId(2l).withLayer(layer)
                .withName(Dependency._FeatName_DependencyType).withType(TYPE_NAME_STRING).build();

        project = Project.builder().withName("Test Project").build();

        doc = SourceDocument.builder().withId(12l).withName("doc").withProject(project).build();

        when(annoService.listSupportedFeatures(layer)).thenReturn(asList(feature));

        sut = new RelationSuggestionSupport(null, learningRecordService, null, annoService, null);
    }

    @Test
    public void testCalculateVisibilityNoRecordsAllHidden() throws Exception
    {
        doReturn(new ArrayList<>()).when(learningRecordService).listLearningRecords(TEST_USER,
                TEST_USER, layer);

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
        doReturn(new ArrayList<>()).when(learningRecordService).listLearningRecords(TEST_USER,
                TEST_USER, layer);

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
}
