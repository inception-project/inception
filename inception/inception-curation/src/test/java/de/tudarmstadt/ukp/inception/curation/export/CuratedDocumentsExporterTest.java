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

import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.zip.ZipFile;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.diag.ChecksRegistry;
import de.tudarmstadt.ukp.clarin.webanno.diag.RepairsRegistry;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageServiceImpl;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageBackupProperties;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageCachePropertiesImpl;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStoragePropertiesImpl;
import de.tudarmstadt.ukp.inception.annotation.storage.driver.filesystem.FileSystemCasStorageDriver;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryPropertiesImpl;
import de.tudarmstadt.ukp.inception.export.DocumentImportExportServiceImpl;
import de.tudarmstadt.ukp.inception.export.config.DocumentImportExportServicePropertiesImpl;
import de.tudarmstadt.ukp.inception.io.xmi.XmiFormatSupport;
import de.tudarmstadt.ukp.inception.io.xmi.config.UimaFormatsPropertiesImpl.XmiFormatProperties;
import de.tudarmstadt.ukp.inception.project.export.ProjectExportServiceImpl;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

@ExtendWith(MockitoExtension.class)
public class CuratedDocumentsExporterTest
{
    private RepositoryProperties repositoryProperties;
    private DocumentImportExportService importExportSerivce;
    private CasStorageService casStorageService;

    private @Mock DocumentService documentService;
    private @Mock AnnotationSchemaService schemaService;
    private @Mock ChecksRegistry checksRegistry;
    private @Mock RepairsRegistry repairsRegistry;

    private Project targetProject;

    private @TempDir File tempDir;
    private long nextDocId = 1;

    private CuratedDocumentsExporter sut;

    @BeforeEach
    public void setUp() throws Exception
    {
        targetProject = Project.builder() //
                .withId(2l) //
                .withName("Test Project") //
                .build();

        var properties = new DocumentImportExportServicePropertiesImpl();

        repositoryProperties = new RepositoryPropertiesImpl();
        repositoryProperties.setPath(tempDir);

        var driver = new FileSystemCasStorageDriver(repositoryProperties,
                new CasStorageBackupProperties(), new CasStoragePropertiesImpl());

        casStorageService = spy(new CasStorageServiceImpl(driver,
                new CasStorageCachePropertiesImpl(), null, schemaService));

        var xmiFormatSupport = new XmiFormatSupport(new XmiFormatProperties());
        importExportSerivce = new DocumentImportExportServiceImpl(asList(xmiFormatSupport),
                casStorageService, schemaService, properties, checksRegistry, repairsRegistry,
                xmiFormatSupport);

        // Dynamically generate a SourceDocument with an incrementing ID when asked for one
        when(documentService.getSourceDocument(any(), any()))
                .then(call -> sourceDocument(call.getArgument(0), call.getArgument(1)));

        sut = new CuratedDocumentsExporter(documentService, importExportSerivce);
    }

    @Test
    public void thatImportingAnnotationProjectWorks_3_6_1() throws Exception
    {
        // Project is already exported
        var zipFile = new ZipFile(
                "src/test/resources/exports/Export+Test+-+Curated+annotation+project_3_6_1.zip");
        var sourceDocCaptor = ArgumentCaptor.forClass(SourceDocument.class);
        var setCaptor = ArgumentCaptor.forClass(AnnotationSet.class);

        // Import the project again
        var exProject = ProjectExportServiceImpl.loadExportedProject(zipFile);
        var importRequest = ProjectImportRequest.builder() //
                .withCreateMissingUsers(true) //
                .withImportPermissions(true) //
                .build();
        sut.importData(importRequest, targetProject, exProject, zipFile);

        verify(documentService, atLeastOnce()).importCas(sourceDocCaptor.capture(),
                setCaptor.capture(), any());

        var importedCases = new ArrayList<Pair<SourceDocument, String>>();
        var docs = sourceDocCaptor.getAllValues();
        var users = setCaptor.getAllValues();
        for (var i = 0; i < docs.size(); i++) {
            importedCases.add(Pair.of(docs.get(i), users.get(i).id()));
        }
        var imported = importedCases;

        // Check that the curation for the document in the project is imported
        assertThat(imported) //
                .extracting(p -> p.getKey().getName()) //
                .containsExactlyInAnyOrder("example_sentence.txt");
        assertThat(imported) //
                .extracting(Pair::getValue) //
                .containsExactlyInAnyOrder(CURATION_USER);
    }

    private Object sourceDocument(Project aProject, String aName)
    {
        var doc = new SourceDocument();
        doc.setId(nextDocId++);
        doc.setProject(aProject);
        doc.setName(aName);
        return doc;
    }
}
