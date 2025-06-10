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
package de.tudarmstadt.ukp.inception.documents.exporters;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.documents.DocumentStorageServiceImpl;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.DocumentStorageService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryPropertiesImpl;

@ExtendWith(MockitoExtension.class)
public class SourceDocumentExporterTest
{
    private RepositoryProperties repositoryProperties;

    private @Mock DocumentService documentService;
    private DocumentStorageService documentStorageService;

    private Project project;

    private SourceDocumentExporter sut;

    @BeforeEach
    public void setUp() throws Exception
    {
        project = Project.builder() //
                .withId(1l) //
                .withName("Test Project") //
                .build();

        repositoryProperties = new RepositoryPropertiesImpl();
        documentStorageService = new DocumentStorageServiceImpl(repositoryProperties);

        when(documentService.listSourceDocuments(any())).then(invocation -> {
            return sourceDocuments();
        });

        sut = new SourceDocumentExporter(documentService, documentStorageService,
                repositoryProperties);
    }

    @Test
    public void thatExportingAndImportingWorks(@TempDir File sourceWorkDir,
            @TempDir File targetWorkDir, @TempDir File stage)
        throws Exception
    {
        repositoryProperties.setPath(sourceWorkDir);

        // Prepare some source files
        for (var doc : sourceDocuments()) {
            try (var is = IOUtils.toInputStream(doc.getName(), UTF_8)) {
                documentStorageService.writeSourceDocumentFile(doc, is);
            }
        }

        // Export the source files
        var exportRequest = new FullProjectExportRequest(project, null, false);
        var monitor = mock(ProjectExportTaskMonitor.class);
        var exProject = new ExportedProject();
        var exportFile = new File(stage, "export.zip");
        try (var zos = new ZipOutputStream(new FileOutputStream(exportFile))) {
            sut.exportData(exportRequest, monitor, exProject, zos);
        }

        // Import the project again
        repositoryProperties.setPath(targetWorkDir);
        var importRequest = ProjectImportRequest.builder() //
                .withCreateMissingUsers(true) //
                .withImportPermissions(true) //
                .build();
        try (var zipFile = new ZipFile(exportFile)) {
            sut.importData(importRequest, project, exProject, zipFile);
        }

        var sourceFiles = listFiles(sourceWorkDir, null, true).stream()
                .map(f -> sourceWorkDir.toPath().relativize(f.toPath())).toList();
        var targetFiles = listFiles(targetWorkDir, null, true).stream()
                .map(f -> targetWorkDir.toPath().relativize(f.toPath())).toList();

        assertThat(targetFiles) //
                .isNotEmpty() //
                .hasSameSizeAs(sourceDocuments()) //
                .containsExactlyInAnyOrderElementsOf(sourceFiles);
    }

    private List<SourceDocument> sourceDocuments()
    {
        return asList(
                SourceDocument.builder().withId(1l).withProject(project).withName("1.txt").build(),
                SourceDocument.builder().withId(2l).withProject(project).withName("2.txt").build());
    }
}
