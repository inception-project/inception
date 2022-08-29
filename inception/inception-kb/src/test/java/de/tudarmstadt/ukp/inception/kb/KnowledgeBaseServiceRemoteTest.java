/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.kb;

import static de.tudarmstadt.ukp.inception.kb.http.PerThreadSslCheckingHttpClientUtils.restoreSslVerification;
import static de.tudarmstadt.ukp.inception.kb.http.PerThreadSslCheckingHttpClientUtils.suspendSslVerification;
import static de.tudarmstadt.ukp.inception.kb.util.TestFixtures.assumeEndpointIsAvailable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseProperties;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBasePropertiesImpl;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.util.TestFixtures;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseProfile;

@Tag("slow")
@Transactional
@DataJpaTest(excludeAutoConfiguration = LiquibaseAutoConfiguration.class)
public class KnowledgeBaseServiceRemoteTest
{

    private KnowledgeBaseServiceImpl sut;

    @TempDir
    File repoPath;

    @Autowired
    private TestEntityManager testEntityManager;

    @BeforeAll
    public static void setUpOnce() throws Exception
    {
        System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
    }

    @BeforeEach
    public void testWatcher(TestInfo aTestInfo)
    {
        String methodName = aTestInfo.getTestMethod().map(Method::getName).orElse("<unknown>");
        System.out.printf("\n=== %s === %s =====================\n", methodName,
                aTestInfo.getDisplayName());

        suspendSslVerification();
    }

    public void setUp(TestConfiguration aSutConfig) throws Exception
    {
        assumeTrue(
                aSutConfig.getKnowledgeBase().getType() != RepositoryType.REMOTE
                        || TestFixtures.isReachable(aSutConfig.getDataUrl()),
                "Remote repository at [" + aSutConfig.getDataUrl() + "] is not reachable");

        KnowledgeBase kb = aSutConfig.getKnowledgeBase();

        RepositoryProperties repoProps = new RepositoryProperties();
        repoProps.setPath(repoPath);
        KnowledgeBaseProperties kbProperties = new KnowledgeBasePropertiesImpl();
        EntityManager entityManager = testEntityManager.getEntityManager();
        TestFixtures testFixtures = new TestFixtures(testEntityManager);
        sut = new KnowledgeBaseServiceImpl(repoProps, kbProperties, entityManager);
        String PROJECT_NAME = "Test project";
        Project project = testFixtures.createProject(PROJECT_NAME);
        kb.setProject(project);
        if (kb.getType() == RepositoryType.LOCAL) {
            sut.registerKnowledgeBase(kb, sut.getNativeConfig());
            sut.updateKnowledgeBase(kb, sut.getKnowledgeBaseConfig(kb));
            importKnowledgeBase(aSutConfig.getKnowledgeBase(), aSutConfig.getDataUrl());
        }
        else if (kb.getType() == RepositoryType.REMOTE) {
            assumeEndpointIsAvailable(aSutConfig.getDataUrl());
            sut.registerKnowledgeBase(kb, sut.getRemoteConfig(aSutConfig.getDataUrl()));
            sut.updateKnowledgeBase(kb, sut.getKnowledgeBaseConfig(kb));
        }
        else {
            throw new IllegalStateException(
                    "Unknown type: " + aSutConfig.getKnowledgeBase().getType());
        }
    }

    @AfterEach
    public void tearDown() throws Exception
    {
        if (testEntityManager != null) {
            testEntityManager.clear();
        }

        if (sut != null) {
            sut.destroy();
        }

        restoreSslVerification();
    }

    @ParameterizedTest(name = "{index}: KB = {0}")
    @MethodSource("data")
    public void thatRootConceptsCanBeRetrieved(TestConfiguration aSutConfig) throws Exception
    {
        setUp(aSutConfig);

        KnowledgeBase kb = aSutConfig.getKnowledgeBase();

        long duration = System.currentTimeMillis();
        List<KBHandle> rootConceptKBHandle = sut.listRootConcepts(kb, true);
        duration = System.currentTimeMillis() - duration;

        System.out.printf("Root concepts retrieved : %d%n", rootConceptKBHandle.size());
        System.out.printf("Time required           : %d ms%n", duration);
        rootConceptKBHandle.stream().limit(10).forEach(h -> System.out.printf("   %s%n", h));

        assertThat(rootConceptKBHandle).as("Check that root concept list is not empty")
                .isNotEmpty();
        for (String expectedRoot : aSutConfig.getRootIdentifier()) {
            assertThat(rootConceptKBHandle.stream().map(KBHandle::getIdentifier))
                    .as("Check that root concept is retrieved").contains(expectedRoot);
        }
    }

    @ParameterizedTest(name = "{index}: KB = {0}")
    @MethodSource("data")
    public void thatPropertyListCanBeRetrieved(TestConfiguration aSutConfig) throws Exception
    {
        setUp(aSutConfig);

        KnowledgeBase kb = aSutConfig.getKnowledgeBase();

        long duration = System.currentTimeMillis();
        List<KBProperty> propertiesKBHandle = sut.listProperties(kb, true);
        duration = System.currentTimeMillis() - duration;

        System.out.printf("Properties retrieved : %d%n", propertiesKBHandle.size());
        System.out.printf("Time required        : %d ms%n", duration);
        propertiesKBHandle.stream().limit(10).forEach(h -> System.out.printf("   %s%n", h));

        assertThat(propertiesKBHandle).as("Check that property list is not empty").isNotEmpty();
    }

    @ParameterizedTest(name = "{index}: KB = {0}")
    @MethodSource("data")
    public void thatParentListCanBeRetireved(TestConfiguration aSutConfig) throws Exception
    {
        setUp(aSutConfig);

        KnowledgeBase kb = aSutConfig.getKnowledgeBase();

        long duration = System.currentTimeMillis();
        List<KBHandle> parentList = sut.getParentConceptList(kb, aSutConfig.getTestIdentifier(),
                true);
        duration = System.currentTimeMillis() - duration;

        System.out.printf("Parents for          : %s%n", aSutConfig.getTestIdentifier());
        System.out.printf("Parents retrieved    : %d%n", parentList.size());
        System.out.printf("Time required        : %d ms%n", duration);
        parentList.stream().limit(10).forEach(h -> System.out.printf("   %s%n", h));

        assertThat(parentList).as("Check that parent list is not empty").isNotEmpty();
    }

    public static List<TestConfiguration> data() throws Exception
    {
        Map<String, KnowledgeBaseProfile> PROFILES = KnowledgeBaseProfile
                .readKnowledgeBaseProfiles();
        int maxResults = 1000;

        Set<String> rootConcepts;
        Map<String, String> parentChildConcepts;
        List<TestConfiguration> kbList = new ArrayList<>();

        {
            KnowledgeBaseProfile profile = PROFILES.get("wine_ontology");
            KnowledgeBase kb_wine = new KnowledgeBase();
            kb_wine.setName("Wine ontology (OWL)");
            kb_wine.setType(profile.getType());
            kb_wine.setReification(profile.getReification());
            kb_wine.setFullTextSearchIri(profile.getAccess().getFullTextSearchIri());
            kb_wine.applyMapping(profile.getMapping());
            kb_wine.setDefaultLanguage(profile.getDefaultLanguage());
            kb_wine.setMaxResults(maxResults);
            rootConcepts = new HashSet<String>();
            rootConcepts.add("http://www.w3.org/TR/2003/PR-owl-guide-20031209/food#Grape");
            parentChildConcepts = new HashMap<String, String>();
            parentChildConcepts.put("http://www.w3.org/TR/2003/PR-owl-guide-20031209/food#Grape",
                    "http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#WineGrape");
            kbList.add(new TestConfiguration("data/wine-ontology.rdf", kb_wine,
                    "http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#ChateauMargaux",
                    rootConcepts, parentChildConcepts));
        }

        // {
        // ValueFactory vf = SimpleValueFactory.getInstance();
        // KnowledgeBase kb_hucit = new KnowledgeBase();
        // kb_hucit.setName("Hucit");
        // kb_hucit.setType(profile.getType());
        // kb_hucit.setReification(Reification.NONE);
        // kb_hucit.setBasePrefix("http://www.ukp.informatik.tu-darmstadt.de/inception/1.0#");
        // kb_hucit.setClassIri(vf.createIRI("http://www.w3.org/2002/07/owl#Class"));
        // kb_hucit.setSubclassIri(
        // vf.createIRI("http://www.w3.org/2000/01/rdf-schema#subClassOf"));
        // kb_hucit.setTypeIri(vf.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
        // kb_hucit.setDescriptionIri(
        // vf.createIRI("http://www.w3.org/2000/01/rdf-schema#comment"));
        // kb_hucit.setLabelIri(vf.createIRI("http://www.w3.org/2000/01/rdf-schema#label"));
        // kb_hucit.setPropertyTypeIri(
        // vf.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#Property"));
        // kb_hucit.setPropertyLabelIri(RDFS.LABEL);
        // kb_hucit.setPropertyDescriptionIri(RDFS.COMMENT);
        // kb_hucit.setDefaultLanguage("en");
        // kb_hucit.setMaxResults(maxResults);
        // rootConcepts = new HashSet<String>();
        // rootConcepts.add("http://www.w3.org/2000/01/rdf-schema#Class");
        // parentChildConcepts = new HashMap<String, String>();
        // parentChildConcepts.put("http://www.w3.org/2000/01/rdf-schema#Class",
        // "http://www.w3.org/2002/07/owl#Class");
        // kbList.add(new TestConfiguration("http://nlp.dainst.org:8888/sparql", kb_hucit,
        // // person -> Achilles :: urn:cts:cwkb:1137
        // "http://purl.org/hucit/kb/authors/1137", rootConcepts, parentChildConcepts));
        // }

        {
            KnowledgeBaseProfile profile = PROFILES.get("wikidata");
            KnowledgeBase kb_wikidata_direct = new KnowledgeBase();
            kb_wikidata_direct.setName("Wikidata (official/direct mapping)");
            kb_wikidata_direct.setType(profile.getType());
            kb_wikidata_direct.setReification(profile.getReification());
            kb_wikidata_direct.setFullTextSearchIri(profile.getAccess().getFullTextSearchIri());
            kb_wikidata_direct.applyMapping(profile.getMapping());
            kb_wikidata_direct.applyRootConcepts(profile);
            kb_wikidata_direct.setDefaultLanguage(profile.getDefaultLanguage());
            kb_wikidata_direct.setMaxResults(maxResults);
            rootConcepts = new HashSet<String>();
            rootConcepts.add("http://www.wikidata.org/entity/Q35120");
            parentChildConcepts = new HashMap<String, String>();
            parentChildConcepts.put("http://www.wikidata.org/entity/Q35120",
                    "http://www.wikidata.org/entity/Q24229398");
            kbList.add(new TestConfiguration(profile.getAccess().getAccessUrl(), kb_wikidata_direct,
                    "http://www.wikidata.org/entity/Q5", rootConcepts, parentChildConcepts));
        }

        // {
        // KnowledgeBaseProfile profile = PROFILES.get("virtuoso");
        // KnowledgeBase kb_wikidata_direct = new KnowledgeBase();
        // kb_wikidata_direct.setName("UKP_Wikidata (Virtuoso)");
        // kb_wikidata_direct.setType(profile.getType());
        // kb_wikidata_direct.setReification(profiles.getReification());
        // kb_wikidata_direct.applyMapping(profile.getMapping());
        // kb_wikidata_direct.setDefaultLanguage(profiles.getDefaultLanguage);
        // rootConcepts = new HashSet<String>();
        // rootConcepts.add("http://www.wikidata.org/entity/Q2419");
        // kbList.add(new TestConfiguration(profile.getAccess().getAccessUrl(), kb_wikidata_direct,
        // "http://www.wikidata.org/entity/Q19576436", rootConcepts));
        // }

        {
            KnowledgeBaseProfile profile = PROFILES.get("db_pedia");
            KnowledgeBase kb_dbpedia = new KnowledgeBase();
            kb_dbpedia.setName(profile.getName());
            kb_dbpedia.setType(profile.getType());
            kb_dbpedia.setReification(profile.getReification());
            kb_dbpedia.setFullTextSearchIri(profile.getAccess().getFullTextSearchIri());
            kb_dbpedia.applyMapping(profile.getMapping());
            kb_dbpedia.applyRootConcepts(profile);
            kb_dbpedia.setDefaultLanguage(profile.getDefaultLanguage());
            kb_dbpedia.setMaxResults(maxResults);
            kb_dbpedia.setDefaultDatasetIri(profile.getDefaultDataset());
            rootConcepts = new HashSet<String>();
            rootConcepts.add("http://www.w3.org/2002/07/owl#Thing");
            parentChildConcepts = new HashMap<String, String>();
            parentChildConcepts.put("http://www.w3.org/2002/07/owl#Thing",
                    "http://dbpedia.org/ontology/Biomolecule");
            kbList.add(new TestConfiguration(profile.getAccess().getAccessUrl(), kb_dbpedia,
                    "http://dbpedia.org/ontology/Organisation", rootConcepts, parentChildConcepts));
        }

        {
            KnowledgeBaseProfile profile = PROFILES.get("yago");
            KnowledgeBase kb_yago = new KnowledgeBase();
            kb_yago.setName(profile.getName());
            kb_yago.setType(profile.getType());
            kb_yago.setReification(profile.getReification());
            kb_yago.setFullTextSearchIri(profile.getAccess().getFullTextSearchIri());
            kb_yago.applyMapping(profile.getMapping());
            kb_yago.applyRootConcepts(profile);
            kb_yago.setDefaultLanguage(profile.getDefaultLanguage());
            kb_yago.setMaxResults(maxResults);
            rootConcepts = new HashSet<String>();
            rootConcepts.add("http://schema.org/Thing");
            parentChildConcepts = new HashMap<String, String>();
            parentChildConcepts.put("http://schema.org/Thing",
                    "http://yago-knowledge.org/resource/wikicat_Alleged_UFO-related_entities");
            kbList.add(new TestConfiguration(profile.getAccess().getAccessUrl(), kb_yago,
                    "http://yago-knowledge.org/resource/Elvis_Presley", rootConcepts,
                    parentChildConcepts));
        }

        {
            KnowledgeBaseProfile profile = PROFILES.get("zbw-stw-economics");
            KnowledgeBase kb_zbw_stw_economics = new KnowledgeBase();
            kb_zbw_stw_economics.setName(profile.getName());
            kb_zbw_stw_economics.setType(profile.getType());
            kb_zbw_stw_economics.setReification(profile.getReification());
            kb_zbw_stw_economics.setFullTextSearchIri(profile.getAccess().getFullTextSearchIri());
            kb_zbw_stw_economics.applyMapping(profile.getMapping());
            kb_zbw_stw_economics.applyRootConcepts(profile);
            kb_zbw_stw_economics.setDefaultLanguage(profile.getDefaultLanguage());
            kb_zbw_stw_economics.setMaxResults(maxResults);
            rootConcepts = new HashSet<String>();
            rootConcepts.add("http://zbw.eu/stw/thsys/a");
            parentChildConcepts = new HashMap<String, String>();
            parentChildConcepts.put("http://zbw.eu/stw/thsys/a", "http://zbw.eu/stw/thsys/70582");
            kbList.add(
                    new TestConfiguration(profile.getAccess().getAccessUrl(), kb_zbw_stw_economics,
                            "http://zbw.eu/stw/thsys/71020", rootConcepts, parentChildConcepts));
        }

        // Commenting this out for the moment because we expect that every ontology contains
        // property definitions. However, this one does not include any property definitions!
        // {
        // KnowledgeBaseProfile profile = PROFILES.get("zbw-gnd");
        // KnowledgeBase kb_zbw_gnd = new KnowledgeBase();
        // kb_zbw_gnd.setName(profile.getName());
        // kb_zbw_gnd.setType(profile.getType());
        // kb_zbw_gnd.setReification(profile.getReification());
        // kb_zbw_gnd.applyMapping(profile.getMapping());
        // kb_zbw_gnd.setDefaultLanguage(profile.getDefaultLanguage());
        // kbList.add(new TestConfiguration(profile.getSparqlUrl(), kb_zbw_gnd));
        // }

        return kbList;
    }

    // Helper

    private void importKnowledgeBase(KnowledgeBase aKnowledgeBase, String resourceName)
        throws Exception
    {
        ClassLoader classLoader = KnowledgeBaseServiceRemoteTest.class.getClassLoader();
        String fileName = classLoader.getResource(resourceName).getFile();
        try (InputStream is = classLoader.getResourceAsStream(resourceName)) {
            sut.importData(aKnowledgeBase, fileName, is);
        }
    }

    public static KnowledgeBase setOWLSchemaMapping(KnowledgeBase kb)
    {
        kb.setClassIri(OWL.CLASS.stringValue());
        kb.setSubclassIri(RDFS.SUBCLASSOF.stringValue());
        kb.setTypeIri(RDF.TYPE.stringValue());
        kb.setDescriptionIri(RDFS.COMMENT.stringValue());
        kb.setLabelIri(RDFS.LABEL.stringValue());
        kb.setPropertyTypeIri(RDF.PROPERTY.stringValue());
        return kb;
    }

    private static class TestConfiguration
    {
        private final String url;
        private final KnowledgeBase kb;
        private final String testIdentifier;
        private final Set<String> rootIdentifier;
        private final Map<String, String> parentChildIdentifier;

        public TestConfiguration(String aUrl, KnowledgeBase aKb, String atestIdentifier,
                Set<String> aRootIdentifier, Map<String, String> aParentChildIdentifier)
        {
            super();
            url = aUrl;
            kb = aKb;
            testIdentifier = atestIdentifier;
            rootIdentifier = aRootIdentifier;
            parentChildIdentifier = aParentChildIdentifier;
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

        public Set<String> getRootIdentifier()
        {
            return rootIdentifier;
        }

        @SuppressWarnings("unused")
        public Map<String, String> getParentChildIdentifier()
        {
            return parentChildIdentifier;
        }

        @Override
        public String toString()
        {
            return kb.getName();
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = { "de.tudarmstadt.ukp.inception.kb.model",
            "de.tudarmstadt.ukp.clarin.webanno.model" })
    public static class SpringConfig
    {
        // No content
    }
}
