/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api.dao.export.exporters;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CURATION_USER;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.BackupProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.CasStorageServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.ImportExportServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.xmi.XmiFormatSupport;

public class CuratedDocumentsExporterTest
{
    public @Rule TemporaryFolder tempFolder = new TemporaryFolder();
    
    private RepositoryProperties repositoryProperties;
    private BackupProperties backupProperties;
    private ImportExportService importExportSerivce;
    private CasStorageService casStorageService;
    
    private @Mock DocumentService documentService;
    private @Mock AnnotationSchemaService schemaService;
    
    private Project project;
    private File workFolder;
    private long nextDocId = 1;

    private CuratedDocumentsExporter sut;

    @Before
    public void setUp() throws Exception
    {
        initMocks(this);

        workFolder = tempFolder.newFolder();

        project = new Project();
        project.setId(1l);
        project.setName("Test Project");
        project.setMode(WebAnnoConst.PROJECT_TYPE_ANNOTATION);

        backupProperties = new BackupProperties();

        repositoryProperties = new RepositoryProperties();
        repositoryProperties.setPath(workFolder);

        casStorageService = spy(new CasStorageServiceImpl(null, schemaService, repositoryProperties,
                backupProperties));
        
        importExportSerivce = new ImportExportServiceImpl(repositoryProperties,
                asList(new XmiFormatSupport()), casStorageService, schemaService);
        
        when(documentService.getCasFile(any(), any()))
                .thenAnswer(invocation -> {
                    return casStorageService.getCasFile(
                            invocation.getArgument(0, SourceDocument.class),
                            invocation.getArgument(1, String.class));
                });
        
        when(documentService.getSourceDocument(any(), any())).then(invocation -> {
            SourceDocument doc = new SourceDocument();
            doc.setId(nextDocId++);
            doc.setProject(invocation.getArgument(0, Project.class));
            doc.setName(invocation.getArgument(1, String.class));
            return doc;
        });
                
        //when(schemaService.listAnnotationLayer(any())).thenReturn(layers());
        
        sut = new CuratedDocumentsExporter(documentService, importExportSerivce);
    }

    @Test
    public void thatImportWorks() throws Exception
    {
        // Export the project and import it again
        List<Pair<SourceDocument, String>> imported = runImportAndFetchDocuments(new ZipFile(
                "src/test/resources/exports/Export+Test+-+Curated+annotation+project_2019-10-27_0910.zip"));

        // Check that after re-importing the exported projects, they are identical to the original
        assertThat(imported).extracting(Pair::getValue)
                .containsExactlyInAnyOrder(CURATION_USER);
    }
    
    private List<Pair<SourceDocument, String>> runImportAndFetchDocuments(ZipFile aZpiFile)
        throws Exception
    {
        ArgumentCaptor<SourceDocument> sourceDocCaptor = ArgumentCaptor
                .forClass(SourceDocument.class);
        ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);

        // Import the project again
        ProjectImportRequest importRequest = new ProjectImportRequest(true);
        sut.importData(importRequest, project, null, aZpiFile);

        verify(casStorageService).getCasFile(sourceDocCaptor.capture(), usernameCaptor.capture());

        List<Pair<SourceDocument, String>> importedCases = new ArrayList<>();
        List<SourceDocument> docs = sourceDocCaptor.getAllValues();
        List<String> users = usernameCaptor.getAllValues();
        for (int i = 0; i < docs.size(); i++) {
            importedCases.add(Pair.of(docs.get(i), users.get(i)));
        }
        
        return importedCases;
    }
}
