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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
@ContextConfiguration(classes = SpringConfig.class)
@Transactional
@DataJpaTest
public class KnowledgeBaseServiceImplWikiDataIntegrationTest  {

    private static final String KBSIWDIT_PROJECT_NAME = "Test KBSIWDIT_project";
    private static final String KBSIWDIT_KB_NAME = "Wikidata (official/direct mapping)";

    @Rule
    public TemporaryFolder KBSIWDIT_temporaryFolder = new TemporaryFolder();

    @Autowired
    private TestEntityManager KBSIWDIT_testEntityManager;

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule KBSIWDIT_springMethodRule = new SpringMethodRule();

    private KnowledgeBaseServiceImpl KBSIWDIT_sut;
    private Project KBSIWDIT_project;
    private KnowledgeBase KBSIWDIT_kb;
    private Reification reification;

    private TestFixtures testFixtures;
    private static Map<String, KnowledgeBaseProfile> PROFILES;

    public KnowledgeBaseServiceImplWikiDataIntegrationTest(Reification aReification)
    {
        reification = aReification;
    }

    @Parameterized.Parameters(name = "Reification = {0}")
    public static Collection<Object[]> data()
    {
        return Arrays.stream(Reification.values()).map(r -> new Object[] { r })
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
        RepositoryProperties repoProps = new RepositoryProperties();
        repoProps.setPath(KBSIWDIT_temporaryFolder.getRoot());
        EntityManager entityManager = KBSIWDIT_testEntityManager.getEntityManager();
        testFixtures = new TestFixtures(KBSIWDIT_testEntityManager);
        KBSIWDIT_sut = new KnowledgeBaseServiceImpl(repoProps, entityManager);
        KBSIWDIT_project = createProject(KBSIWDIT_PROJECT_NAME);
        KBSIWDIT_kb = buildKnowledgeBase(KBSIWDIT_project, KBSIWDIT_KB_NAME);
        String wikidataAccessUrl = PROFILES.get("wikidata").getAccess().getAccessUrl();
        testFixtures.assumeEndpointIsAvailable(wikidataAccessUrl);
        KBSIWDIT_sut.registerKnowledgeBase(KBSIWDIT_kb, KBSIWDIT_sut.getRemoteConfig(wikidataAccessUrl));

    }

    @After
    public void tearDown() throws Exception
    {
        KBSIWDIT_testEntityManager.clear();
        KBSIWDIT_sut.destroy();
    }

    @Test
    public void readConcept_WithNonexistentConcept_ShouldReturnEmptyResult()
    {
        Optional<KBConcept> savedConcept = KBSIWDIT_sut.readConcept(KBSIWDIT_kb,
                "https://nonexistent.identifier.test", true);
        assertThat(savedConcept.isPresent()).as("Check that no concept was read").isFalse();
    }
    
    @Test
    public void readConcept_WithExistentConcept_ShouldReturnResult()
    {
        Optional<KBConcept> concept = KBSIWDIT_sut.readConcept(KBSIWDIT_kb, "http://www.wikidata.org/entity/Q171644",
                true);
        assertThat(concept.get().getName()).as("Check that concept has the same UI label")
                .isIn("12 Hours of Reims");
    }
    
    @Test
    public void listChildConcept_WithExistentConcept_ShouldReturnResult()
    {
        List<KBHandle> concept = KBSIWDIT_sut.listChildConcepts(KBSIWDIT_kb, "http://www.wikidata.org/entity/Q171644",
                true);

        assertThat(concept.iterator().next().getUiLabel())
                .as("Check that concept has the same UI label")
                .isIn("12-Stunden-Rennen von Reims 1965", "1965 12 Hours of Reims");
    }
    
    @Test
    public void listRootConcepts()
    {
        Stream<String> rootConcepts = KBSIWDIT_sut.listRootConcepts(KBSIWDIT_kb, false).stream()
                .map(KBHandle::getIdentifier);
        String expectedInstances = "http://www.wikidata.org/entity/Q35120";
        
        assertThat(rootConcepts)
            .as("Check that root concepts have been found")
            .contains(expectedInstances);
    }

    
    @Test
    public void listProperties() {
        Stream<String> properties = KBSIWDIT_sut.listProperties(KBSIWDIT_kb, true)
            .stream()
            .map(KBObject::getIdentifier);
        
        assertThat(properties)
            .as("Check that properties have been found")
            .hasSize(KBSIWDIT_kb.getMaxResults());
    }
    
    @Test
    public void readInstance_WithNonexistentInstance_ShouldReturnEmptyResult()
    {
        KBSIWDIT_sut.registerKnowledgeBase(KBSIWDIT_kb, KBSIWDIT_sut.getNativeConfig());

        Optional<KBInstance> savedInstance = KBSIWDIT_sut.readInstance(KBSIWDIT_kb,
                "https://nonexistent.identifier.test");

        assertThat(savedInstance.isPresent()).as("Check that no instance was read").isFalse();
    }

    @Test
    public void listInstances()
    {
        Stream<String> instances = KBSIWDIT_sut
                .listInstances(KBSIWDIT_kb, "http://www.wikidata.org/entity/Q2897", true).stream()
                .map(KBHandle::getIdentifier);
        String[] expectedInstances = { "http://www.wikidata.org/entity/Q22663448",
                "http://www.wikidata.org/entity/Q22663448",
                "http://www.wikidata.org/entity/Q30059050" };
        assertThat(instances).as("Check that instances have been found")
                .contains(expectedInstances);

    }
    
    @Test
    public void listStatements() {
        KBHandle handle = new KBHandle("http://www.wikidata.org/entity/Q50556889");

        Stream<String> properties = KBSIWDIT_sut.listStatements(KBSIWDIT_kb, handle, true).stream()
                .map(KBStatement::getProperty)
                .map(KBProperty::getIdentifier);

        if (reification == Reification.NONE) {
            String[] expectedInstances = { 
                    "http://www.wikidata.org/prop/P2894",
                    "http://www.wikidata.org/prop/direct/P2894",
                    "http://www.wikidata.org/prop/direct/P31", 
                    "http://www.wikidata.org/prop/P31" };
            assertThat(properties).as("Check that properties have been found")
                    .contains(expectedInstances);
        }
        else {
            String[] expectedInstances = { 
                    "http://www.wikidata.org/prop/P585",
                    "http://www.wikidata.org/prop/P31",
                    "http://www.wikidata.org/prop/P361", 
                    "http://www.wikidata.org/prop/P2894",
                    "http://www.wikidata.org/prop/P31" };
            assertThat(properties).as("Check that properties have been found")
                    .contains(expectedInstances);
        }
    }
    
    
    // Helper

    private Project createProject(String name)
    {
        return testFixtures.createProject(name);
    }

    private KnowledgeBase buildKnowledgeBase(Project KBSIWDIT_project, String name) throws IOException
    {
        PROFILES = KnowledgeBaseProfile.readKnowledgeBaseProfiles();
        KnowledgeBase KBSIWDIT_kb_wikidata_direct = new KnowledgeBase();
        KBSIWDIT_kb_wikidata_direct.setProject(KBSIWDIT_project);
        KBSIWDIT_kb_wikidata_direct.setName("Wikidata (official/direct mapping)");
        KBSIWDIT_kb_wikidata_direct.setType(PROFILES.get("wikidata").getType());
        KBSIWDIT_kb_wikidata_direct.applyMapping(PROFILES.get("wikidata").getMapping());
        KBSIWDIT_kb_wikidata_direct.applyRootConcepts(PROFILES.get("wikidata"));
        KBSIWDIT_kb_wikidata_direct.setReification(reification);
        KBSIWDIT_kb_wikidata_direct.setDefaultLanguage("en");
        KBSIWDIT_kb_wikidata_direct.setMaxResults(1000);

        return KBSIWDIT_kb_wikidata_direct;
    }
}
