/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.clarin.webanno.api.dao;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.xmi.XmiFormatSupport;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;

public class ImportExportServiceImplTest
{
    private BackupProperties backupProperties;
    private RepositoryProperties repositoryProperties;
    private CasStorageServiceImpl storageService;
    private @Mock AnnotationSchemaService schemaService;
    
    public @Rule TemporaryFolder testFolder = new TemporaryFolder();
    
    private ImportExportServiceImpl sut;

    @Before
    public void setup() throws Exception
    {
        initMocks(this);
        
        schemaService = mock(AnnotationSchemaServiceImpl.class);
        
        backupProperties = new BackupProperties();

        repositoryProperties = new RepositoryProperties();
        repositoryProperties.setPath(testFolder.newFolder());

        storageService = new CasStorageServiceImpl(null, repositoryProperties, backupProperties);

        sut = new ImportExportServiceImpl(repositoryProperties, asList(new XmiFormatSupport()),
                storageService, schemaService);
        sut.onContextRefreshedEvent();
        
        when(schemaService.listAnnotationLayer(any(Project.class))).thenReturn(emptyList());
        // We don't want to re-write the prepareCasForExport method - call the original
        when(schemaService.prepareCasForExport(any(), any())).thenCallRealMethod();
        // The prepareCasForExport method internally calls getFullProjectTypeSystem, so we need to
        // ensure this is actually callable and doesn't run into a mocked version which simply 
        // returns null.
        when(schemaService.getFullProjectTypeSystem(any(), anyBoolean())).thenCallRealMethod();
    }

    @Test
    public void thatExportContainsNoCasMetadata() throws Exception
    {
        SourceDocument sd = makeSourceDocument(1l, 1l);

        // Create type system with built-in types, internal types, but without any project-specific
        // types.
        List<TypeSystemDescription> typeSystems = new ArrayList<>();
        typeSystems.add(createTypeSystemDescription());
        typeSystems.add(CasMetadataUtils.getInternalTypeSystem());
        TypeSystemDescription ts = mergeTypeSystems(typeSystems);
        
        // Prepare a test CAS with a CASMetadata annotation (DocumentMetaData is added as well
        // because the DKPro Core writers used by the ImportExportService expect it.
        JCas jcas = JCasFactory.createJCas(ts);
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
