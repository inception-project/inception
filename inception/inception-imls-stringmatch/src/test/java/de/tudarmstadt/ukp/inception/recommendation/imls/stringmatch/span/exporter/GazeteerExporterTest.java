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
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.exporter;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazeteer.GazeteerService;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazeteer.exporter.GazeteerExporter;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazeteer.model.Gazeteer;

@ExtendWith(MockitoExtension.class)
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

    public @TempDir File temporaryFolder;

    private GazeteerExporter sut;

    @BeforeEach
    public void setUp() throws Exception
    {
        sourceProject = Project.builder() //
                .withId(1l) //
                .withName("Test Project") //
                .build();

        targetProject = Project.builder() //
                .withId(2l) //
                .withName("Test Project") //
                .build();

        sourceLayer = new AnnotationLayer("span", "span", SpanLayerSupport.TYPE, sourceProject,
                false, TOKENS, NO_OVERLAP);
        sourceFeature = new AnnotationFeature(sourceProject, sourceLayer, "value", "value",
                TYPE_NAME_STRING);
        sourceRecommender = new Recommender("rec1", sourceLayer);
        sourceRecommender.setFeature(sourceFeature);

        targetLayer = new AnnotationLayer("span", "span", SpanLayerSupport.TYPE, sourceProject,
                false, TOKENS, NO_OVERLAP);
        targetFeature = new AnnotationFeature(sourceProject, targetLayer, "value", "value",
                TYPE_NAME_STRING);
        targetRecommender = new Recommender("rec1", targetLayer);
        targetRecommender.setFeature(targetFeature);

        when(gazeteerService.listGazeteers(sourceRecommender)).thenReturn(gazeteers());

        when(gazeteerService.getGazeteerFile(Mockito.any())).thenAnswer(invocation -> {
            Gazeteer gaz = invocation.getArgument(0);
            var gazFile = temporaryFolder.toPath().resolve(gaz.getId() + ".txt").toFile();
            var data = "John\tVAL" + gaz.getId();
            FileUtils.writeStringToFile(gazFile, data, UTF_8);
            return gazFile;
        });

        when(recommendationService.listRecommenders(sourceProject))
                .thenReturn(List.of(sourceRecommender));

        when(recommendationService.getRecommender(any(), any()))
                .thenReturn(Optional.of(targetRecommender));

        sut = new GazeteerExporter(recommendationService, gazeteerService);
    }

    @Test
    public void thatExportingWorks() throws Exception
    {
        // Export the project
        var exportRequest = new FullProjectExportRequest(sourceProject, null, false);
        var monitor = new ProjectExportTaskMonitor(sourceProject, null, "test",
                exportRequest.getFilenamePrefix());
        var exportedProject = new ExportedProject();
        var stage = Mockito.mock(ZipOutputStream.class);
        sut.exportData(exportRequest, monitor, exportedProject, stage);

        // Import the project again
        var gazeteerCaptor = ArgumentCaptor.forClass(Gazeteer.class);
        doNothing().when(gazeteerService).createOrUpdateGazeteer(gazeteerCaptor.capture());

        var gazeteerFileCaptor = ArgumentCaptor.forClass(Gazeteer.class);
        doNothing().when(gazeteerService).importGazeteerFile(gazeteerFileCaptor.capture(), any());

        var importRequest = ProjectImportRequest.builder() //
                .withCreateMissingUsers(true) //
                .withImportPermissions(true) //
                .build();
        var zipFile = mock(ZipFile.class);

        sut.importData(importRequest, targetProject, exportedProject, zipFile);

        assertThat(gazeteerCaptor.getAllValues())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "project")
                .containsExactlyInAnyOrderElementsOf(gazeteers());

        assertThat(gazeteerFileCaptor.getAllValues())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "project")
                .containsExactlyInAnyOrderElementsOf(gazeteers());
    }

    private List<Gazeteer> gazeteers() throws Exception
    {
        var gaz1 = new Gazeteer("gaz1", sourceRecommender);
        gaz1.setId(1l);

        var gaz2 = new Gazeteer("gaz2", sourceRecommender);
        gaz2.setId(2l);

        return asList(gaz1, gaz2);
    }
}
