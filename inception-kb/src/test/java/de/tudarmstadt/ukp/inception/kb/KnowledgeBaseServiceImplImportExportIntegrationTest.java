/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.kb;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.reification.Reification;
import de.tudarmstadt.ukp.inception.kb.util.TestFixtures;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseProfile;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = SpringConfig.class)
@Transactional
@DataJpaTest
public class KnowledgeBaseServiceImplImportExportIntegrationTest {

    private static final String PROJECT_NAME = "Test project";
    private static final String KB_NAME = "Test knowledge base";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Autowired
    private TestEntityManager testEntityManager;
    private TestFixtures testFixtures;

    private KnowledgeBaseServiceImpl sut;
    private Project project;
    private KnowledgeBase kb;

    @BeforeClass
    public static void setUpOnce() {
        System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
    }

    @Before
    public void setUp() {
        RepositoryProperties repoProps = new RepositoryProperties();
        repoProps.setPath(temporaryFolder.getRoot());
        EntityManager entityManager = testEntityManager.getEntityManager();
        testFixtures = new TestFixtures(testEntityManager);
        sut = new KnowledgeBaseServiceImpl(repoProps, entityManager);
        project = createProject(PROJECT_NAME);
        kb = buildKnowledgeBase(project, KB_NAME);
    }

    @After
    public void tearDown() throws Exception {
        testEntityManager.clear();
        sut.destroy();
    }

    @Test
    public void thatApplicationContextStarts() {
    }

    @Test
    public void importData_WithExistingTtl_ShouldImportTriples() throws Exception {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        importKnowledgeBase("data/pets.ttl");

        Stream<String> conceptLabels = sut.listAllConcepts(kb, false).stream().map(KBObject::getName);
        Stream<String> propertyLabels = sut.listProperties(kb, false).stream().map(KBObject::getName);
        assertThat(conceptLabels)
            .as("Check that concepts all have been imported")
            .containsExactlyInAnyOrder("Animal", "Character", "Cat", "Dog");
        assertThat(propertyLabels)
            .as("Check that properties all have been imported")
            .containsExactlyInAnyOrder("Loves", "Hates", "Has Character", "Year Of Birth");
    }

    @Test
    public void importData_WithReadOnlyKb_ShouldDoNothing() throws Exception {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        kb.setReadOnly(true);

        importKnowledgeBase("data/pets.ttl");

        Stream<String> conceptLabels = sut.listAllConcepts(kb, false).stream().map(KBObject::getName);
        Stream<String> propertyLabels = sut.listProperties(kb, false).stream().map(KBObject::getName);
        assertThat(conceptLabels)
            .as("Check that no concepts have been imported")
            .isEmpty();
        assertThat(propertyLabels)
            .as("Check that no properties have been imported")
            .isEmpty();
    }

    @Test
    public void importData_WithTwoFilesAndOneKnowledgeBase_ShouldImportAllTriples() throws Exception {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        String[] resourceNames = {"data/pets.ttl", "data/more_pets.ttl"};
        for (String resourceName : resourceNames) {
            importKnowledgeBase(resourceName);
        }

        Stream<String> conceptLabels = sut.listAllConcepts(kb, false).stream().map(KBObject::getName);
        Stream<String> propertyLabels = sut.listProperties(kb, false).stream().map(KBObject::getName);

        assertThat(conceptLabels)
            .as("Check that concepts all have been imported")
            .containsExactlyInAnyOrder("Animal", "Character", "Cat", "Dog", "Manatee", "Turtle", "Biological class");
        assertThat(propertyLabels)
            .as("Check that properties all have been imported")
            .containsExactlyInAnyOrder("Loves", "Hates", "Has Character", "Year Of Birth", "Has biological class");
    }

    @Test
    public void importData_WithMisTypedStatements_ShouldImportWithoutError() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        String resourceName = "turtle/mismatching_literal_statement.ttl";
        String fileName = classLoader.getResource(resourceName).getFile();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        try (InputStream is = classLoader.getResourceAsStream(resourceName)) {
            sut.importData(kb, fileName, is);
        }

        KBInstance kahmi = sut.readInstance(kb, "http://mbugert.de/pets#kahmi").get();
        Stream<String> conceptLabels = sut.listAllConcepts(kb, false).stream().map(KBObject::getName);
        Stream<String> propertyLabels = sut.listProperties(kb, false).stream().map(KBObject::getName);
        Stream<Object> kahmiValues = sut.listStatements(kb, kahmi, false)
            .stream()
            .map(KBStatement::getValue);
        assertThat(conceptLabels)
            .as("Check that all concepts have been imported")
            .containsExactlyInAnyOrder("Cat", "Character");
        assertThat(propertyLabels)
            .as("Check that all properties have been imported")
            .containsExactlyInAnyOrder("Has Character");
        assertThat(kahmiValues)
            .as("Check that statements with wrong types have been imported")
            .containsExactlyInAnyOrder(666);
    }

    @Test
    public void exportData_WithLocalKnowledgeBase_ShouldExportKnowledgeBase() throws Exception {
        KBConcept concept = new KBConcept();
        concept.setName("TestConcept");
        KBProperty property = new KBProperty();
        property.setName("TestProperty");
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        sut.createConcept(kb, concept);
        sut.createProperty(kb, property);

        File kbFile = temporaryFolder.newFile("exported_kb.ttl");
        try (OutputStream os = new FileOutputStream(kbFile)) {
            sut.exportData(kb, RDFFormat.TURTLE, os);
        }

        KnowledgeBase importedKb = buildKnowledgeBase(project, "Imported knowledge base");
        sut.registerKnowledgeBase(importedKb, sut.getNativeConfig());
        try (InputStream is = new FileInputStream(kbFile)) {
            sut.importData(importedKb, kbFile.getAbsolutePath(), is);
        }
        List<String> conceptLabels = sut.listAllConcepts(importedKb, false)
            .stream()
            .map(KBObject::getName)
            .collect(Collectors.toList());
        List<String> propertyLabels = sut.listProperties(importedKb, false)
            .stream()
            .map(KBObject::getName)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        assertThat(conceptLabels)
            .as("Check that concepts all have been exported")
            .containsExactlyInAnyOrder("TestConcept");
        assertThat(propertyLabels)
            .as("Check that properties all have been exported")
            .containsExactlyInAnyOrder("TestProperty");
    }

    @Test
    public void exportData_WithRemoteKnowledgeBase_ShouldDoNothing() throws Exception {
        File outputFile = temporaryFolder.newFile();
        kb.setType(RepositoryType.REMOTE);
        sut.registerKnowledgeBase(kb, sut.getRemoteConfig(KnowledgeBaseProfile.readKnowledgeBaseProfiles().get("babel_net").getAccess().getAccessUrl()));

        try (OutputStream os = new FileOutputStream(outputFile)) {
            sut.exportData(kb, RDFFormat.TURTLE, os);
        }

        assertThat(outputFile)
            .as("Check that file has not been written to")
            .matches(f -> outputFile.length() == 0);
    }

    // Helper

    private Project createProject(String name) {
        Project p = new Project();
        p.setName(name);
        p.setMode(WebAnnoConst.PROJECT_TYPE_ANNOTATION);
        return testEntityManager.persist(p);
    }

    private KnowledgeBase buildKnowledgeBase(Project project, String name) {
        return testFixtures.buildKnowledgeBase(project, name, Reification.NONE);
    }

    private void importKnowledgeBase(String resourceName) throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        String fileName = classLoader.getResource(resourceName).getFile();
        try (InputStream is = classLoader.getResourceAsStream(resourceName)) {
            sut.importData(kb, fileName, is);
        }
    }
}
