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
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseProperties;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBQualifier;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.reification.Reification;
import de.tudarmstadt.ukp.inception.kb.util.TestFixtures;

@RunWith(Parameterized.class)
@SpringBootTest(classes = SpringConfig.class)
@Transactional
@DataJpaTest
public class KnowledgeBaseServiceImplQualifierIntegrationTest {

    private static final String PROJECT_NAME = "Test project";
    private static final String KB_NAME = "Test knowledge base";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Autowired
    private TestEntityManager testEntityManager;

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private KnowledgeBaseServiceImpl sut;
    private Project project;
    private KnowledgeBase kb;
    private Reification reification;
    private TestFixtures testFixtures;
    private KnowledgeBaseProperties kbProperties = new KnowledgeBaseProperties();

    public KnowledgeBaseServiceImplQualifierIntegrationTest(Reification aReification)
    {
        reification = aReification;
    }

    @Parameterized.Parameters(name = "Reification = {0}")
    public static Collection<Object[]> data() {
        return Arrays.stream(Reification.values())
            .map(r -> new Object[]{r})
            .collect(Collectors.toList());
    }

    @BeforeClass
    public static void setUpOnce()
    {
        System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
    }

    @Before
    public void setUp() throws Exception
    {
        EntityManager entityManager = testEntityManager.getEntityManager();
        testFixtures = new TestFixtures(testEntityManager);
        sut = new KnowledgeBaseServiceImpl(temporaryFolder.getRoot(), entityManager, kbProperties);
        project = testFixtures.createProject(PROJECT_NAME);
        kb = testFixtures.buildKnowledgeBase(project, KB_NAME, reification);
    }

    @After
    public void tearDown() throws Exception
    {
        testEntityManager.clear();
        sut.destroy();
    }

    @Test
    public void addQualifier_WithUnsavedQualifier_shouldCreateQualifier()
    {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        if (kb.getReification().equals(Reification.WIKIDATA)) {
            KBConcept concept = testFixtures.buildConcept();
            KBProperty property = testFixtures.buildProperty();
            KBHandle conceptHandle = sut.createConcept(kb, concept);
            KBHandle propertyHandle = sut.createProperty(kb, property);
            KBStatement statement = testFixtures.buildStatement(conceptHandle,
                propertyHandle, "Test statement");
            sut.initStatement(kb, statement);
            sut.upsertStatement(kb, statement);
            KBQualifier qualifier = testFixtures.buildQualifier(statement, propertyHandle, "Test "
                + "qualifier");
            sut.addQualifier(kb, qualifier);

            List<KBStatement> statements = sut.listStatements(kb, conceptHandle, false);
            assertThat(statements.get(0).getQualifiers())
                .hasSize(1)
                .element(0)
                .hasFieldOrProperty("kbProperty")
                .hasFieldOrPropertyWithValue("value", "Test qualifier");
        }
    }

    @Test
    public void addQualifier_WithReadOnlyKnowledgeBase_ShouldDoNothing()
    {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        if (kb.getReification().equals(Reification.WIKIDATA)) {
            KBConcept concept = testFixtures.buildConcept();
            KBProperty property = testFixtures.buildProperty();
            KBHandle conceptHandle = sut.createConcept(kb, concept);
            KBHandle propertyHandle = sut.createProperty(kb, property);
            KBStatement statement = testFixtures.buildStatement(conceptHandle,
                propertyHandle, "Test statement");
            sut.initStatement(kb, statement);
            sut.upsertStatement(kb, statement);

            kb.setReadOnly(true);
            sut.updateKnowledgeBase(kb, sut.getKnowledgeBaseConfig(kb));

            int qualifierCountBeforeDeletion = sut.listQualifiers(kb, statement).size();
            KBQualifier qualifier = testFixtures.buildQualifier(statement, propertyHandle, "Test "
                + "qualifier");

            sut.addQualifier(kb, qualifier);

            int qualifierCountAfterDeletion = sut.listQualifiers(kb, statement).size();
            assertThat(qualifierCountBeforeDeletion)
                .as("Check that statement was not added")
                .isEqualTo(qualifierCountAfterDeletion);
        }
    }

    @Test
    public void deleteQualifier_WithExistingQualifier_ShouldDeleteQualifier()
    {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        if (kb.getReification().equals(Reification.WIKIDATA)) {
            KBConcept concept = testFixtures.buildConcept();
            KBProperty property = testFixtures.buildProperty();
            KBHandle conceptHandle = sut.createConcept(kb, concept);
            KBHandle propertyHandle = sut.createProperty(kb, property);
            KBStatement statement = testFixtures.buildStatement(conceptHandle,
                propertyHandle, "Test statement");
            sut.initStatement(kb, statement);
            sut.upsertStatement(kb, statement);
            KBQualifier qualifier = testFixtures.buildQualifier(statement, propertyHandle, "Test "
                + "qualifier");
            sut.addQualifier(kb, qualifier);

            sut.deleteQualifier(kb, qualifier);
            List<KBStatement> statements = sut.listStatements(kb, conceptHandle, false);
            List<KBQualifier> qualifiers = sut.listQualifiers(kb, statement);

            assertThat(statements.get(0).getQualifiers())
                .isEmpty();

            assertThat(qualifiers)
                .as("Check that the qualifier was deleted correctly")
                .noneMatch(qua -> "Test qualifier".equals(qua.getValue()));
        }
    }

    @Test
    public void deleteQualifier_WithNonExistentQualifier_ShouldDoNothing()
    {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        if (kb.getReification().equals(Reification.WIKIDATA)) {
            KBConcept concept = testFixtures.buildConcept();
            KBProperty property = testFixtures.buildProperty();
            KBHandle conceptHandle = sut.createConcept(kb, concept);
            KBHandle propertyHandle = sut.createProperty(kb, property);
            KBStatement statement = testFixtures.buildStatement(conceptHandle,
                propertyHandle, "Test statement");
            sut.initStatement(kb, statement);
            KBQualifier qualifier = testFixtures.buildQualifier(statement, propertyHandle, "Test "
                + "qualifier");

            assertThatCode(() -> {
                sut.deleteQualifier(kb, qualifier);
            }).doesNotThrowAnyException();
        }
    }

    @Test
    public void deleteQualifier__WithReadOnlyKnowledgeBase_ShouldDoNothing()
    {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        if (kb.getReification().equals(Reification.WIKIDATA)) {
            KBConcept concept = testFixtures.buildConcept();
            KBProperty property = testFixtures.buildProperty();
            KBHandle conceptHandle = sut.createConcept(kb, concept);
            KBHandle propertyHandle = sut.createProperty(kb, property);
            KBStatement statement = testFixtures
                .buildStatement(conceptHandle, propertyHandle, "Test statement");
            sut.initStatement(kb, statement);
            sut.upsertStatement(kb, statement);
            KBQualifier qualifier = testFixtures
                .buildQualifier(statement, propertyHandle, "Test " + "qualifier");
            sut.addQualifier(kb, qualifier);
            kb.setReadOnly(true);
            sut.updateKnowledgeBase(kb, sut.getKnowledgeBaseConfig(kb));

            int qualifierCountBeforeDeletion = sut.listQualifiers(kb, statement).size();
            sut.deleteQualifier(kb, qualifier);

            int qualifierCountAfterDeletion = sut.listQualifiers(kb, statement).size();
            assertThat(qualifierCountBeforeDeletion).as("Check that statement was not deleted")
                .isEqualTo(qualifierCountAfterDeletion);
        }
    }

    @Test
    public void deleteStatement_WithExistingStatementAndQualifier_ShouldDeleteAll()
    {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        if (kb.getReification().equals(Reification.WIKIDATA)) {
            KBConcept concept = testFixtures.buildConcept();
            KBProperty property = testFixtures.buildProperty();
            KBHandle conceptHandle = sut.createConcept(kb, concept);
            KBHandle propertyHandle = sut.createProperty(kb, property);
            KBStatement statement = testFixtures.buildStatement(conceptHandle,
                propertyHandle, "Test statement");
            sut.initStatement(kb, statement);
            sut.upsertStatement(kb, statement);
            KBQualifier qualifier = testFixtures.buildQualifier(statement, propertyHandle, "Test "
                + "qualifier");
            sut.addQualifier(kb, qualifier);

            sut.deleteStatement(kb, statement);

            List<KBStatement> statements = sut.listStatements(kb, conceptHandle, false);
            List<KBQualifier> qualifiers = sut.listQualifiers(kb, statement);
            assertThat(statements)
                .as("Check that the statement was deleted correctly")
                .noneMatch(stmt -> "Test statement".equals(stmt.getValue()));

            assertThat(qualifiers)
                .isEmpty();
        }
    }

    @Test
    public void upsertQualifier_withUnsavedQualifier_shouldCreateQualifier()
    {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        if (kb.getReification().equals(Reification.WIKIDATA)) {
            KBConcept concept = testFixtures.buildConcept();
            KBProperty property = testFixtures.buildProperty();
            KBHandle conceptHandle = sut.createConcept(kb, concept);
            KBHandle propertyHandle = sut.createProperty(kb, property);
            KBStatement statement = testFixtures.buildStatement(conceptHandle,
                propertyHandle, "Test statement");
            sut.initStatement(kb, statement);
            sut.upsertStatement(kb, statement);
            KBQualifier qualifier = testFixtures.buildQualifier(statement, propertyHandle, "Test "
                + "qualifier");
            sut.upsertQualifier(kb, qualifier);
            assertThat(qualifier.getKbStatement().getQualifiers())
                .as("Check that KBStatement has updated correctly")
                .hasSize(1)
                .element(0)
                .hasFieldOrProperty("kbProperty")
                .hasFieldOrPropertyWithValue("value", "Test qualifier");

            List<KBStatement> statements = sut.listStatements(kb, conceptHandle, false);
            assertThat(statements.get(0).getQualifiers())
                .as("Check that Knowledge Base has updated correctly")
                .hasSize(1)
                .element(0)
                .hasFieldOrProperty("kbProperty")
                .hasFieldOrPropertyWithValue("value", "Test qualifier");
        }
    }

    @Test
    public void upsertQualifier_withExistingQualifier_shouldUpdateQualifier()
    {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        if (kb.getReification().equals(Reification.WIKIDATA)) {
            KBConcept concept = testFixtures.buildConcept();
            KBProperty property = testFixtures.buildProperty();
            KBHandle conceptHandle = sut.createConcept(kb, concept);
            KBHandle propertyHandle = sut.createProperty(kb, property);
            KBStatement statement = testFixtures.buildStatement(conceptHandle,
                propertyHandle, "Test statement");
            sut.initStatement(kb, statement);
            sut.upsertStatement(kb, statement);
            KBQualifier qualifier = testFixtures.buildQualifier(statement, propertyHandle, "Test "
                + "qualifier");
            sut.upsertQualifier(kb, qualifier);

            qualifier.setValue("changed Qualifier");
            sut.upsertQualifier(kb, qualifier);
            assertThat(qualifier.getKbStatement().getQualifiers())
                .as("Check that KBStatement has updated correctly")
                .hasSize(1)
                .element(0)
                .hasFieldOrProperty("kbProperty")
                .hasFieldOrPropertyWithValue("value", "changed Qualifier");

            List<KBStatement> statements = sut.listStatements(kb, conceptHandle, false);
            assertThat(statements.get(0).getQualifiers())
                .as("Check that Knowledge Base has updated correctly")
                .hasSize(1)
                .element(0)
                .hasFieldOrProperty("kbProperty")
                .hasFieldOrPropertyWithValue("value", "changed Qualifier");
        }
    }

    @Test
    public void upsertQualifier_withReadOnlyKnowledgeBase_shouldDoNothing()
    {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        if (kb.getReification().equals(Reification.WIKIDATA)) {
            KBConcept concept = testFixtures.buildConcept();
            KBProperty property = testFixtures.buildProperty();
            KBHandle conceptHandle = sut.createConcept(kb, concept);
            KBHandle propertyHandle = sut.createProperty(kb, property);
            KBStatement statement = testFixtures.buildStatement(conceptHandle,
                propertyHandle, "Test statement");
            sut.initStatement(kb, statement);
            sut.upsertStatement(kb, statement);

            kb.setReadOnly(true);
            sut.updateKnowledgeBase(kb, sut.getKnowledgeBaseConfig(kb));

            int qualifierCountBeforeDeletion = sut.listQualifiers(kb, statement).size();
            KBQualifier qualifier = testFixtures.buildQualifier(statement, propertyHandle, "Test "
                + "qualifier");
            sut.upsertQualifier(kb, qualifier);

            int qualifierCountAfterDeletion = sut.listQualifiers(kb, statement).size();
            assertThat(qualifierCountBeforeDeletion)
                .as("Check that statement was not updated")
                .isEqualTo(qualifierCountAfterDeletion);
        }
    }

    @Test
    public void listQualifiers_WithExistentQualifier_ShouldReturnOnlyThisQualifier()
    {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        if (kb.getReification().equals(Reification.WIKIDATA)) {
            KBConcept concept = testFixtures.buildConcept();
            KBProperty property = testFixtures.buildProperty();
            KBHandle conceptHandle = sut.createConcept(kb, concept);
            KBHandle propertyHandle = sut.createProperty(kb, property);
            KBStatement statement = testFixtures
                .buildStatement(conceptHandle, propertyHandle, "Test statement");
            sut.initStatement(kb, statement);
            sut.upsertStatement(kb, statement);
            KBQualifier qualifier = testFixtures
                .buildQualifier(statement, propertyHandle, "Test " + "qualifier");
            sut.addQualifier(kb, qualifier);

            List<KBQualifier> qualifiers = sut.listQualifiers(kb, statement);

            assertThat(qualifiers).as("Check that saved qualifier is found").hasSize(1).element(0)
                .hasFieldOrPropertyWithValue("value", "Test qualifier");
        }
    }

    @Test
    public void listQualifiers_WithNonExistentQualifier_ShouldReturnNothing()
    {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        if (kb.getReification().equals(Reification.WIKIDATA)) {
            KBConcept concept = testFixtures.buildConcept();
            KBProperty property = testFixtures.buildProperty();
            KBHandle conceptHandle = sut.createConcept(kb, concept);
            KBHandle propertyHandle = sut.createProperty(kb, property);
            KBStatement statement = testFixtures.buildStatement(conceptHandle,
                propertyHandle, "Test statement");
            sut.initStatement(kb, statement);
            sut.upsertStatement(kb, statement);

            List<KBQualifier> qualifiers = sut.listQualifiers(kb, statement);

            assertThat(qualifiers)
                .isEmpty();
        }
    }

}
