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
package de.tudarmstadt.ukp.inception.curation.export;

import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.CURATION_USER;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.xmi.XmiFormatSupport;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageServiceImpl;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageBackupProperties;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageCachePropertiesImpl;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStoragePropertiesImpl;
import de.tudarmstadt.ukp.inception.annotation.storage.driver.CasStorageDriver;
import de.tudarmstadt.ukp.inception.annotation.storage.driver.filesystem.FileSystemCasStorageDriver;
import de.tudarmstadt.ukp.inception.export.DocumentImportExportServiceImpl;
import de.tudarmstadt.ukp.inception.export.config.DocumentImportExportServiceProperties;
import de.tudarmstadt.ukp.inception.export.config.DocumentImportExportServicePropertiesImpl;
import de.tudarmstadt.ukp.inception.project.export.ProjectExportServiceImpl;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

@ExtendWith(MockitoExtension.class)
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
        workFolder = tempFolder;

        project = new Project();
        project.setId(1l);
        project.setName("Test Project");

        DocumentImportExportServiceProperties properties = new DocumentImportExportServicePropertiesImpl();

        repositoryProperties = new RepositoryProperties();
        repositoryProperties.setPath(workFolder);

        CasStorageDriver driver = new FileSystemCasStorageDriver(repositoryProperties,
                new CasStorageBackupProperties(), new CasStoragePropertiesImpl());

        casStorageService = spy(new CasStorageServiceImpl(driver, new CasStorageCachePropertiesImpl(), null,
                schemaService));

        importExportSerivce = new DocumentImportExportServiceImpl(repositoryProperties,
                asList(new XmiFormatSupport()), casStorageService, schemaService, properties);

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

        verify(documentService, atLeastOnce()).importCas(sourceDocCaptor.capture(),
                usernameCaptor.capture(), any());

        List<Pair<SourceDocument, String>> importedCases = new ArrayList<>();
        List<SourceDocument> docs = sourceDocCaptor.getAllValues();
        List<String> users = usernameCaptor.getAllValues();
        for (int i = 0; i < docs.size(); i++) {
            importedCases.add(Pair.of(docs.get(i), users.get(i)));
        }

        return importedCases;
    }
}
