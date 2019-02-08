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
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.exporter;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.uima.cas.CAS;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.gazeteer.GazeteerService;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.model.Gazeteer;

public class GazeteerExporterTest
{
    private @Mock GazeteerService gazeteerService;
    private @Mock RecommendationService recommendationService;

    private Project sourceProject;
    private AnnotationLayer sourceLayer;
    private AnnotationFeature sourceFeature;
    private Recommender sourceRecommender;
    
    private Project targetProject;
    private AnnotationLayer targetLayer;
    private AnnotationFeature targetFeature;
    private Recommender targetRecommender;

    public @Rule TemporaryFolder temporaryFolder = new TemporaryFolder();

    private GazeteerExporter sut;

    @Before
    public void setUp() throws Exception
    {
        initMocks(this);

        sourceProject = new Project();
        sourceProject.setId(1l);
        sourceProject.setName("Test Project");
        sourceProject.setMode(WebAnnoConst.PROJECT_TYPE_ANNOTATION);

        sourceLayer = new AnnotationLayer("span", "span", SPAN_TYPE, sourceProject, false, TOKENS);
        sourceFeature = new AnnotationFeature(sourceProject, sourceLayer, "value", "value",
                CAS.TYPE_NAME_STRING);
        sourceRecommender = new Recommender("rec1", sourceLayer);
        sourceRecommender.setFeature(sourceFeature);
        
        targetProject = new Project();
        targetProject.setId(2l);
        targetProject.setName("Test Project");
        targetProject.setMode(WebAnnoConst.PROJECT_TYPE_ANNOTATION);

        targetLayer = new AnnotationLayer("span", "span", SPAN_TYPE, sourceProject, false, TOKENS);
        targetFeature = new AnnotationFeature(sourceProject, targetLayer, "value", "value",
                CAS.TYPE_NAME_STRING);
        targetRecommender = new Recommender("rec1", targetLayer);
        targetRecommender.setFeature(targetFeature);
        
        when(gazeteerService.listGazeteers(sourceRecommender)).thenReturn(gazeteers());
        
        when(gazeteerService.getGazeteerFile(Mockito.any())).thenAnswer(invocation -> {
            Gazeteer gaz = invocation.getArgument(0);
            File gazFile = temporaryFolder.newFile(gaz.getId() + ".txt");
            String data = "John\tVAL" + gaz.getId();
            FileUtils.writeStringToFile(gazFile, data, UTF_8);
            return gazFile;
        });

        when(recommendationService.listRecommenders(sourceProject))
                .thenReturn(asList(sourceRecommender));
        
        when(recommendationService.getRecommender(any(), any()))
                .thenReturn(Optional.of(targetRecommender));

        sut = new GazeteerExporter(recommendationService, gazeteerService);
    }

    @Test
    public void thatExportingWorks() throws Exception
    {
        // Export the project
        ProjectExportRequest exportRequest = new ProjectExportRequest();
        exportRequest.setProject(sourceProject);
        ExportedProject exportedProject = new ExportedProject();
        sut.exportData(exportRequest, exportedProject, temporaryFolder.getRoot());

        // Import the project again
        ArgumentCaptor<Gazeteer> gazeteerCaptor = ArgumentCaptor.forClass(Gazeteer.class);
        doNothing().when(gazeteerService).createOrUpdateGazeteer(gazeteerCaptor.capture());

        ArgumentCaptor<Gazeteer> gazeteerFileCaptor = ArgumentCaptor.forClass(Gazeteer.class);
        doNothing().when(gazeteerService).importGazeteerFile(gazeteerFileCaptor.capture(), any());

        ProjectImportRequest importRequest = new ProjectImportRequest(true);
        ZipFile zipFile = mock(ZipFile.class);

        sut.importData(importRequest, targetProject, exportedProject, zipFile);

        assertThat(gazeteerCaptor.getAllValues())
                .usingElementComparatorIgnoringFields("id", "project")
                .containsExactlyInAnyOrderElementsOf(gazeteers());
        
        assertThat(gazeteerFileCaptor.getAllValues())
                .usingElementComparatorIgnoringFields("id", "project")
                .containsExactlyInAnyOrderElementsOf(gazeteers());
    }

    private List<Gazeteer> gazeteers() throws Exception
    {
        Gazeteer gaz1 = new Gazeteer("gaz1", sourceRecommender);
        gaz1.setId(1l);

        Gazeteer gaz2 = new Gazeteer("gaz2", sourceRecommender);
        gaz2.setId(2l);

        return asList(gaz1, gaz2);
    }
}
