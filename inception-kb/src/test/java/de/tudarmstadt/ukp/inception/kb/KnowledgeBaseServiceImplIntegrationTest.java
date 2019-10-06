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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.reification.Reification;
import de.tudarmstadt.ukp.inception.kb.util.TestFixtures;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseProfile;

@RunWith(Parameterized.class)
@ContextConfiguration(classes =  SpringConfig.class)
@Transactional
@DataJpaTest
public class KnowledgeBaseServiceImplIntegrationTest  {

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

    public KnowledgeBaseServiceImplIntegrationTest(Reification aReification) {
        reification = aReification;
    }

    @Parameterized.Parameters(name = "Reification = {0}")
    public static Collection<Object[]> data()
    {
        return Arrays.stream(Reification.values()).map(r -> new Object[] { r })
            .collect(Collectors.toList());
    }

    @BeforeClass
    public static void setUpOnce() {
        System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
    }

    @Before
    public void setUp() throws Exception {
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
    public void registerKnowledgeBaseWithNewKnowledgeBaseShouldSaveNewKnowledgeBase() {

        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        KnowledgeBase savedKb = testEntityManager.find(KnowledgeBase.class, kb.getRepositoryId());
        assertThat(savedKb)
            .as("Check that knowledge base was saved correctly")
            .hasFieldOrPropertyWithValue("name", KB_NAME)
            .hasFieldOrPropertyWithValue("project", project)
            .extracting("repositoryId").isNotNull();
    }

    @Test
    public void getKnowledgeBasesWithOneStoredKnowledgeBaseShouldReturnStoredKnowledgeBase() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        List<KnowledgeBase> knowledgeBases = sut.getKnowledgeBases(project);

        assertThat(knowledgeBases)
            .as("Check that only the previously created knowledge base is found")
            .hasSize(1)
            .contains(kb);
    }

    @Test
    public void getKnowledgeBasesWithoutKnowledgeBasesShouldReturnEmptyList() {
        Project project = createProject("Empty project");

        List<KnowledgeBase> knowledgeBases = sut.getKnowledgeBases(project);

        assertThat(knowledgeBases)
            .as("Check that no knowledge base is found")
            .isEmpty();
    }

    @Test
    public void knowledgeBaseExistsWithExistingKnowledgeBaseShouldReturnTrue() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        assertThat(sut.knowledgeBaseExists(project, kb.getName()))
            .as("Check that knowledge base with given name already exists")
            .isTrue();
    }

    @Test
    public void knowledgeBaseExistsWithNonexistentKnowledgeBaseShouldReturnFalse() {

        assertThat(sut.knowledgeBaseExists(project, kb.getName()))
            .as("Check that knowledge base with given name does not already exists")
            .isFalse();
    }

    @Test
    public void updateKnowledgeBaseWithValidValuesShouldUpdateKnowledgeBase() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        kb.setName("New name");
        kb.setClassIri(OWL.CLASS);
        kb.setSubclassIri(OWL.NOTHING);
        kb.setTypeIri(OWL.THING);
        kb.setDescriptionIri(IriConstants.SCHEMA_DESCRIPTION);
        kb.setLabelIri(RDFS.LITERAL);
        kb.setPropertyTypeIri(OWL.OBJECTPROPERTY);
        kb.setReadOnly(true);
        kb.setEnabled(false);
        kb.setBasePrefix("MyBasePrefix");
        ValueFactory vf = SimpleValueFactory.getInstance();
        IRI rootConcept1 = vf.createIRI("http://www.ics.forth.gr/isl/CRMinf/I1_Argumentation");
        IRI rootConcept2 = vf.createIRI("file:/data-to-load/07bde589-588c-4f0d-8715-c71c0ba2bfdb/crm-extensions/F10_Person");
        List<IRI> concepts = new ArrayList<IRI>();
        concepts.add(rootConcept1);
        concepts.add(rootConcept2);
        kb.setRootConcepts(concepts);
        sut.updateKnowledgeBase(kb, sut.getNativeConfig());

        KnowledgeBase savedKb = testEntityManager.find(KnowledgeBase.class, kb.getRepositoryId());
        assertThat(savedKb)
            .as("Check that knowledge base was updated correctly")
            .hasFieldOrPropertyWithValue("name", "New name")
            .hasFieldOrPropertyWithValue("classIri", OWL.CLASS)
            .hasFieldOrPropertyWithValue("subclassIri", OWL.NOTHING)
            .hasFieldOrPropertyWithValue("typeIri", OWL.THING)
            .hasFieldOrPropertyWithValue("descriptionIri", IriConstants.SCHEMA_DESCRIPTION)
            .hasFieldOrPropertyWithValue("name", "New name")
            .hasFieldOrPropertyWithValue("readOnly", true)
            .hasFieldOrPropertyWithValue("enabled", false)
            .hasFieldOrPropertyWithValue("labelIri", RDFS.LITERAL)
            .hasFieldOrPropertyWithValue("propertyTypeIri", OWL.OBJECTPROPERTY)
            .hasFieldOrPropertyWithValue("basePrefix", "MyBasePrefix")
            .hasFieldOrPropertyWithValue("rootConcepts", Arrays.asList(rootConcept1, rootConcept2));

    }

    @Test
    public void updateKnowledgeBaseWithUnregisteredKnowledgeBaseShouldThrowIllegalStateException() {
        assertThatExceptionOfType(IllegalStateException.class)
            .as("Check that updating knowledge base requires registration")
            .isThrownBy(() -> sut.updateKnowledgeBase(kb, sut.getNativeConfig()));
    }

    @Test
    public void removeKnowledgeBaseWithStoredKnowledgeBaseShouldDeleteKnowledgeBase() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        sut.removeKnowledgeBase(kb);

        List<KnowledgeBase> knowledgeBases = sut.getKnowledgeBases(project);
        assertThat(knowledgeBases)
            .as("Check that the knowledge base has been deleted")
            .hasSize(0);
    }

    @Test
    public void removeKnowledgeBaseWithUnregisteredKnowledgeBaseShouldThrowIllegalStateException() {
        assertThatExceptionOfType(IllegalStateException.class)
            .as("Check that updating knowledge base requires registration")
            .isThrownBy(() -> sut.removeKnowledgeBase(kb));
    }

    @Test
    public void clearWithNonemptyKnowledgeBaseShouldDeleteAllCustomEntities() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        sut.createConcept(kb, buildConcept());
        sut.createProperty(kb, buildProperty());

        sut.clear(kb);

        List<KBObject> handles = new ArrayList<>();
        handles.addAll(sut.listAllConcepts(kb, false));
        handles.addAll(sut.listProperties(kb, false));
        assertThat(handles)
            .as("Check that no custom entities are found after clearing")
            .isEmpty();
    }

    @Test
    public void clearWithNonemptyKnowledgeBaseShouldNotDeleteImplicitEntities() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        sut.createConcept(kb, buildConcept());
        sut.createProperty(kb, buildProperty());

        sut.clear(kb);

        List<KBObject> handles = new ArrayList<>();
        handles.addAll(sut.listAllConcepts(kb, true));
        handles.addAll(sut.listProperties(kb, true));
        assertThat(handles)
            .as("Check that only entities with implicit namespace are found after clearing")
            .allMatch(this::hasImplicitNamespace);
    }

    @Test
    public void emptyWithEmptyKnowledgeBaseShouldReturnTrue() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        boolean isEmpty = sut.isEmpty(kb);

        assertThat(isEmpty)
            .as("Check that knowledge base is empty")
            .isTrue();
    }

    @Test
    public void emptyWithNonemptyKnowledgeBaseShouldReturnFalse() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        sut.createConcept(kb, buildConcept());

        boolean isEmpty = sut.isEmpty(kb);

        assertThat(isEmpty)
            .as("Check that knowledge base is not empty")
            .isFalse();
    }

    @Test
    public void nonemptyWithEmptyKnowledgeBaseShouldReturnTrue() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        
        sut.defineBaseProperties(kb);
        
        List<KBProperty> listProperties = sut.listProperties(kb, true);
        Stream<String> listIdentifier = listProperties.stream().map(KBObject::getIdentifier);
        String[] expectedProps = { kb.getSubclassIri().stringValue(),
                kb.getLabelIri().stringValue(), kb.getDescriptionIri().stringValue(),
                kb.getTypeIri().stringValue() };
        
        assertEquals(listProperties.size(), 5);
        assertThat(listIdentifier).as("Check that base properties are created")
                .contains(expectedProps);
    }

    @Test
    public void createConceptWithEmptyIdentifierShouldCreateNewConcept() {
        KBConcept concept = buildConcept();

        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        sut.createConcept(kb, concept);

        KBConcept savedConcept = sut.readConcept(kb, concept.getIdentifier(), true).get();
        assertThat(savedConcept)
            .as("Check that concept was saved correctly")
            .hasFieldOrPropertyWithValue("description", concept.getDescription())
            .hasFieldOrPropertyWithValue("name", concept.getName());
    }
    
    @Test
    public void createConceptWithCustomBasePrefixShouldCreateNewConceptWithCustomPrefix()
    {
        KBConcept concept = buildConcept();

        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        String customPrefix = "http://www.ukp.informatik.tu-darmstadt.de/customPrefix#";
        kb.setBasePrefix(customPrefix);
        sut.createConcept(kb, concept);

        KBConcept savedConcept = sut.readConcept(kb, concept.getIdentifier(), true).get();
        assertThat(savedConcept).as("Check that concept was saved correctly")
                .hasFieldOrPropertyWithValue("description", concept.getDescription())
                .hasFieldOrPropertyWithValue("name", concept.getName());

        String id = savedConcept.getIdentifier();
        String savedConceptPrefix = id.substring(0, id.lastIndexOf("#") + 1);
        assertEquals(customPrefix, savedConceptPrefix);
    }

    @Test
    public void createConceptWithNonemptyIdentifierShouldThrowIllegalArgumentException() {
        KBConcept concept = new KBConcept();
        concept.setIdentifier("Nonempty Identifier");

        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        assertThatIllegalArgumentException()
            .as("Check that creating a concept requires empty identifier")
            .isThrownBy(() -> sut.createConcept(kb, concept) )
            .withMessage("Identifier must be empty on create");
    }

    @Test
    public void createConceptWithReadOnlyKnowledgeBaseShouldDoNothing() {
        KBConcept concept = buildConcept();
        kb.setReadOnly(true);
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        assertThatExceptionOfType(ReadOnlyException.class)
            .isThrownBy(() -> sut.createConcept(kb, concept));
    }

    @Test
    public void readConceptWithExistingConceptShouldReturnSavedConcept() {
        KBConcept concept = buildConcept();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        sut.createConcept(kb, concept);

        KBConcept savedConcept = sut.readConcept(kb, concept.getIdentifier(), true).get();

        assertThat(savedConcept)
            .as("Check that concept was read correctly")
            .hasFieldOrPropertyWithValue("description", concept.getDescription())
            .hasFieldOrPropertyWithValue("name", concept.getName());
    }

    @Test
    public void readConceptWithNonexistentConceptShouldReturnEmptyResult() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        Optional<KBConcept> savedConcept = sut.readConcept(kb, "https://nonexistent.identifier.test", true);

        assertThat(savedConcept.isPresent())
            .as("Check that no concept was read")
            .isFalse();
    }

    @Test
    public void updateConceptWithAlteredConceptShouldUpdateConcept() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBConcept concept = buildConcept();
        sut.createConcept(kb, concept);

        concept.setDescription("New description");
        concept.setName("New name");
        sut.updateConcept(kb, concept);

        KBConcept savedConcept = sut.readConcept(kb, concept.getIdentifier(), true).get();
        assertThat(savedConcept)
            .as("Check that concept was updated correctly")
            .hasFieldOrPropertyWithValue("description", "New description")
            .hasFieldOrPropertyWithValue("name", "New name");
    }

    @Test
    // TODO: Check whether this is a feature or not
    public void updateConceptWithNonexistentConceptShouldCreateConcept() {
        KBConcept concept = buildConcept();
        concept.setIdentifier("https://nonexistent.identifier.test");
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        sut.updateConcept(kb, concept);

        KBConcept savedConcept = sut.readConcept(kb, "https://nonexistent.identifier.test", true).get();
        assertThat(savedConcept)
            .hasFieldOrPropertyWithValue("description", concept.getDescription())
            .hasFieldOrPropertyWithValue("name", concept.getName());
    }

    @Test
    public void updateConceptWithConceptWithBlankIdentifierShouldThrowIllegalArgumentException() {
        KBConcept concept = buildConcept();
        concept.setIdentifier("");
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        assertThatIllegalArgumentException()
            .as("Check that updating a concept requires nonempty identifier")
            .isThrownBy(() -> sut.updateConcept(kb, concept))
            .withMessage("Identifier cannot be empty on update");
    }

    @Test
    public void updateConceptWithConceptWithNullIdentifierShouldThrowIllegalArgumentException() {
        KBConcept concept = buildConcept();
        concept.setIdentifier(null);
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        assertThatIllegalArgumentException()
            .as("Check that updating a concept requires non-null identifier")
            .isThrownBy(() -> sut.updateConcept(kb, concept))
            .withMessage("Identifier cannot be empty on update");
    }

    @Test
    public void updateConceptWithReadOnlyKnowledgeBaseShouldDoNothing() {
        KBConcept concept = buildConcept();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        sut.createConcept(kb, concept);
        setReadOnly(kb);

        concept.setDescription("New description");
        concept.setName("New name");

        assertThatExceptionOfType(ReadOnlyException.class)
            .isThrownBy(() -> sut.updateConcept(kb, concept));

        KBConcept savedConcept = sut.readConcept(kb, concept.getIdentifier(), true).get();
        assertThat(savedConcept)
            .as("Check that concept has not been updated")
            .hasFieldOrPropertyWithValue("description", "Concept description")
            .hasFieldOrPropertyWithValue("name", "Concept name");
    }

    @Test
    public void deleteConceptWithConceptReferencedAsObjectShouldDeleteConceptAndStatement() {
        KBInstance instance = buildInstance();
        KBProperty property = buildProperty();
        KBConcept concept = buildConcept();

        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        sut.createInstance(kb, instance);
        sut.createProperty(kb, property);
        sut.createConcept(kb, concept);

        sut.upsertStatement(kb,
                buildStatement(kb, instance.toKBHandle(), property, concept.getIdentifier()));

        sut.deleteConcept(kb, concept);

        assertThat(sut.listStatementsWithPredicateOrObjectReference(kb, concept.getIdentifier()))
            .isEmpty();

        Optional<KBConcept> savedConcept = sut.readConcept(kb, concept.getIdentifier(), true);
        assertThat(savedConcept.isPresent())
            .as("Check that concept was not found after delete")
            .isFalse();

    }

    @Test
    public void deleteConceptWithExistingConceptShouldDeleteConcept() {
        KBConcept concept = buildConcept();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        sut.createConcept(kb, concept);

        sut.deleteConcept(kb, concept);

        Optional<KBConcept> savedConcept = sut.readConcept(kb, concept.getIdentifier(), true);
        assertThat(savedConcept.isPresent())
            .as("Check that concept was not found after delete")
            .isFalse();
    }

    @Test
    public void deleteConceptWithNonexistentConceptShouldNoNothing() {
        KBConcept concept = buildConcept();
        concept.setIdentifier("https://nonexistent.identifier.test");
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        assertThatCode(() -> sut.deleteConcept(kb, concept))
            .as("Check that deleting non-existant concept does nothing")
            .doesNotThrowAnyException();
    }

    @Test
    public void deleteConceptWithReadOnlyKnowledgeBaseShouldDoNothing() {
        KBConcept concept = buildConcept();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        sut.createConcept(kb, concept);
        setReadOnly(kb);

        assertThatExceptionOfType(ReadOnlyException.class)
                .isThrownBy(() -> sut.deleteConcept(kb, concept));

        Optional<KBConcept> savedConcept = sut.readConcept(kb, concept.getIdentifier(), true);
        assertThat(savedConcept.isPresent())
            .as("Check that concept was not deleted")
            .isTrue();
    }

    @Test
    public void listConceptsWithASavedConceptAndNotAllShouldFindOneConcept() {
        KBConcept concept = buildConcept();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        sut.createConcept(kb, concept);

        List<KBHandle> concepts = sut.listAllConcepts(kb, false);

        assertThat(concepts)
            .as("Check that concepts contain the one, saved item")
            .hasSize(1)
            .element(0)
            .hasFieldOrPropertyWithValue("identifier", concept.getIdentifier())
            .hasFieldOrProperty("name")
            .matches(h -> h.getIdentifier().startsWith(IriConstants.INCEPTION_NAMESPACE));
    }

    @Test
    public void listConceptsWithNoSavedConceptAndAllShouldFindRdfConcepts() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        List<KBHandle> concepts = sut.listAllConcepts(kb, true);

        assertThat(concepts)
            .as("Check that all concepts have implicit namespaces")
            .allMatch(this::hasImplicitNamespace);
    }

    @Test
    public void createPropertyWithEmptyIdentifierShouldCreateNewProperty() {
        KBProperty property = buildProperty();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        sut.createProperty(kb, property);

        KBProperty savedProperty = sut.readProperty(kb, property.getIdentifier()).get();
        assertThat(savedProperty)
            .as("Check that property was created correctly")
            .hasNoNullFieldsOrPropertiesExcept("language")
            .hasFieldOrPropertyWithValue("description", property.getDescription())
            .hasFieldOrPropertyWithValue("domain", property.getDomain())
            .hasFieldOrPropertyWithValue("name", property.getName())
            .hasFieldOrPropertyWithValue("range", property.getRange());
    }
    
    @Test
    public void createPropertyWithCustomBasePrefixShouldCreateNewPropertyWithCustomPrefix()
    {
        assumeFalse("Wikidata reification has hardcoded property prefix", 
                Reification.WIKIDATA.equals(kb.getReification()));
        
        KBProperty property = buildProperty();

        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        String customPrefix = "http://www.ukp.informatik.tu-darmstadt.de/customPrefix#";
        kb.setBasePrefix(customPrefix);
        sut.createProperty(kb, property);

        KBProperty savedProperty = sut.readProperty(kb, property.getIdentifier()).get();
        assertThat(savedProperty).as("Check that property was saved correctly")
                .hasFieldOrPropertyWithValue("description", property.getDescription())
                .hasFieldOrPropertyWithValue("name", property.getName());

        String id = savedProperty.getIdentifier();
        String savedPropertyPrefix = id.substring(0, id.lastIndexOf("#") + 1);
        assertEquals(customPrefix, savedPropertyPrefix);
    }

    @Test
    public void createPropertyWithNonemptyIdentifierShouldThrowIllegalArgumentException() {
        KBProperty property = buildProperty();
        property.setIdentifier("Nonempty Identifier");

        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        assertThatIllegalArgumentException()
            .as("Check that creating a property requires empty identifier")
            .isThrownBy(() -> sut.createProperty(kb, property) )
            .withMessage("Identifier must be empty on create");
    }

    @Test
    public void createPropertyWithReadOnlyKnowledgeBaseShouldDoNothing() {
        KBProperty property = buildProperty();
        kb.setReadOnly(true);
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        assertThatExceptionOfType(ReadOnlyException.class)
            .isThrownBy(() -> sut.createProperty(kb, property));
    }

    @Test
    public void readPropertyWithExistingConceptShouldReturnSavedProperty() {
        KBProperty property = buildProperty();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        sut.createProperty(kb, property);

        KBProperty savedProperty = sut.readProperty(kb, property.getIdentifier()).get();

        assertThat(savedProperty)
            .as("Check that property was saved correctly")
            .hasNoNullFieldsOrPropertiesExcept("language")
            .hasFieldOrPropertyWithValue("description", property.getDescription())
            .hasFieldOrPropertyWithValue("domain", property.getDomain())
            .hasFieldOrPropertyWithValue("name", property.getName())
            .hasFieldOrPropertyWithValue("range", property.getRange());
    }

    @Test
    public void readPropertyWithNonexistentPropertyShouldReturnEmptyResult() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        Optional<KBProperty> savedProperty = sut.readProperty(kb, "https://nonexistent.identifier.test");

        assertThat(savedProperty.isPresent())
            .as("Check that no property was read")
            .isFalse();
    }

    @Test
    public void updatePropertyWithAlteredPropertyShouldUpdateProperty() {
        KBProperty property = buildProperty();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        sut.createProperty(kb, property);

        property.setDescription("New property description");
        property.setDomain("https://new.schema.com/#domain");
        property.setName("New property name");
        property.setRange("https://new.schema.com/#range");
        sut.updateProperty(kb, property);

        KBProperty savedProperty = sut.readProperty(kb, property.getIdentifier()).get();
        assertThat(savedProperty)
            .as("Check that property was updated correctly")
            .hasFieldOrPropertyWithValue("description", property.getDescription())
            .hasFieldOrPropertyWithValue("domain", property.getDomain())
            .hasFieldOrPropertyWithValue("name", property.getName())
            .hasFieldOrPropertyWithValue("range", property.getRange());
    }

    @Test
    // TODO: Check whether this is a feature or not
    public void updatePropertyWithNonexistentPropertyShouldCreateProperty() {
        KBProperty property = buildProperty();
        property.setIdentifier("https://nonexistent.identifier.test");
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        sut.updateProperty(kb, property);

        KBProperty savedProperty = sut.readProperty(kb, "https://nonexistent.identifier.test").get();
        assertThat(savedProperty)
            .as("Check that property was updated correctly")
            .hasFieldOrPropertyWithValue("description", property.getDescription())
            .hasFieldOrPropertyWithValue("domain", property.getDomain())
            .hasFieldOrPropertyWithValue("name", property.getName())
            .hasFieldOrPropertyWithValue("range", property.getRange());
    }

    @Test
    public void updatePropertyWithPropertyWithBlankIdentifierShouldThrowIllegalArgumentException() {
        KBProperty property = buildProperty();
        property.setIdentifier("");
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        assertThatIllegalArgumentException()
            .as("Check that updating a property requires nonempty identifier")
            .isThrownBy(() -> sut.updateProperty(kb, property))
            .withMessage("Identifier cannot be empty on update");
    }

    @Test
    public void updatePropertyWithPropertyWithNullIdentifierShouldThrowIllegalArgumentException() {
        KBProperty property = buildProperty();
        property.setIdentifier(null);
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        assertThatIllegalArgumentException()
            .as("Check that updating a property requires nonempty identifier")
            .isThrownBy(() -> sut.updateProperty(kb, property))
            .withMessage("Identifier cannot be empty on update");
    }

    @Test
    public void updatePropertyWithReadOnlyKnowledgeBaseShouldDoNothing() {
        KBProperty property = buildProperty();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        sut.createProperty(kb, property);
        setReadOnly(kb);

        property.setDescription("New property description");
        property.setDomain("https://new.schema.com/#domain");
        property.setName("New property name");
        property.setRange("https://new.schema.com/#range");
        
        assertThatExceptionOfType(ReadOnlyException.class)
            .isThrownBy(() -> sut.updateProperty(kb, property));

        KBProperty savedProperty = sut.readProperty(kb, property.getIdentifier()).get();
        assertThat(savedProperty)
            .as("Check that property has not been updated")
            .hasFieldOrPropertyWithValue("description", "Property description")
            .hasFieldOrPropertyWithValue("domain", "https://test.schema.com/#domain")
            .hasFieldOrPropertyWithValue("name", "Property name")
            .hasFieldOrPropertyWithValue("range", "https://test.schema.com/#range");
    }

    @Test
    public void deletePropertyWithExistingPropertyShouldDeleteProperty() {
        KBProperty property = buildProperty();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        sut.createProperty(kb, property);

        sut.deleteProperty(kb, property);

        Optional<KBProperty> savedProperty = sut.readProperty(kb, property.getIdentifier());
        assertThat(savedProperty.isPresent())
            .as("Check that property was not found after delete")
            .isFalse();
    }

    @Test
    public void deletePropertyWithNotExistingPropertyShouldNoNothing() {
        KBProperty property = buildProperty();
        property.setIdentifier("https://nonexistent.identifier.test");
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        assertThatCode(() -> sut.deleteProperty(kb, property))
            .as("Check that deleting non-existant property does nothing")
            .doesNotThrowAnyException();
    }

    @Test
    public void deletePropertyWithReadOnlyKnowledgeBaseShouldNoNothing() {
        KBProperty property = buildProperty();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        sut.createProperty(kb, property);
        setReadOnly(kb);

        assertThatExceptionOfType(ReadOnlyException.class)
            .isThrownBy(() -> sut.deleteProperty(kb, property));
        
        Optional<KBProperty> savedProperty = sut.readProperty(kb, property.getIdentifier());
        assertThat(savedProperty.isPresent())
            .as("Check that property was not deleted")
            .isTrue();
    }

    @Test
    public void listPropertiesWithASavedConceptAndNotAllShouldFindOneConcept() {
        KBProperty property = buildProperty();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        sut.createProperty(kb, property);

        List<KBProperty> properties = sut.listProperties(kb, false);

        assertThat(properties)
            .as("Check that properties contain the one, saved item")
            .hasSize(1)
            .element(0)
            .hasFieldOrPropertyWithValue("identifier", property.getIdentifier())
            .hasFieldOrProperty("name");
    }

    @Test
    public void listPropertiesWithNoSavedConceptAndAllShouldFindRdfConcepts() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        List<KBProperty> properties = sut.listProperties(kb, true);

        assertThat(properties)
            .as("Check that all properties have implicit namespaces")
            .allMatch(this::hasImplicitNamespace);
    }

    @Test
    public void createInstanceWithEmptyIdentifierShouldCreateNewInstance() {
        KBInstance instance = buildInstance();

        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        sut.createInstance(kb, instance);

        KBInstance savedInstance = sut.readInstance(kb, instance.getIdentifier()).get();
        assertThat(savedInstance)
            .as("Check that instance was saved correctly")
            .hasFieldOrPropertyWithValue("description", instance.getDescription())
            .hasFieldOrPropertyWithValue("name", instance.getName());
    }
    
    @Test
    public void createInstanceWithCustomBasePrefixShouldCreateNewInstanceWithCustomPrefix()
    {
        KBInstance instance = buildInstance();

        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        String customPrefix = "http://www.ukp.informatik.tu-darmstadt.de/customPrefix#";
        kb.setBasePrefix(customPrefix);
        sut.createInstance(kb, instance);

        KBInstance savedInstance = sut.readInstance(kb, instance.getIdentifier()).get();
        assertThat(savedInstance).as("Check that Instance was saved correctly")
                .hasFieldOrPropertyWithValue("description", instance.getDescription())
                .hasFieldOrPropertyWithValue("name", instance.getName());

        String id = savedInstance.getIdentifier();
        String savedInstancePrefix = id.substring(0, id.lastIndexOf("#") + 1);
        assertEquals(customPrefix, savedInstancePrefix);
    }

    @Test
    public void createInstanceWithNonemptyIdentifierShouldThrowIllegalArgumentException() {
        KBInstance instance = new KBInstance();
        instance.setIdentifier("Nonempty Identifier");

        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        assertThatIllegalArgumentException()
            .as("Check that creating a instance requires empty identifier")
            .isThrownBy(() -> sut.createInstance(kb, instance) )
            .withMessage("Identifier must be empty on create");
    }

    @Test
    public void createInstanceWithReadOnlyKnowledgeBaseShouldDoNothing() {
        KBInstance instance = buildInstance();
        kb.setReadOnly(true);
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        assertThatExceptionOfType(ReadOnlyException.class)
            .isThrownBy(() -> sut.createInstance(kb, instance));
    }

    @Test
    public void readInstanceWithExistingInstanceShouldReturnSavedInstance() {
        KBInstance instance = buildInstance();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        sut.createInstance(kb, instance);

        KBInstance savedInstance = sut.readInstance(kb, instance.getIdentifier()).get();

        assertThat(savedInstance)
            .as("Check that instance was read correctly")
            .hasFieldOrPropertyWithValue("description", instance.getDescription())
            .hasFieldOrPropertyWithValue("name", instance.getName());
    }

    @Test
    public void readInstanceWithNonexistentInstanceShouldReturnEmptyResult() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        Optional<KBInstance> savedInstance = sut.readInstance(kb, "https://nonexistent.identifier.test");

        assertThat(savedInstance.isPresent())
            .as("Check that no instance was read")
            .isFalse();
    }

    @Test
    public void updateInstanceWithAlteredInstanceShouldUpdateInstance() {
        KBInstance instance = buildInstance();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        sut.createInstance(kb, instance);

        instance.setDescription("New description");
        instance.setName("New name");
        sut.updateInstance(kb, instance);

        KBInstance savedInstance = sut.readInstance(kb, instance.getIdentifier()).get();
        assertThat(savedInstance)
            .as("Check that instance was updated correctly")
            .hasFieldOrPropertyWithValue("description", "New description")
            .hasFieldOrPropertyWithValue("name", "New name");
    }

    @Test
    // TODO: Check whether this is a feature or not
    public void updateInstanceWithNonexistentInstanceShouldCreateInstance() {
        KBInstance instance = buildInstance();
        instance.setIdentifier("https://nonexistent.identifier.test");
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        sut.updateInstance(kb, instance);

        KBInstance savedInstance = sut.readInstance(kb, "https://nonexistent.identifier.test").get();
        assertThat(savedInstance)
            .hasFieldOrPropertyWithValue("description", instance.getDescription())
            .hasFieldOrPropertyWithValue("name", instance.getName());
    }

    @Test
    public void updateInstanceWithInstanceWithBlankIdentifierShouldThrowIllegalArgumentException() {
        KBInstance instance = buildInstance();
        instance.setIdentifier("");
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        assertThatIllegalArgumentException()
            .as("Check that updating a instance requires nonempty identifier")
            .isThrownBy(() -> sut.updateInstance(kb, instance))
            .withMessage("Identifier cannot be empty on update");
    }

    @Test
    public void updateInstanceWithInstanceWithNullIdentifierShouldThrowIllegalArgumentException() {
        KBInstance instance = buildInstance();
        instance.setIdentifier(null);
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        assertThatIllegalArgumentException()
            .as("Check that updating a instance requires non-null identifier")
            .isThrownBy(() -> sut.updateInstance(kb, instance))
            .withMessage("Identifier cannot be empty on update");
    }

    @Test
    public void updateInstanceWithReadOnlyKnowledgeBaseShouldDoNothing() {
        KBInstance instance = buildInstance();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        sut.createInstance(kb, instance);
        setReadOnly(kb);

        instance.setDescription("New description");
        instance.setName("New name");

        assertThatExceptionOfType(ReadOnlyException.class)
            .isThrownBy(() ->  sut.updateInstance(kb, instance));
        
        KBInstance savedInstance = sut.readInstance(kb, instance.getIdentifier()).get();
        assertThat(savedInstance)
            .as("Check that instance has not been updated")
            .hasFieldOrPropertyWithValue("description", "Instance description")
            .hasFieldOrPropertyWithValue("name", "Instance name");
    }

    @Test
    public void deleteInstanceWithExistingInstanceShouldDeleteInstance() {
        KBInstance instance = buildInstance();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        sut.createInstance(kb, instance);

        sut.deleteInstance(kb, instance);

        Optional<KBInstance> savedInstance = sut.readInstance(kb, instance.getIdentifier());
        assertThat(savedInstance.isPresent())
            .as("Check that instance was not found after delete")
            .isFalse();
    }

    @Test
    public void deleteInstanceWithInstanceReferencedAsObjectShouldDeleteInstanceAndStatement()
    {
        KBInstance instance = buildInstance();
        KBProperty property = buildProperty();
        KBInstance instance2 = buildInstance();

        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        sut.createInstance(kb, instance);
        sut.createProperty(kb, property);
        sut.createInstance(kb, instance2);

        sut.upsertStatement(kb,
                buildStatement(kb, instance.toKBHandle(), property, instance2.getIdentifier()));

        sut.deleteInstance(kb, instance2);

        assertThat(sut.listStatementsWithPredicateOrObjectReference(kb, instance2.getIdentifier()))
            .isEmpty();

        Optional<KBInstance> savedInstance = sut.readInstance(kb, instance2.getIdentifier());
        assertThat(savedInstance.isPresent()).as("Check that Instance was not found after delete")
                .isFalse();

    }

    @Test
    public void deleteInstanceWithNonexistentPropertyShouldNoNothing() {
        KBInstance instance = buildInstance();
        instance.setIdentifier("https://nonexistent.identifier.test");
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        assertThatCode(() -> sut.deleteInstance(kb, instance))
            .as("Check that deleting non-existant instance does nothing")
            .doesNotThrowAnyException();
    }

    @Test
    public void deleteInstanceWithReadOnlyKnowledgeBaseShouldNoNothing() {
        KBInstance instance = buildInstance();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        sut.createInstance(kb, instance);
        setReadOnly(kb);

        assertThatExceptionOfType(ReadOnlyException.class)
            .isThrownBy(() -> sut.deleteInstance(kb, instance));
        
        Optional<KBInstance> savedInstance = sut.readInstance(kb, instance.getIdentifier());
        assertThat(savedInstance.isPresent())
            .as("Check that instance was not deleted")
            .isTrue();
    }

    @Test
    public void listInstancesWithASavedInstanceAndNotAllShouldFindOneInstance() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBConcept concept = buildConcept();
        KBInstance instance = buildInstance();
        sut.createConcept(kb, concept);
        instance.setType(URI.create(concept.getIdentifier()));
        sut.createInstance(kb, instance);

        List<KBHandle> instances = sut.listInstances(kb, concept.getIdentifier(), false);

        assertThat(instances)
            .as("Check that instances contain the one, saved item")
            .hasSize(1)
            .element(0)
            .hasFieldOrPropertyWithValue("identifier", instance.getIdentifier())
            .hasFieldOrProperty("name")
            .matches(h -> h.getIdentifier().startsWith(IriConstants.INCEPTION_NAMESPACE));
    }

    @Test
    public void upsertStatementWithUnsavedStatementShouldCreateStatement() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBConcept concept = buildConcept();
        KBProperty property = buildProperty();
        sut.createConcept(kb, concept);
        sut.createProperty(kb, property);
        KBStatement statement = buildStatement(kb, concept.toKBHandle(), property, "Test statement");

        sut.upsertStatement(kb, statement);

        List<KBStatement> statements = sut.listStatements(kb, concept.toKBHandle(), false);
        assertThat(statements)
            .as("Check that the statement was saved correctly")
            .filteredOn(this::isNotAbstractNorClosedStatement)
            .hasSize(1)
            .element(0)
            .hasFieldOrProperty("instance")
            .hasFieldOrProperty("property")
            .hasFieldOrPropertyWithValue("value", "Test statement")
            .hasFieldOrPropertyWithValue("inferred", false);
    }

    @Test
    public void upsertStatementWithExistingStatementShouldUpdateStatement() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBConcept concept = buildConcept();
        KBProperty property = buildProperty();
        sut.createConcept(kb, concept);
        sut.createProperty(kb, property);
        KBStatement statement = buildStatement(kb, concept.toKBHandle(), property,
                "Test statement");
        sut.upsertStatement(kb, statement);

        statement.setValue("Altered test property");
        sut.upsertStatement(kb, statement);

        List<KBStatement> statements = sut.listStatements(kb, concept.toKBHandle(), false);
        assertThat(statements)
            .as("Check that the statement was updated correctly")
            .filteredOn(this::isNotAbstractNorClosedStatement)
            .hasSize(1)
            .element(0)
            .hasFieldOrProperty("instance")
            .hasFieldOrProperty("property")
            .hasFieldOrPropertyWithValue("value", "Altered test property")
            .hasFieldOrPropertyWithValue("inferred", false);
    }

    @Test
    public void upsertStatementWithReadOnlyKnowledgeBaseShouldDoNothing() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBConcept concept = buildConcept();
        KBProperty property = buildProperty();
        sut.createConcept(kb, concept);
        sut.createProperty(kb, property);
        KBStatement statement = buildStatement(kb, concept.toKBHandle(), property, "Test statement");
        setReadOnly(kb);

        int statementCountBeforeUpsert = sut.listStatements(kb, concept.toKBHandle(), false).size();
        assertThatExceptionOfType(ReadOnlyException.class)
                .isThrownBy(() -> sut.upsertStatement(kb, statement));

        int statementCountAfterUpsert = sut.listStatements(kb, concept.toKBHandle(), false).size();
        assertThat(statementCountBeforeUpsert)
            .as("Check that statement was not created")
            .isEqualTo(statementCountAfterUpsert);
    }

    @Test
    public void deleteStatementWithExistingStatementShouldDeleteStatement() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBConcept concept = buildConcept();
        KBProperty property = buildProperty();
        sut.createConcept(kb, concept);
        sut.createProperty(kb, property);
        KBStatement statement = buildStatement(kb, concept.toKBHandle(), property, "Test statement");
        sut.upsertStatement(kb, statement);

        sut.deleteStatement(kb, statement);

        List<KBStatement> statements = sut.listStatements(kb, concept.toKBHandle(), false);
        assertThat(statements)
            .as("Check that the statement was deleted correctly")
            .noneMatch(stmt -> "Test statement".equals(stmt.getValue()));
    }

    @Test
    public void deleteStatementWithNonExistentStatementShouldDoNothing() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBConcept concept = buildConcept();
        KBProperty property = buildProperty();
        sut.createConcept(kb, concept);
        sut.createProperty(kb, property);
        KBStatement statement = buildStatement(kb, concept.toKBHandle(), property, "Test statement");

        assertThatCode(() -> {
            sut.deleteStatement(kb, statement);
        }).doesNotThrowAnyException();
    }

    @Test
    public void deleteStatementWithReadOnlyKnowledgeBaseShouldDoNothing() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBConcept concept = buildConcept();
        KBProperty property = buildProperty();
        sut.createConcept(kb, concept);
        sut.createProperty(kb, property);
        KBStatement statement = buildStatement(kb, concept.toKBHandle(), property, "Test statement");
        sut.upsertStatement(kb, statement);
        setReadOnly(kb);

        int statementCountBeforeDeletion = sut.listStatements(kb, concept.toKBHandle(), false).size();
        
        assertThatExceptionOfType(ReadOnlyException.class)
                .isThrownBy(() -> sut.deleteStatement(kb, statement));

        int statementCountAfterDeletion = sut.listStatements(kb, concept.toKBHandle(), false).size();
        assertThat(statementCountAfterDeletion)
            .as("Check that statement was not deleted")
            .isEqualTo(statementCountBeforeDeletion);
    }

    @Test
    public void listStatementsWithExistentStatementAndNotAllShouldReturnOnlyThisStatement() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBConcept concept = buildConcept();
        KBProperty property = buildProperty();
        sut.createConcept(kb, concept);
        sut.createProperty(kb, property);
        KBStatement statement = buildStatement(kb, concept.toKBHandle(), property, "Test statement");
        sut.upsertStatement(kb, statement);

        List<KBStatement> statements = sut.listStatements(kb, concept.toKBHandle(), false);

        assertThat(statements)
            .as("Check that saved statement is found")
            .filteredOn(this::isNotAbstractNorClosedStatement)
            .hasSize(1)
            .element(0)
            .hasFieldOrPropertyWithValue("value", "Test statement");

        assertThat(statements.get(0).getOriginalTriples())
            .as("Check that original statements are recreated")
            .containsExactlyInAnyOrderElementsOf(statement.getOriginalTriples());
    }

    @Test
    public void listStatementsWithNonexistentStatementAndAllShouldRetuenAllStatements() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBConcept concept = buildConcept();
        sut.createConcept(kb, concept);

        List<KBStatement> statements = sut.listStatements(kb, concept.toKBHandle(), true);

        assertThat(statements)
            .filteredOn(this::isNotAbstractNorClosedStatement)
            .as("Check that all statements have implicit namespace")
            .allMatch(stmt -> hasImplicitNamespace(stmt.getProperty()));
    }

    @Test
    public void getConceptRootsWithWildlifeOntologyShouldReturnRootConcepts() throws Exception
    {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        importKnowledgeBase("data/wildlife_ontology.ttl");
        setSchema(kb, OWL.CLASS, RDFS.SUBCLASSOF, RDF.TYPE, RDFS.COMMENT, RDFS.LABEL, RDF.PROPERTY);

        List<KBHandle> rootConcepts = sut.listRootConcepts(kb, false);
        
        assertThat(rootConcepts)
            .as("Check that all root concepts have been found")
            .usingElementComparatorOnFields(
                "identifier", "name")
            .containsExactlyInAnyOrder(
                new KBHandle("http://purl.org/ontology/wo/Adaptation", "Adaptation"),
                new KBHandle("http://purl.org/ontology/wo/AnimalIntelligence", "Animal Intelligence"),
                new KBHandle("http://purl.org/dc/dcmitype/Collection", null),
                new KBHandle("http://purl.org/ontology/wo/ConservationStatus", "Conservation Status"),
                new KBHandle("http://purl.org/ontology/wo/Ecozone", "Ecozone"),
                new KBHandle("http://purl.org/ontology/wo/Habitat", "Habitat"),
                new KBHandle("http://purl.org/ontology/wo/RedListStatus", "Red List Status"),
                new KBHandle("http://purl.org/ontology/wo/TaxonName", "Taxon Name"),
                new KBHandle("http://purl.org/ontology/wo/TaxonRank", "Taxonomic Rank"));
    }

    @Test
    public void getConceptRootsWithWildlifeOntologyAndExplicityDefinedConceptsShouldReturnRootConcepts() throws Exception {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        ValueFactory vf = SimpleValueFactory.getInstance();
        IRI rootConcept1 = vf.createIRI("http://purl.org/ontology/wo/AnimalIntelligence");
        IRI rootConcept2 = vf.createIRI("http://purl.org/ontology/wo/Ecozone");
        List<IRI> concepts = new ArrayList<IRI>();
        concepts.add(rootConcept1);
        concepts.add(rootConcept2);
        kb.setDefaultLanguage("en");
        kb.setRootConcepts(concepts);
        sut.updateKnowledgeBase(kb);

        importKnowledgeBase("data/wildlife_ontology.ttl");
        setSchema(kb, OWL.CLASS, RDFS.SUBCLASSOF, RDF.TYPE, RDFS.COMMENT, RDFS.LABEL, RDF.PROPERTY);

        Stream<String> rootConcepts = sut.listRootConcepts(kb, false).stream()
                .map(KBHandle::getName);

        String[] expectedLabels = {
            "Animal Intelligence", "Ecozone"
        };
        assertThat(rootConcepts)
            .as("Check that all root concepts have been found")
            .containsExactlyInAnyOrder(expectedLabels);
    }

    @Test
    public void getConceptRootsWithSparqlPlaygroundReturnsOnlyRootConcepts() throws Exception {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        importKnowledgeBase("data/sparql_playground.ttl");
        setSchema(kb, RDFS.CLASS, RDFS.SUBCLASSOF, RDF.TYPE, RDFS.COMMENT, RDFS.LABEL, RDF.PROPERTY);

        Stream<String> childConcepts = sut.listRootConcepts(kb, false).stream()
                .map(KBHandle::getName);

        String[] expectedLabels = { "creature" };
        assertThat(childConcepts).as("Check that only root concepts")
                .containsExactlyInAnyOrder(expectedLabels);
   
    }

    @Test
    public void getChildConceptsWithSparqlPlaygroundReturnsAnimals() throws Exception {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        importKnowledgeBase("data/sparql_playground.ttl");
        setSchema(kb, RDFS.CLASS, RDFS.SUBCLASSOF, RDF.TYPE, RDFS.COMMENT, RDFS.LABEL, RDF.PROPERTY);
        KBConcept concept = sut.readConcept(kb, "http://example.org/tuto/ontology#Animal", true).get();

        Stream<String> childConcepts = sut.listChildConcepts(kb, concept.getIdentifier(), false)
            .stream()
            .map(KBHandle::getName);
        
        String[] expectedLabels = {
            "cat", "dog", "monkey"
        };
        assertThat(childConcepts)
            .as("Check that all child concepts have been found")
            .containsExactlyInAnyOrder(expectedLabels);
    }

    @Test
    public void getChildConceptsWithStreamsReturnsOnlyImmediateChildren() throws Exception {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        importKnowledgeBase("data/streams.ttl");
        KBConcept concept = sut.readConcept(kb, "http://mrklie.com/schemas/streams#input", true).get();
        setSchema(kb, RDFS.CLASS, RDFS.SUBCLASSOF, RDF.TYPE, RDFS.COMMENT, RDFS.LABEL, RDF.PROPERTY);

        Stream<String> childConcepts = sut.listChildConcepts(kb, concept.getIdentifier(), false)
            .stream()
            .map(KBHandle::getName);

        String[] expectedLabels = {
            "ByteArrayInputStream", "FileInputStream", "FilterInputStream", "ObjectInputStream",
            "PipedInputStream","SequenceInputStream", "StringBufferInputStream"
        };
        assertThat(childConcepts)
            .as("Check that all immediate child concepts have been found")
            .containsExactlyInAnyOrder(expectedLabels);
    }
    
    @Test
    public void getEnabledKnowledgeBasesWithOneEnabledOneDisabledReturnsOnlyEnabledKB()
    {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        KnowledgeBase kb2 = buildKnowledgeBase(project, "TestKB2");
        kb2.setEnabled(false);
        sut.registerKnowledgeBase(kb2, sut.getNativeConfig());

        List<KnowledgeBase> enabledKBs = sut.getEnabledKnowledgeBases(project);

        assertThat(enabledKBs).as("Check that only the enabled KB (kb) is in this list")
                .contains(kb).hasSize(1);

    }

    @Test
    public void getEnabledKnowledgeBasesWithoutEnabledKnowledgeBasesShouldReturnEmptyList()
    {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        kb.setEnabled(false);
        sut.updateKnowledgeBase(kb, sut.getNativeConfig());

        List<KnowledgeBase> knowledgeBases = sut.getEnabledKnowledgeBases(project);

        assertThat(knowledgeBases).as("Check that the list is empty").isEmpty();
    }

    @Test
    public void listStatementsWithPredicateOrObjectReferenceWithExistingStatementsShouldOnlyReturnStatementsWhereIdIsPredOrObj()
    {
        KBInstance subject = buildInstance();
        KBInstance object = buildInstance();
        KBProperty property = buildProperty();

        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        sut.createInstance(kb, subject);
        sut.createProperty(kb, property);
        sut.createInstance(kb, object);

        KBStatement stmt1 = buildStatement(kb, subject.toKBHandle(), property, object.getIdentifier());

        sut.upsertStatement(kb, stmt1);
        List<Statement> result = sut.listStatementsWithPredicateOrObjectReference(kb, object.getIdentifier());
        assertThat(result)
            .allMatch(new Predicate<Statement>() {

                @Override
                public boolean test(Statement arg0)
                {
                    return arg0.getObject().stringValue().equals(object.getIdentifier());
                }

            });
        assertTrue(result.size() >= 1);

    }

    @Test
    public void thatExistsFindsExistingStatement()
    {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBConcept concept = buildConcept();
        KBProperty property = buildProperty();
        sut.createConcept(kb, concept);
        sut.createProperty(kb, property);
        KBStatement statement = buildStatement(kb, concept.toKBHandle(), property, "Test statement");

        sut.upsertStatement(kb, statement);

        KBStatement mockStatement = buildStatement(kb, concept.toKBHandle(), property,
                "Test statement");
        assertTrue(sut.exists(kb, mockStatement));
    }

    @Test
    public void thatExistsDoesNotFindNonExistingStatement()
    {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBConcept concept = buildConcept();
        KBProperty property = buildProperty();
        sut.createConcept(kb, concept);
        sut.createProperty(kb, property);
        KBStatement statement = buildStatement(kb, concept.toKBHandle(), property, "Test");

        sut.upsertStatement(kb, statement);

        KBStatement mockStatement = buildStatement(kb, concept.toKBHandle(), property,
                "Test statement");
        assertFalse(sut.exists(kb, mockStatement));
    }

    @Test
    public void thatTheInstanceIsRetrievedInTheCorrectLanguage()
    {
        KBInstance germanInstance = buildInstanceWithLanguage("de");
        KBInstance englishInstance = buildInstanceWithLanguage("en");

        kb.setDefaultLanguage("en");
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        sut.createInstance(kb, germanInstance);

        // Create English instance and ensure that both have the same identifier
        sut.update(kb, (conn) -> {
            englishInstance.setIdentifier(germanInstance.getIdentifier());
            englishInstance.write(conn, kb);
        });

        KBInstance firstInstance = sut.readInstance(kb, germanInstance.getIdentifier()).get();
        assertThat(firstInstance.getLanguage())
            .as("Check that the English instance is retrieved.")
            .isEqualTo("en");
    }

    @Test
    public void thatTheLanguageOfKbInstanceCanBeModified()
    {
        KBInstance englishInstance = buildInstanceWithLanguage("en");

        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        sut.createInstance(kb, englishInstance);

        englishInstance.setLanguage("de");
        sut.updateInstance(kb, englishInstance);
        
        // Make sure we retrieve the German version now
        kb.setDefaultLanguage("de");
        
        KBInstance germanInstance = sut.readInstance(kb, englishInstance.getIdentifier()).get();
        assertThat(germanInstance.getLanguage())
            .as("Check that the language has successfully been changed.")
            .isEqualTo("de");
    }

    @Test
    public void thatTheLanguageOfKbPropertyCanBeModified()
    {
        KBProperty englishProperty = buildPropertyWithLanguage("en");

        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        sut.createProperty(kb, englishProperty);

        englishProperty.setLanguage("de");
        sut.updateProperty(kb, englishProperty);
        
        // Make sure we retrieve the German version now
        kb.setDefaultLanguage("de");
        
        KBProperty germanProperty = sut.readProperty(kb, englishProperty.getIdentifier()).get();
        assertThat(germanProperty.getLanguage())
            .as("Check that the language has successfully been changed.")
            .isEqualTo("de");
    }

    @Test
    public void thatTheLanguageOfKbConceptCanBeModified()
    {
        KBConcept englishConcept = buildConceptWithLanguage("en");

        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        sut.createConcept(kb, englishConcept);

        englishConcept.setLanguage("de");
        sut.updateConcept(kb, englishConcept);

        // Make sure we retrieve the German version now
        kb.setDefaultLanguage("de");
        
        KBConcept germanConcept = sut.readConcept(kb, englishConcept.getIdentifier(), true).get();
        assertThat(germanConcept.getLanguage())
            .as("Check that the language has successfully been changed.")
            .isEqualTo("de");
    }

    @Test
    public void readKnowledgeBaseProfilesShouldReturnValidHashMapWithProfiles() throws IOException {
        Map<String, KnowledgeBaseProfile> profiles = KnowledgeBaseProfile.readKnowledgeBaseProfiles();

        assertThat(profiles)
            .allSatisfy((key, profile) -> {
                assertThat(key).isNotNull();
            });

    }

    @Test
    public void readKBIdentifiersShouldReturnCorrectClassInstances()
    {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        KBConcept concept = buildConcept();
        sut.createConcept(kb, concept);
        KBInstance instance = buildInstance();
        sut.createInstance(kb, instance);
        KBProperty property = buildProperty();
        sut.createProperty(kb, property);

        assertThat(sut.readItem(kb, concept.getIdentifier()).get())
            .as("Check that reading a concept id returns an instance of KBConcept")
            .isInstanceOf(KBConcept.class);
        assertThat(sut.readItem(kb, instance.getIdentifier()).get())
            .as("Check that reading an instance id returns an instance of KBInstance")
            .isInstanceOf(KBInstance.class);
        assertThat(sut.readItem(kb, property.getIdentifier()).get())
            .as("Check that reading a property id returns an instance of KBProperty")
            .isInstanceOf(KBProperty.class);
    }

    @Test
    public void checkIfKBIsEnabledByIdWithExistingAndEnabledKBShouldReturnTrue() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        String repoId = kb.getRepositoryId();
        assertThat(sut.isKnowledgeBaseEnabled(project, repoId))
            .as("Check that correct accessibility value is returned for enabled kb ")
            .isTrue();
    }

    @Test
    public void checkIfKBIsEnabledByIdWithDisabledKBAndNonExistingIdShouldReturnFalse() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        kb.setEnabled(false);
        String repoId = kb.getRepositoryId();

        assertThat(sut.isKnowledgeBaseEnabled(project, repoId))
            .as("Check that correct accessibility value is returned for disabled kb ")
            .isFalse();

        assertThat(sut.isKnowledgeBaseEnabled(project, "NonExistingID"))
            .as("Check that correct accessibility value is returned for non existing id ")
            .isFalse();
    }

    // Helper
    private Project createProject(String name) {
        return testFixtures.createProject(name);
    }

    private KnowledgeBase buildKnowledgeBase(Project project, String name) {
        return testFixtures.buildKnowledgeBase(project, name, reification);
    }

    private KBConcept buildConcept() {
        return testFixtures.buildConcept();
    }

    private KBConcept buildConceptWithLanguage(String aLanguage) {
        return testFixtures.buildConceptWithLanguage(aLanguage);
    }

    private KBProperty buildProperty() {
        return testFixtures.buildProperty();
    }

    private KBProperty buildPropertyWithLanguage(String aLanguage) {
        return testFixtures.buildPropertyWithLanguage(aLanguage);
    }

    private KBInstance buildInstance() {
        return testFixtures.buildInstance();
    }

    private KBInstance buildInstanceWithLanguage(String aLanguage) {
        return testFixtures.buildInstanceWithLanguage(aLanguage);
    }

    private KBStatement buildStatement(KnowledgeBase knowledgeBase, KBHandle conceptHandle,
            KBProperty aProperty, String value)
    {
        KBStatement stmt = testFixtures.buildStatement(conceptHandle, aProperty, value);
        return stmt;
    }

    private boolean isNotAbstractNorClosedStatement(KBStatement statement) {
        String id = statement.getProperty().getIdentifier();
        return !(id.endsWith("#abstract") || id.endsWith("#closed"));
    }

    private boolean hasImplicitNamespace(KBObject handle)
    {
        return IriConstants.IMPLICIT_NAMESPACES.stream()
                .anyMatch(ns -> handle.getIdentifier().startsWith(ns));
    }

    private void importKnowledgeBase(String resourceName) throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        String fileName = classLoader.getResource(resourceName).getFile();
        try (InputStream is = classLoader.getResourceAsStream(resourceName)) {
            sut.importData(kb, fileName, is);
        }
    }
    private void setReadOnly(KnowledgeBase kb) {
        kb.setReadOnly(true);
        sut.updateKnowledgeBase(kb, sut.getKnowledgeBaseConfig(kb));
    }

    private void setSchema(KnowledgeBase kb, IRI classIri, IRI subclassIri, IRI typeIri,
        IRI descriptionIri, IRI labelIri, IRI propertyTypeIri) {
        kb.setClassIri(classIri);
        kb.setSubclassIri(subclassIri);
        kb.setTypeIri(typeIri);
        kb.setDescriptionIri(descriptionIri);
        kb.setLabelIri(labelIri);
        kb.setPropertyTypeIri(propertyTypeIri);
        sut.updateKnowledgeBase(kb, sut.getKnowledgeBaseConfig(kb));
    }
}
