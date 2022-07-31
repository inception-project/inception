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
package de.tudarmstadt.ukp.inception.kb;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static de.tudarmstadt.ukp.inception.kb.IriConstants.WIKIDATA_CLASS;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sparql.config.SPARQLRepositoryConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBasePropertiesImpl;
import de.tudarmstadt.ukp.inception.kb.exporter.KnowledgeBaseExporter;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

@ExtendWith(MockitoExtension.class)
public class KnowledgeBaseExporterTest
{
    private static final String TestURLEndpoint = "https://collection.britishmuseum.org/sparql";

    private @Mock KnowledgeBaseService kbService;
    private @Mock AnnotationSchemaService schemaService;

    private Project sourceProject;
    private Project targetProject;

    public @TempDir File temporaryFolder;

    private KnowledgeBaseExporter sut;

    @BeforeEach
    public void setUp() throws Exception
    {
        sourceProject = new Project();
        sourceProject.setId(1l);
        sourceProject.setName("Test Project");

        targetProject = new Project();
        sourceProject.setId(2l);
        targetProject.setName("Test Project");

        when(kbService.getKnowledgeBases(sourceProject)).thenReturn(knowledgeBases());

        when(kbService.getKnowledgeBaseConfig(any()))
                .thenReturn(new SPARQLRepositoryConfig(TestURLEndpoint));

        when(schemaService.listAnnotationFeature(sourceProject))
                .thenReturn(features(sourceProject));

        sut = new KnowledgeBaseExporter(kbService, new KnowledgeBasePropertiesImpl(),
                schemaService);
    }

    @Test
    public void thatExportingWorks() throws Exception
    {
        // Export the project
        FullProjectExportRequest exportRequest = new FullProjectExportRequest(sourceProject, null,
                false);
        ProjectExportTaskMonitor monitor = new ProjectExportTaskMonitor(sourceProject, null,
                "test");
        ExportedProject exportedProject = new ExportedProject();
        sut.exportData(exportRequest, monitor, exportedProject, temporaryFolder);

        // Import the project again
        ArgumentCaptor<KnowledgeBase> exportKbCaptor = ArgumentCaptor.forClass(KnowledgeBase.class);
        doNothing().when(kbService).registerKnowledgeBase(exportKbCaptor.capture(), any());

        ProjectImportRequest importRequest = new ProjectImportRequest(true);
        ZipFile zipFile = mock(ZipFile.class);

        sut.importData(importRequest, targetProject, exportedProject, zipFile);

        // Check how many localKBs have been exported
        List<KnowledgeBase> exportedKbs = exportKbCaptor.getAllValues();
        int numOfLocalKBs = exportedKbs.stream()
                .filter(kb -> kb.getType().equals(RepositoryType.LOCAL))
                .collect(Collectors.toList()).size();

        // Verify that importData is called as many times as there are localKBs
        verify(kbService, times(numOfLocalKBs)).importData(any(), any(), any());

        assertThat(exportedKbs) //
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("repositoryId",
                        "project")
                .containsExactlyInAnyOrderElementsOf(knowledgeBases());
    }

    @Test
    public void thatRemappingConceptFeaturesOnImportWorks() throws Exception
    {
        // Export the project
        FullProjectExportRequest exportRequest = new FullProjectExportRequest(sourceProject, null,
                false);
        ProjectExportTaskMonitor monitor = new ProjectExportTaskMonitor(sourceProject, null,
                "test");
        ExportedProject exportedProject = new ExportedProject();
        sut.exportData(exportRequest, monitor, exportedProject, temporaryFolder);

        // Mock that the KB ID changes during import when registerKnowledgeBase is called
        doAnswer(i -> {
            KnowledgeBase kb = i.getArgument(0);
            kb.setRepositoryId("imported-" + kb.getRepositoryId());
            return null;
        }).when(kbService).registerKnowledgeBase(any(), any());

        // Mock the features in the imported project
        when(schemaService.listAnnotationFeature(targetProject))
                .thenReturn(features(targetProject));

        // Capture remapped features
        ArgumentCaptor<AnnotationFeature> importedAnnotationFeatureCaptor = ArgumentCaptor
                .forClass(AnnotationFeature.class);
        doNothing().when(schemaService).createFeature(importedAnnotationFeatureCaptor.capture());

        // Import the project again
        ProjectImportRequest importRequest = new ProjectImportRequest(true);
        ZipFile zipFile = mock(ZipFile.class);
        sut.importData(importRequest, targetProject, exportedProject, zipFile);

        // Verify that features were actually processed
        verify(schemaService, times(schemaService.listAnnotationFeature(sourceProject).size()))
                .createFeature(any());

        // Check that IDs have been remapped
        List<AnnotationFeature> importedFeatures = importedAnnotationFeatureCaptor.getAllValues();
        assertThat(importedFeatures).extracting(feature -> {
            ConceptFeatureTraits traits = JSONUtil.fromJsonString(ConceptFeatureTraits.class,
                    feature.getTraits());
            return traits.getRepositoryId();
        }).allSatisfy(id -> assertThat(id).startsWith("imported-"));
    }

    private List<KnowledgeBase> knowledgeBases() throws Exception
    {
        KnowledgeBase kb1 = buildKnowledgeBase("kb1");
        kb1.setType(RepositoryType.LOCAL);
        kb1.setClassIri(WIKIDATA_CLASS.stringValue());

        KnowledgeBase kb2 = buildKnowledgeBase("kb2");
        kb2.setType(RepositoryType.REMOTE);
        kb2.setClassIri(OWL.CLASS.stringValue());

        KnowledgeBase kb3 = buildKnowledgeBase("kb3");
        kb3.setType(RepositoryType.REMOTE);
        kb3.setClassIri(RDFS.CLASS.stringValue());

        KnowledgeBase kb4 = buildKnowledgeBase("kb4");
        kb4.setType(RepositoryType.LOCAL);
        kb4.setClassIri(RDFS.CLASS.stringValue());

        return Arrays.asList(kb1, kb2, kb3);
    }

    private List<AnnotationFeature> features(Project aProject) throws Exception
    {
        AnnotationLayer layer1 = new AnnotationLayer("layer", "layer", WebAnnoConst.SPAN_TYPE,
                aProject, false, TOKENS, NO_OVERLAP);

        AnnotationFeature feat1 = new AnnotationFeature(1, layer1, "conceptFeature", "kb:conceptA");
        ConceptFeatureTraits traits1 = new ConceptFeatureTraits();
        traits1.setRepositoryId("id-kb1");
        feat1.setTraits(JSONUtil.toJsonString(traits1));

        return asList(feat1);
    }

    private KnowledgeBase buildKnowledgeBase(String name) throws Exception
    {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setRepositoryId("id-" + name);
        kb.setName(name);
        kb.setProject(sourceProject);
        kb.setSubclassIri(RDFS.SUBCLASSOF.stringValue());
        kb.setTypeIri(RDF.TYPE.stringValue());
        kb.setDescriptionIri(RDFS.COMMENT.stringValue());
        kb.setLabelIri(RDFS.LABEL.stringValue());
        kb.setPropertyTypeIri(RDF.PROPERTY.stringValue());
        kb.setPropertyLabelIri(RDFS.LABEL.stringValue());
        kb.setPropertyDescriptionIri(RDFS.COMMENT.stringValue());
        kb.setSubPropertyIri(RDFS.SUBPROPERTYOF.stringValue());
        kb.setMaxResults(1000);
        kb.setRootConcepts(asList("http://www.ics.forth.gr/isl/CRMinf/I1_Argumentation",
                "http://www.ics.forth.gr/isl/CRMinf/I1_Argumentation"));
        kb.setDefaultLanguage("en");
        return kb;
    }
}
