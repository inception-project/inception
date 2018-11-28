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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sparql.config.SPARQLRepositoryConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseProperties;
import de.tudarmstadt.ukp.inception.kb.exporter.KnowledgeBaseExporter;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class KnowledgeBaseExporterTest
{
    private static final String TestURLEndpoint = "https://collection.britishmuseum.org/sparql";

    private @Mock KnowledgeBaseService kbService;
    private @Mock AnnotationSchemaService schemaService;

    private Project sourceProject;
    private Project targetProject;

    public @Rule TemporaryFolder temporaryFolder = new TemporaryFolder();

    private KnowledgeBaseExporter sut;

    @Before
    public void setUp() throws Exception
    {
        initMocks(this);

        sourceProject = new Project();
        sourceProject.setId(1l);
        sourceProject.setName("Test Project");
        sourceProject.setMode(WebAnnoConst.PROJECT_TYPE_ANNOTATION);

        targetProject = new Project();
        sourceProject.setId(2l);
        targetProject.setName("Test Project");
        targetProject.setMode(WebAnnoConst.PROJECT_TYPE_ANNOTATION);

        when(kbService.getKnowledgeBases(sourceProject)).thenReturn(knowledgeBases());

        when(kbService.getKnowledgeBaseConfig(any()))
            .thenReturn(new SPARQLRepositoryConfig(TestURLEndpoint));

        when(schemaService.listAnnotationFeature(sourceProject)).thenReturn(features(sourceProject));

        sut = new KnowledgeBaseExporter(kbService, new KnowledgeBaseProperties(), schemaService);
    }

    @Test
    public void thatExportingWorks() throws Exception
    {
        // Export the project
        ProjectExportRequest exportRequest = new ProjectExportRequest();
        exportRequest.setProject(sourceProject);
        ExportedProject exportedProject = new ExportedProject();
        sut.exportData(exportRequest, exportedProject, temporaryFolder.getRoot());

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
        verify(kbService, times(numOfLocalKBs)).importData(any(),
            any(), any());

        assertThat(exportedKbs)
            .usingElementComparatorIgnoringFields("repositoryId", "project")
            .containsExactlyInAnyOrderElementsOf(knowledgeBases());
    }

    @Test
    public void thatRemappingConceptFeaturesOnImportWorks() throws Exception
    {
        // Export the project
        ProjectExportRequest exportRequest = new ProjectExportRequest();
        exportRequest.setProject(sourceProject);
        ExportedProject exportedProject = new ExportedProject();
        sut.exportData(exportRequest, exportedProject, temporaryFolder.getRoot());

        // Mock that the KB ID changes during import when registerKnowledgeBase is called
        doAnswer(i -> {
            KnowledgeBase kb = i.getArgument(0);
            kb.setRepositoryId("imported-" + kb.getRepositoryId());
            return null;
        }).when(kbService).registerKnowledgeBase(any(), any());

        // Mock the features in the imported project
        when(schemaService.listAnnotationFeature(targetProject)).thenReturn(features(targetProject));

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
        assertThat(importedFeatures)
            .extracting(feature -> {
                ConceptFeatureTraits traits = JSONUtil.fromJsonString(ConceptFeatureTraits.class,
                    feature.getTraits());
                return traits.getRepositoryId();
            })
            .allSatisfy(id -> assertThat(id).startsWith("imported-"));
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

    private List<AnnotationFeature> features(Project aProject) throws Exception
    {
        AnnotationLayer layer1 = new AnnotationLayer("layer", "layer", WebAnnoConst.SPAN_TYPE,
            aProject, false, AnchoringMode.TOKENS);

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
        kb.setSubclassIri(RDFS.SUBCLASSOF);
        kb.setTypeIri(RDF.TYPE);
        kb.setDescriptionIri(RDFS.COMMENT);
        kb.setLabelIri(RDFS.LABEL);
        kb.setPropertyTypeIri(RDF.PROPERTY);
        kb.setPropertyLabelIri(RDFS.LABEL);
        kb.setPropertyDescriptionIri(RDFS.COMMENT);
        kb.setSubPropertyIri(RDFS.SUBPROPERTYOF);
        kb.setMaxResults(1000);
        ValueFactory vf = SimpleValueFactory.getInstance();
        kb.setExplicitlyDefinedRootConcepts(Arrays
            .asList(vf.createIRI("http://www.ics.forth.gr/isl/CRMinf/I1_Argumentation"),
                vf.createIRI("http://www.ics.forth.gr/isl/CRMinf/I1_Argumentation")));
        kb.setDefaultLanguage("en");
        return kb;
    }
}
