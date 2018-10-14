/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipFile;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;

public class RecommenderExporterTest
{

    private @Mock AnnotationSchemaService annotationService;
    private @Mock RecommendationService recommendationService;
    private Project project;
    private AnnotationLayer layer;

    private RecommenderExporter sut;

    @Before
    public void setUp()
    {
        initMocks(this);

        layer = new AnnotationLayer();
        layer.setName("Layer");

        project = new Project();
        project.setName("Test Project");
        project.setMode(WebAnnoConst.PROJECT_TYPE_ANNOTATION);
        
        when(recommendationService.listRecommenders(project)).thenReturn(recommenders());

        when(annotationService.getLayer(layer.getName(), project)).thenReturn(layer);

        sut = new RecommenderExporter(annotationService, recommendationService);
    }

    @Test
    public void thatExportingWorks()
    {
        // Export the project
        ProjectExportRequest exportRequest = new ProjectExportRequest();
        exportRequest.setProject(project);
        ExportedProject exportedProject = new ExportedProject();
        File file = mock(File.class);

        sut.exportData(exportRequest, exportedProject, file);

        // Import the project again
        ArgumentCaptor<Recommender> captor = ArgumentCaptor.forClass(Recommender.class);
        doNothing().when(recommendationService).createOrUpdateRecommender(captor.capture());

        ProjectImportRequest importRequest = new ProjectImportRequest(true);
        ZipFile zipFile = mock(ZipFile.class);
        sut.importData(importRequest, project, exportedProject, zipFile);

        // Check that after reimporting the exported projects, they are identical to the original
        assertThat(captor.getAllValues())
                .usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrderElementsOf(recommenders());
    }

    private List<Recommender> recommenders()
    {
        Recommender recommender1 = buildRecommender("1");
        recommender1.setAlwaysSelected(true);
        recommender1.setEnabled(true);
        recommender1.setThreshold(1);

        Recommender recommender2 = buildRecommender("2");
        recommender2.setAlwaysSelected(false);
        recommender2.setEnabled(false);
        recommender2.setThreshold(2);

        Recommender recommender3 = buildRecommender("3");
        recommender3.setAlwaysSelected(true);
        recommender3.setEnabled(false);
        recommender3.setThreshold(3);

        Recommender recommender4 = buildRecommender("4");
        recommender4.setAlwaysSelected(false);
        recommender4.setEnabled(true);
        recommender4.setThreshold(4);

        return Arrays.asList(recommender1, recommender2, recommender3, recommender4);
    }

    private Recommender buildRecommender(String id)
    {
        Recommender recommender = new Recommender();
        recommender.setFeature("Feature " + id);
        recommender.setName("Recommender " + id);
        recommender.setTool("Tool " + id);
        recommender.setTraits("Traits " + id);
        recommender.setLayer(layer);
        recommender.setProject(project);
        return recommender;
    }

}
