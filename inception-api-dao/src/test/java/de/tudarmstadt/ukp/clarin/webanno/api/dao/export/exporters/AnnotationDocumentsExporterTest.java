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

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.INITIAL_CAS_PSEUDO_USER;
import static java.util.Arrays.asList;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.Mock;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.BackupProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.CasStorageServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.ImportExportServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.export.ProjectExportServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedSourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.xmi.XmiFormatSupport;

public class AnnotationDocumentsExporterTest
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

    private AnnotationDocumentExporter sut;

    @Before
    public void setUp() throws Exception
    {
        initMocks(this);

        workFolder = tempFolder.newFolder();

        project = new Project();
        project.setId(1l);
        project.setName("Test Project");

        backupProperties = new BackupProperties();

        repositoryProperties = new RepositoryProperties();
        repositoryProperties.setPath(workFolder);

        casStorageService = new CasStorageServiceImpl(null, schemaService, repositoryProperties,
                backupProperties);

        importExportSerivce = new ImportExportServiceImpl(repositoryProperties,
                asList(new XmiFormatSupport()), casStorageService, schemaService);

        sut = new AnnotationDocumentExporter(documentService, null, importExportSerivce,
                repositoryProperties);
    }

    @Test
    public void thatImportingAnnotationProjectWorks_3_6_1() throws Exception
    {
        // Export the project and import it again
        List<Pair<SourceDocument, String>> imported = runImportAndFetchDocuments(new ZipFile(
                "src/test/resources/exports/Export+Test+-+Curated+annotation+project_3_6_1.zip"));

        // Check that the curation for the document in the project is imported
        assertThat(imported).extracting(p -> p.getKey().getName())
                .containsExactlyInAnyOrder("example_sentence.txt", "example_sentence.txt");
        assertThat(imported).extracting(Pair::getValue)
                .containsExactlyInAnyOrder(INITIAL_CAS_PSEUDO_USER, "admin");
    }

    private List<Pair<SourceDocument, String>> runImportAndFetchDocuments(ZipFile aZipFile)
        throws Exception
    {
        // Import the project again
        ExportedProject exProject = ProjectExportServiceImpl.loadExportedProject(aZipFile);

        // Provide source documents based on data in the exported project
        when(documentService.listSourceDocuments(any())).then(invocation -> {
            long i = 1;
            List<SourceDocument> docs = new ArrayList<>();
            for (ExportedSourceDocument exDoc : exProject.getSourceDocuments()) {
                SourceDocument doc = new SourceDocument();
                doc.setId(i++);
                doc.setName(exDoc.getName());
                doc.setProject(project);
                docs.add(doc);
            }

            return docs;
        });

        ProjectImportRequest importRequest = new ProjectImportRequest(true);
        sut.importData(importRequest, project, exProject, aZipFile);

        List<Pair<SourceDocument, String>> importedCases = new ArrayList<>();
        for (SourceDocument doc : documentService.listSourceDocuments(project)) {
            File annFolder = casStorageService.getAnnotationFolder(doc);
            for (File serFile : annFolder.listFiles((dir, name) -> name.endsWith(".ser"))) {
                importedCases.add(Pair.of(doc, removeExtension(serFile.getName())));
            }
        }

        return importedCases;
    }
}
