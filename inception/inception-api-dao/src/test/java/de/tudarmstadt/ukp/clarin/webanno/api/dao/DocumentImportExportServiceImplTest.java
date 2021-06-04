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
package de.tudarmstadt.ukp.clarin.webanno.api.dao;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.EXCLUSIVE_WRITE_ACCESS;
import static java.util.Collections.emptyList;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.slf4j.MDC;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasMetadataUtils;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageDriver;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.FileSystemCasStorageDriver;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.config.BackupProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.config.CasStoragePropertiesImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.docimexport.config.DocumentImportExportServiceProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.docimexport.config.DocumentImportExportServicePropertiesImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;
import de.tudarmstadt.ukp.clarin.webanno.xmi.XmiFormatSupport;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;

public class DocumentImportExportServiceImplTest
{
    private CasStorageSession casStorageSession;
    private @Spy AnnotationSchemaService schemaService;

    public @TempDir File testFolder;

    private DocumentImportExportServiceImpl sut;

    @BeforeEach
    public void setup() throws Exception
    {
        openMocks(this);

        // schemaService = mock(AnnotationSchemaServiceImpl.class);
        schemaService = Mockito.spy(new AnnotationSchemaServiceImpl());

        DocumentImportExportServiceProperties properties = new DocumentImportExportServicePropertiesImpl();

        RepositoryProperties repositoryProperties = new RepositoryProperties();
        repositoryProperties.setPath(testFolder);

        MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

        CasStorageDriver driver = new FileSystemCasStorageDriver(repositoryProperties,
                new BackupProperties());

        CasStorageServiceImpl storageService = new CasStorageServiceImpl(driver, null, null,
                new CasStoragePropertiesImpl());

        sut = new DocumentImportExportServiceImpl(repositoryProperties,
                List.of(new XmiFormatSupport()), storageService, schemaService, properties);
        sut.onContextRefreshedEvent();

        doReturn(emptyList()).when(schemaService).listAnnotationLayer(any());
        doReturn(emptyList()).when(schemaService).listAnnotationFeature((Project) any());
        // The prepareCasForExport method internally calls getFullProjectTypeSystem, so we need to
        // ensure this is actually callable and doesn't run into a mocked version which simply
        // returns null.
        when(schemaService.getFullProjectTypeSystem(any(), anyBoolean())).thenCallRealMethod();
        when(schemaService.getTypeSystemForExport(any())).thenCallRealMethod();
        doCallRealMethod().when(schemaService).prepareCasForExport(any(), any(), any(), any());
        doCallRealMethod().when(schemaService).upgradeCas(any(), any(),
                any(TypeSystemDescription.class));

        casStorageSession = CasStorageSession.open();
    }

    @AfterEach
    public void tearDown()
    {
        CasStorageSession.get().close();
    }

    @Test
    public void thatExportContainsNoCasMetadata() throws Exception
    {
        SourceDocument sd = makeSourceDocument(1L, 1L);

        // Create type system with built-in types, internal types, but without any project-specific
        // types.
        List<TypeSystemDescription> typeSystems = new ArrayList<>();
        typeSystems.add(createTypeSystemDescription());
        typeSystems.add(CasMetadataUtils.getInternalTypeSystem());
        TypeSystemDescription ts = mergeTypeSystems(typeSystems);

        // Prepare a test CAS with a CASMetadata annotation (DocumentMetaData is added as well
        // because the DKPro Core writers used by the ImportExportService expect it.
        JCas jcas = JCasFactory.createJCas(ts);
        casStorageSession.add("jcas", EXCLUSIVE_WRITE_ACCESS, jcas.getCas());
        jcas.setDocumentText("This is a test .");
        DocumentMetaData.create(jcas);
        CASMetadata cmd = new CASMetadata(jcas);
        cmd.addToIndexes(jcas);

        // Pass the CAS through the export mechanism. Write as XMI because that is one of the
        // formats which best retains the information from the CAS and is nicely human-readable
        // if the test needs to be debugged.
        File exportedXmi = sut.exportCasToFile(jcas.getCas(), sd, "testfile",
                sut.getFormatById(XmiFormatSupport.ID).get(), true);

        // Read the XMI back from the ZIP that was created by the exporter. This is because XMI
        // files are always serialized as XMI file + type system file.
        JCas jcas2 = JCasFactory.createJCas(ts);
        casStorageSession.add("jcas2", EXCLUSIVE_WRITE_ACCESS, jcas.getCas());
        try (ZipArchiveInputStream zipInput = new ZipArchiveInputStream(
                new FileInputStream(exportedXmi))) {
            ZipArchiveEntry entry;
            while ((entry = zipInput.getNextZipEntry()) != null) {
                if (entry.getName().endsWith(".xmi")) {
                    XmiCasDeserializer.deserialize(zipInput, jcas2.getCas());
                    break;
                }
            }
        }
        finally {
            exportedXmi.delete();
        }

        List<CASMetadata> result = new ArrayList<>(select(jcas2, CASMetadata.class));
        assertThat(result).hasSize(0);
    }

    private SourceDocument makeSourceDocument(long aProjectId, long aDocumentId)
    {
        Project project = new Project();
        project.setId(aProjectId);

        SourceDocument doc = new SourceDocument();
        doc.setProject(project);
        doc.setId(aDocumentId);

        return doc;
    }
}
