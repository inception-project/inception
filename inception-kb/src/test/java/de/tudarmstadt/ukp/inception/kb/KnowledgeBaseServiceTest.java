package de.tudarmstadt.ukp.inception.kb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThat;

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
    private String KB_NAME = "TestKB";

    private KnowledgeBaseServiceImpl sut;
    private Project project;
    private KnowledgeBase kb;
    private TestFixtures testFixtures;
    private Reification reification;
    
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Autowired
    private TestEntityManager testEntityManager;

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();
    
    @BeforeClass
    public static void setUpOnce() {
        System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
    }
    
    public KnowledgeBaseServiceTest(Reification aReification) {
        reification = aReification;
    }

    @Parameterized.Parameters(name = "Reification = {0}")
    public static Collection<Object[]> data()
    {
        return Arrays.stream(Reification.values()).map(r -> new Object[] { r })
            .collect(Collectors.toList());
    }
    
    @Before
    public void setUp() throws Exception {
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
    public void testListPropertiesKnowledgeBaseIRI_WithLocalOWL() throws Exception
    {
        kb = buildKnowledgeBase(project, KB_NAME.concat("-" + "OWL"));
        kb.setType(RepositoryType.LOCAL);
        sut.registerKnowledgeBase(kb, sut.getNativeConfig());
        setSchema(kb, OWL.CLASS, RDFS.SUBCLASSOF, RDF.TYPE, RDFS.COMMENT, RDFS.LABEL, RDF.PROPERTY);
        importKnowledgeBase("data/wine-ontology.rdf");
        List<KBHandle> propertiesKBHandle = sut.listProperties(kb,  RDF.PROPERTY, true, true);
        System.out.println("\n \n Size of List Properties OWL::::::::" + propertiesKBHandle.size() + "\n \n");
        assertThat(propertiesKBHandle).as("Check that list is not empty").isNotEmpty();

    }
    
    
    @Test
    public void testListPropertiesKnowledgeBaseIRI_WithWikidata() throws IOException
    {
        String kbProfile = "wikidata";
        KnowledgeBaseProfile  profile = sut.readKnowledgeBaseProfiles().get(kbProfile);
        KnowledgeBaseMapping mapping = profile.getMapping();
        kb = buildKnowledgeBase(project, KB_NAME.concat("-" + kbProfile));
        kb.setType(RepositoryType.REMOTE);
        sut.registerKnowledgeBase(kb, sut.getRemoteConfig(profile.getSparqlUrl()));
        setSchema(kb, mapping.getClassIri(), mapping.getSubclassIri(), mapping.getTypeIri(),mapping.getDescriptionIri(), mapping.getLabelIri(), mapping.getPropertyTypeIri());
        List<KBHandle> propertiesKBHandle = sut.listProperties(kb, mapping.getPropertyTypeIri(), true, true);
        System.out.println("\n \n Size of List Properties Wikidata::::::::" + propertiesKBHandle.size() + "\n \n");
        assertThat(propertiesKBHandle).as("Check that list is not empty").isNotEmpty();

    }

    
    @Test
    public void testListPropertiesKnowledgeBaseIRI_WithUKPVirtuoso() throws IOException
    {
        String kbProfile = "wikidata";
        KnowledgeBaseProfile  profile = sut.readKnowledgeBaseProfiles().get(kbProfile);
        KnowledgeBaseMapping mapping = profile.getMapping();
        kb = buildKnowledgeBase(project, KB_NAME.concat("-UKP-" + kbProfile));
        kb.setType(RepositoryType.REMOTE);
        sut.registerKnowledgeBase(kb, sut.getRemoteConfig("http://knowledgebase.ukp.informatik.tu-darmstadt.de:8890/sparql"));
        setSchema(kb, mapping.getClassIri(), mapping.getSubclassIri(), mapping.getTypeIri(),mapping.getDescriptionIri(), mapping.getLabelIri(), mapping.getPropertyTypeIri());
        List<KBHandle> propertiesKBHandle = sut.listProperties(kb, mapping.getPropertyTypeIri(), true, true);
        System.out.println("\n \n Size of List Properties UKPVirtuoso ::::::::" + propertiesKBHandle.size() + "\n \n");
        assertThat(propertiesKBHandle).as("Check that list is not empty").isNotEmpty();

    }
    

    @Ignore
    public void testListPropertiesKnowledgeBaseIRI_WithBabbel() throws IOException
    {   
        String kbProfile = "babel_net";
        KnowledgeBaseProfile  profile = sut.readKnowledgeBaseProfiles().get(kbProfile);
        KnowledgeBaseMapping mapping = profile.getMapping();
        kb = buildKnowledgeBase(project, KB_NAME.concat("-" + kbProfile));
        kb.setType(RepositoryType.REMOTE);
        sut.registerKnowledgeBase(kb, sut.getRemoteConfig(profile.getSparqlUrl()));
        setSchema(kb, OWL.CLASS, RDFS.SUBCLASSOF, RDF.TYPE, RDFS.COMMENT, RDFS.LABEL, RDF.PROPERTY);
        List<KBHandle> propertiesKBHandle = sut.listProperties(kb, mapping.getPropertyTypeIri(), true, true);
        System.out.println("\n \n Size of List Properties Babbel::::::::" + propertiesKBHandle.size() + "\n \n");
        assertThat(propertiesKBHandle).as("Check that list is not empty").isNotEmpty();

    }
    
    @Test
    public void testListPropertiesKnowledgeBaseIRI_WithDBPedia() throws IOException
    {   
        String kbProfile = "db_pedia";
        KnowledgeBaseProfile  profile = sut.readKnowledgeBaseProfiles().get(kbProfile);
        KnowledgeBaseMapping mapping = profile.getMapping();
        kb = buildKnowledgeBase(project, KB_NAME.concat("-" + kbProfile));
        kb.setType(RepositoryType.REMOTE);
        sut.registerKnowledgeBase(kb, sut.getRemoteConfig(profile.getSparqlUrl()));
        setSchema(kb, OWL.CLASS, RDFS.SUBCLASSOF, RDF.TYPE, RDFS.COMMENT, RDFS.LABEL, RDF.PROPERTY);
        List<KBHandle> propertiesKBHandle = sut.listProperties(kb, mapping.getPropertyTypeIri(), true, true);
        System.out.println("\n \n Size of List Properties DBPedia ::::::::" + propertiesKBHandle.size() + "\n \n");
        assertThat(propertiesKBHandle).as("Check that list is not empty").isNotEmpty();

    }

    @Test
    public void testListPropertiesKnowledgeBaseIRI_WithYago() throws IOException
    {   
        String kbProfile = "yago";
        KnowledgeBaseProfile  profile = sut.readKnowledgeBaseProfiles().get(kbProfile);
        KnowledgeBaseMapping mapping = profile.getMapping();
        kb = buildKnowledgeBase(project, KB_NAME.concat("-" + kbProfile));
        kb.setType(RepositoryType.REMOTE);
        sut.registerKnowledgeBase(kb, sut.getRemoteConfig(profile.getSparqlUrl()));
        setSchema(kb, OWL.CLASS, RDFS.SUBCLASSOF, RDF.TYPE, RDFS.COMMENT, RDFS.LABEL, RDF.PROPERTY);
        List<KBHandle> propertiesKBHandle = sut.listProperties(kb, mapping.getPropertyTypeIri(), true, true);
        System.out.println("\n \n Size of List Properties Yago ::::::::"
                + propertiesKBHandle.size() + "\n \n");
        assertThat(propertiesKBHandle).as("Check that list is not empty").isNotEmpty();

    }

    // Helper
    
    private KnowledgeBase buildKnowledgeBase(Project project, String name) {
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

    private void importKnowledgeBase(String resourceName) throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        String fileName = classLoader.getResource(resourceName).getFile();
        try (InputStream is = classLoader.getResourceAsStream(resourceName)) {
            sut.importData(kb, fileName, is);
        }
    }
    
    private void setSchema(KnowledgeBase kb, IRI classIri, IRI subclassIri, IRI typeIri,
        IRI descriptionIri, IRI labelIri, IRI propertyTypeIri) {
        kb.setClassIri(classIri);
        kb.setSubclassIri(subclassIri);
        kb.setTypeIri(typeIri);
        kb.setDescriptionIri(descriptionIri);
        kb.setLabelIri(labelIri);
        kb.setPropertyTypeIri(propertyTypeIri);
        kb.setReification(reification);
        sut.updateKnowledgeBase(kb, sut.getKnowledgeBaseConfig(kb));
    }
    
    
}
