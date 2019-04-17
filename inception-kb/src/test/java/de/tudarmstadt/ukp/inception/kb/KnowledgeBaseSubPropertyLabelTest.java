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

import javax.persistence.EntityManager;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
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

import de.tudarmstadt.ukp.clarin.webanno.api.dao.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
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
public class KnowledgeBaseSubPropertyLabelTest
{
    private static final String PROJECT_NAME = "Test project";

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
    
    public KnowledgeBaseSubPropertyLabelTest(Reification aReification) {
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
    public void setUp() {
        RepositoryProperties repoProps = new RepositoryProperties();
        repoProps.setPath(temporaryFolder.getRoot());
        EntityManager entityManager = testEntityManager.getEntityManager();
        testFixtures = new TestFixtures(testEntityManager);
        sut = new KnowledgeBaseServiceImpl(repoProps, entityManager);
        project = createProject(PROJECT_NAME);
    }

    @After
    public void tearDown() throws Exception {
        testEntityManager.clear();
        sut.destroy();
    }
    
    @Test
    public void thatChildConceptsLabel() throws IOException
    {
        kb = buildRemoteKnowledgeBase(project, "GND");
        String gndAccessURL = PROFILES.get("zbw-gnd").getAccess().getAccessUrl();
        testFixtures.assumeEndpointIsAvailable(gndAccessURL, 5000);
        sut.registerKnowledgeBase(kb, sut.getRemoteConfig(gndAccessURL));

        long duration = System.currentTimeMillis();
        String concept = "http://d-nb.info/standards/elementset/gnd#Family";
        List<KBHandle> instanceKBHandle = sut.listInstances(kb, concept, true);
        duration = System.currentTimeMillis() - duration;

        System.out.printf("Instances retrieved for %s : %d%n", concept, instanceKBHandle.size());
        System.out.printf("Time required           : %d ms%n", duration);
        instanceKBHandle.stream().limit(10).forEach(h -> System.out.printf("   %s%n", h));

        assertThat(instanceKBHandle).as("Check that instance list is not empty")
                   .isNotEmpty();
        assertThat(instanceKBHandle.stream().map(KBHandle::getName))
                    .as("Check that child concept is retreived")
                    .contains("Abele, Familie");
    }

    @Test
    public void readInstance_ShouldReturnInstanceWithSubPropertyLabel() throws IOException
    {
        kb = buildRemoteKnowledgeBase(project, "GND");
        String gndAccessURL = PROFILES.get("zbw-gnd").getAccess().getAccessUrl();
        testFixtures.assumeEndpointIsAvailable(gndAccessURL, 5000);
        sut.registerKnowledgeBase(kb, sut.getRemoteConfig(gndAccessURL));

        String instanceId = "http://d-nb.info/gnd/7509336-4";
        Optional<KBInstance> instance = sut.readInstance(kb, instanceId);

        assertThat(instance).as("Check that instance is present")
            .isPresent();
        assertThat(instance.get().getName())
            .as("Check that correct label is retrieved")
            .contains("Abingdon, Bettine");
    }

    @Test
    public void readProperty_ShouldReturnPropertyWithSubPropertyLabel() throws IOException
    {
        kb = buildLocalKnowledgeBase(project, "Wine");
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        KBProperty subpropertylabel = createSubPropertyLabel(kb);

        KBProperty property = testFixtures.buildProperty();
        //set name to null so that the subproperty label becomes the main label
        property.setName(null);
        sut.createProperty(kb, property);

        String labelLiteral = "Sub Property Label";
        createStmtWithLiteral(kb, KBHandle.of(property), subpropertylabel, labelLiteral);

        Optional<KBProperty> optProperty = sut.readProperty(kb, property.getIdentifier());
        assertThat(optProperty).as("Check that property is present")
            .isPresent();
        assertThat(optProperty.get().getName())
            .as("Check that correct label is retrieved")
            .contains(labelLiteral);
    }


    //Helper
    
    private Project createProject(String name) {
        return testFixtures.createProject(name);
    }

    private KnowledgeBase buildRemoteKnowledgeBase(Project project, String name) throws IOException
    {
        PROFILES = readKnowledgeBaseProfiles();
        KnowledgeBase gnd = new KnowledgeBase();
        gnd.setProject(project);
        gnd.setName(name);
        gnd.setType(RepositoryType.REMOTE);
        gnd.applyMapping(PROFILES.get("zbw-gnd").getMapping());
        gnd.applyRootConcepts(PROFILES.get("zbw-gnd"));
        gnd.setReification(reification);
        gnd.setDefaultLanguage("en");
        gnd.setMaxResults(1000);

        return gnd;
    }

    private KnowledgeBase buildLocalKnowledgeBase(Project project, String name) throws IOException
    {
        PROFILES = readKnowledgeBaseProfiles();
        KnowledgeBase wine = new KnowledgeBase();
        wine.setProject(project);
        wine.setName(name);
        wine.setType(RepositoryType.LOCAL);
        wine.applyMapping(PROFILES.get("wine_ontology").getMapping());
        wine.applyRootConcepts(PROFILES.get("wine_ontology"));
        wine.setReification(reification);
        wine.setDefaultLanguage("en");
        wine.setMaxResults(1000);

        return wine;
    }

    private KBProperty createSubPropertyLabel(KnowledgeBase aKB)
    {
        KBProperty subLabel = testFixtures.buildProperty();
        sut.createProperty(aKB, subLabel);

        KBProperty subPropertyHandle = new KBProperty(null, aKB.getSubPropertyIri().stringValue());

        KBStatement subPropertyStmt = new KBStatement(null, subLabel.toKBHandle(),
                subPropertyHandle, aKB.getLabelIri());

        upsertStatement(aKB, subPropertyStmt);

        return subLabel;
    }

    private void createStmtWithLiteral(KnowledgeBase aKB, KBHandle aSubject, KBProperty aProperty,
        String aLiteral)
    {
        SimpleValueFactory vf = SimpleValueFactory.getInstance();
        KBStatement stmt = new KBStatement(null, aSubject, aProperty, vf.createLiteral(aLiteral));
        upsertStatement(aKB, stmt);
    }

    private void upsertStatement(KnowledgeBase aKB, KBStatement aStatement)
    {
        //set reification to NONE just for "upserting" the statement, then restore old value
        Reification kbReification = kb.getReification();
        try {
            kb.setReification(Reification.NONE);
            sut.upsertStatement(aKB, aStatement);

        }
        finally {
            kb.setReification(kbReification);
        }
    }

   
    public static Map<String, KnowledgeBaseProfile> readKnowledgeBaseProfiles() throws IOException
    {
        return KnowledgeBaseProfile.readKnowledgeBaseProfiles();
    }
}
