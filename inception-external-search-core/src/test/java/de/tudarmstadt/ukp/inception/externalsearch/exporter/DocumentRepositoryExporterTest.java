/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.inception.externalsearch.exporter;

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

import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchService;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;

public class DocumentRepositoryExporterTest {
    private @Mock ExternalSearchService externalSearchService;
    private Project project;
    
    private DocumentRepositoryExporter sut;
    
    @Before
    public void setUp()
    {
        initMocks(this);
        
        project = new Project();
        project.setName("Test Project");
        project.setMode(WebAnnoConst.PROJECT_TYPE_ANNOTATION);
        
        when(externalSearchService.listDocumentRepositories(project)).
                thenReturn(documentRepositories());
        
        sut = new DocumentRepositoryExporter(externalSearchService);
    }
    
    @Test
    public void thatExportingWorks()
    {
        
        // Export the project and import it again
        ArgumentCaptor<DocumentRepository> captor = runExportImportAndFetchDocumentRepositories();
        
        // Check that after re-importing the exported projects, they are identical to the original
        assertThat(captor.getAllValues())
                .usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrderElementsOf(documentRepositories());
    }
    
    private ArgumentCaptor<DocumentRepository> runExportImportAndFetchDocumentRepositories()
    {
        // Export the project
        ProjectExportRequest exportRequest = new ProjectExportRequest();
        exportRequest.setProject(project);
        ExportedProject exportedProject = new ExportedProject();
        File file = mock(File.class);
        
        sut.exportData(exportRequest, exportedProject, file);
        
        // Import the project again
        ArgumentCaptor<DocumentRepository> captor =
                ArgumentCaptor.forClass(DocumentRepository.class);
        doNothing().when(externalSearchService).createOrUpdateDocumentRepository(captor.capture());
        
        ProjectImportRequest importRequest = new ProjectImportRequest(true);
        ZipFile zipFile = mock(ZipFile.class);
        sut.importData(importRequest, project, exportedProject, zipFile);
        
        return captor;
    }
    
    private List<DocumentRepository> documentRepositories()
    {
        DocumentRepository dr1 = buildDocumentRepository(1L);
        DocumentRepository dr2 = buildDocumentRepository(2L);
        DocumentRepository dr3 = buildDocumentRepository(3L);
        DocumentRepository dr4 = buildDocumentRepository(4L);
        
        return Arrays.asList(dr1, dr2, dr3, dr4);
    }
    
    private DocumentRepository buildDocumentRepository(Long id)
    {
        DocumentRepository dr = new DocumentRepository();
        dr.setId(id);
        dr.setName("DocumentRepository " + id.toString());
        dr.setType("Type " + id.toString());
        dr.setProject(project);
        dr.setProperties("Properties " + id.toString());
        return dr;
    }
}
