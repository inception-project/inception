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
package de.tudarmstadt.ukp.clarin.webanno.api.dao.export.exporters;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CURATION_USER;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.DocumentImportExportServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.config.BackupProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.config.CasStoragePropertiesImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.docimexport.config.DocumentImportExportServiceProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.docimexport.config.DocumentImportExportServicePropertiesImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.export.ProjectExportServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.xmi.XmiFormatSupport;

public class CuratedDocumentsExporterTest
{
    public @TempDir File tempFolder;

    private RepositoryProperties repositoryProperties;
    private DocumentImportExportService importExportSerivce;
    private CasStorageService casStorageService;

    private @Mock DocumentService documentService;
    private @Mock AnnotationSchemaService schemaService;

    private Project project;
    private File workFolder;
    private long nextDocId = 1;

    private CuratedDocumentsExporter sut;

    @BeforeEach
    public void setUp() throws Exception
    {
        initMocks(this);

        workFolder = tempFolder;

        project = new Project();
        project.setId(1l);
        project.setName("Test Project");

        DocumentImportExportServiceProperties properties = new DocumentImportExportServicePropertiesImpl();

        repositoryProperties = new RepositoryProperties();
        repositoryProperties.setPath(workFolder);

        casStorageService = spy(new CasStorageServiceImpl(null, schemaService, repositoryProperties,
                new CasStoragePropertiesImpl(), new BackupProperties()));

        importExportSerivce = new DocumentImportExportServiceImpl(repositoryProperties,
                asList(new XmiFormatSupport()), casStorageService, schemaService, properties);

        // documentService.getCasFile() is just a stupid wrapper around storageService.getCasFile()
        // and it is easiest we emulate it here
        when(documentService.getCasFile(any(), any())).thenAnswer(invocation -> {
            return casStorageService.getCasFile(invocation.getArgument(0, SourceDocument.class),
                    invocation.getArgument(1, String.class));
        });

        // Dynamically generate a SourceDocument with an incrementing ID when asked for one
        when(documentService.getSourceDocument(any(), any())).then(invocation -> {
            SourceDocument doc = new SourceDocument();
            doc.setId(nextDocId++);
            doc.setProject(invocation.getArgument(0, Project.class));
            doc.setName(invocation.getArgument(1, String.class));
            return doc;
        });

        sut = new CuratedDocumentsExporter(documentService, importExportSerivce);
    }

    @Test
    public void thatImportingAnnotationProjectWorks_3_6_1() throws Exception
    {
        // Export the project and import it again
        List<Pair<SourceDocument, String>> imported = runImportAndFetchDocuments(new ZipFile(
                "src/test/resources/exports/Export+Test+-+Curated+annotation+project_3_6_1.zip"));

        // Check that the curation for the document in the project is imported
        assertThat(imported).extracting(p -> p.getKey().getName())
                .containsExactlyInAnyOrder("example_sentence.txt");
        assertThat(imported).extracting(Pair::getValue).containsExactlyInAnyOrder(CURATION_USER);
    }

    private List<Pair<SourceDocument, String>> runImportAndFetchDocuments(ZipFile aZipFile)
        throws Exception
    {
        ArgumentCaptor<SourceDocument> sourceDocCaptor = ArgumentCaptor
                .forClass(SourceDocument.class);
        ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);

        // Import the project again
        ExportedProject exProject = ProjectExportServiceImpl.loadExportedProject(aZipFile);
        ProjectImportRequest importRequest = new ProjectImportRequest(true);
        sut.importData(importRequest, project, exProject, aZipFile);

        verify(casStorageService, atLeastOnce()).getCasFile(sourceDocCaptor.capture(),
                usernameCaptor.capture());

        List<Pair<SourceDocument, String>> importedCases = new ArrayList<>();
        List<SourceDocument> docs = sourceDocCaptor.getAllValues();
        List<String> users = usernameCaptor.getAllValues();
        for (int i = 0; i < docs.size(); i++) {
            importedCases.add(Pair.of(docs.get(i), users.get(i)));
        }

        return importedCases;
    }
}
