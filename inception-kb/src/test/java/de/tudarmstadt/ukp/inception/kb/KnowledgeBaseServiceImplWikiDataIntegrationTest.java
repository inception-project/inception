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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

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
public class KnowledgeBaseServiceImplWikiDataIntegrationTest  {

    private static final String PROJECT_NAME = "Test project";
    private static final String KB_NAME = "Wikidata (official/direct mapping)";

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
    private static Map<String, KnowledgeBaseProfile> PROFILES;
    public KnowledgeBaseServiceImplWikiDataIntegrationTest(Reification aReification) {
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
        sut.registerKnowledgeBase(kb, sut.getRemoteConfig(PROFILES.get("wikidata").getSparqlUrl()));

    }

    @After
    public void tearDown() throws Exception {
        testEntityManager.clear();
        sut.destroy();
    }

    @Test
    public void readConcept_WithNonexistentConcept_ShouldReturnEmptyResult() {
        Optional<KBConcept> savedConcept = sut.readConcept(kb, "https://nonexistent.identifier.test");
        assertThat(savedConcept.isPresent())
            .as("Check that no concept was read")
            .isFalse();
    }
    
    @Test
    public void readConcept_WithExistentConcept_ShouldReturnResult() {
        Optional<KBConcept> savedConcept = sut.readConcept(kb, "http://www.wikidata.org/entity/Q171644");
        assertThat(savedConcept.get().getName())
            .as("Check that concept has the same UI label")
            .matches("12 Hours of Reims");
    }

    @Test
    public void listChildConcept_WithExistentConcept_ShouldReturnResult() {
        List<KBHandle> savedConcept = sut.listChildConcepts(kb, "http://www.wikidata.org/entity/Q171644", true);
        assertThat(savedConcept.iterator().next().getUiLabel())
            .as("Check that concept has the same UI label")
            .matches("1965 12 Hours of Reims");
    }
    
    @Test
    public void listConcepts() {
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        List<KBHandle> concepts = sut.listRootConcepts(kb, true);

        assertThat(concepts)
            .as("Check that all concepts have implicit namespaces")
            .allMatch(this::hasImplicitNamespace);
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

    // Helper

    private Project createProject(String name) {
        return testFixtures.createProject(name);
    }

    private KnowledgeBase buildKnowledgeBase(Project project, String name) throws IOException {
        PROFILES = readKnowledgeBaseProfiles();
        KnowledgeBase kb_wikidata_direct = new KnowledgeBase();
        kb_wikidata_direct.setProject(project);
        kb_wikidata_direct.setName("Wikidata (official/direct mapping)");
        kb_wikidata_direct.setType(RepositoryType.REMOTE);
        kb_wikidata_direct.applyMapping(PROFILES.get("wikidata").getMapping());
        kb_wikidata_direct.setReification(reification);
       
        return kb_wikidata_direct;
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
    
    public static Map<String, KnowledgeBaseProfile> readKnowledgeBaseProfiles() throws IOException
    {
        try (Reader r = new InputStreamReader(KnowledgeBaseServiceRemoteTest.class
                .getResourceAsStream("knowledgebase-profiles.yaml"), StandardCharsets.UTF_8)) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            return mapper.readValue(r, new TypeReference<HashMap<String, KnowledgeBaseProfile>>()
            {
            });
        }
    }
    
    
    
}
