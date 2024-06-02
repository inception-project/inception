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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;
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
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

@ExtendWith(MockitoExtension.class)
public class LearningRecordExporterTest
{
    private @Mock AnnotationSchemaService annotationService;
    private @Mock DocumentService documentService;
    private @Mock LearningRecordService learningRecordService;

    private Project project;
    private SourceDocument document;
    private AnnotationLayer layer;
    private AnnotationFeature feature;

    private LearningRecordExporter sut;

    @BeforeEach
    public void setUp()
    {
        project = Project.builder() //
                .withId(1l) //
                .withName("Test Project") //
                .build();

        document = SourceDocument.builder() //
                .withId(1l) //
                .withProject(project) //
                .withName("Document") //
                .build();

        layer = AnnotationLayer.builder() //
                .withId(1l) //
                .withProject(project) //
                .withName("Layer") //
                .build();

        feature = AnnotationFeature.builder() //
                .withId(1l) //
                .withProject(project) //
                .withName("feature1") //
                .build();

        when(annotationService.findLayer(project, layer.getName())).thenReturn(layer);
        when(annotationService.getFeature(eq(feature.getName()), eq(layer))).thenReturn(feature);
        when(documentService.getSourceDocument(project, document.getName())).thenReturn(document);

        sut = new LearningRecordExporter(annotationService, documentService, learningRecordService);
    }

    @Test
    public void thatExportingWorks()
    {
        when(learningRecordService.listLearningRecords(project)).thenReturn(records());

        // Export the project and import it again
        var captor = runExportImportAndFetchRecommenders();

        // Check that after re-importing the exported data is identical to the original
        assertThat(captor.getAllValues().stream().flatMap(Stream::of)) //
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "actionDate")
                .containsExactlyInAnyOrderElementsOf(records());
    }

    private ArgumentCaptor<LearningRecord[]> runExportImportAndFetchRecommenders()
    {
        // Export the project
        var exportRequest = new FullProjectExportRequest(project, null, false);
        var monitor = new ProjectExportTaskMonitor(project, null, "test");
        var exportedProject = new ExportedProject();
        var file = mock(ZipOutputStream.class);

        sut.exportData(exportRequest, monitor, exportedProject, file);

        // Import the project again
        var captor = ArgumentCaptor.forClass(LearningRecord[].class);
        doNothing().when(learningRecordService).createLearningRecords(captor.capture());

        var importRequest = new ProjectImportRequest(true);
        var zipFile = mock(ZipFile.class);
        sut.importData(importRequest, project, exportedProject, zipFile);

        return captor;
    }

    private List<LearningRecord> records()
    {
        var r1 = LearningRecord.builder() //
                .withAnnotation("label") //
                .withAnnotationFeature(feature) //
                .withChangeLocation(LearningRecordChangeLocation.MAIN_EDITOR) //
                .withId(1l) //
                .withLayer(layer) //
                .withOffsetBegin(10) //
                .withOffsetEnd(20) //
                .withOffsetBegin2(-1) //
                .withOffsetEnd2(-1) //
                .withSourceDocument(document) //
                .withSuggestionType(SpanLayerSupport.TYPE) //
                .withTokenText("0123456789") //
                .withUser("user1") //
                .withUserAction(LearningRecordUserAction.ACCEPTED) //
                .build();

        return asList(r1);
    }
}
