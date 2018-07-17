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
public class KnowledgeBaseServiceTest
{
    private static final String PROJECT_NAME = "Test project";
    
    private static KnowledgeBaseServiceImpl sut;
    private static Project project;
    private static TestFixtures testFixtures;

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

    public KnowledgeBaseServiceTest(KnowledgeBase akb, Reification aReification) throws Exception
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

    public static List<KnowledgeBase> addKBProfileSetup() throws Exception
    {
        List<KnowledgeBase> kbList = new ArrayList<KnowledgeBase>();

        // Configuration for Local Ontology
        String kbProfileName = "Wine";
        kb = buildDefaultKnowledgeBase(kbProfileName);
        kb.setType(RepositoryType.LOCAL);
        // sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        setSchema(kb, OWL.CLASS, RDFS.SUBCLASSOF, RDF.TYPE, RDFS.COMMENT, RDFS.LABEL, RDF.PROPERTY);
        kbList.add(kb);

        // Configurations for Remote KB
        kbProfileMap = readKnowledgeBaseProfiles();

        // Configuration for Wikidata official
        kbProfileName = "wikidata";
        kb = buildDefaultKnowledgeBase(kbProfileName);
        kb.setType(RepositoryType.REMOTE);
        KnowledgeBaseMapping mapping = kbProfileMap.get(kbProfileName).getMapping();
        setSchema(kb, mapping);
        kbList.add(kb);

        // Configuration for Wikidata UKP Virtuoso
        kbProfileName = "virtuoso";
        kb = buildDefaultKnowledgeBase(kbProfileName);
        kb.setType(RepositoryType.REMOTE);
        mapping = kbProfileMap.get(kbProfileName).getMapping();
        setSchema(kb, mapping);
        kbList.add(kb);

        // Configuration for Babbel net
        // kbProfileName = "babel_net";
        // kbName = String.join("_", ROOT_KB_NAME, kbProfileName);
        // kb = buildDefaultKnowledgeBase(project, kbName);
        // kb.setType(RepositoryType.REMOTE);
        // mapping = kbProfileMap.get(kbProfileName).getMapping();
        // setSchema(kb, mapping);
        // kbList.add(kb);

        // Configuration for DBPedia
        kbProfileName = "db_pedia";
        kb = buildDefaultKnowledgeBase(kbProfileName);
        kb.setType(RepositoryType.REMOTE);
        mapping = kbProfileMap.get(kbProfileName).getMapping();
        setSchema(kb, mapping);
        kbList.add(kb);

        // Configuration for Yago
        kbProfileName = "yago";
        kb = buildDefaultKnowledgeBase(kbProfileName);
        kb.setType(RepositoryType.REMOTE);
        mapping = kbProfileMap.get(kbProfileName).getMapping();
        setSchema(kb, mapping);
        kbList.add(kb);

        return kbList;
    }

    @Test
    public void testRootConcept()
    {
        kb.setReification(reification);
        List<KBHandle> rootConceptKBHandle = sut.listRootConcepts(kb, true);
        assertThat(rootConceptKBHandle).as("Check that root concept list is not empty")
                .isNotEmpty();
    }

    @Test
    public void testPropertyList()
    {
        kb.setReification(reification);
        List<KBHandle> propertiesKBHandle = sut.listProperties(kb, kb.getPropertyTypeIri(), true,
                true);
        assertThat(propertiesKBHandle).as("Check that list is not empty").isNotEmpty();
    }

    // Helper
    public static void setUpKB(KnowledgeBase kb)
    {
        sut.updateKnowledgeBase(kb, sut.getKnowledgeBaseConfig(kb));
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
    }

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

    private static void importKnowledgeBase(String resourceName) throws Exception
    {
        ClassLoader classLoader = KnowledgeBaseServiceTest.class.getClassLoader();
        String fileName = classLoader.getResource(resourceName).getFile();
        try (InputStream is = classLoader.getResourceAsStream(resourceName)) {
            sut.importData(kb, fileName, is);
        }
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
        kb.setClassIri(mapping.getClassIri());
        kb.setSubclassIri(mapping.getSubclassIri());
        kb.setTypeIri(mapping.getTypeIri());
        kb.setDescriptionIri(mapping.getDescriptionIri());
        kb.setLabelIri(mapping.getLabelIri());
        kb.setPropertyTypeIri(mapping.getPropertyTypeIri());
        kb.setReification(reification);
    }

    public static Map<String, KnowledgeBaseProfile> readKnowledgeBaseProfiles() throws IOException
    {
        try (Reader r = new InputStreamReader(
                KnowledgeBaseServiceTest.class.getResourceAsStream("knowledgebase-profiles.yaml"),
                StandardCharsets.UTF_8)) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            return mapper.readValue(r, new TypeReference<HashMap<String, KnowledgeBaseProfile>>()
            {
            });
        }
    }

}
