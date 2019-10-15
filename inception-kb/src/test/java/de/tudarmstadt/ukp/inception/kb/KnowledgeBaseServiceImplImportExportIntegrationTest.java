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

    private static final String PROJECT_NAME = "Test KBSIIEIT_project";
    private static final String KB_NAME = "Test knowledge base";

    @Rule
    public TemporaryFolder KBSIIEIT_temporaryFolder = new TemporaryFolder();

    @Autowired
    private TestEntityManager KBSIIEIT_testEntityManager;
    private TestFixtures KBSIIEIT_testFixtures;

    private KnowledgeBaseServiceImpl KBSIIEIT_sut;
    private Project KBSIIEIT_project;
    private KnowledgeBase KBSIIEIT_kb;

    @BeforeClass
    public static void KBSIIEIT_setUpOnce() {
        System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
    }

    @Before
    public void KBSIIEIT_setUp() {
        RepositoryProperties repoProps = new RepositoryProperties();
        repoProps.setPath(KBSIIEIT_temporaryFolder.getRoot());
        EntityManager entityManager = KBSIIEIT_testEntityManager.getEntityManager();
        KBSIIEIT_testFixtures = new TestFixtures(KBSIIEIT_testEntityManager);
        KBSIIEIT_sut = new KnowledgeBaseServiceImpl(repoProps, entityManager);
        KBSIIEIT_project = createProject(PROJECT_NAME);
        KBSIIEIT_kb = buildKnowledgeBase(KBSIIEIT_project, KB_NAME);
    }

    @After
    public void KBSIIEIT_tearDown() throws Exception {
        KBSIIEIT_testEntityManager.clear();
        KBSIIEIT_sut.destroy();
    }

    @Test
    public void thatApplicationContextStarts() {
    }

    @Test
    public void importData_WithExistingTtl_ShouldImportTriples() throws Exception {
        KBSIIEIT_sut.registerKnowledgeBase(KBSIIEIT_kb, KBSIIEIT_sut.getNativeConfig());

        importKnowledgeBase("data/pets.ttl");

        Stream<String> conceptLabels = KBSIIEIT_sut.listAllConcepts(KBSIIEIT_kb, false).stream().map(KBObject::getName);
        Stream<String> propertyLabels = KBSIIEIT_sut.listProperties(KBSIIEIT_kb, false).stream().map(KBObject::getName);
        assertThat(conceptLabels)
            .as("Check that concepts all have been imported")
            .containsExactlyInAnyOrder("Animal", "Character", "Cat", "Dog");
        assertThat(propertyLabels)
            .as("Check that properties all have been imported")
            .containsExactlyInAnyOrder("Loves", "Hates", "Has Character", "Year Of Birth");
    }

    @Test
    public void importData_WithReadOnlyKb_ShouldDoNothing() throws Exception {
        KBSIIEIT_sut.registerKnowledgeBase(KBSIIEIT_kb, KBSIIEIT_sut.getNativeConfig());
        KBSIIEIT_kb.setReadOnly(true);

        importKnowledgeBase("data/pets.ttl");

        Stream<String> conceptLabels = KBSIIEIT_sut.listAllConcepts(KBSIIEIT_kb, false).stream().map(KBObject::getName);
        Stream<String> propertyLabels = KBSIIEIT_sut.listProperties(KBSIIEIT_kb, false).stream().map(KBObject::getName);
        assertThat(conceptLabels)
            .as("Check that no concepts have been imported")
            .isEmpty();
        assertThat(propertyLabels)
            .as("Check that no properties have been imported")
            .isEmpty();
    }

    @Test
    public void importData_WithTwoFilesAndOneKnowledgeBase_ShouldImportAllTriples() throws Exception {
        KBSIIEIT_sut.registerKnowledgeBase(KBSIIEIT_kb, KBSIIEIT_sut.getNativeConfig());
        String[] resourceNames = {"data/pets.ttl", "data/more_pets.ttl"};
        for (String resourceName : resourceNames) {
            importKnowledgeBase(resourceName);
        }

        Stream<String> conceptLabels = KBSIIEIT_sut.listAllConcepts(KBSIIEIT_kb, false).stream().map(KBObject::getName);
        Stream<String> propertyLabels = KBSIIEIT_sut.listProperties(KBSIIEIT_kb, false).stream().map(KBObject::getName);

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
        KBSIIEIT_sut.registerKnowledgeBase(KBSIIEIT_kb, KBSIIEIT_sut.getNativeConfig());

        try (InputStream is = classLoader.getResourceAsStream(resourceName)) {
            KBSIIEIT_sut.importData(KBSIIEIT_kb, fileName, is);
        }

        KBInstance kahmi = KBSIIEIT_sut.readInstance(KBSIIEIT_kb, "http://mbugert.de/pets#kahmi").get();
        Stream<String> conceptLabels = KBSIIEIT_sut.listAllConcepts(KBSIIEIT_kb, false).stream().map(KBObject::getName);
        Stream<String> propertyLabels = KBSIIEIT_sut.listProperties(KBSIIEIT_kb, false).stream().map(KBObject::getName);
        Stream<Object> kahmiValues = KBSIIEIT_sut.listStatements(KBSIIEIT_kb, kahmi, false)
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
        KBSIIEIT_sut.registerKnowledgeBase(KBSIIEIT_kb, KBSIIEIT_sut.getNativeConfig());
        KBSIIEIT_sut.createConcept(KBSIIEIT_kb, concept);
        KBSIIEIT_sut.createProperty(KBSIIEIT_kb, property);

        File KBSIIEIT_kbFile = KBSIIEIT_temporaryFolder.newFile("exported_KBSIIEIT_kb.ttl");
        try (OutputStream os = new FileOutputStream(KBSIIEIT_kbFile)) {
            KBSIIEIT_sut.exportData(KBSIIEIT_kb, RDFFormat.TURTLE, os);
        }

        KnowledgeBase importedKb = buildKnowledgeBase(KBSIIEIT_project, "Imported knowledge base");
        KBSIIEIT_sut.registerKnowledgeBase(importedKb, KBSIIEIT_sut.getNativeConfig());
        try (InputStream is = new FileInputStream(KBSIIEIT_kbFile)) {
            KBSIIEIT_sut.importData(importedKb, KBSIIEIT_kbFile.getAbsolutePath(), is);
        }
        List<String> conceptLabels = KBSIIEIT_sut.listAllConcepts(importedKb, false)
            .stream()
            .map(KBObject::getName)
            .collect(Collectors.toList());
        List<String> propertyLabels = KBSIIEIT_sut.listProperties(importedKb, false)
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
        File outputFile = KBSIIEIT_temporaryFolder.newFile();
        KBSIIEIT_kb.setType(RepositoryType.REMOTE);
        KBSIIEIT_sut.registerKnowledgeBase(KBSIIEIT_kb, KBSIIEIT_sut.getRemoteConfig(KnowledgeBaseProfile.readKnowledgeBaseProfiles().get("babel_net").getAccess().getAccessUrl()));

        try (OutputStream os = new FileOutputStream(outputFile)) {
            KBSIIEIT_sut.exportData(KBSIIEIT_kb, RDFFormat.TURTLE, os);
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
        return KBSIIEIT_testEntityManager.persist(p);
    }

    private KnowledgeBase buildKnowledgeBase(Project KBSIIEIT_project, String name) {
        return KBSIIEIT_testFixtures.buildKnowledgeBase(KBSIIEIT_project, name, Reification.NONE);
    }

    private void importKnowledgeBase(String resourceName) throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        String fileName = classLoader.getResource(resourceName).getFile();
        try (InputStream is = classLoader.getResourceAsStream(resourceName)) {
            KBSIIEIT_sut.importData(KBSIIEIT_kb, fileName, is);
        }
    }
}
