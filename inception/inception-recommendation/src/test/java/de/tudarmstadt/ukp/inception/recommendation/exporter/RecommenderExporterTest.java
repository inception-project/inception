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
package de.tudarmstadt.ukp.inception.recommendation.exporter;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.NEW;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.MAX_RECOMMENDATIONS_CAP;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.MAX_RECOMMENDATIONS_DEFAULT;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

@ExtendWith(MockitoExtension.class)
public class RecommenderExporterTest
{
    private @Mock AnnotationSchemaService annotationService;
    private @Mock RecommendationService recommendationService;
    private Project sourceProject;
    private Project targetProject;
    private AnnotationLayer layer;

    private RecommenderExporter sut;

    @BeforeEach
    public void setUp()
    {
        layer = new AnnotationLayer();
        layer.setName("Layer");

        sourceProject = Project.builder() //
                .withId(1l) //
                .withName("Test Project") //
                .build();

        targetProject = Project.builder() //
                .withId(2l) //
                .withName("Test Project") //
                .build();

        when(annotationService.findLayer(sourceProject, layer.getName())).thenReturn(layer);
        when(annotationService.getFeature(eq("Feature 1"), any(AnnotationLayer.class)))
                .thenReturn(buildFeature("1"));

        sut = new RecommenderExporter(annotationService, recommendationService);
    }

    @Test
    public void thatExportingWorks()
    {
        when(annotationService.getFeature(eq("Feature 2"), any(AnnotationLayer.class)))
                .thenReturn(buildFeature("2"));
        when(annotationService.getFeature(eq("Feature 3"), any(AnnotationLayer.class)))
                .thenReturn(buildFeature("3"));
        when(annotationService.getFeature(eq("Feature 4"), any(AnnotationLayer.class)))
                .thenReturn(buildFeature("4"));
        when(recommendationService.listRecommenders(sourceProject))
                .thenReturn(recommenders(sourceProject));

        // Export the project and import it again
        var captor = runExportImportAndFetchRecommenders();

        // Check that after re-importing the exported projects, they are identical to the original
        assertThat(captor.getAllValues()) //
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrderElementsOf(recommenders(targetProject));
    }

    @Test
    public void thatMaxRecommendationCapIsEnforcedOnImport()
    {
        var recommender = buildRecommender(sourceProject, "1");
        recommender.setAlwaysSelected(true);
        recommender.setEnabled(true);
        recommender.setThreshold(.1);
        recommender.setSkipEvaluation(true);
        recommender.setMaxRecommendations(1000);

        when(recommendationService.listRecommenders(sourceProject)).thenReturn(asList(recommender));

        // Export the project and import it again
        var captor = runExportImportAndFetchRecommenders();

        // Check that after re-importing the exported projects, they are identical to the original
        assertThat(captor.getAllValues()).hasSize(1);
        assertThat(captor.getAllValues().get(0))
                .matches(rec -> rec.getMaxRecommendations() == MAX_RECOMMENDATIONS_CAP);
    }

    @Test
    public void thatMissingMaxRecommendationIsSetToDefault()
    {
        var recommender = buildRecommender(sourceProject, "1");
        recommender.setAlwaysSelected(true);
        recommender.setEnabled(true);
        recommender.setThreshold(.1);
        recommender.setSkipEvaluation(true);
        recommender.setMaxRecommendations(0);

        when(recommendationService.listRecommenders(sourceProject)).thenReturn(asList(recommender));

        // Export the project and import it again
        var captor = runExportImportAndFetchRecommenders();

        // Check that after re-importing the exported projects, they are identical to the original
        assertThat(captor.getAllValues()).hasSize(1);
        assertThat(captor.getAllValues().get(0)) //
                .hasFieldOrPropertyWithValue("maxRecommendations", MAX_RECOMMENDATIONS_DEFAULT);
    }

    private ArgumentCaptor<Recommender> runExportImportAndFetchRecommenders()
    {
        // Export the project
        var exportRequest = new FullProjectExportRequest(sourceProject, null, false);
        var monitor = new ProjectExportTaskMonitor(sourceProject, null, "test",
                exportRequest.getFilenamePrefix());
        var exportedProject = new ExportedProject();
        var file = mock(ZipOutputStream.class);

        sut.exportData(exportRequest, monitor, exportedProject, file);

        // Import the project again
        var captor = ArgumentCaptor.forClass(Recommender.class);
        doNothing().when(recommendationService).createOrUpdateRecommender(captor.capture());

        var importRequest = ProjectImportRequest.builder() //
                .withCreateMissingUsers(true) //
                .withImportPermissions(true) //
                .build();
        var zipFile = mock(ZipFile.class);
        sut.importData(importRequest, targetProject, exportedProject, zipFile);

        return captor;
    }

    private List<Recommender> recommenders(Project aProject)
    {
        var recommender1 = buildRecommender(aProject, "1");
        recommender1.setAlwaysSelected(true);
        recommender1.setEnabled(true);
        recommender1.setThreshold(.1);
        recommender1.setSkipEvaluation(true);
        recommender1.setMaxRecommendations(3);
        recommender1.setStatesIgnoredForTraining(Set.of(NEW, IN_PROGRESS, FINISHED));

        var recommender2 = buildRecommender(aProject, "2");
        recommender2.setAlwaysSelected(false);
        recommender2.setEnabled(false);
        recommender2.setThreshold(.2);
        recommender2.setSkipEvaluation(false);
        recommender2.setMaxRecommendations(4);
        recommender2.setStatesIgnoredForTraining(Set.of(NEW, IN_PROGRESS));

        var recommender3 = buildRecommender(aProject, "3");
        recommender3.setAlwaysSelected(true);
        recommender3.setEnabled(false);
        recommender3.setThreshold(.3);
        recommender3.setSkipEvaluation(false);
        recommender3.setMaxRecommendations(5);
        recommender3.setStatesIgnoredForTraining(Set.of(AnnotationDocumentState.values()));

        var recommender4 = buildRecommender(aProject, "4");
        recommender4.setAlwaysSelected(false);
        recommender4.setEnabled(true);
        recommender4.setThreshold(.4);
        recommender4.setSkipEvaluation(true);
        recommender4.setMaxRecommendations(6);
        recommender4.setStatesIgnoredForTraining(Set.of());

        return asList(recommender1, recommender2, recommender3, recommender4);
    }

    private AnnotationFeature buildFeature(String id)
    {
        var feature = new AnnotationFeature();
        feature.setName("Feature " + id);
        return feature;
    }

    private Recommender buildRecommender(Project aProject, String id)
    {
        var feature = buildFeature(id);

        var recommender = new Recommender();
        recommender.setFeature(feature);
        recommender.setName("Recommender " + id);
        recommender.setTool("Tool " + id);
        recommender.setTraits("Traits " + id);
        recommender.setLayer(layer);
        recommender.setProject(aProject);
        return recommender;
    }
}
