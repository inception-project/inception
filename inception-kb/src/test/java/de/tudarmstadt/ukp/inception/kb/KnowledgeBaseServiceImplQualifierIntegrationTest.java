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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;

import javax.persistence.EntityManager;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBQualifier;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.reification.Reification;
import de.tudarmstadt.ukp.inception.kb.util.TestFixtures;

@ContextConfiguration(classes =  SpringConfig.class)
@Transactional
@DataJpaTest
public class KnowledgeBaseServiceImplQualifierIntegrationTest {

    private static final String PROJECT_NAME = "Test project";
    private static final String KB_NAME = "Test knowledge base";

    @Rule
    public TemporaryFolder KBSIQIT_temporaryFolder = new TemporaryFolder();

    @Autowired
    private TestEntityManager KBSIQIT_testEntityManager;

    @ClassRule
    public static final SpringClassRule KBSIQIT_SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule KBSIQIT_springMethodRule = new SpringMethodRule();

    private KnowledgeBaseServiceImpl KBSIQIT_sut;
    private Project KBSIQIT_project;
    private KnowledgeBase KBSIQIT_kb;
    private TestFixtures KBSIQIT_testFixtures;

    private KBConcept KBSIQIT_concept;
    private KBProperty KBSIQIT_property;
    private KBHandle KBSIQIT_conceptHandle;
    private KBHandle KBSIQIT_propertyHandle;
    private KBStatement KBSIQIT_statement;

    @BeforeClass
    public static void KBSIQIT_setUpOnce()
    {
        System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
    }

    @Before
    public void KBSIQIT_setUp() throws Exception
    {
        RepositoryProperties repoProps = new RepositoryProperties();
        repoProps.setPath(KBSIQIT_temporaryFolder.getRoot());
        EntityManager entityManager = KBSIQIT_testEntityManager.getEntityManager();
        KBSIQIT_testFixtures = new TestFixtures(KBSIQIT_testEntityManager);
        KBSIQIT_sut = new KnowledgeBaseServiceImpl(repoProps, entityManager);
        KBSIQIT_project = KBSIQIT_testFixtures.createProject(PROJECT_NAME);
        KBSIQIT_kb = KBSIQIT_testFixtures.buildKnowledgeBase(KBSIQIT_project, KB_NAME, Reification.WIKIDATA);
        KBSIQIT_sut.registerKnowledgeBase(KBSIQIT_kb, KBSIQIT_sut.getNativeConfig());
        
        KBSIQIT_concept = KBSIQIT_testFixtures.buildConcept();
        KBSIQIT_property = KBSIQIT_testFixtures.buildProperty();
        KBSIQIT_sut.createConcept(KBSIQIT_kb, KBSIQIT_concept);
        KBSIQIT_sut.createProperty(KBSIQIT_kb, KBSIQIT_property);
        KBSIQIT_conceptHandle = KBSIQIT_concept.toKBHandle();
        KBSIQIT_propertyHandle = KBSIQIT_property.toKBHandle();
        KBSIQIT_statement = KBSIQIT_testFixtures.buildStatement(KBSIQIT_conceptHandle, KBSIQIT_property, "Test statement");
        KBSIQIT_sut.upsertStatement(KBSIQIT_kb, KBSIQIT_statement);
    }

    @After
    public void KBSIQIT_tearDown() throws Exception
    {
        KBSIQIT_testEntityManager.clear();
        KBSIQIT_sut.destroy();
    }

    @Test
    public void addQualifier_WithUnsavedQualifier_shouldCreateQualifier()
    {
        KBSIQIT_sut.addQualifier(KBSIQIT_kb, KBSIQIT_testFixtures.buildQualifier(KBSIQIT_statement, KBSIQIT_property, "Test qualifier"));

        List<KBStatement> statements = KBSIQIT_sut.listStatements(KBSIQIT_kb, KBSIQIT_conceptHandle, false);

        assertThat(statements).hasSize(1);
        
        assertThat(statements.get(0).getQualifiers())
            .hasSize(1)
            .element(0)
            .hasFieldOrProperty("property")
            .hasFieldOrPropertyWithValue("value", "Test qualifier");
    }

    @Test
    public void addQualifier_WithReadOnlyKnowledgeBase_ShouldDoNothing()
    {
        KBSIQIT_kb.setReadOnly(true);
        
        KBSIQIT_sut.updateKnowledgeBase(KBSIQIT_kb, KBSIQIT_sut.getKnowledgeBaseConfig(KBSIQIT_kb));

        int qualifierCountBeforeDeletion = KBSIQIT_sut.listQualifiers(KBSIQIT_kb, KBSIQIT_statement).size();
        
        assertThatExceptionOfType(ReadOnlyException.class)
            .isThrownBy(() -> KBSIQIT_sut.addQualifier(KBSIQIT_kb,
                KBSIQIT_testFixtures.buildQualifier(KBSIQIT_statement, KBSIQIT_property, "Test qualifier")));

        int qualifierCountAfterDeletion = KBSIQIT_sut.listQualifiers(KBSIQIT_kb, KBSIQIT_statement).size();
        
        assertThat(qualifierCountBeforeDeletion)
            .as("Check that statement was not added")
            .isEqualTo(qualifierCountAfterDeletion);
    }

    @Test
    public void upsertQualifier_withUnsavedQualifier_shouldCreateQualifier()
    {
        KBQualifier qualifier = KBSIQIT_testFixtures.buildQualifier(KBSIQIT_statement, KBSIQIT_property, "Test qualifier");
        
        KBSIQIT_sut.upsertQualifier(KBSIQIT_kb, qualifier);

        KBSIQIT_sut.read(KBSIQIT_kb, conn -> {
            RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TURTLE, System.out);
            conn.export(rdfWriter);
            System.out.println("------");
            return null;
        });

        List<KBStatement> statements = KBSIQIT_sut.listStatements(KBSIQIT_kb, KBSIQIT_conceptHandle, false);

        assertThat(qualifier.getStatement().getQualifiers())
            .as("Check that KBStatement has updated correctly")
            .hasSize(1)
            .element(0)
            .hasFieldOrProperty("property")
            .hasFieldOrPropertyWithValue("value", "Test qualifier");
        
        assertThat(statements.get(0).getQualifiers())
            .as("Check that Knowledge Base has updated correctly")
            .hasSize(1)
            .element(0)
            .hasFieldOrProperty("property")
            .hasFieldOrPropertyWithValue("value", "Test qualifier");
    }

    @Test
    public void upsertQualifier_withExistingQualifier_shouldUpdateQualifier()
    {
        KBQualifier qualifier = KBSIQIT_testFixtures.buildQualifier(KBSIQIT_statement, KBSIQIT_property, "Test qualifier");
        
        KBSIQIT_sut.upsertQualifier(KBSIQIT_kb, qualifier);
    
        qualifier.setValue("changed Qualifier");
        
        KBSIQIT_sut.upsertQualifier(KBSIQIT_kb, qualifier);
        
        assertThat(qualifier.getStatement().getQualifiers())
            .as("Check that KBStatement has updated correctly")
            .hasSize(1)
            .element(0)
            .hasFieldOrProperty("property")
            .hasFieldOrPropertyWithValue("value", "changed Qualifier");
    
        List<KBStatement> statements = KBSIQIT_sut.listStatements(KBSIQIT_kb, KBSIQIT_conceptHandle, false);
        
        assertThat(statements.get(0).getQualifiers())
            .as("Check that Knowledge Base has updated correctly")
            .hasSize(1)
            .element(0)
            .hasFieldOrProperty("property")
            .hasFieldOrPropertyWithValue("value", "changed Qualifier");
    }

    @Test
    public void upsertQualifier_withReadOnlyKnowledgeBase_shouldDoNothing()
    {
        KBSIQIT_kb.setReadOnly(true);
        
        KBSIQIT_sut.updateKnowledgeBase(KBSIQIT_kb, KBSIQIT_sut.getKnowledgeBaseConfig(KBSIQIT_kb));
    
        int qualifierCountBeforeDeletion = KBSIQIT_sut.listQualifiers(KBSIQIT_kb, KBSIQIT_statement).size();
        
        KBQualifier qualifier = KBSIQIT_testFixtures.buildQualifier(KBSIQIT_statement, KBSIQIT_property, "Test qualifier");
        
        assertThatExceptionOfType(ReadOnlyException.class)
            .isThrownBy(() -> KBSIQIT_sut.upsertQualifier(KBSIQIT_kb, qualifier));
    
        int qualifierCountAfterDeletion = KBSIQIT_sut.listQualifiers(KBSIQIT_kb, KBSIQIT_statement).size();
        assertThat(qualifierCountBeforeDeletion)
            .as("Check that statement was not updated")
            .isEqualTo(qualifierCountAfterDeletion);
    }

    @Test
    public void deleteQualifier_WithExistingQualifier_ShouldDeleteQualifier()
    {
        KBQualifier qualifier = KBSIQIT_testFixtures.buildQualifier(KBSIQIT_statement, KBSIQIT_property, "Test qualifier");
        
        KBSIQIT_sut.addQualifier(KBSIQIT_kb, qualifier);
        
        KBSIQIT_sut.deleteQualifier(KBSIQIT_kb, qualifier);
        
        List<KBStatement> statements = KBSIQIT_sut.listStatements(KBSIQIT_kb, KBSIQIT_conceptHandle, false);
        List<KBQualifier> qualifiers = KBSIQIT_sut.listQualifiers(KBSIQIT_kb, KBSIQIT_statement);

        assertThat(statements.get(0).getQualifiers())
            .isEmpty();

        assertThat(qualifiers)
            .as("Check that the qualifier was deleted correctly")
            .noneMatch(qua -> "Test qualifier".equals(qua.getValue()));
    }

    @Test
    public void deleteQualifier_WithNonExistentQualifier_ShouldDoNothing()
    {
        assertThatCode(() -> {
            KBSIQIT_sut.deleteQualifier(KBSIQIT_kb,
                    KBSIQIT_testFixtures.buildQualifier(KBSIQIT_statement, KBSIQIT_property, "Test qualifier"));
        }).doesNotThrowAnyException();
    }

    @Test
    public void deleteQualifier__WithReadOnlyKnowledgeBase_ShouldDoNothing()
    {
        KBQualifier qualifier = KBSIQIT_testFixtures.buildQualifier(KBSIQIT_statement, KBSIQIT_property, "Test qualifier");
        
        KBSIQIT_sut.addQualifier(KBSIQIT_kb, qualifier);
        KBSIQIT_kb.setReadOnly(true);
        
        KBSIQIT_sut.updateKnowledgeBase(KBSIQIT_kb, KBSIQIT_sut.getKnowledgeBaseConfig(KBSIQIT_kb));

        int qualifierCountBeforeDeletion = KBSIQIT_sut.listQualifiers(KBSIQIT_kb, KBSIQIT_statement).size();
        assertThatExceptionOfType(ReadOnlyException.class)
            .isThrownBy(() -> KBSIQIT_sut.deleteQualifier(KBSIQIT_kb, qualifier));

        int qualifierCountAfterDeletion = KBSIQIT_sut.listQualifiers(KBSIQIT_kb, KBSIQIT_statement).size();
        assertThat(qualifierCountBeforeDeletion).as("Check that statement was not deleted")
            .isEqualTo(qualifierCountAfterDeletion);
    }

    @Test
    public void deleteStatement_WithExistingStatementAndQualifier_ShouldDeleteAll()
    {
        KBSIQIT_sut.addQualifier(KBSIQIT_kb, KBSIQIT_testFixtures.buildQualifier(KBSIQIT_statement, KBSIQIT_property, "Test qualifier"));

        KBSIQIT_sut.deleteStatement(KBSIQIT_kb, KBSIQIT_statement);

        List<KBStatement> statements = KBSIQIT_sut.listStatements(KBSIQIT_kb, KBSIQIT_conceptHandle, false);
        List<KBQualifier> qualifiers = KBSIQIT_sut.listQualifiers(KBSIQIT_kb, KBSIQIT_statement);
        assertThat(statements)
            .as("Check that the statement was deleted correctly")
            .noneMatch(stmt -> "Test statement".equals(stmt.getValue()));

        assertThat(qualifiers)
            .isEmpty();
    }

    @Test
    public void listQualifiers_WithExistentQualifier_ShouldReturnOnlyThisQualifier()
    {
        KBSIQIT_sut.addQualifier(KBSIQIT_kb, KBSIQIT_testFixtures.buildQualifier(KBSIQIT_statement, KBSIQIT_property, "Test qualifier"));

        List<KBQualifier> qualifiers = KBSIQIT_sut.listQualifiers(KBSIQIT_kb, KBSIQIT_statement);

        assertThat(qualifiers).as("Check that saved qualifier is found")
            .hasSize(1)
            .element(0)
            .hasFieldOrPropertyWithValue("value", "Test qualifier");
    }

    @Test
    public void listQualifiers_WithNonExistentQualifier_ShouldReturnNothing()
    {
        List<KBQualifier> qualifiers = KBSIQIT_sut.listQualifiers(KBSIQIT_kb, KBSIQIT_statement);

        assertThat(qualifiers)
            .isEmpty();
    }
}
