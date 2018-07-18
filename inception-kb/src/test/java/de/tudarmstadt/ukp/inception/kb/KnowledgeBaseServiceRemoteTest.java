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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.eclipse.rdf4j.model.IRI;
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
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.reification.Reification;
import de.tudarmstadt.ukp.inception.kb.util.TestFixtures;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseMapping;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseProfile;

@RunWith(Parameterized.class)
@SpringBootTest(classes = SpringConfig.class)
@Transactional
@DataJpaTest
public class KnowledgeBaseServiceRemoteTest
{
    private final String PROJECT_NAME = "Test project";
    
    private KnowledgeBaseServiceImpl sut;
    private Project project;
    private TestFixtures testFixtures;

    private static Reification reification;
    private static KnowledgeBase kb;

    private static Map<String, KnowledgeBaseProfile> kbProfileMap;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Autowired
    private TestEntityManager testEntityManager;

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

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
        sut = new KnowledgeBaseServiceImpl(temporaryFolder.getRoot(), entityManager);
        project = testFixtures.createProject(PROJECT_NAME);
        kb.setProject(project);
        if (kb.getType() == RepositoryType.LOCAL) {
            sut.registerKnowledgeBase(kb, sut.getNativeConfig());
            sut.updateKnowledgeBase(kb, sut.getKnowledgeBaseConfig(kb));
            importKnowledgeBase("data/wine-ontology.rdf");
        }
        else if (kb.getType() == RepositoryType.REMOTE) {
            KnowledgeBaseProfile profile = kbProfileMap
                    .get(kb.getName());
            sut.registerKnowledgeBase(kb, sut.getRemoteConfig(profile.getSparqlUrl()));
            sut.updateKnowledgeBase(kb, sut.getKnowledgeBaseConfig(kb));
        }
    }

    @After
    public void tearDown() throws Exception
    {
        testEntityManager.clear();
        sut.destroy();
    }

    public KnowledgeBaseServiceRemoteTest(KnowledgeBase akb, Reification aReification) throws Exception
    {
        reification = aReification;
        kb = akb;

    }

    @Parameterized.Parameters(name = "Reification = {1} : KB = {0}")
    public static List<Object[]> data() throws Exception
    {
        List<Object[]> dataList = new ArrayList<Object[]>();
        List<KnowledgeBase> kbList = addKBProfileSetup();
        for (KnowledgeBase kb : kbList) {
            for (Reification r : Reification.values()) {
                kb.setReification(r);
                dataList.add(new Object[] { kb, r });
            }
        }
        return dataList;
    }

    @Test
    public void thatRootConceptsCanBeRetrieved()
    {
        List<KBHandle> rootConceptKBHandle = sut.listRootConcepts(kb, true);
        assertThat(rootConceptKBHandle).as("Check that root concept list is not empty")
                .isNotEmpty();
    }

    @Test
    public void thatPropertyListCanBeRetrieved()
    {
        List<KBHandle> propertiesKBHandle = sut.listProperties(kb, kb.getPropertyTypeIri(), true,
                true);
        assertThat(propertiesKBHandle).as("Check that property list is not empty").isNotEmpty();
    }

    // Helper

    private static KnowledgeBase buildDefaultKnowledgeBase(String name)
    {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(name);
        kb.setType(RepositoryType.LOCAL);
        kb.setClassIri(RDFS.CLASS);
        kb.setSubclassIri(RDFS.SUBCLASSOF);
        kb.setTypeIri(RDF.TYPE);
        kb.setLabelIri(RDFS.LABEL);
        kb.setPropertyTypeIri(RDF.PROPERTY);
        kb.setDescriptionIri(RDFS.COMMENT);
        return kb;

    }

    private void importKnowledgeBase(String resourceName) throws Exception
    {
        ClassLoader classLoader = KnowledgeBaseServiceRemoteTest.class.getClassLoader();
        String fileName = classLoader.getResource(resourceName).getFile();
        try (InputStream is = classLoader.getResourceAsStream(resourceName)) {
            sut.importData(kb, fileName, is);
        }
    }

    public static List<KnowledgeBase> addKBProfileSetup() throws Exception
    {
        List<KnowledgeBase> kbList = new ArrayList<KnowledgeBase>();
        // Configuration for Local Ontology
        String kbProfileName = "Wine";
        kb = buildDefaultKnowledgeBase(kbProfileName);
        kb.setType(RepositoryType.LOCAL);
        setSchema(kb, OWL.CLASS, RDFS.SUBCLASSOF, RDF.TYPE, RDFS.COMMENT, RDFS.LABEL, RDF.PROPERTY);
        kbList.add(kb);

        // Configurations for Remote KB
        kbProfileMap = readKnowledgeBaseProfiles();
        //String[] kbProfileArray = { "wikidata", "virtuoso", "db_pedia", "yago" };
        String[] kbProfileArray = { "wikidata", "db_pedia", "yago" };
        for (String name : kbProfileArray) {
            kbList.add(getParameterizedKB(name));
        }

        return kbList;
    }
    
    public static KnowledgeBase getParameterizedKB(String kbProfileName) {
        kb = buildDefaultKnowledgeBase(kbProfileName);
        kb.setType(RepositoryType.REMOTE);
        KnowledgeBaseMapping mapping = kbProfileMap.get(kbProfileName).getMapping();
        setSchema(kb, mapping);
        return kb;
        
    }
    
    private static void setSchema(KnowledgeBase kb, IRI classIri, IRI subclassIri, IRI typeIri,
            IRI descriptionIri, IRI labelIri, IRI propertyTypeIri)
    {
        kb.setClassIri(classIri);
        kb.setSubclassIri(subclassIri);
        kb.setTypeIri(typeIri);
        kb.setDescriptionIri(descriptionIri);
        kb.setLabelIri(labelIri);
        kb.setPropertyTypeIri(propertyTypeIri);
        kb.setReification(reification);
        // sut.updateKnowledgeBase(kb, sut.getKnowledgeBaseConfig(kb));
    }

    private static void setSchema(KnowledgeBase kb, KnowledgeBaseMapping mapping)
    {
        setSchema(kb, mapping.getClassIri(), mapping.getSubclassIri(), mapping.getTypeIri(),
                mapping.getDescriptionIri(), mapping.getLabelIri(), mapping.getPropertyTypeIri());
    }

    public static Map<String, KnowledgeBaseProfile> readKnowledgeBaseProfiles() throws IOException
    {
        try (Reader r = new InputStreamReader(
                KnowledgeBaseServiceRemoteTest.class.getResourceAsStream("knowledgebase-profiles.yaml"),
                StandardCharsets.UTF_8)) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            return mapper.readValue(r, new TypeReference<HashMap<String, KnowledgeBaseProfile>>()
            {
            });
        }
    }

}
