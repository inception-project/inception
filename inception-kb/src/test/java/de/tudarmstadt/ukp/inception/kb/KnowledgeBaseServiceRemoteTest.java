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
import java.util.Set;

import javax.persistence.EntityManager;

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
import org.junit.rules.TestWatcher;
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
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseProfile;

@RunWith(Parameterized.class)
@SpringBootTest(classes = SpringConfig.class)
@Transactional
@DataJpaTest
public class KnowledgeBaseServiceRemoteTest
{
    private final String PROJECT_NAME = "Test project";

    private static Map<String, KnowledgeBaseProfile> PROFILES;
    
    private final TestConfiguration sutConfig;
    
    private KnowledgeBaseServiceImpl sut;
    
    private Project project;
    private TestFixtures testFixtures;

    @Rule
    public TestWatcher watcher = new TestWatcher()
    {
        @Override
        protected void starting(org.junit.runner.Description aDescription)
        {
            String methodName = aDescription.getMethodName();
            System.out.println("\n=== " + methodName + " =====================");
        };
    };
    
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Autowired
    private TestEntityManager testEntityManager;

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @BeforeClass
    public static void setUpOnce() throws Exception
    {
        System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
    }

    @Before
    public void setUp() throws Exception
    {
        KnowledgeBase kb = sutConfig.getKnowledgeBase();
        
        EntityManager entityManager = testEntityManager.getEntityManager();
        testFixtures = new TestFixtures(testEntityManager);
        sut = new KnowledgeBaseServiceImpl(temporaryFolder.getRoot(), entityManager);
        project = testFixtures.createProject(PROJECT_NAME);
        kb.setProject(project);
        if (kb.getType() == RepositoryType.LOCAL) {
            sut.registerKnowledgeBase(kb, sut.getNativeConfig());
            sut.updateKnowledgeBase(kb, sut.getKnowledgeBaseConfig(kb));
            importKnowledgeBase(sutConfig.getDataUrl());
        }
        else if (kb.getType() == RepositoryType.REMOTE) {
            sut.registerKnowledgeBase(kb, sut.getRemoteConfig(sutConfig.getDataUrl()));
            sut.updateKnowledgeBase(kb, sut.getKnowledgeBaseConfig(kb));
        }
        else {
            throw new IllegalStateException("Unknown type: " + sutConfig.getKnowledgeBase().getType());
        }
    }

    @After
    public void tearDown() throws Exception
    {
        testEntityManager.clear();
        sut.destroy();
    }

    public KnowledgeBaseServiceRemoteTest(TestConfiguration aConfig)
        throws Exception
    {
        sutConfig = aConfig;
    }

    @Parameterized.Parameters(name = "KB = {0}")
    public static List<Object[]> data() throws Exception
    {
        PROFILES = readKnowledgeBaseProfiles();

        List<TestConfiguration> kbList = new ArrayList<>();
        
        { 
            KnowledgeBase kb_wine = new KnowledgeBase();
            kb_wine.setName("Wine ontology (OWL)");
            kb_wine.setType(RepositoryType.LOCAL);
            kb_wine.setReification(Reification.NONE);
            kb_wine.setClassIri(OWL.CLASS);
            kb_wine.setSubclassIri(RDFS.SUBCLASSOF);
            kb_wine.setTypeIri(RDF.TYPE);
            kb_wine.setLabelIri(RDFS.LABEL);
            kb_wine.setPropertyTypeIri(RDF.PROPERTY);
            kb_wine.setDescriptionIri(RDFS.COMMENT);
            kbList.add(new TestConfiguration("data/wine-ontology.rdf", kb_wine, "http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#ChateauMargaux"));
        }
        
        {
            KnowledgeBase kb_wikidata_direct = new KnowledgeBase();
            kb_wikidata_direct.setName("Wikidata (official/direct mapping)");
            kb_wikidata_direct.setType(RepositoryType.REMOTE);
            kb_wikidata_direct.setReification(Reification.NONE);
            kb_wikidata_direct.applyMapping(PROFILES.get("wikidata").getMapping());
            kbList.add(new TestConfiguration(PROFILES.get("wikidata").getSparqlUrl(),
                    kb_wikidata_direct, "http://www.wikidata.org/entity/Q19576436"));
        }

        // This profile is yet incomplete and needs to be fixed
        // {
        // ValueFactory vf = SimpleValueFactory.getInstance();
        //
        // KnowledgeBase kb_wikidata_reified = new KnowledgeBase();
        // kb_wikidata_reified.setName("Wikidata (official/reified)");
        // kb_wikidata_reified.setType(RepositoryType.REMOTE);
        // kb_wikidata_reified.setReification(Reification.WIKIDATA);
        // kb_wikidata_reified.setClassIri(OWL.CLASS); // FIXME
        // kb_wikidata_reified.setSubclassIri(vf.createIRI("http://www.wikidata.org/prop/P279"));
        // kb_wikidata_reified.setTypeIri(vf.createIRI("http://www.wikidata.org/prop/P31"));
        // kb_wikidata_reified.setLabelIri(RDFS.LABEL);
        // kb_wikidata_reified.setPropertyTypeIri(RDF.PROPERTY); // FIXME
        // kb_wikidata_reified.setDescriptionIri(vf.createIRI("http://schema.org/description"));
        // kbList.add(kb_wikidata_reified);
        // }

        {
            KnowledgeBase kb_dbpedia = new KnowledgeBase();
            kb_dbpedia.setName("DKPedia (official)");
            kb_dbpedia.setType(RepositoryType.REMOTE);
            kb_dbpedia.setReification(Reification.NONE);
            kb_dbpedia.applyMapping(PROFILES.get("db_pedia").getMapping());
            kbList.add(new TestConfiguration(PROFILES.get("db_pedia").getSparqlUrl(),
                    kb_dbpedia, "http://www.wikidata.org/entity/Q20280393" ));
        }
       
        {
            KnowledgeBase kb_yago = new KnowledgeBase();
            kb_yago.setName("Yago (official)");
            kb_yago.setType(RepositoryType.REMOTE);
            kb_yago.setReification(Reification.NONE);
            kb_yago.applyMapping(PROFILES.get("yago").getMapping());
            kbList.add(new TestConfiguration(PROFILES.get("yago").getSparqlUrl(),
                    kb_yago, "http://www.wikidata.org/entity/Q21445637S003fc070-45f0-80bd-ae2d-072cde5aad89"));
        }
        
        {
            KnowledgeBase kb_zbw = new KnowledgeBase();
            kb_zbw.setName("ZBW (official/direct mapping)");
            kb_zbw.setType(RepositoryType.REMOTE);
            kb_zbw.setReification(Reification.NONE);
            kb_zbw = setOWLSchemaMapping(kb_zbw);
            kbList.add(new TestConfiguration("http://zbw.eu/beta/sparql/stw/query",
                    kb_zbw, "http://zbw.eu/stw/thsys/71020"));
        }
        
        List<Object[]> dataList = new ArrayList<>();
        for (TestConfiguration kb : kbList) {
            dataList.add(new Object[] { kb });
        }
        return dataList;
    }

    @Test
    public void thatRootConceptsCanBeRetrieved()
    {
        KnowledgeBase kb = sutConfig.getKnowledgeBase();
        
        long duration = System.currentTimeMillis();
        List<KBHandle> rootConceptKBHandle = sut.listRootConcepts(kb, true);
        duration = System.currentTimeMillis() - duration;

        System.out.printf("Root concepts retrieved : %d%n", rootConceptKBHandle.size());
        System.out.printf("Time required           : %d ms%n", duration);
        rootConceptKBHandle.stream().limit(10).forEach(h -> System.out.printf("   %s%n", h));
        
        assertThat(rootConceptKBHandle).as("Check that root concept list is not empty")
                .isNotEmpty();
    }

    @Test
    public void thatPropertyListCanBeRetrieved()
    {
        KnowledgeBase kb = sutConfig.getKnowledgeBase();
        
        long duration = System.currentTimeMillis();
        List<KBHandle> propertiesKBHandle = sut.listProperties(kb, true);
        duration = System.currentTimeMillis() - duration;

        System.out.printf("Properties retrieved : %d%n", propertiesKBHandle.size());
        System.out.printf("Time required        : %d ms%n", duration);
        propertiesKBHandle.stream().limit(10).forEach(h -> System.out.printf("   %s%n", h));

        assertThat(propertiesKBHandle).as("Check that property list is not empty").isNotEmpty();

    }

    @Test
    public void thatParentListCanBeRetireved()
    {
        KnowledgeBase kb = sutConfig.getKnowledgeBase();
        
        long duration = System.currentTimeMillis();
        Set<KBHandle> parentList = sut.getParentConceptList(kb, sutConfig.getTestIdentifier(), true);
        duration = System.currentTimeMillis() - duration;

        System.out.printf("Parent List retrieved : %d%n", parentList.size());
        System.out.printf("Time required        : %d ms%n", duration);
        parentList.stream().limit(10).forEach(h -> System.out.printf("   %s%n", h));

        assertThat(parentList).as("Check that parent list is not empty").isNotEmpty();

    }

    // Helper

    private void importKnowledgeBase(String resourceName) throws Exception
    {
        ClassLoader classLoader = KnowledgeBaseServiceRemoteTest.class.getClassLoader();
        String fileName = classLoader.getResource(resourceName).getFile();
        try (InputStream is = classLoader.getResourceAsStream(resourceName)) {
            sut.importData(sutConfig.getKnowledgeBase(), fileName, is);
        }
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
    
    public static KnowledgeBase setOWLSchemaMapping(KnowledgeBase kb) {
        kb.setClassIri(OWL.CLASS);
        kb.setSubclassIri(RDFS.SUBCLASSOF);
        kb.setTypeIri(RDF.TYPE);
        kb.setDescriptionIri(RDFS.COMMENT);
        kb.setLabelIri(RDFS.LABEL);
        kb.setPropertyTypeIri(RDF.PROPERTY);
        return kb;
    }
    
    private static class TestConfiguration
    {
        private final String url;
        private final KnowledgeBase kb;
        private final String testIdentifier;
        public TestConfiguration(String aUrl, KnowledgeBase aKb, String atestIdentifier)
        {
            super();
            url = aUrl;
            kb = aKb;
            testIdentifier = atestIdentifier;
        }
        
        public KnowledgeBase getKnowledgeBase()
        {
            return kb;
        }
        
        public String getDataUrl()
        {
            return url;
        }

        public String getTestIdentifier()
        {
            return testIdentifier;
        }

        @Override
        public String toString()
        {
            return kb.getName();
        }
    }
}
