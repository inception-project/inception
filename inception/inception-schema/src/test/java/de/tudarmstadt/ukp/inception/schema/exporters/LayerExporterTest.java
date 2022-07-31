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
package de.tudarmstadt.ukp.inception.schema.exporters;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SENTENCES;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.ANY_OVERLAP;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.SPAN_TYPE;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ValidationMode;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

@ExtendWith(MockitoExtension.class)
public class LayerExporterTest
{
    public @TempDir File tempFolder;

    private @Mock AnnotationSchemaService annotationService;

    private Project project;
    private File workFolder;

    private LayerExporter sut;

    @BeforeEach
    public void setUp() throws Exception
    {
        project = new Project();
        project.setId(1l);
        project.setName("Test Project");

        workFolder = tempFolder;

        when(annotationService.listAnnotationLayer(any())).thenReturn(layers());

        sut = new LayerExporter(annotationService);
    }

    @Test
    public void thatExportingWorks() throws Exception
    {
        // Export the project and import it again
        ArgumentCaptor<AnnotationLayer> captor = runExportImportAndFetchLayers();

        // Check that after re-importing the exported projects, they are identical to the original
        assertThat(captor.getAllValues()).usingElementComparatorIgnoringFields("id")
                .containsExactlyInAnyOrderElementsOf(layers());
    }

    private List<AnnotationLayer> layers()
    {
        AnnotationLayer layer1 = new AnnotationLayer("webanno.custom.Span", "Span", SPAN_TYPE,
                project, false, SINGLE_TOKEN, NO_OVERLAP);
        layer1.setValidationMode(ValidationMode.ALWAYS);

        AnnotationLayer layer2 = new AnnotationLayer("webanno.custom.Span2", "Span2", SPAN_TYPE,
                project, false, SENTENCES, NO_OVERLAP);
        layer2.setValidationMode(ValidationMode.NEVER);

        AnnotationLayer layer3 = new AnnotationLayer("webanno.custom.Relation", "Relation",
                RELATION_TYPE, project, true, TOKENS, ANY_OVERLAP);

        return asList(layer1, layer2, layer3);
    }

    private ArgumentCaptor<AnnotationLayer> runExportImportAndFetchLayers() throws Exception
    {
        // Export the project
        FullProjectExportRequest exportRequest = new FullProjectExportRequest(project, null, false);
        ProjectExportTaskMonitor monitor = new ProjectExportTaskMonitor(project, null, "test");
        ExportedProject exportedProject = new ExportedProject();

        sut.exportData(exportRequest, monitor, exportedProject, workFolder);

        // Import the project again
        ArgumentCaptor<AnnotationLayer> captor = ArgumentCaptor.forClass(AnnotationLayer.class);
        doNothing().when(annotationService).createOrUpdateLayer(captor.capture());

        ProjectImportRequest importRequest = new ProjectImportRequest(true);
        ZipFile zipFile = mock(ZipFile.class);
        sut.importData(importRequest, project, exportedProject, zipFile);

        return captor;
    }
}
