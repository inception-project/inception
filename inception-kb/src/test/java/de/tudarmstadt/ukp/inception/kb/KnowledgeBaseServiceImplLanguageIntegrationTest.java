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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;

import org.eclipse.rdf4j.model.IRI;
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
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.reification.Reification;
import de.tudarmstadt.ukp.inception.kb.util.TestFixtures;

@RunWith(Parameterized.class)
@SpringBootTest(classes = SpringConfig.class)
@Transactional
@DataJpaTest
public class KnowledgeBaseServiceImplLanguageIntegrationTest  {

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

    public KnowledgeBaseServiceImplLanguageIntegrationTest(Reification aReification) {
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
        kb.setDefaultLanguage("en");
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
            .hasFieldOrPropertyWithValue("defaultLanguage", "en")
            .hasFieldOrPropertyWithValue("explicitlyDefinedRootConcepts", Arrays.asList(rootConcept1, rootConcept2));
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
            .hasFieldOrPropertyWithValue("name",handle.getName())
            .matches(h -> h.getIdentifier().startsWith(IriConstants.INCEPTION_NAMESPACE));
    }

    @Test
    public void updateProperty_WithAlteredProperty_ShouldUpdateProperty() {
        KBProperty property = buildProperty();
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        KBHandle handle = sut.createProperty(kb, property);

        property.setDescription("New property description");
        property.setDomain("https://new.schema.com/#domain");
        property.setName("New property name");
        property.setRange("https://new.schema.com/#range");
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
            .hasFieldOrPropertyWithValue("name",handle.getName())
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
            .hasFieldOrPropertyWithValue("name",instanceHandle.getName())
            .matches(h -> h.getIdentifier().startsWith(IriConstants.INCEPTION_NAMESPACE));
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
        String[] expectedLabels = { "Creature" };
        assertThat(childConcepts).as("Check that only root concepts")
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
            "Cat", "Dog", "Monkey"
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
            "bytearrayinput", "fileinput", "filterinput", "objectinput",
            "pipedinput","sequenceinput", "stringbufferinput"
        };
        assertThat(childConcepts)
            .as("Check that all immediate child concepts have been found")
            .containsExactlyInAnyOrder(expectedLabels);
    }

    // Helper

    private Project createProject(String name) {
        return testFixtures.createProject(name);
    }

    private KnowledgeBase buildKnowledgeBase(Project project, String name) {
        KnowledgeBase kb = testFixtures.buildKnowledgeBase(project, name, reification);
        kb.setDefaultLanguage("en");
        return kb;
    }

    private KBConcept buildConcept() {
        KBConcept concept = testFixtures.buildConcept();
        concept.setLanguage("en");
        return concept;
    }

    private KBProperty buildProperty() {
        return testFixtures.buildProperty();
    }

    private KBInstance buildInstance() {
        return testFixtures.buildInstance();
    }

    private boolean hasImplicitNamespace(KBHandle handle)
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
