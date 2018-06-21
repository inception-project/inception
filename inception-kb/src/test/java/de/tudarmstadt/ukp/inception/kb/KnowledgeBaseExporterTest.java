/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.inception.kb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.sparql.config.SPARQLRepositoryConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.exporter.KnowledgeBaseExporter;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class KnowledgeBaseExporterTest
{
    private static final String TestURLEndpoint = "https://collection.britishmuseum.org/sparql";
        
    private @Mock KnowledgeBaseService kbService;

    private Project project;
    
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private KnowledgeBaseExporter sut;
    
    @Before
    public void setUp() throws Exception
    {
        initMocks(this);

        project = new Project();
        project.setName("Test Project");
        when(kbService.getKnowledgeBases(project)).thenReturn(knowledgeBases());
        
        ArgumentCaptor<KnowledgeBase> kbCaptor = ArgumentCaptor.forClass(KnowledgeBase.class);
        when(kbService.getKnowledgeBaseConfig(kbCaptor.capture()))
                .thenReturn(new SPARQLRepositoryConfig(TestURLEndpoint));
        
        sut = new KnowledgeBaseExporter(kbService);
    }
    
    @Test
    public void thatExportingWorks() throws Exception
    {
        // Export the project
        ProjectExportRequest exportRequest = new ProjectExportRequest();
        exportRequest.setProject(project);
        ExportedProject exportedProject = new ExportedProject();
        sut.exportData(exportRequest, exportedProject, temporaryFolder.getRoot());

        // Import the project again
        ArgumentCaptor<KnowledgeBase> exportKbCaptor = ArgumentCaptor.forClass(KnowledgeBase.class);
        ArgumentCaptor<RepositoryImplConfig> cfgCaptor = ArgumentCaptor
                .forClass(RepositoryImplConfig.class);
        doNothing().when(kbService).registerKnowledgeBase(exportKbCaptor.capture(),
                cfgCaptor.capture());

        ProjectImportRequest importRequest = new ProjectImportRequest(true);
        ZipFile zipFile = mock(ZipFile.class);

        sut.importData(importRequest, project, exportedProject, zipFile);

        // Check how many localKBs have been exported
        List<KnowledgeBase> exportedKbs = exportKbCaptor.getAllValues();
        int numOfLocalKBs = exportedKbs.stream()
                .filter(kb -> kb.getType().equals(RepositoryType.LOCAL))
                .collect(Collectors.toList()).size();

        // Verify that importData is called as many times as there are localKBs
        ArgumentCaptor<String> fileNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<InputStream> inputStreamCaptor = ArgumentCaptor.forClass(InputStream.class);
        ArgumentCaptor<KnowledgeBase> importKbCaptor = ArgumentCaptor.forClass(KnowledgeBase.class);
        verify(kbService, times(numOfLocalKBs)).importData(importKbCaptor.capture(),
                fileNameCaptor.capture(), inputStreamCaptor.capture());

        assertThat(exportedKbs).usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrderElementsOf(knowledgeBases());
    }
    
    private List<KnowledgeBase> knowledgeBases() throws Exception
    {
        KnowledgeBase kb1 = buildKnowledgeBase("kb1");
        kb1.setType(RepositoryType.LOCAL);
        kb1.setClassIri(IriConstants.WIKIDATA_CLASS);
        
        KnowledgeBase kb2 = buildKnowledgeBase("kb2");
        kb2.setType(RepositoryType.REMOTE);
        kb2.setClassIri(OWL.CLASS);

        
        KnowledgeBase kb3 = buildKnowledgeBase("kb3");
        kb3.setType(RepositoryType.REMOTE);
        kb3.setClassIri(RDFS.CLASS);

        
        KnowledgeBase kb4 = buildKnowledgeBase("kb4");
        kb4.setType(RepositoryType.LOCAL);
        kb4.setClassIri(RDFS.CLASS);

        
        return Arrays.asList(kb1, kb2, kb3);        
    }
    
    private KnowledgeBase buildKnowledgeBase(String name) throws Exception {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(name);
        kb.setProject(project);
        kb.setSubclassIri(RDFS.SUBCLASSOF);
        kb.setTypeIri(RDF.TYPE);
        kb.setDescriptionIri(RDFS.COMMENT);
        kb.setLabelIri(RDFS.LABEL);
        kb.setPropertyTypeIri(RDF.PROPERTY);
        return kb;
    }  
}
