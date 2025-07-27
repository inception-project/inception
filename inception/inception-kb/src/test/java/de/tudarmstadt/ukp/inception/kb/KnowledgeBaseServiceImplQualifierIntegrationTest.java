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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

import java.io.File;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.List;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryPropertiesImpl;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseProperties;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBasePropertiesImpl;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBQualifier;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.reification.Reification;
import de.tudarmstadt.ukp.inception.kb.util.TestFixtures;
import jakarta.persistence.EntityManager;

@Transactional
@DataJpaTest( //
        showSql = false, //
        properties = { //
                "spring.main.banner-mode=off" }, //
        excludeAutoConfiguration = LiquibaseAutoConfiguration.class)
@Execution(SAME_THREAD)
public class KnowledgeBaseServiceImplQualifierIntegrationTest
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static {
        // System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
        System.setProperty("spring.main.banner-mode", "off");
    }

    private static final String PROJECT_NAME = "Test project";
    private static final String KB_NAME = "Test knowledge base";

    @TempDir
    File temporaryFolder;

    @Autowired
    private TestEntityManager testEntityManager;

    private KnowledgeBaseServiceImpl sut;
    private Project project;
    private KnowledgeBase kb;
    private TestFixtures testFixtures;

    private KBConcept concept;
    private KBProperty property;
    private KBHandle conceptHandle;
    private KBStatement statement;

    @BeforeEach
    public void setUp() throws Exception
    {
        RepositoryProperties repoProps = new RepositoryPropertiesImpl();
        repoProps.setPath(temporaryFolder);
        KnowledgeBaseProperties kbProperties = new KnowledgeBasePropertiesImpl();
        EntityManager entityManager = testEntityManager.getEntityManager();
        testFixtures = new TestFixtures(testEntityManager);
        sut = new KnowledgeBaseServiceImpl(repoProps, kbProperties, entityManager);
        project = testFixtures.createProject(PROJECT_NAME);
        kb = testFixtures.buildKnowledgeBase(project, KB_NAME, Reification.WIKIDATA);
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        concept = testFixtures.buildConcept();
        property = testFixtures.buildProperty();
        sut.createConcept(kb, concept);
        sut.createProperty(kb, property);
        conceptHandle = concept.toKBHandle();
        statement = testFixtures.buildStatement(conceptHandle, property, "Test statement");
        sut.upsertStatement(kb, statement);
    }

    @AfterEach
    public void tearDown() throws Exception
    {
        testEntityManager.clear();
        sut.destroy();
    }

    @Test
    public void addQualifier_WithUnsavedQualifier_shouldCreateQualifier()
    {
        sut.addQualifier(kb, testFixtures.buildQualifier(statement, property, "Test qualifier"));

        List<KBStatement> statements = sut.listStatements(kb, conceptHandle, false);

        assertThat(statements).hasSize(1);

        assertThat(statements.get(0).getQualifiers()).hasSize(1).element(0)
                .hasFieldOrProperty("property")
                .hasFieldOrPropertyWithValue("value", "Test qualifier");
    }

    @Test
    public void addQualifier_WithReadOnlyKnowledgeBase_ShouldDoNothing()
    {
        kb.setReadOnly(true);

        sut.updateKnowledgeBase(kb, sut.getKnowledgeBaseConfig(kb));

        int qualifierCountBeforeDeletion = sut.listQualifiers(kb, statement).size();

        assertThatExceptionOfType(ReadOnlyException.class).isThrownBy(() -> sut.addQualifier(kb,
                testFixtures.buildQualifier(statement, property, "Test qualifier")));

        int qualifierCountAfterDeletion = sut.listQualifiers(kb, statement).size();

        assertThat(qualifierCountBeforeDeletion).as("Check that statement was not added")
                .isEqualTo(qualifierCountAfterDeletion);
    }

    @Test
    public void upsertQualifier_withUnsavedQualifier_shouldCreateQualifier()
    {
        KBQualifier qualifier = testFixtures.buildQualifier(statement, property, "Test qualifier");

        sut.upsertQualifier(kb, qualifier);

        sut.read(kb, conn -> {
            var buffer = new StringWriter();
            RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TURTLE, buffer);
            conn.export(rdfWriter);
            LOG.info("{}", buffer);
            return null;
        });

        List<KBStatement> statements = sut.listStatements(kb, conceptHandle, false);

        assertThat(qualifier.getStatement().getQualifiers())
                .as("Check that KBStatement has updated correctly").hasSize(1).element(0)
                .hasFieldOrProperty("property")
                .hasFieldOrPropertyWithValue("value", "Test qualifier");

        assertThat(statements.get(0).getQualifiers())
                .as("Check that Knowledge Base has updated correctly").hasSize(1).element(0)
                .hasFieldOrProperty("property")
                .hasFieldOrPropertyWithValue("value", "Test qualifier");
    }

    @Test
    public void upsertQualifier_withExistingQualifier_shouldUpdateQualifier()
    {
        KBQualifier qualifier = testFixtures.buildQualifier(statement, property, "Test qualifier");

        sut.upsertQualifier(kb, qualifier);

        qualifier.setValue("changed Qualifier");

        sut.upsertQualifier(kb, qualifier);

        assertThat(qualifier.getStatement().getQualifiers())
                .as("Check that KBStatement has updated correctly").hasSize(1).element(0)
                .hasFieldOrProperty("property")
                .hasFieldOrPropertyWithValue("value", "changed Qualifier");

        List<KBStatement> statements = sut.listStatements(kb, conceptHandle, false);

        assertThat(statements.get(0).getQualifiers())
                .as("Check that Knowledge Base has updated correctly").hasSize(1).element(0)
                .hasFieldOrProperty("property")
                .hasFieldOrPropertyWithValue("value", "changed Qualifier");
    }

    @Test
    public void upsertQualifier_withReadOnlyKnowledgeBase_shouldDoNothing()
    {
        kb.setReadOnly(true);

        sut.updateKnowledgeBase(kb, sut.getKnowledgeBaseConfig(kb));

        int qualifierCountBeforeDeletion = sut.listQualifiers(kb, statement).size();

        KBQualifier qualifier = testFixtures.buildQualifier(statement, property, "Test qualifier");

        assertThatExceptionOfType(ReadOnlyException.class)
                .isThrownBy(() -> sut.upsertQualifier(kb, qualifier));

        int qualifierCountAfterDeletion = sut.listQualifiers(kb, statement).size();
        assertThat(qualifierCountBeforeDeletion).as("Check that statement was not updated")
                .isEqualTo(qualifierCountAfterDeletion);
    }

    @Test
    public void deleteQualifier_WithExistingQualifier_ShouldDeleteQualifier()
    {
        KBQualifier qualifier = testFixtures.buildQualifier(statement, property, "Test qualifier");

        sut.addQualifier(kb, qualifier);

        sut.deleteQualifier(kb, qualifier);

        List<KBStatement> statements = sut.listStatements(kb, conceptHandle, false);
        List<KBQualifier> qualifiers = sut.listQualifiers(kb, statement);

        assertThat(statements.get(0).getQualifiers()).isEmpty();

        assertThat(qualifiers).as("Check that the qualifier was deleted correctly")
                .noneMatch(qua -> "Test qualifier".equals(qua.getValue()));
    }

    @Test
    public void deleteQualifier_WithNonExistentQualifier_ShouldDoNothing()
    {
        assertThatCode(() -> sut.deleteQualifier(kb,
                testFixtures.buildQualifier(statement, property, "Test qualifier")))
                        .doesNotThrowAnyException();
    }

    @Test
    public void deleteQualifier__WithReadOnlyKnowledgeBase_ShouldDoNothing()
    {
        KBQualifier qualifier = testFixtures.buildQualifier(statement, property, "Test qualifier");

        sut.addQualifier(kb, qualifier);
        kb.setReadOnly(true);

        sut.updateKnowledgeBase(kb, sut.getKnowledgeBaseConfig(kb));

        int qualifierCountBeforeDeletion = sut.listQualifiers(kb, statement).size();
        assertThatExceptionOfType(ReadOnlyException.class)
                .isThrownBy(() -> sut.deleteQualifier(kb, qualifier));

        int qualifierCountAfterDeletion = sut.listQualifiers(kb, statement).size();
        assertThat(qualifierCountBeforeDeletion).as("Check that statement was not deleted")
                .isEqualTo(qualifierCountAfterDeletion);
    }

    @Test
    public void deleteStatement_WithExistingStatementAndQualifier_ShouldDeleteAll()
    {
        sut.addQualifier(kb, testFixtures.buildQualifier(statement, property, "Test qualifier"));

        sut.deleteStatement(kb, statement);

        List<KBStatement> statements = sut.listStatements(kb, conceptHandle, false);
        List<KBQualifier> qualifiers = sut.listQualifiers(kb, statement);
        assertThat(statements).as("Check that the statement was deleted correctly")
                .noneMatch(stmt -> "Test statement".equals(stmt.getValue()));

        assertThat(qualifiers).isEmpty();
    }

    @Test
    public void listQualifiers_WithExistentQualifier_ShouldReturnOnlyThisQualifier()
    {
        sut.addQualifier(kb, testFixtures.buildQualifier(statement, property, "Test qualifier"));

        List<KBQualifier> qualifiers = sut.listQualifiers(kb, statement);

        assertThat(qualifiers).as("Check that saved qualifier is found").hasSize(1).element(0)
                .hasFieldOrPropertyWithValue("value", "Test qualifier");
    }

    @Test
    public void listQualifiers_WithNonExistentQualifier_ShouldReturnNothing()
    {
        List<KBQualifier> qualifiers = sut.listQualifiers(kb, statement);

        assertThat(qualifiers).isEmpty();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = { "de.tudarmstadt.ukp.inception.kb.model",
            "de.tudarmstadt.ukp.clarin.webanno.model" })
    public static class SpringConfig
    {
        // No content
    }
}
