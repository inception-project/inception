package de.tudarmstadt.ukp.inception.kb;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.transaction.annotation.Transactional;

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
    private static String ROOT_KB_NAME = "TestKB";

    private static KnowledgeBaseServiceImpl sut;
    private static Project project;
    private static KnowledgeBase kb;
    private TestFixtures testFixtures;
    private static Reification reification;

    private final Logger log = LoggerFactory.getLogger(getClass());

    
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Autowired
    private TestEntityManager testEntityManager;

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @BeforeClass
    public static void setUpOnce()
    {
        System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
    }

    public KnowledgeBaseServiceTest(Reification aReification)
    {
        reification = aReification;
    }

    @Parameterized.Parameters(name = "Reification = {0}")
    public static Collection<Object[]> data()
    {
        return Arrays.stream(Reification.values()).map(r -> new Object[] { r })
                .collect(Collectors.toList());
    }
    
    
    @Parameterized.Parameters(name = "Reification = {0}")
    public static Collection<Object[]> dataKBSettings()
    {
        
        String kbProfile = "wikidata";
        String kbName = String.join("_", ROOT_KB_NAME, "OWL");
        kb = buildDefaultKnowledgeBase(project, kbName);
        kb.setType(RepositoryType.LOCAL);
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        if (kb.getType() == RepositoryType.LOCAL) {
            setSchema(kb, OWL.CLASS, RDFS.SUBCLASSOF, RDF.TYPE, RDFS.COMMENT, RDFS.LABEL, RDF.PROPERTY);
            importKnowledgeBase("data/wine-ontology.rdf");
        }
        else {
            
        }
       
        List<KBHandle> propertiesKBHandle = sut.listProperties(kb, RDF.PROPERTY, true, true);
        List<KBHandle> rootConceptKBHandle = sut.listRootConcepts(kb, true);
        log.debug(
                "\nSize of List Concept " + kbName + "::::::::" + +rootConceptKBHandle.size());
        log.debug("Size of List Properties " + kbName + "::::::::"
                + propertiesKBHandle.size() + "\n \n");
        assertThat(rootConceptKBHandle).as("Check that root concept list is not empty")
                .isNotEmpty();
        assertThat(propertiesKBHandle).as("Check that list is not empty").isNotEmpty();

    
        
        return Arrays.stream(Reification.values()).map(r -> new Object[] { r })
                .collect(Collectors.toList());
    }
    
    
    
    
    
//    @Parameterized.Parameters(name = "Reification = {0}")
//    public static Collection<Object[]> data()
//    {
//        return Arrays.stream(Reification.values()).map(r -> new Object[] { r })
//                .collect(Collectors.toList());
//    }
    

    @Before
    public void setUp() throws Exception
    {
        EntityManager entityManager = testEntityManager.getEntityManager();
        testFixtures = new TestFixtures(testEntityManager);
        sut = new KnowledgeBaseServiceImpl(temporaryFolder.getRoot(), entityManager);
        project = testFixtures.createProject(PROJECT_NAME);
    }

    @After
    public void tearDown() throws Exception
    {
        testEntityManager.clear();
        sut.destroy();
    }

    @Test
    public void testKnowledgeBase_WithLocalOWL() throws Exception
    {
        String kbName = String.join("_", ROOT_KB_NAME, "OWL");
        kb = buildDefaultKnowledgeBase(project, kbName);
        kb.setType(RepositoryType.LOCAL);
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        setSchema(kb, OWL.CLASS, RDFS.SUBCLASSOF, RDF.TYPE, RDFS.COMMENT, RDFS.LABEL, RDF.PROPERTY);
        importKnowledgeBase("data/wine-ontology.rdf");
        List<KBHandle> propertiesKBHandle = sut.listProperties(kb, RDF.PROPERTY, true, true);
        List<KBHandle> rootConceptKBHandle = sut.listRootConcepts(kb, true);
        log.debug(
                "\nSize of List Concept " + kbName + "::::::::" + +rootConceptKBHandle.size());
        log.debug("Size of List Properties " + kbName + "::::::::"
                + propertiesKBHandle.size() + "\n \n");
        assertThat(rootConceptKBHandle).as("Check that root concept list is not empty")
                .isNotEmpty();
        assertThat(propertiesKBHandle).as("Check that list is not empty").isNotEmpty();

    }

    @Test
    public void testKnowledgeBase_WithWikidata() throws IOException
    {
        String kbProfile = "wikidata";
        String kbName = String.join("_", ROOT_KB_NAME, kbProfile);
        kb = buildDefaultKnowledgeBase(project, kbName);
        kb.setType(RepositoryType.REMOTE);
        KnowledgeBaseProfile profile = sut.readKnowledgeBaseProfiles().get(kbProfile);
        KnowledgeBaseMapping mapping = profile.getMapping();
        sut.registerKnowledgeBase(kb, sut.getRemoteConfig(profile.getSparqlUrl()));
        setSchema(kb, mapping);
        List<KBHandle> propertiesKBHandle = sut.listProperties(kb, mapping.getPropertyTypeIri(),
                true, true);
        List<KBHandle> rootConceptKBHandle = sut.listRootConcepts(kb, true);
        log.debug(
                "\nSize of List Concept " + kbName + "::::::::" + +rootConceptKBHandle.size());
        log.debug("Size of List Properties " + kbName + "::::::::"
                + +propertiesKBHandle.size() + "\n \n");
        assertThat(rootConceptKBHandle).as("Check that root concept list is not empty")
                .isNotEmpty();
        assertThat(propertiesKBHandle).as("Check that list is not empty").isNotEmpty();

    }

    @Test
    public void testKnowledgeBase_WithUKPVirtuoso() throws IOException
    {
        String kbProfile = "wikidata";
        String kbName = String.join("_", ROOT_KB_NAME, "UKP", kbProfile);
        kb = buildDefaultKnowledgeBase(project, kbName);
        kb.setType(RepositoryType.REMOTE);
        KnowledgeBaseProfile profile = sut.readKnowledgeBaseProfiles().get(kbProfile);
        KnowledgeBaseMapping mapping = profile.getMapping();
        sut.registerKnowledgeBase(kb, sut.getRemoteConfig(
                "http://knowledgebase.ukp.informatik.tu-darmstadt.de:8890/sparql"));
        setSchema(kb, mapping);
        List<KBHandle> propertiesKBHandle = sut.listProperties(kb, mapping.getPropertyTypeIri(),
                true, true);
        List<KBHandle> rootConceptKBHandle = sut.listRootConcepts(kb, true);
        log.debug(
                "\nSize of List Concept " + kbName + "::::::::" + +rootConceptKBHandle.size());
        log.debug("Size of List Properties " + kbName + "::::::::"
                + +propertiesKBHandle.size() + "\n \n");
        assertThat(rootConceptKBHandle).as("Check that root concept list is not empty")
                .isNotEmpty();
        assertThat(propertiesKBHandle).as("Check that list is not empty").isNotEmpty();

    }

    @Ignore
    public void testKnowledgeBase_WithBabbelNet() throws IOException
    {
        String kbProfile = "babel_net";
        String kbName = String.join("_", ROOT_KB_NAME, kbProfile);
        kb = buildDefaultKnowledgeBase(project, kbName);
        kb.setType(RepositoryType.REMOTE);
        KnowledgeBaseProfile profile = sut.readKnowledgeBaseProfiles().get(kbProfile);
        KnowledgeBaseMapping mapping = profile.getMapping();
        sut.registerKnowledgeBase(kb, sut.getRemoteConfig(profile.getSparqlUrl()));
        setSchema(kb, mapping);
        List<KBHandle> propertiesKBHandle = sut.listProperties(kb, mapping.getPropertyTypeIri(),
                true, true);
        List<KBHandle> rootConceptKBHandle = sut.listRootConcepts(kb, true);
        log.debug(
                "\nSize of List Concept " + kbName + "::::::::" + +rootConceptKBHandle.size());
        log.debug("Size of List Properties " + kbName + "::::::::"
                + +propertiesKBHandle.size() + "\n \n");
        assertThat(rootConceptKBHandle).as("Check that root concept list is not empty")
                .isNotEmpty();
        assertThat(propertiesKBHandle).as("Check that list is not empty").isNotEmpty();

    }

    @Test
    public void testKnowledgeBase_WithDBPedia() throws IOException
    {
        String kbProfile = "db_pedia";
        String kbName = String.join("_", ROOT_KB_NAME, kbProfile);
        kb = buildDefaultKnowledgeBase(project, kbName);
        kb.setType(RepositoryType.REMOTE);
        KnowledgeBaseProfile profile = sut.readKnowledgeBaseProfiles().get(kbProfile);
        KnowledgeBaseMapping mapping = profile.getMapping();
        sut.registerKnowledgeBase(kb, sut.getRemoteConfig(profile.getSparqlUrl()));
        setSchema(kb, mapping);
        List<KBHandle> propertiesKBHandle = sut.listProperties(kb, mapping.getPropertyTypeIri(),
                true, true);
        List<KBHandle> rootConceptKBHandle = sut.listRootConcepts(kb, true);
        log.debug(
                "\nSize of List Concept " + kbName + "::::::::" + +rootConceptKBHandle.size());
        log.debug("Size of List Properties " + kbName + "::::::::"
                + +propertiesKBHandle.size() + "\n \n");
        assertThat(rootConceptKBHandle).as("Check that root concept list is not empty")
                .isNotEmpty();
        assertThat(propertiesKBHandle).as("Check that properties list is not empty").isNotEmpty();

    }

    @Test
    public void testKnowledgeBase_WithYago() throws IOException
    {
        String kbProfile = "yago";
        String kbName = String.join("_", ROOT_KB_NAME, kbProfile);
        kb = buildDefaultKnowledgeBase(project, kbName);
        kb.setType(RepositoryType.REMOTE);
        KnowledgeBaseProfile profile = sut.readKnowledgeBaseProfiles().get(kbProfile);
        KnowledgeBaseMapping mapping = profile.getMapping();
        sut.registerKnowledgeBase(kb, sut.getRemoteConfig(profile.getSparqlUrl()));
        setSchema(kb, mapping);
        List<KBHandle> propertiesKBHandle = sut.listProperties(kb, mapping.getPropertyTypeIri(),
                true, true);
        List<KBHandle> rootConceptKBHandle = sut.listRootConcepts(kb, true);
        log.debug(
                "\nSize of List Concept " + kbName + "::::::::" + +rootConceptKBHandle.size());
        log.debug("Size of List Properties " + kbName + "::::::::"
                + +propertiesKBHandle.size() + "\n \n");
        assertThat(rootConceptKBHandle).as("Check that root concept list is not empty")
                .isNotEmpty();
        assertThat(propertiesKBHandle).as("Check that list is not empty").isNotEmpty();

    }

    // Helper

    private static KnowledgeBase buildDefaultKnowledgeBase(Project project, String name)
    {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(name);
        kb.setProject(project);
        kb.setType(RepositoryType.LOCAL);
        kb.setClassIri(RDFS.CLASS);
        kb.setSubclassIri(RDFS.SUBCLASSOF);
        kb.setTypeIri(RDF.TYPE);
        kb.setLabelIri(RDFS.LABEL);
        kb.setPropertyTypeIri(RDF.PROPERTY);
        kb.setDescriptionIri(RDFS.COMMENT);

        kb.setReification(reification);
        return kb;

    }

    private static void importKnowledgeBase(String resourceName) throws Exception
    {
        ClassLoader classLoader = getClass().getClassLoader();
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
        sut.updateKnowledgeBase(kb, sut.getKnowledgeBaseConfig(kb));
    }
    
    private void setSchema(KnowledgeBase kb, KnowledgeBaseMapping mapping)
    {
        kb.setClassIri(mapping.getClassIri());
        kb.setSubclassIri(mapping.getSubclassIri());
        kb.setTypeIri(mapping.getTypeIri());
        kb.setDescriptionIri(mapping.getDescriptionIri());
        kb.setLabelIri(mapping.getLabelIri());
        kb.setPropertyTypeIri(mapping.getPropertyTypeIri());
        kb.setReification(reification);
        sut.updateKnowledgeBase(kb, sut.getKnowledgeBaseConfig(kb));
    }

}
