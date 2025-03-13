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
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.SPAN_TYPE;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
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
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ValidationMode;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

@ExtendWith(MockitoExtension.class)
public class LayerExporterTest
{
    private @Mock AnnotationSchemaService annotationService;

    private LayerExporter sut;

    private Project sourceProject;
    private Project targetProject;

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

        sut = new LayerExporter(annotationService);
    }

    @Test
    public void thatExportingWorks() throws Exception
    {
        when(annotationService.listAnnotationLayer(any())).thenReturn(layers(sourceProject));

        // Export the project and import it again
        // Export the project
        var exportRequest = new FullProjectExportRequest(sourceProject, null, false);
        var monitor = new ProjectExportTaskMonitor(sourceProject, null, "test",
                exportRequest.getFilenamePrefix());
        var exportedProject = new ExportedProject();
        var stage = mock(ZipOutputStream.class);

        sut.exportData(exportRequest, monitor, exportedProject, stage);

        // Import the project again
        var captor = ArgumentCaptor.forClass(AnnotationLayer.class);
        doNothing().when(annotationService).createOrUpdateLayer(captor.capture());

        var importRequest = ProjectImportRequest.builder() //
                .withCreateMissingUsers(true) //
                .withImportPermissions(true) //
                .build();
        var zipFile = mock(ZipFile.class);
        sut.importData(importRequest, targetProject, exportedProject, zipFile);

        // Check that after re-importing the exported projects, they are identical to the original
        assertThat(captor.getAllValues()) //
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
                .containsExactlyInAnyOrderElementsOf(layers(targetProject));
    }

    private List<AnnotationLayer> layers(Project project)
    {
        var layer1 = new AnnotationLayer("webanno.custom.Span", "Span", SPAN_TYPE, project, false,
                SINGLE_TOKEN, NO_OVERLAP);
        layer1.setValidationMode(ValidationMode.ALWAYS);

        var layer2 = new AnnotationLayer("webanno.custom.Span2", "Span2", SPAN_TYPE, project, false,
                SENTENCES, NO_OVERLAP);
        layer2.setValidationMode(ValidationMode.NEVER);

        var layer3 = new AnnotationLayer("webanno.custom.Relation", "Relation", RELATION_TYPE,
                project, true, TOKENS, ANY_OVERLAP);

        return asList(layer1, layer2, layer3);
    }

}
