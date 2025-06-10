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
package de.tudarmstadt.ukp.inception.project.export;

import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.INITIAL_CAS_PSEUDO_USER;
import static java.util.Arrays.asList;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.diag.ChecksRegistry;
import de.tudarmstadt.ukp.clarin.webanno.diag.RepairsRegistry;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageServiceImpl;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageBackupProperties;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageCachePropertiesImpl;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStoragePropertiesImpl;
import de.tudarmstadt.ukp.inception.annotation.storage.driver.filesystem.FileSystemCasStorageDriver;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryPropertiesImpl;
import de.tudarmstadt.ukp.inception.export.DocumentImportExportServiceImpl;
import de.tudarmstadt.ukp.inception.export.config.DocumentImportExportServicePropertiesImpl;
import de.tudarmstadt.ukp.inception.io.xmi.XmiFormatSupport;
import de.tudarmstadt.ukp.inception.io.xmi.config.UimaFormatsPropertiesImpl.XmiFormatProperties;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.exporters.AnnotationDocumentExporter;

@ExtendWith(MockitoExtension.class)
public class AnnotationDocumentsExporterTest
{
    public @TempDir File tempFolder;

    private RepositoryPropertiesImpl repositoryProperties;
    private DocumentImportExportService importExportSerivce;
    private FileSystemCasStorageDriver driver;
    private CasStorageServiceImpl casStorageService;

    private @Mock DocumentService documentService;
    private @Mock AnnotationSchemaService schemaService;
    private @Mock ChecksRegistry checksRegistry;
    private @Mock RepairsRegistry repairsRegistry;
    private @Mock UserDao userService;

    private Project sourceProject;
    private Project targetProject;

    private AnnotationDocumentExporter sut;

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

        var properties = new DocumentImportExportServicePropertiesImpl();

        repositoryProperties = new RepositoryPropertiesImpl();
        repositoryProperties.setPath(tempFolder);

        driver = new FileSystemCasStorageDriver(repositoryProperties,
                new CasStorageBackupProperties(), new CasStoragePropertiesImpl());

        casStorageService = new CasStorageServiceImpl(driver, new CasStorageCachePropertiesImpl(),
                null, schemaService);

        var xmiFormatSupport = new XmiFormatSupport(new XmiFormatProperties());
        importExportSerivce = new DocumentImportExportServiceImpl(asList(xmiFormatSupport),
                casStorageService, schemaService, properties, checksRegistry, repairsRegistry,
                xmiFormatSupport);

        sut = new AnnotationDocumentExporter(documentService, userService, importExportSerivce,
                repositoryProperties);
    }

    @Test
    void thatExportingAndImportingAgainWorks() throws Exception
    {
        when(documentService.listAnnotationDocuments(any(Project.class))) //
                .thenReturn(annotationDocuments(sourceProject));
        when(documentService.listSourceDocuments(any(Project.class))) //
                .thenReturn(sourceDocuments(sourceProject));
        when(documentService.existsCas(any())) //
                .thenReturn(true);
        when(userService.get(any(String.class)))
                .thenAnswer(invocation -> new User(invocation.getArgument(0)));

        var exportFile = new File(tempFolder, "export.zip");

        // Export the project
        var exportRequest = new FullProjectExportRequest(sourceProject, null, false);
        var monitor = new ProjectExportTaskMonitor(sourceProject, null, "test",
                exportRequest.getFilenamePrefix());
        var exportedProject = new ExportedProject();

        try (var zos = new ZipOutputStream(new FileOutputStream(exportFile))) {
            sut.exportData(exportRequest, monitor, exportedProject, zos);
        }

        // Import the project again
        reset(documentService);
        when(documentService.listSourceDocuments(any(Project.class))) //
                .thenReturn(sourceDocuments(targetProject));

        var captor = ArgumentCaptor.forClass(AnnotationDocument.class);
        when(documentService.createOrUpdateAnnotationDocument(captor.capture())).thenReturn(null);

        var importRequest = ProjectImportRequest.builder().build();
        try (var zipFile = new ZipFile(exportFile)) {
            sut.importData(importRequest, targetProject, exportedProject, zipFile);
        }

        assertThat(captor.getAllValues()) //
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "project") //
                .containsExactlyElementsOf(annotationDocuments(targetProject));

        assertThat(tempFolder) //
                .isDirectoryRecursivelyContaining(
                        "glob:**/project/2/document/9/annotation/INITIAL_CAS.ser") //
                .isDirectoryRecursivelyContaining(
                        "glob:**/project/2/document/9/annotation/someuser.ser");
    }

    @Test
    void thatImportingAnnotationProjectWorks_3_6_1() throws Exception
    {
        var aZipFile = new ZipFile(
                "src/test/resources/exports/Export+Test+-+Curated+annotation+project_3_6_1.zip");
        var exProject = ProjectExportServiceImpl.loadExportedProject(aZipFile);

        // Provide source documents based on data in the exported project
        when(documentService.listSourceDocuments(any()))
                .then(invocation -> sourceDocuments(exProject, targetProject));

        var importRequest = ProjectImportRequest.builder() //
                .withCreateMissingUsers(true) //
                .withImportPermissions(true) //
                .build();
        sut.importData(importRequest, targetProject, exProject, aZipFile);

        var importedCases = new ArrayList<Pair<SourceDocument, String>>();
        for (var doc : documentService.listSourceDocuments(sourceProject)) {
            var annFolder = driver.getAnnotationFolder(doc);
            for (var serFile : annFolder.listFiles((dir, name) -> name.endsWith(".ser"))) {
                importedCases.add(Pair.of(doc, removeExtension(serFile.getName())));
            }
        }
        var imported = importedCases;

        // Check that the curation for the document in the project is imported
        assertThat(imported).extracting(p -> p.getKey().getName())
                .containsExactlyInAnyOrder("example_sentence.txt", "example_sentence.txt");
        assertThat(imported).extracting(Pair::getValue)
                .containsExactlyInAnyOrder(INITIAL_CAS_PSEUDO_USER, "admin");
    }

    private List<AnnotationDocument> annotationDocuments(Project aProject)
    {
        var docs = new ArrayList<AnnotationDocument>();
        for (var i = 1l; i <= 10l; i++) {
            var doc = sourceDocument(i, aProject);
            var adoc = AnnotationDocument.builder() //
                    .withId(i) //
                    .withUser("someuser") //
                    .forDocument(doc) //
                    .withState(AnnotationDocumentState.IN_PROGRESS) //
                    .build();
            docs.add(adoc);
        }
        return docs;
    }

    private List<SourceDocument> sourceDocuments(Project aProject)
    {
        var docs = new ArrayList<SourceDocument>();
        for (var i = 1l; i <= 10l; i++) {
            var doc = sourceDocument(i, aProject);
            docs.add(doc);
        }
        return docs;
    }

    private SourceDocument sourceDocument(long i, Project aProject)
    {
        var doc = SourceDocument.builder() //
                .withId(i) //
                .withProject(aProject) //
                .withName(i + ".txt") //
                .build();
        return doc;
    }

    private List<SourceDocument> sourceDocuments(ExportedProject exProject, Project aProject)
    {
        long i = 1;
        var docs = new ArrayList<SourceDocument>();
        for (var exDoc : exProject.getSourceDocuments()) {
            var doc = new SourceDocument();
            doc.setId(i++);
            doc.setName(exDoc.getName());
            doc.setProject(aProject);
            docs.add(doc);
        }
        return docs;
    }
}
