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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Objects;
import java.util.stream.Stream;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryPropertiesImpl;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBasePropertiesImpl;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.reification.Reification;
import de.tudarmstadt.ukp.inception.kb.util.TestFixtures;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseProfile;

@DataJpaTest( //
        showSql = false, //
        properties = { //
                "spring.main.banner-mode=off" }, //
        excludeAutoConfiguration = LiquibaseAutoConfiguration.class)
@EnableAutoConfiguration
@EntityScan({ //
        "de.tudarmstadt.ukp.inception.kb.model", //
        "de.tudarmstadt.ukp.clarin.webanno.model" })
@Execution(SAME_THREAD)
public class KnowledgeBaseServiceImplImportExportIntegrationTest
{
    private static final String PROJECT_NAME = "Test project";
    private static final String KB_NAME = "Test knowledge base";

    private @TempDir File temporaryFolder;

    private @Autowired TestEntityManager testEntityManager;
    private TestFixtures testFixtures;

    private KnowledgeBaseServiceImpl sut;
    private Project project;
    private KnowledgeBase kb;

    // @BeforeAll
    // public static void setUpOnce()
    // {
    // System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
    // }

    @BeforeEach
    public void setUp()
    {
        var repoProps = new RepositoryPropertiesImpl();
        repoProps.setPath(temporaryFolder);

        var kbProperties = new KnowledgeBasePropertiesImpl();
        var entityManager = testEntityManager.getEntityManager();

        testFixtures = new TestFixtures(testEntityManager);
        sut = new KnowledgeBaseServiceImpl(repoProps, kbProperties, entityManager);
        project = createProject(PROJECT_NAME);
        kb = buildKnowledgeBase(project, KB_NAME);
    }

    @AfterEach
    public void tearDown() throws Exception
    {
        testEntityManager.clear();
        sut.destroy();
    }

    @Test
    public void thatApplicationContextStarts()
    {
    }

    @Test
    public void importData_WithExistingTtl_ShouldImportTriples() throws Exception
    {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        importKnowledgeBase("data/pets.ttl");

        Stream<String> conceptLabels = sut.listAllConcepts(kb, false).stream()
                .map(KBObject::getName);
        Stream<String> propertyLabels = sut.listProperties(kb, false).stream()
                .map(KBObject::getName);
        assertThat(conceptLabels).as("Check that concepts all have been imported")
                .containsExactlyInAnyOrder("Animal", "Character", "Cat", "Dog");
        assertThat(propertyLabels).as("Check that properties all have been imported")
                .containsExactlyInAnyOrder("Loves", "Hates", "Has Character", "Year Of Birth");
    }

    @Test
    public void importData_WithReadOnlyKb_ShouldDoNothing() throws Exception
    {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        kb.setReadOnly(true);

        importKnowledgeBase("data/pets.ttl");

        Stream<String> conceptLabels = sut.listAllConcepts(kb, false).stream()
                .map(KBObject::getName);
        Stream<String> propertyLabels = sut.listProperties(kb, false).stream()
                .map(KBObject::getName);
        assertThat(conceptLabels).as("Check that no concepts have been imported").isEmpty();
        assertThat(propertyLabels).as("Check that no properties have been imported").isEmpty();
    }

    @Test
    public void importData_WithTwoFilesAndOneKnowledgeBase_ShouldImportAllTriples() throws Exception
    {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        String[] resourceNames = { "data/pets.ttl", "data/more_pets.ttl" };
        for (var resourceName : resourceNames) {
            importKnowledgeBase(resourceName);
        }

        var conceptLabels = sut.listAllConcepts(kb, false).stream().map(KBObject::getName);
        var propertyLabels = sut.listProperties(kb, false).stream().map(KBObject::getName);

        assertThat(conceptLabels).as("Check that concepts all have been imported")
                .containsExactlyInAnyOrder("Animal", "Character", "Cat", "Dog", "Manatee", "Turtle",
                        "Biological class");
        assertThat(propertyLabels).as("Check that properties all have been imported")
                .containsExactlyInAnyOrder("Loves", "Hates", "Has Character", "Year Of Birth",
                        "Has biological class");
    }

    @Test
    public void importData_WithMisTypedStatements_ShouldImportWithoutError() throws Exception
    {
        var classLoader = getClass().getClassLoader();
        var resourceName = "turtle/mismatching_literal_statement.ttl";
        var fileName = classLoader.getResource(resourceName).getFile();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        try (var is = classLoader.getResourceAsStream(resourceName)) {
            sut.importData(kb, fileName, is);
        }

        var kahmi = sut.readInstance(kb, "http://mbugert.de/pets#kahmi").get();
        var conceptLabels = sut.listAllConcepts(kb, false).stream().map(KBObject::getName);
        var propertyLabels = sut.listProperties(kb, false).stream().map(KBObject::getName);
        var kahmiValues = sut.listStatements(kb, kahmi, false).stream().map(KBStatement::getValue);
        assertThat(conceptLabels).as("Check that all concepts have been imported")
                .containsExactlyInAnyOrder("Cat", "Character");
        assertThat(propertyLabels).as("Check that all properties have been imported")
                .containsExactlyInAnyOrder("Has Character");
        assertThat(kahmiValues).as("Check that statements with wrong types have been imported")
                .containsExactlyInAnyOrder(666);
    }

    @Test
    public void exportData_WithLocalKnowledgeBase_ShouldExportKnowledgeBase() throws Exception
    {
        var concept = new KBConcept();
        concept.setName("TestConcept");
        var property = new KBProperty();
        property.setName("TestProperty");
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        sut.createConcept(kb, concept);
        sut.createProperty(kb, property);

        var kbFile = temporaryFolder.toPath().resolve("exported_kb.ttl").toFile();
        try (var os = new FileOutputStream(kbFile)) {
            sut.exportData(kb, RDFFormat.TURTLE, os);
        }

        var importedKb = buildKnowledgeBase(project, "Imported knowledge base");
        sut.registerKnowledgeBase(importedKb, sut.getNativeConfig());
        try (var is = new FileInputStream(kbFile)) {
            sut.importData(importedKb, kbFile.getAbsolutePath(), is);
        }
        var conceptLabels = sut.listAllConcepts(importedKb, false).stream().map(KBObject::getName)
                .toList();
        var propertyLabels = sut.listProperties(importedKb, false).stream().map(KBObject::getName)
                .filter(Objects::nonNull).toList();
        assertThat(conceptLabels).as("Check that concepts all have been exported")
                .containsExactlyInAnyOrder("TestConcept");
        assertThat(propertyLabels).as("Check that properties all have been exported")
                .containsExactlyInAnyOrder("TestProperty");
    }

    @Test
    public void exportData_WithRemoteKnowledgeBase_ShouldDoNothing() throws Exception
    {
        var outputFile = temporaryFolder.toPath().resolve("outputfile").toFile();
        kb.setType(RepositoryType.REMOTE);
        sut.registerKnowledgeBase(kb, sut.getRemoteConfig(KnowledgeBaseProfile
                .readKnowledgeBaseProfiles().get("babel_net").getAccess().getAccessUrl()));

        try (var os = new FileOutputStream(outputFile)) {
            sut.exportData(kb, RDFFormat.TURTLE, os);
        }

        assertThat(outputFile).as("Check that file has not been written to")
                .matches(f -> outputFile.length() == 0);
    }

    // Helper

    private Project createProject(String name)
    {
        var p = new Project();
        p.setName(name);
        return testEntityManager.persist(p);
    }

    private KnowledgeBase buildKnowledgeBase(Project aProject, String aName)
    {
        return testFixtures.buildKnowledgeBase(aProject, aName, Reification.NONE);
    }

    private void importKnowledgeBase(String resourceName) throws Exception
    {
        var classLoader = getClass().getClassLoader();
        var fileName = classLoader.getResource(resourceName).getFile();
        try (var is = classLoader.getResourceAsStream(resourceName)) {
            sut.importData(kb, fileName, is);
        }
    }

    @SpringBootConfiguration
    public static class SpringConfig
    {
        // No content
    }
}
