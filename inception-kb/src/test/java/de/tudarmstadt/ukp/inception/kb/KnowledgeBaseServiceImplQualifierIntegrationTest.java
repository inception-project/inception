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
    public TemporaryFolder temporaryFolder2 = new TemporaryFolder();

    @Autowired
    private TestEntityManager testEntityManager2;

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE2 = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule2 = new SpringMethodRule();

    private KnowledgeBaseServiceImpl sut2;
    private Project project2;
    private KnowledgeBase kb2;
    private TestFixtures testFixtures2;

    private KBConcept concept2;
    private KBProperty property2;
    private KBHandle conceptHandle2;
    private KBHandle propertyHandle2;
    private KBStatement statement2;

    @BeforeClass
    public static void setUpOnce2()
    {
        System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
    }

    @Before
    public void setUp2() throws Exception
    {
        RepositoryProperties repoProps = new RepositoryProperties();
        repoProps.setPath(temporaryFolder2.getRoot());
        EntityManager entityManager = testEntityManager2.getEntityManager();
        testFixtures2 = new TestFixtures(testEntityManager2);
        sut2 = new KnowledgeBaseServiceImpl(repoProps, entityManager);
        project2 = testFixtures2.createProject(PROJECT_NAME);
        kb2 = testFixtures2.buildKnowledgeBase(project2, KB_NAME, Reification.WIKIDATA);
        sut2.registerKnowledgeBase(kb2, sut2.getNativeConfig());
        
        concept2 = testFixtures2.buildConcept();
        property2 = testFixtures2.buildProperty();
        sut2.createConcept(kb2, concept2);
        sut2.createProperty(kb2, property2);
        conceptHandle2 = concept2.toKBHandle();
        propertyHandle2 = property2.toKBHandle();
        statement2 = testFixtures2.buildStatement(conceptHandle2, property2, "Test statement");
        sut2.upsertStatement(kb2, statement2);
    }

    @After
    public void tearDown() throws Exception
    {
        testEntityManager2.clear();
        sut2.destroy();
    }

    @Test
    public void addQualifier_WithUnsavedQualifier_shouldCreateQualifier()
    {
        sut2.addQualifier(kb2, testFixtures2.buildQualifier(statement2, property2, "Test qualifier"));

        List<KBStatement> statements = sut2.listStatements(kb2, conceptHandle2, false);

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
        kb2.setReadOnly(true);
        
        sut2.updateKnowledgeBase(kb2, sut2.getKnowledgeBaseConfig(kb2));

        int qualifierCountBeforeDeletion = sut2.listQualifiers(kb2, statement2).size();
        
        assertThatExceptionOfType(ReadOnlyException.class)
            .isThrownBy(() -> sut2.addQualifier(kb2,
                testFixtures2.buildQualifier(statement2, property2, "Test qualifier")));

        int qualifierCountAfterDeletion = sut2.listQualifiers(kb2, statement2).size();
        
        assertThat(qualifierCountBeforeDeletion)
            .as("Check that statement was not added")
            .isEqualTo(qualifierCountAfterDeletion);
    }

    @Test
    public void upsertQualifier_withUnsavedQualifier_shouldCreateQualifier()
    {
        KBQualifier qualifier = testFixtures2.buildQualifier(statement2, property2, "Test qualifier");
        
        sut2.upsertQualifier(kb2, qualifier);

        sut2.read(kb2, conn -> {
            RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TURTLE, System.out);
            conn.export(rdfWriter);
            System.out.println("------");
            return null;
        });

        List<KBStatement> statements = sut2.listStatements(kb2, conceptHandle2, false);

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
        KBQualifier qualifier = testFixtures2.buildQualifier(statement2, property2, "Test qualifier");
        
        sut2.upsertQualifier(kb2, qualifier);
    
        qualifier.setValue("changed Qualifier");
        
        sut2.upsertQualifier(kb2, qualifier);
        
        assertThat(qualifier.getStatement().getQualifiers())
            .as("Check that KBStatement has updated correctly")
            .hasSize(1)
            .element(0)
            .hasFieldOrProperty("property")
            .hasFieldOrPropertyWithValue("value", "changed Qualifier");
    
        List<KBStatement> statements = sut2.listStatements(kb2, conceptHandle2, false);
        
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
        kb2.setReadOnly(true);
        
        sut2.updateKnowledgeBase(kb2, sut2.getKnowledgeBaseConfig(kb2));
    
        int qualifierCountBeforeDeletion = sut2.listQualifiers(kb2, statement2).size();
        
        KBQualifier qualifier = testFixtures2.buildQualifier(statement2, property2, "Test qualifier");
        
        assertThatExceptionOfType(ReadOnlyException.class)
            .isThrownBy(() -> sut2.upsertQualifier(kb2, qualifier));
    
        int qualifierCountAfterDeletion = sut2.listQualifiers(kb2, statement2).size();
        assertThat(qualifierCountBeforeDeletion)
            .as("Check that statement was not updated")
            .isEqualTo(qualifierCountAfterDeletion);
    }

    @Test
    public void deleteQualifier_WithExistingQualifier_ShouldDeleteQualifier()
    {
        KBQualifier qualifier = testFixtures2.buildQualifier(statement2, property2, "Test qualifier");
        
        sut2.addQualifier(kb2, qualifier);
        
        sut2.deleteQualifier(kb2, qualifier);
        
        List<KBStatement> statements = sut2.listStatements(kb2, conceptHandle2, false);
        List<KBQualifier> qualifiers = sut2.listQualifiers(kb2, statement2);

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
            sut2.deleteQualifier(kb2,
                    testFixtures2.buildQualifier(statement2, property2, "Test qualifier"));
        }).doesNotThrowAnyException();
    }

    @Test
    public void deleteQualifier__WithReadOnlyKnowledgeBase_ShouldDoNothing()
    {
        KBQualifier qualifier = testFixtures2.buildQualifier(statement2, property2, "Test qualifier");
        
        sut2.addQualifier(kb2, qualifier);
        kb2.setReadOnly(true);
        
        sut2.updateKnowledgeBase(kb2, sut2.getKnowledgeBaseConfig(kb2));

        int qualifierCountBeforeDeletion = sut2.listQualifiers(kb2, statement2).size();
        assertThatExceptionOfType(ReadOnlyException.class)
            .isThrownBy(() -> sut2.deleteQualifier(kb2, qualifier));

        int qualifierCountAfterDeletion = sut2.listQualifiers(kb2, statement2).size();
        assertThat(qualifierCountBeforeDeletion).as("Check that statement was not deleted")
            .isEqualTo(qualifierCountAfterDeletion);
    }

    @Test
    public void deleteStatement_WithExistingStatementAndQualifier_ShouldDeleteAll()
    {
        sut2.addQualifier(kb2, testFixtures2.buildQualifier(statement2, property2, "Test qualifier"));

        sut2.deleteStatement(kb2, statement2);

        List<KBStatement> statements = sut2.listStatements(kb2, conceptHandle2, false);
        List<KBQualifier> qualifiers = sut2.listQualifiers(kb2, statement2);
        assertThat(statements)
            .as("Check that the statement was deleted correctly")
            .noneMatch(stmt -> "Test statement".equals(stmt.getValue()));

        assertThat(qualifiers)
            .isEmpty();
    }

    @Test
    public void listQualifiers_WithExistentQualifier_ShouldReturnOnlyThisQualifier()
    {
        sut2.addQualifier(kb2, testFixtures2.buildQualifier(statement2, property2, "Test qualifier"));

        List<KBQualifier> qualifiers = sut2.listQualifiers(kb2, statement2);

        assertThat(qualifiers).as("Check that saved qualifier is found")
            .hasSize(1)
            .element(0)
            .hasFieldOrPropertyWithValue("value", "Test qualifier");
    }

    @Test
    public void listQualifiers_WithNonExistentQualifier_ShouldReturnNothing()
    {
        List<KBQualifier> qualifiers = sut2.listQualifiers(kb2, statement2);

        assertThat(qualifiers)
            .isEmpty();
    }
}
