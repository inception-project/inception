package de.tudarmstadt.ukp.inception.kb;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.dao.RepositoryProperties;
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
public class KnowledgeBaseSubPropertyLabelTest
{
    private static final String PROJECT_NAME = "Test project";
    private static final String KB_NAME = "GND";

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
    public void setUp() throws Exception {
        RepositoryProperties repoProps = new RepositoryProperties();
        repoProps.setPath(temporaryFolder.getRoot());
        EntityManager entityManager = testEntityManager.getEntityManager();
        testFixtures = new TestFixtures(testEntityManager);
        sut = new KnowledgeBaseServiceImpl(repoProps, entityManager);
        project = createProject(PROJECT_NAME);
        kb = buildKnowledgeBase(project, KB_NAME);
        String gndAccessURL = PROFILES.get("zbw-gnd").getAccess().getAccessUrl();
        testFixtures.assumeEndpointIsAvailable(gndAccessURL, 5000);
        sut.registerKnowledgeBase(kb, sut.getRemoteConfig(gndAccessURL));

    }

    @After
    public void tearDown() throws Exception {
        testEntityManager.clear();
        sut.destroy();
    }
    
    @Test
    public void thatChildConceptsLabel()
    {
        
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
    
    
    //Helper
    
    private Project createProject(String name) {
        return testFixtures.createProject(name);
    }

    private KnowledgeBase buildKnowledgeBase(Project project, String name) throws IOException {
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
