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

    private static final String PROJECT_NAME = "Test project5";
    private static final String KB_NAME = "Test knowledge base";

    @Rule
    public TemporaryFolder temporaryFolder5 = new TemporaryFolder();

    @Autowired
    private TestEntityManager testEntityManager5;
    private TestFixtures testFixtures5;

    private KnowledgeBaseServiceImpl sut5;
    private Project project5;
    private KnowledgeBase kb5;

    @BeforeClass
    public static void setUpOnce5() {
        System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
    }

    @Before
    public void setUp5() {
        RepositoryProperties repoProps = new RepositoryProperties();
        repoProps.setPath(temporaryFolder5.getRoot());
        EntityManager entityManager = testEntityManager5.getEntityManager();
        testFixtures5 = new TestFixtures(testEntityManager5);
        sut5 = new KnowledgeBaseServiceImpl(repoProps, entityManager);
        project5 = createProject(PROJECT_NAME);
        kb5 = buildKnowledgeBase(project5, KB_NAME);
    }

    @After
    public void tearDown5() throws Exception {
        testEntityManager5.clear();
        sut5.destroy();
    }

    @Test
    public void thatApplicationContextStarts() {
    }

    @Test
    public void importData_WithExistingTtl_ShouldImportTriples() throws Exception {
        sut5.registerKnowledgeBase(kb5, sut5.getNativeConfig());

        importKnowledgeBase("data/pets.ttl");

        Stream<String> conceptLabels = sut5.listAllConcepts(kb5, false).stream().map(KBObject::getName);
        Stream<String> propertyLabels = sut5.listProperties(kb5, false).stream().map(KBObject::getName);
        assertThat(conceptLabels)
            .as("Check that concepts all have been imported")
            .containsExactlyInAnyOrder("Animal", "Character", "Cat", "Dog");
        assertThat(propertyLabels)
            .as("Check that properties all have been imported")
            .containsExactlyInAnyOrder("Loves", "Hates", "Has Character", "Year Of Birth");
    }

    @Test
    public void importData_WithReadOnlyKb_ShouldDoNothing() throws Exception {
        sut5.registerKnowledgeBase(kb5, sut5.getNativeConfig());
        kb5.setReadOnly(true);

        importKnowledgeBase("data/pets.ttl");

        Stream<String> conceptLabels = sut5.listAllConcepts(kb5, false).stream().map(KBObject::getName);
        Stream<String> propertyLabels = sut5.listProperties(kb5, false).stream().map(KBObject::getName);
        assertThat(conceptLabels)
            .as("Check that no concepts have been imported")
            .isEmpty();
        assertThat(propertyLabels)
            .as("Check that no properties have been imported")
            .isEmpty();
    }

    @Test
    public void importData_WithTwoFilesAndOneKnowledgeBase_ShouldImportAllTriples() throws Exception {
        sut5.registerKnowledgeBase(kb5, sut5.getNativeConfig());
        String[] resourceNames = {"data/pets.ttl", "data/more_pets.ttl"};
        for (String resourceName : resourceNames) {
            importKnowledgeBase(resourceName);
        }

        Stream<String> conceptLabels = sut5.listAllConcepts(kb5, false).stream().map(KBObject::getName);
        Stream<String> propertyLabels = sut5.listProperties(kb5, false).stream().map(KBObject::getName);

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
        sut5.registerKnowledgeBase(kb5, sut5.getNativeConfig());

        try (InputStream is = classLoader.getResourceAsStream(resourceName)) {
            sut5.importData(kb5, fileName, is);
        }

        KBInstance kahmi = sut5.readInstance(kb5, "http://mbugert.de/pets#kahmi").get();
        Stream<String> conceptLabels = sut5.listAllConcepts(kb5, false).stream().map(KBObject::getName);
        Stream<String> propertyLabels = sut5.listProperties(kb5, false).stream().map(KBObject::getName);
        Stream<Object> kahmiValues = sut5.listStatements(kb5, kahmi, false)
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
        sut5.registerKnowledgeBase(kb5, sut5.getNativeConfig());
        sut5.createConcept(kb5, concept);
        sut5.createProperty(kb5, property);

        File kb5File = temporaryFolder5.newFile("exported_kb5.ttl");
        try (OutputStream os = new FileOutputStream(kb5File)) {
            sut5.exportData(kb5, RDFFormat.TURTLE, os);
        }

        KnowledgeBase importedKb = buildKnowledgeBase(project5, "Imported knowledge base");
        sut5.registerKnowledgeBase(importedKb, sut5.getNativeConfig());
        try (InputStream is = new FileInputStream(kb5File)) {
            sut5.importData(importedKb, kb5File.getAbsolutePath(), is);
        }
        List<String> conceptLabels = sut5.listAllConcepts(importedKb, false)
            .stream()
            .map(KBObject::getName)
            .collect(Collectors.toList());
        List<String> propertyLabels = sut5.listProperties(importedKb, false)
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
        File outputFile = temporaryFolder5.newFile();
        kb5.setType(RepositoryType.REMOTE);
        sut5.registerKnowledgeBase(kb5, sut5.getRemoteConfig(KnowledgeBaseProfile.readKnowledgeBaseProfiles().get("babel_net").getAccess().getAccessUrl()));

        try (OutputStream os = new FileOutputStream(outputFile)) {
            sut5.exportData(kb5, RDFFormat.TURTLE, os);
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
        return testEntityManager5.persist(p);
    }

    private KnowledgeBase buildKnowledgeBase(Project project5, String name) {
        return testFixtures5.buildKnowledgeBase(project5, name, Reification.NONE);
    }

    private void importKnowledgeBase(String resourceName) throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        String fileName = classLoader.getResource(resourceName).getFile();
        try (InputStream is = classLoader.getResourceAsStream(resourceName)) {
            sut5.importData(kb5, fileName, is);
        }
    }
}
