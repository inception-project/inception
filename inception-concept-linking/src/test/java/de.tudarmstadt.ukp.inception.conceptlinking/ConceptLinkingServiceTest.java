package de.tudarmstadt.ukp.inception.conceptlinking;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.conceptlinking.config.EntityLinkingProperties;
import de.tudarmstadt.ukp.inception.conceptlinking.service.ConceptLinkingService;
import de.tudarmstadt.ukp.inception.conceptlinking.util.TestFixtures;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseServiceImpl;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.reification.Reification;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SpringConfig.class)
@Transactional
@DataJpaTest
public class ConceptLinkingServiceTest
{
    private static final String PROJECT_NAME = "Test project";
    private static final String KB_NAME = "Test knowledge base";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Autowired
    private TestEntityManager testEntityManager;
    @Autowired
    private KnowledgeBaseService kbService;

    private ConceptLinkingService clService;

    private KnowledgeBase kb;

    @Before
    public void setUp()
    {
        EntityManager entityManager = testEntityManager.getEntityManager();
        TestFixtures testFixtures = new TestFixtures(testEntityManager);
        kbService = new KnowledgeBaseServiceImpl(temporaryFolder.getRoot(), entityManager);
        clService = new ConceptLinkingService(kbService, new EntityLinkingProperties());
        Project project = testFixtures.createProject(PROJECT_NAME);
        kb = testFixtures.buildKnowledgeBase(project, KB_NAME, Reification.NONE);
    }

    @Test
    public void thatLuceneSailIndexedConceptIsRetrievableWithFullTextSearch() throws Exception
    {
        kbService.registerKnowledgeBase(kb, kbService.getNativeConfig());
        importKnowledgeBase("data/pets.ttl");
        kbService.indexLocalKb(kb);

        List<KBHandle> handles = clService.disambiguate(kb, null, "soc", 0, null);

        assertThat(handles.stream().map(KBHandle::getName))
            .as("Check whether \"Socke\" has been retrieved.")
            .contains("Socke");
    }

    @Test
    public void thatAddedLuceneSailIndexedConceptIsRetrievableWithFullTextSearch() throws Exception
    {
        kbService.registerKnowledgeBase(kb, kbService.getNativeConfig());
        importKnowledgeBase("data/pets.ttl");
        kbService.indexLocalKb(kb);

        kbService.createConcept(kb, new KBConcept("manatee"));
        List<KBHandle> handles = clService.disambiguate(kb, null, "man", 0, null);

        assertThat(handles.stream().map(KBHandle::getName))
            .as("Check whether \"manatee\" has been retrieved.")
            .contains("manatee");
    }

    private void importKnowledgeBase(String resourceName) throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        String fileName = classLoader.getResource(resourceName).getFile();
        try (InputStream is = classLoader.getResourceAsStream(resourceName)) {
            kbService.importData(kb, fileName, is);
        }
    }
}
