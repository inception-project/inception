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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.reification.Reification;
import de.tudarmstadt.ukp.inception.kb.util.TestFixtures;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseProfile;

@RunWith(Parameterized.class)
@SpringBootTest(classes = SpringConfig.class)
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
        EntityManager entityManager = testEntityManager.getEntityManager();
        testFixtures = new TestFixtures(testEntityManager);
        sut = new KnowledgeBaseServiceImpl(temporaryFolder.getRoot(), entityManager);
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
    public void registerKnowledgeBase_WithNewKnowledgeBase_ShouldSaveNewKnowledgeBase() {

        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        KnowledgeBase savedKb = testEntityManager.find(KnowledgeBase.class, kb.getRepositoryId());
        assertThat(savedKb)
            .as("Check that knowledge base was saved correctly")
            .hasFieldOrPropertyWithValue("name", KB_NAME)
            .hasFieldOrPropertyWithValue("project", project)
            .extracting("repositoryId").isNotNull();
    }

    @Test
    public void getKnowledgeBases_WithOneStoredKnowledgeBase_ShouldReturnStoredKnowledgeBase() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        List<KnowledgeBase> knowledgeBases = sut.getKnowledgeBases(project);

        assertThat(knowledgeBases)
            .as("Check that only the previously created knowledge base is found")
            .hasSize(1)
            .contains(kb);
    }

    @Test
    public void getKnowledgeBases_WithoutKnowledgeBases_ShouldReturnEmptyList() {
        Project project = createProject("Empty project");

        List<KnowledgeBase> knowledgeBases = sut.getKnowledgeBases(project);

        assertThat(knowledgeBases)
            .as("Check that no knowledge base is found")
            .isEmpty();
    }

    @Test
    public void knowledgeBaseExists_WithExistingKnowledgeBase_ShouldReturnTrue() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        assertThat(sut.knowledgeBaseExists(project, kb.getName()))
            .as("Check that knowledge base with given name already exists")
            .isTrue();
    }

    @Test
    public void knowledgeBaseExists_WithNonexistentKnowledgeBase_ShouldReturnFalse() {

        assertThat(sut.knowledgeBaseExists(project, kb.getName()))
            .as("Check that knowledge base with given name does not already exists")
            .isFalse();
    }

    @Test
    public void updateKnowledgeBase_WithValidValues_ShouldUpdateKnowledgeBase() {
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
        kb.setExplicitlyDefinedRootConcepts(concepts);
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
            .hasFieldOrPropertyWithValue("explicitlyDefinedRootConcepts", Arrays.asList(rootConcept1, rootConcept2));

    }

    @Test
    public void updateKnowledgeBase_WithUnregisteredKnowledgeBase_ShouldThrowIllegalStateException() {
        assertThatExceptionOfType(IllegalStateException.class)
            .as("Check that updating knowledge base requires registration")
            .isThrownBy(() -> sut.updateKnowledgeBase(kb, sut.getNativeConfig()));
    }

    @Test
    public void removeKnowledgeBase_WithStoredKnowledgeBase_ShouldDeleteKnowledgeBase() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        sut.removeKnowledgeBase(kb);

        List<KnowledgeBase> knowledgeBases = sut.getKnowledgeBases(project);
        assertThat(knowledgeBases)
            .as("Check that the knowledge base has been deleted")
            .hasSize(0);
    }

    @Test
    public void removeKnowledgeBase_WithUnregisteredKnowledgeBase_ShouldThrowIllegalStateException() {
        assertThatExceptionOfType(IllegalStateException.class)
            .as("Check that updating knowledge base requires registration")
            .isThrownBy(() -> sut.removeKnowledgeBase(kb));
    }

    @Test
    public void clear_WithNonemptyKnowledgeBase_ShouldDeleteAllCustomEntities() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        sut.createConcept(kb, buildConcept());
        sut.createProperty(kb, buildProperty());

        sut.clear(kb);

        List<KBHandle> handles = new ArrayList<>();
        handles.addAll(sut.listConcepts(kb, false));
        handles.addAll(sut.listProperties(kb, false));
        assertThat(handles)
            .as("Check that no custom entities are found after clearing")
            .isEmpty();
    }

    @Test
    public void clear_WithNonemptyKnowledgeBase_ShouldNotDeleteImplicitEntities() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        sut.createConcept(kb, buildConcept());
        sut.createProperty(kb, buildProperty());

        sut.clear(kb);

        List<KBHandle> handles = new ArrayList<>();
        handles.addAll(sut.listConcepts(kb, true));
        handles.addAll(sut.listProperties(kb, true));
        assertThat(handles)
            .as("Check that only entities with implicit namespace are found after clearing")
            .allMatch(this::hasImplicitNamespace);
    }

    @Test
    public void empty_WithEmptyKnowledgeBase_ShouldReturnTrue() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        boolean isEmpty = sut.isEmpty(kb);

        assertThat(isEmpty)
            .as("Check that knowledge base is empty")
            .isTrue();
    }

    @Test
    public void empty_WithNonemptyKnowledgeBase_ShouldReturnFalse() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        sut.createConcept(kb, buildConcept());

        boolean isEmpty = sut.isEmpty(kb);

        assertThat(isEmpty)
            .as("Check that knowledge base is not empty")
            .isFalse();
    }

    @Test
    public void createConcept_WithEmptyIdentifier_ShouldCreateNewConcept() {
        KBConcept concept = buildConcept();

        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBHandle handle = sut.createConcept(kb, concept);

        KBConcept savedConcept = sut.readConcept(kb, handle.getIdentifier()).get();
        assertThat(savedConcept)
            .as("Check that concept was saved correctly")
            .hasFieldOrPropertyWithValue("description", concept.getDescription())
            .hasFieldOrPropertyWithValue("name", concept.getName());
    }
    
    @Test
    public void createConcept_WithCustomBasePrefix_ShouldCreateNewConceptWithCustomPrefix()
    {
        KBConcept concept = buildConcept();

        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        String customPrefix = "http://www.ukp.informatik.tu-darmstadt.de/customPrefix#";
        kb.setBasePrefix(customPrefix);
        KBHandle handle = sut.createConcept(kb, concept);

        KBConcept savedConcept = sut.readConcept(kb, handle.getIdentifier()).get();
        assertThat(savedConcept).as("Check that concept was saved correctly")
                .hasFieldOrPropertyWithValue("description", concept.getDescription())
                .hasFieldOrPropertyWithValue("name", concept.getName());

        String id = savedConcept.getIdentifier();
        String savedConceptPrefix = id.substring(0, id.lastIndexOf("#") + 1);
        assertEquals(customPrefix, savedConceptPrefix);
    }

    @Test
    public void createConcept_WithNonemptyIdentifier_ShouldThrowIllegalArgumentException() {
        KBConcept concept = new KBConcept();
        concept.setIdentifier("Nonempty Identifier");

        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        assertThatIllegalArgumentException()
            .as("Check that creating a concept requires empty identifier")
            .isThrownBy(() -> sut.createConcept(kb, concept) )
            .withMessage("Identifier must be empty on create");
    }

    @Test
    public void createConcept_WithReadOnlyKnowledgeBase_ShouldDoNothing() {
        KBConcept concept = buildConcept();
        kb.setReadOnly(true);
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        KBHandle handle = sut.createConcept(kb, concept);

        assertThat(handle)
            .as("Check that concept has not been created")
            .isNull();
    }

    @Test
    public void readConcept_WithExistingConcept_ShouldReturnSavedConcept() {
        KBConcept concept = buildConcept();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBHandle handle = sut.createConcept(kb, concept);

        KBConcept savedConcept = sut.readConcept(kb, handle.getIdentifier()).get();

        assertThat(savedConcept)
            .as("Check that concept was read correctly")
            .hasFieldOrPropertyWithValue("description", concept.getDescription())
            .hasFieldOrPropertyWithValue("name", concept.getName());
    }

    @Test
    public void readConcept_WithNonexistentConcept_ShouldReturnEmptyResult() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        Optional<KBConcept> savedConcept = sut.readConcept(kb, "https://nonexistent.identifier.test");

        assertThat(savedConcept.isPresent())
            .as("Check that no concept was read")
            .isFalse();
    }

    @Test
    public void updateConcept_WithAlteredConcept_ShouldUpdateConcept() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBConcept concept = buildConcept();
        KBHandle handle = sut.createConcept(kb, concept);

        concept.setDescription("New description");
        concept.setName("New name");
        sut.updateConcept(kb, concept);

        KBConcept savedConcept = sut.readConcept(kb, handle.getIdentifier()).get();
        assertThat(savedConcept)
            .as("Check that concept was updated correctly")
            .hasFieldOrPropertyWithValue("description", "New description")
            .hasFieldOrPropertyWithValue("name", "New name");
    }

    @Test
    // TODO: Check whether this is a feature or not
    public void updateConcept_WithNonexistentConcept_ShouldCreateConcept() {
        KBConcept concept = buildConcept();
        concept.setIdentifier("https://nonexistent.identifier.test");
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        sut.updateConcept(kb, concept);

        KBConcept savedConcept = sut.readConcept(kb, "https://nonexistent.identifier.test").get();
        assertThat(savedConcept)
            .hasFieldOrPropertyWithValue("description", concept.getDescription())
            .hasFieldOrPropertyWithValue("name", concept.getName());
    }

    @Test
    public void updateConcept_WithConceptWithBlankIdentifier_ShouldThrowIllegalArgumentException() {
        KBConcept concept = buildConcept();
        concept.setIdentifier("");
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        assertThatIllegalArgumentException()
            .as("Check that updating a concept requires nonempty identifier")
            .isThrownBy(() -> sut.updateConcept(kb, concept))
            .withMessage("Identifier cannot be empty on update");
    }

    @Test
    public void updateConcept_WithConceptWithNullIdentifier_ShouldThrowIllegalArgumentException() {
        KBConcept concept = buildConcept();
        concept.setIdentifier(null);
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        assertThatIllegalArgumentException()
            .as("Check that updating a concept requires non-null identifier")
            .isThrownBy(() -> sut.updateConcept(kb, concept))
            .withMessage("Identifier cannot be empty on update");
    }

    @Test
    public void updateConcept_WithReadOnlyKnowledgeBase_ShouldDoNothing() {
        KBConcept concept = buildConcept();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBHandle handle = sut.createConcept(kb, concept);
        setReadOnly(kb);

        concept.setDescription("New description");
        concept.setName("New name");
        sut.updateConcept(kb, concept);

        KBConcept savedConcept = sut.readConcept(kb, handle.getIdentifier()).get();
        assertThat(savedConcept)
            .as("Check that concept has not been updated")
            .hasFieldOrPropertyWithValue("description", "Concept description")
            .hasFieldOrPropertyWithValue("name", "Concept name");
    }

    @Test
    public void deleteConcept_WithConceptReferencedAsObject_ShouldDeleteConceptAndStatement() {
        KBInstance instance = buildInstance();
        KBProperty property = buildProperty();
        KBConcept concept = buildConcept();

        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBHandle subHandle = sut.createInstance(kb, instance);
        KBHandle predHandle = sut.createProperty(kb, property);
        KBHandle conceptHandle = sut.createConcept(kb, concept);
        String conceptId = conceptHandle.getIdentifier();

        sut.upsertStatement(kb, buildStatement(kb, subHandle, predHandle, conceptId));

        sut.deleteConcept(kb, concept);

        assertThat(sut.listStatementsWithPredicateOrObjectReference(kb, conceptId))
            .isEmpty();

        Optional<KBConcept> savedConcept = sut.readConcept(kb, conceptId);
        assertThat(savedConcept.isPresent())
            .as("Check that concept was not found after delete")
            .isFalse();

    }

    @Test
    public void deleteConcept_WithExistingConcept_ShouldDeleteConcept() {
        KBConcept concept = buildConcept();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBHandle handle = sut.createConcept(kb, concept);

        sut.deleteConcept(kb, concept);

        Optional<KBConcept> savedConcept = sut.readConcept(kb, handle.getIdentifier());
        assertThat(savedConcept.isPresent())
            .as("Check that concept was not found after delete")
            .isFalse();
    }

    @Test
    public void deleteConcept_WithNonexistentConcept_ShouldNoNothing() {
        KBConcept concept = buildConcept();
        concept.setIdentifier("https://nonexistent.identifier.test");
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        assertThatCode(() -> sut.deleteConcept(kb, concept))
            .as("Check that deleting non-existant concept does nothing")
            .doesNotThrowAnyException();
    }

    @Test
    public void deleteConcept_WithReadOnlyKnowledgeBase_ShouldDoNothing() {
        KBConcept concept = buildConcept();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBHandle handle = sut.createConcept(kb, concept);
        setReadOnly(kb);

        sut.deleteConcept(kb, concept);

        Optional<KBConcept> savedConcept = sut.readConcept(kb, handle.getIdentifier());
        assertThat(savedConcept.isPresent())
            .as("Check that concept was not deleted")
            .isTrue();
    }

    @Test
    public void listConcepts_WithASavedConceptAndNotAll_ShouldFindOneConcept() {
        KBConcept concept = buildConcept();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBHandle handle = sut.createConcept(kb, concept);

        List<KBHandle> concepts = sut.listConcepts(kb, false);

        assertThat(concepts)
            .as("Check that concepts contain the one, saved item")
            .hasSize(1)
            .element(0)
            .hasFieldOrPropertyWithValue("identifier", handle.getIdentifier())
            .hasFieldOrPropertyWithValue("name", handle.getName())
            .matches(h -> h.getIdentifier().startsWith(IriConstants.INCEPTION_NAMESPACE));
    }

    @Test
    public void listConcepts_WithNoSavedConceptAndAll_ShouldFindRdfConcepts() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        List<KBHandle> concepts = sut.listConcepts(kb, true);

        assertThat(concepts)
            .as("Check that all concepts have implicit namespaces")
            .allMatch(this::hasImplicitNamespace);
    }

    @Test
    public void createProperty_WithEmptyIdentifier_ShouldCreateNewProperty() {
        KBProperty property = buildProperty();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        KBHandle handle = sut.createProperty(kb, property);

        KBProperty savedProperty = sut.readProperty(kb, handle.getIdentifier()).get();
        assertThat(savedProperty)
            .as("Check that property was created correctly")
            .hasNoNullFieldsOrProperties()
            .hasFieldOrPropertyWithValue("description", property.getDescription())
            .hasFieldOrPropertyWithValue("domain", property.getDomain())
            .hasFieldOrPropertyWithValue("name", property.getName())
            .hasFieldOrPropertyWithValue("range", property.getRange());
    }
    
    @Test
    public void createProperty_WithCustomBasePrefix_ShouldCreateNewPropertyWithCustomPrefix()
    {
        KBProperty property = buildProperty();

        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        String customPrefix = "http://www.ukp.informatik.tu-darmstadt.de/customPrefix#";
        kb.setBasePrefix(customPrefix);
        KBHandle handle = sut.createProperty(kb, property);

        KBProperty savedProperty = sut.readProperty(kb, handle.getIdentifier()).get();
        assertThat(savedProperty).as("Check that property was saved correctly")
                .hasFieldOrPropertyWithValue("description", property.getDescription())
                .hasFieldOrPropertyWithValue("name", property.getName());

        String id = savedProperty.getIdentifier();
        String savedPropertyPrefix = id.substring(0, id.lastIndexOf("#") + 1);
        assertEquals(customPrefix, savedPropertyPrefix);
    }

    @Test
    public void createProperty_WithNonemptyIdentifier_ShouldThrowIllegalArgumentException() {
        KBProperty property = buildProperty();
        property.setIdentifier("Nonempty Identifier");

        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        assertThatIllegalArgumentException()
            .as("Check that creating a property requires empty identifier")
            .isThrownBy(() -> sut.createProperty(kb, property) )
            .withMessage("Identifier must be empty on create");
    }

    @Test
    public void createProperty_WithReadOnlyKnowledgeBase_ShouldDoNothing() {
        KBProperty property = buildProperty();
        kb.setReadOnly(true);
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        KBHandle handle = sut.createProperty(kb, property);

        assertThat(handle)
            .as("Check that property has not been created")
            .isNull();
    }

    @Test
    public void readProperty_WithExistingConcept_ShouldReturnSavedProperty() {
        KBProperty property = buildProperty();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBHandle handle = sut.createProperty(kb, property);

        KBProperty savedProperty = sut.readProperty(kb, handle.getIdentifier()).get();

        assertThat(savedProperty)
            .as("Check that property was saved correctly")
            .hasNoNullFieldsOrProperties()
            .hasFieldOrPropertyWithValue("description", property.getDescription())
            .hasFieldOrPropertyWithValue("domain", property.getDomain())
            .hasFieldOrPropertyWithValue("name", property.getName())
            .hasFieldOrPropertyWithValue("range", property.getRange());
    }

    @Test
    public void readProperty_WithNonexistentProperty_ShouldReturnEmptyResult() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        Optional<KBProperty> savedProperty = sut.readProperty(kb, "https://nonexistent.identifier.test");

        assertThat(savedProperty.isPresent())
            .as("Check that no property was read")
            .isFalse();
    }

    @Test
    public void updateProperty_WithAlteredProperty_ShouldUpdateProperty() {
        KBProperty property = buildProperty();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBHandle handle = sut.createProperty(kb, property);

        property.setDescription("New property description");
        property.setDomain(URI.create("https://new.schema.com/#domain"));
        property.setName("New property name");
        property.setRange(URI.create("https://new.schema.com/#range"));
        sut.updateProperty(kb, property);

        KBProperty savedProperty = sut.readProperty(kb, handle.getIdentifier()).get();
        assertThat(savedProperty)
            .as("Check that property was updated correctly")
            .hasFieldOrPropertyWithValue("description", property.getDescription())
            .hasFieldOrPropertyWithValue("domain", property.getDomain())
            .hasFieldOrPropertyWithValue("name", property.getName())
            .hasFieldOrPropertyWithValue("range", property.getRange());
    }

    @Test
    // TODO: Check whether this is a feature or not
    public void updateProperty_WithNonexistentProperty_ShouldCreateProperty() {
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
    public void updateProperty_WithPropertyWithBlankIdentifier_ShouldThrowIllegalArgumentException() {
        KBProperty property = buildProperty();
        property.setIdentifier("");
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        assertThatIllegalArgumentException()
            .as("Check that updating a property requires nonempty identifier")
            .isThrownBy(() -> sut.updateProperty(kb, property))
            .withMessage("Identifier cannot be empty on update");
    }

    @Test
    public void updateProperty_WithPropertyWithNullIdentifier_ShouldThrowIllegalArgumentException() {
        KBProperty property = buildProperty();
        property.setIdentifier(null);
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        assertThatIllegalArgumentException()
            .as("Check that updating a property requires nonempty identifier")
            .isThrownBy(() -> sut.updateProperty(kb, property))
            .withMessage("Identifier cannot be empty on update");
    }

    @Test
    public void updateProperty_WithReadOnlyKnowledgeBase_ShouldDoNothing() {
        KBProperty property = buildProperty();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBHandle handle = sut.createProperty(kb, property);
        setReadOnly(kb);

        property.setDescription("New property description");
        property.setDomain(URI.create("https://new.schema.com/#domain"));
        property.setName("New property name");
        property.setRange(URI.create("https://new.schema.com/#range"));
        sut.updateProperty(kb, property);

        KBProperty savedProperty = sut.readProperty(kb, handle.getIdentifier()).get();
        assertThat(savedProperty)
            .as("Check that property has not been updated")
            .hasFieldOrPropertyWithValue("description", "Property description")
            .hasFieldOrPropertyWithValue("domain", URI.create("https://test.schema.com/#domain"))
            .hasFieldOrPropertyWithValue("name", "Property name")
            .hasFieldOrPropertyWithValue("range", URI.create("https://test.schema.com/#range"));
    }

    @Test
    public void deleteProperty_WithExistingProperty_ShouldDeleteProperty() {
        KBProperty property = buildProperty();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBHandle handle = sut.createProperty(kb, property);

        sut.deleteProperty(kb, property);

        Optional<KBProperty> savedProperty = sut.readProperty(kb, handle.getIdentifier());
        assertThat(savedProperty.isPresent())
            .as("Check that property was not found after delete")
            .isFalse();
    }

    @Test
    public void deleteProperty_WithNotExistingProperty_ShouldNoNothing() {
        KBProperty property = buildProperty();
        property.setIdentifier("https://nonexistent.identifier.test");
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        assertThatCode(() -> sut.deleteProperty(kb, property))
            .as("Check that deleting non-existant property does nothing")
            .doesNotThrowAnyException();
    }

    @Test
    public void deleteProperty_WithReadOnlyKnowledgeBase_ShouldNoNothing() {
        KBProperty property = buildProperty();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBHandle handle = sut.createProperty(kb, property);
        setReadOnly(kb);

        sut.deleteProperty(kb, property);

        Optional<KBProperty> savedProperty = sut.readProperty(kb, handle.getIdentifier());
        assertThat(savedProperty.isPresent())
            .as("Check that property was not deleted")
            .isTrue();
    }

    @Test
    public void listProperties_WithASavedConceptAndNotAll_ShouldFindOneConcept() {
        KBProperty property = buildProperty();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBHandle handle = sut.createProperty(kb, property);

        List<KBHandle> properties = sut.listProperties(kb, false);

        assertThat(properties)
            .as("Check that properties contain the one, saved item")
            .hasSize(1)
            .element(0)
            .hasFieldOrPropertyWithValue("identifier", handle.getIdentifier())
            .hasFieldOrPropertyWithValue("name", handle.getName())
            .matches(h -> h.getIdentifier().startsWith(IriConstants.INCEPTION_NAMESPACE));
    }

    @Test
    public void listProperties_WithNoSavedConceptAndAll_ShouldFindRdfConcepts() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        List<KBHandle> properties = sut.listProperties(kb, true);

        assertThat(properties)
            .as("Check that all properties have implicit namespaces")
            .allMatch(this::hasImplicitNamespace);
    }

    @Test
    public void createInstance_WithEmptyIdentifier_ShouldCreateNewInstance() {
        KBInstance instance = buildInstance();

        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBHandle handle = sut.createInstance(kb, instance);

        KBInstance savedInstance = sut.readInstance(kb, handle.getIdentifier()).get();
        assertThat(savedInstance)
            .as("Check that instance was saved correctly")
            .hasFieldOrPropertyWithValue("description", instance.getDescription())
            .hasFieldOrPropertyWithValue("name", instance.getName());
    }
    
    @Test
    public void createInstance_WithCustomBasePrefix_ShouldCreateNewInstanceWithCustomPrefix()
    {
        KBInstance instance = buildInstance();

        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        String customPrefix = "http://www.ukp.informatik.tu-darmstadt.de/customPrefix#";
        kb.setBasePrefix(customPrefix);
        KBHandle handle = sut.createInstance(kb, instance);

        KBInstance savedInstance = sut.readInstance(kb, handle.getIdentifier()).get();
        assertThat(savedInstance).as("Check that Instance was saved correctly")
                .hasFieldOrPropertyWithValue("description", instance.getDescription())
                .hasFieldOrPropertyWithValue("name", instance.getName());

        String id = savedInstance.getIdentifier();
        String savedInstancePrefix = id.substring(0, id.lastIndexOf("#") + 1);
        assertEquals(customPrefix, savedInstancePrefix);
    }

    @Test
    public void createInstance_WithNonemptyIdentifier_ShouldThrowIllegalArgumentException() {
        KBInstance instance = new KBInstance();
        instance.setIdentifier("Nonempty Identifier");

        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        assertThatIllegalArgumentException()
            .as("Check that creating a instance requires empty identifier")
            .isThrownBy(() -> sut.createInstance(kb, instance) )
            .withMessage("Identifier must be empty on create");
    }

    @Test
    public void createInstance_WithReadOnlyKnowledgeBase_ShouldDoNothing() {
        KBInstance instance = buildInstance();
        kb.setReadOnly(true);
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        KBHandle handle = sut.createInstance(kb, instance);

        assertThat(handle)
            .as("Check that instance has not been created")
            .isNull();
    }

    @Test
    public void readInstance_WithExistingInstance_ShouldReturnSavedInstance() {
        KBInstance instance = buildInstance();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBHandle handle = sut.createInstance(kb, instance);

        KBInstance savedInstance = sut.readInstance(kb, handle.getIdentifier()).get();

        assertThat(savedInstance)
            .as("Check that instance was read correctly")
            .hasFieldOrPropertyWithValue("description", instance.getDescription())
            .hasFieldOrPropertyWithValue("name", instance.getName());
    }

    @Test
    public void readInstance_WithNonexistentInstance_ShouldReturnEmptyResult() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        Optional<KBInstance> savedInstance = sut.readInstance(kb, "https://nonexistent.identifier.test");

        assertThat(savedInstance.isPresent())
            .as("Check that no instance was read")
            .isFalse();
    }

    @Test
    public void updateInstance_WithAlteredInstance_ShouldUpdateInstance() {
        KBInstance instance = buildInstance();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBHandle handle = sut.createInstance(kb, instance);

        instance.setDescription("New description");
        instance.setName("New name");
        sut.updateInstance(kb, instance);

        KBInstance savedInstance = sut.readInstance(kb, handle.getIdentifier()).get();
        assertThat(savedInstance)
            .as("Check that instance was updated correctly")
            .hasFieldOrPropertyWithValue("description", "New description")
            .hasFieldOrPropertyWithValue("name", "New name");
    }

    @Test
    // TODO: Check whether this is a feature or not
    public void updateInstance_WithNonexistentInstance_ShouldCreateInstance() {
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
    public void updateInstance_WithInstanceWithBlankIdentifier_ShouldThrowIllegalArgumentException() {
        KBInstance instance = buildInstance();
        instance.setIdentifier("");
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        assertThatIllegalArgumentException()
            .as("Check that updating a instance requires nonempty identifier")
            .isThrownBy(() -> sut.updateInstance(kb, instance))
            .withMessage("Identifier cannot be empty on update");
    }

    @Test
    public void updateInstance_WithInstanceWithNullIdentifier_ShouldThrowIllegalArgumentException() {
        KBInstance instance = buildInstance();
        instance.setIdentifier(null);
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        assertThatIllegalArgumentException()
            .as("Check that updating a instance requires non-null identifier")
            .isThrownBy(() -> sut.updateInstance(kb, instance))
            .withMessage("Identifier cannot be empty on update");
    }

    @Test
    public void updateInstance_WithReadOnlyKnowledgeBase_ShouldDoNothing() {
        KBInstance instance = buildInstance();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBHandle handle = sut.createInstance(kb, instance);
        setReadOnly(kb);

        instance.setDescription("New description");
        instance.setName("New name");
        sut.updateInstance(kb, instance);

        KBInstance savedInstance = sut.readInstance(kb, handle.getIdentifier()).get();
        assertThat(savedInstance)
            .as("Check that instance has not been updated")
            .hasFieldOrPropertyWithValue("description", "Instance description")
            .hasFieldOrPropertyWithValue("name", "Instance name");
    }

    @Test
    public void deleteInstance_WithExistingInstance_ShouldDeleteInstance() {
        KBInstance instance = buildInstance();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBHandle handle = sut.createInstance(kb, instance);

        sut.deleteInstance(kb, instance);

        Optional<KBInstance> savedInstance = sut.readInstance(kb, handle.getIdentifier());
        assertThat(savedInstance.isPresent())
            .as("Check that instance was not found after delete")
            .isFalse();
    }

    @Test
    public void deleteInstance_WithInstanceReferencedAsObject_ShouldDeleteInstanceAndStatement()
    {
        KBInstance instance = buildInstance();
        KBProperty property = buildProperty();
        KBInstance Instance = buildInstance();

        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBHandle subHandle = sut.createInstance(kb, instance);
        KBHandle predHandle = sut.createProperty(kb, property);
        KBHandle instanceHandle = sut.createInstance(kb, Instance);
        String instanceId = instanceHandle.getIdentifier();

        sut.upsertStatement(kb, buildStatement(kb, subHandle, predHandle, instanceId));

        sut.deleteInstance(kb, Instance);

        assertThat(sut.listStatementsWithPredicateOrObjectReference(kb, instanceId))
            .isEmpty();

        Optional<KBInstance> savedInstance = sut.readInstance(kb, instanceId);
        assertThat(savedInstance.isPresent()).as("Check that Instance was not found after delete")
                .isFalse();

    }

    @Test
    public void deleteInstance_WithNonexistentProperty_ShouldNoNothing() {
        KBInstance instance = buildInstance();
        instance.setIdentifier("https://nonexistent.identifier.test");
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        assertThatCode(() -> sut.deleteInstance(kb, instance))
            .as("Check that deleting non-existant instance does nothing")
            .doesNotThrowAnyException();
    }

    @Test
    public void deleteInstance_WithReadOnlyKnowledgeBase_ShouldNoNothing() {
        KBInstance instance = buildInstance();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBHandle handle = sut.createInstance(kb, instance);
        setReadOnly(kb);

        sut.deleteInstance(kb, instance);

        Optional<KBInstance> savedInstance = sut.readInstance(kb, handle.getIdentifier());
        assertThat(savedInstance.isPresent())
            .as("Check that instance was not deleted")
            .isTrue();
    }

    @Test
    public void listInstances_WithASavedInstanceAndNotAll_ShouldFindOneInstance() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBConcept concept = buildConcept();
        KBInstance instance = buildInstance();
        KBHandle conceptHandle = sut.createConcept(kb, concept);
        instance.setType(URI.create(concept.getIdentifier()));
        KBHandle instanceHandle = sut.createInstance(kb, instance);

        List<KBHandle> instances = sut.listInstances(kb, conceptHandle.getIdentifier(), false);

        assertThat(instances)
            .as("Check that instances contain the one, saved item")
            .hasSize(1)
            .element(0)
            .hasFieldOrPropertyWithValue("identifier", instanceHandle.getIdentifier())
            .hasFieldOrPropertyWithValue("name", instanceHandle.getName())
            .matches(h -> h.getIdentifier().startsWith(IriConstants.INCEPTION_NAMESPACE));
    }

    @Test
    public void upsertStatement_WithUnsavedStatement_ShouldCreateStatement() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBConcept concept = buildConcept();
        KBProperty property = buildProperty();
        KBHandle conceptHandle = sut.createConcept(kb, concept);
        KBHandle propertyHandle = sut.createProperty(kb, property);
        KBStatement statement = buildStatement(kb, conceptHandle, propertyHandle, "Test statement");

        sut.upsertStatement(kb, statement);

        List<KBStatement> statements = sut.listStatements(kb, conceptHandle, false);
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
    public void upsertStatement_WithExistingStatement_ShouldUpdateStatement() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBConcept concept = buildConcept();
        KBProperty property = buildProperty();
        KBHandle conceptHandle = sut.createConcept(kb, concept);
        KBHandle propertyHandle = sut.createProperty(kb, property);
        KBStatement statement = buildStatement(kb, conceptHandle, propertyHandle, "Test statement");
        sut.upsertStatement(kb, statement);

        statement.setValue("Altered test property");
        sut.upsertStatement(kb, statement);

        List<KBStatement> statements = sut.listStatements(kb, conceptHandle, false);
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
    public void upsertStatement_WithReadOnlyKnowledgeBase_ShouldDoNothing() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBConcept concept = buildConcept();
        KBProperty property = buildProperty();
        KBHandle conceptHandle = sut.createConcept(kb, concept);
        KBHandle propertyHandle = sut.createProperty(kb, property);
        KBStatement statement = buildStatement(kb, conceptHandle, propertyHandle, "Test statement");
        setReadOnly(kb);

        int statementCountBeforeUpsert = sut.listStatements(kb, conceptHandle, false).size();
        sut.upsertStatement(kb, statement);

        int statementCountAfterUpsert = sut.listStatements(kb, conceptHandle, false).size();
        assertThat(statementCountBeforeUpsert)
            .as("Check that statement was not created")
            .isEqualTo(statementCountAfterUpsert);
    }

    @Test
    public void deleteStatement_WithExistingStatement_ShouldDeleteStatement() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBConcept concept = buildConcept();
        KBProperty property = buildProperty();
        KBHandle conceptHandle = sut.createConcept(kb, concept);
        KBHandle propertyHandle = sut.createProperty(kb, property);
        KBStatement statement = buildStatement(kb, conceptHandle, propertyHandle, "Test statement");
        sut.upsertStatement(kb, statement);

        sut.deleteStatement(kb, statement);

        List<KBStatement> statements = sut.listStatements(kb, conceptHandle, false);
        assertThat(statements)
            .as("Check that the statement was deleted correctly")
            .noneMatch(stmt -> "Test statement".equals(stmt.getValue()));
    }

    @Test
    public void deleteStatement_WithNonExistentStatement_ShouldDoNothing() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBConcept concept = buildConcept();
        KBProperty property = buildProperty();
        KBHandle conceptHandle = sut.createConcept(kb, concept);
        KBHandle propertyHandle = sut.createProperty(kb, property);
        KBStatement statement = buildStatement(kb, conceptHandle, propertyHandle, "Test statement");

        assertThatCode(() -> {
            sut.deleteStatement(kb, statement);
        }).doesNotThrowAnyException();
    }

    @Test
    public void deleteStatement_WithReadOnlyKnowledgeBase_ShouldDoNothing() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBConcept concept = buildConcept();
        KBProperty property = buildProperty();
        KBHandle conceptHandle = sut.createConcept(kb, concept);
        KBHandle propertyHandle = sut.createProperty(kb, property);
        KBStatement statement = buildStatement(kb, conceptHandle, propertyHandle, "Test statement");
        sut.upsertStatement(kb, statement);
        setReadOnly(kb);

        int statementCountBeforeDeletion = sut.listStatements(kb, conceptHandle, false).size();
        sut.deleteStatement(kb, statement);

        int statementCountAfterDeletion = sut.listStatements(kb, conceptHandle, false).size();
        assertThat(statementCountAfterDeletion)
            .as("Check that statement was not deleted")
            .isEqualTo(statementCountBeforeDeletion);
    }

    @Test
    public void listStatements_WithExistentStatementAndNotAll_ShouldReturnOnlyThisStatement() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBConcept concept = buildConcept();
        KBProperty property = buildProperty();
        KBHandle conceptHandle = sut.createConcept(kb, concept);
        KBHandle propertyHandle = sut.createProperty(kb, property);
        KBStatement statement = buildStatement(kb, conceptHandle, propertyHandle, "Test statement");
        sut.upsertStatement(kb, statement);

        List<KBStatement> statements = sut.listStatements(kb, conceptHandle, false);

        assertThat(statements)
            .as("Check that saved statement is found")
            .filteredOn(this::isNotAbstractNorClosedStatement)
            .hasSize(1)
            .element(0)
            .hasFieldOrPropertyWithValue("value", "Test statement");

        assertThat(statements.get(0).getOriginalStatements())
            .as("Check that original statements are recreated")
            .containsExactlyInAnyOrderElementsOf(statement.getOriginalStatements());
    }

    @Test
    public void listStatements_WithNonexistentStatementAndAll_ShouldRetuenAllStatements() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBConcept concept = buildConcept();
        KBHandle conceptHandle = sut.createConcept(kb, concept);

        List<KBStatement> statements = sut.listStatements(kb, conceptHandle, true);

        assertThat(statements)
            .filteredOn(this::isNotAbstractNorClosedStatement)
            .as("Check that all statements have implicit namespace")
            .allMatch(stmt -> hasImplicitNamespace(stmt.getProperty()));
    }

    @Test
    public void getConceptRoots_WithWildlifeOntology_ShouldReturnRootConcepts() throws Exception {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        importKnowledgeBase("data/wildlife_ontology.ttl");
        setSchema(kb, OWL.CLASS, RDFS.SUBCLASSOF, RDF.TYPE, RDFS.COMMENT, RDFS.LABEL, RDF.PROPERTY);

        Stream<String> rootConcepts = sut.listRootConcepts(kb, false).stream()
                .map(KBHandle::getName);

        String[] expectedLabels = {
            "Adaptation", "Animal Intelligence", "Collection", "Conservation Status", "Ecozone",
            "Habitat", "Red List Status", "Taxon Name", "Taxonomic Rank"
        };
        assertThat(rootConcepts)
            .as("Check that all root concepts have been found")
            .containsExactlyInAnyOrder(expectedLabels);
    }

    @Test
    public void getConceptRoots_WithWildlifeOntologyAndExplicityDefinedConcepts_ShouldReturnRootConcepts() throws Exception {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        ValueFactory vf = SimpleValueFactory.getInstance();
        IRI rootConcept1 = vf.createIRI("http://purl.org/ontology/wo/AnimalIntelligence");
        IRI rootConcept2 = vf.createIRI("http://purl.org/ontology/wo/Ecozone");
        List<IRI> concepts = new ArrayList<IRI>();
        concepts.add(rootConcept1);
        concepts.add(rootConcept2);
        kb.setExplicitlyDefinedRootConcepts(concepts);
        sut.updateKnowledgeBase(kb, sut.getNativeConfig());

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
    public void getConceptRoots_WithSparqlPlayground_ReturnsOnlyRootConcepts() throws Exception {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        importKnowledgeBase("data/sparql_playground.ttl");
        setSchema(kb, RDFS.CLASS, RDFS.SUBCLASSOF, RDF.TYPE, RDFS.COMMENT, RDFS.LABEL, RDF.PROPERTY);

        Stream<String> childConcepts = sut.listRootConcepts(kb, false).stream()
                .map(KBHandle::getName);

        String[] expectedLabels = { "creature" };
        assertThat(childConcepts)
            .as("Check that only root concepts")
            .containsExactlyInAnyOrder(expectedLabels);
    }

    @Test
    public void getChildConcepts_WithSparqlPlayground_ReturnsAnimals() throws Exception {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        importKnowledgeBase("data/sparql_playground.ttl");
        setSchema(kb, RDFS.CLASS, RDFS.SUBCLASSOF, RDF.TYPE, RDFS.COMMENT, RDFS.LABEL, RDF.PROPERTY);
        KBConcept concept = sut.readConcept(kb, "http://example.org/tuto/ontology#Animal").get();

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
    public void getChildConcepts_WithStreams_ReturnsOnlyImmediateChildren() throws Exception {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        importKnowledgeBase("data/streams.ttl");
        KBConcept concept = sut.readConcept(kb, "http://mrklie.com/schemas/streams#input").get();
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
    public void getEnabledKnowledgeBases_WithOneEnabledOneDisabled_ReturnsOnlyEnabledKB()
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
    public void getEnabledKnowledgeBases_WithoutEnabledKnowledgeBases_ShouldReturnEmptyList()
    {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        kb.setEnabled(false);
        sut.updateKnowledgeBase(kb, sut.getNativeConfig());

        List<KnowledgeBase> knowledgeBases = sut.getEnabledKnowledgeBases(project);

        assertThat(knowledgeBases).as("Check that the list is empty").isEmpty();
    }

    @Test
    public void listStatementsWithPredicateOrObjectReference_WithExistingStatements_ShouldOnlyReturnStatementsWhereIdIsPredOrObj()
    {
        KBInstance subjectInstance = buildInstance();
        KBInstance objectInstance = buildInstance();
        KBProperty property = buildProperty();

        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        KBHandle subjectHandle = sut.createInstance(kb, subjectInstance);
        KBHandle predHandle = sut.createProperty(kb, property);
        KBHandle objectHandle = sut.createInstance(kb, objectInstance);
        String testInstanceId = objectHandle.getIdentifier();

        KBStatement stmt1 = buildStatement(kb, subjectHandle, predHandle, testInstanceId);

        sut.upsertStatement(kb, stmt1);
        List<Statement> result = sut.listStatementsWithPredicateOrObjectReference(kb, testInstanceId);
        assertThat(result)
            .allMatch(new Predicate<Statement>() {

                @Override
                public boolean test(Statement arg0)
                {
                    return arg0.getObject().stringValue().equals(testInstanceId);
                }

            });
        assertTrue(result.size() >= 1);

    }

    @Test
    public void statementsMatchSPO_WithMatchedStatement_ShouldReturnTrue()
    {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBConcept concept = buildConcept();
        KBProperty property = buildProperty();
        KBHandle conceptHandle = sut.createConcept(kb, concept);
        KBHandle propertyHandle = sut.createProperty(kb, property);
        KBStatement statement = buildStatement(kb, conceptHandle, propertyHandle, "Test statement");

        sut.upsertStatement(kb, statement);

        KBStatement mockStatement = buildStatement(kb, conceptHandle, propertyHandle,
            "Test statement");
        assertTrue(sut.statementsMatchSPO(kb, mockStatement));
    }

    @Test
    public void statementsMatchSPO_WithMissmatchedStatement_ShouldReturnFalse()
    {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBConcept concept = buildConcept();
        KBProperty property = buildProperty();
        KBHandle conceptHandle = sut.createConcept(kb, concept);
        KBHandle propertyHandle = sut.createProperty(kb, property);
        KBStatement statement = buildStatement(kb, conceptHandle, propertyHandle, "Test");

        sut.upsertStatement(kb, statement);

        KBStatement mockStatement = buildStatement(kb, conceptHandle, propertyHandle,
            "Test statement");
        assertFalse(sut.statementsMatchSPO(kb, mockStatement));
    }

    @Test
    public void readFirst()
    {
        KBInstance germanInstance = buildInstanceWithLanguage("de");
        KBInstance englishInstance = buildInstanceWithLanguage("en");

        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBHandle germanHandle = sut.createInstance(kb, germanInstance);

        // Create English instance and ensure that both have the same identifier
        KBHandle englishHandle = sut.update(kb, (conn) -> {
            englishInstance.setIdentifier(germanHandle.getIdentifier());
            englishInstance.write(conn, kb);
            return new KBHandle(germanHandle.getIdentifier(), englishInstance.getName());
        });

        KBInstance firstInstance = sut.readInstance(kb, germanHandle.getIdentifier()).get();
        assertThat(firstInstance.getLanguage())
            .as("Check that the English instance is retrieved.")
            .isEqualTo("en");
    }

    @Test
    public void readKnowledgeBaseProfiles_ShouldReturnValidHashMapWithProfiles() throws IOException {
        Map<String, KnowledgeBaseProfile> profiles = sut.readKnowledgeBaseProfiles();

        assertThat(profiles)
            .allSatisfy((key, profile) -> {
                assertThat(key).isNotNull();
                assertThat(profile).hasNoNullFieldsOrProperties();
            });

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

    private KBProperty buildProperty() {
        return testFixtures.buildProperty();
    }

    private KBInstance buildInstance() {
        return testFixtures.buildInstance();
    }

    private KBInstance buildInstanceWithLanguage(String aLanguage) {
        return testFixtures.buildInstanceWithLanguage(aLanguage);
    }

    private KBStatement buildStatement(KnowledgeBase knowledgeBase, KBHandle conceptHandle,
        KBHandle propertyHandle, String value)
    {
        KBStatement stmt = testFixtures.buildStatement(conceptHandle, propertyHandle, value);
        sut.initStatement(knowledgeBase, stmt);
        return stmt;
    }

    private boolean isNotAbstractNorClosedStatement(KBStatement statement) {
        String id = statement.getProperty().getIdentifier();
        return !(id.endsWith("#abstract") || id.endsWith("#closed"));
    }

    private boolean hasImplicitNamespace(KBHandle handle) {
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
