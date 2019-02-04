package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.gazeteer;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PROJECT_TYPE_ANNOTATION;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.dao.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.model.Gazeteer;

@RunWith(SpringRunner.class) 
@DataJpaTest
@Transactional
@EntityScan(
        basePackages = {
            "de.tudarmstadt.ukp.inception",
            "de.tudarmstadt.ukp.clarin.webanno.model"
})
public class GazeteerServiceImplTest
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Autowired
    private TestEntityManager testEntityManager;

    private GazeteerServiceImpl sut;

    private Project project;
    private AnnotationLayer spanLayer;
    private AnnotationFeature spanFeat1;
    private Recommender rec1;
    
    @Before
    public void setup()
    {
        EntityManager em = testEntityManager.getEntityManager();
        
        RepositoryProperties repoProps = new RepositoryProperties();
        repoProps.setPath(temporaryFolder.getRoot());
        
        sut = new GazeteerServiceImpl(repoProps, em);
        
        project = new Project();
        project.setName("test");
        project.setMode(PROJECT_TYPE_ANNOTATION);
        em.persist(project);
        
        spanLayer = new AnnotationLayer("span", "span", SPAN_TYPE, project, false, TOKENS);
        em.persist(spanLayer);

        spanFeat1 = new AnnotationFeature(project, spanLayer, "feat1", "feat1", TYPE_NAME_STRING);
        em.persist(spanFeat1);
        
        rec1 = new Recommender("rec1", spanLayer);
        rec1.setFeature(spanFeat1);
        em.persist(rec1);
    }

    @Test
    public void thatCreateListAndDeleteGazeteerWorks() throws Exception
    {
        // Add first gazeteer
        Gazeteer gaz1 = new Gazeteer("gaz1", rec1);
        sut.createOrUpdateGazeteer(gaz1);

        assertThat(sut.listGazeteers(rec1)).containsExactly(gaz1);

        // Add second gazeteer
        Gazeteer gaz2 = new Gazeteer("gaz2", rec1);
        sut.createOrUpdateGazeteer(gaz2);

        assertThat(gaz1.getId()).isNotNull();
        assertThat(gaz2.getId()).isNotNull();
        assertThat(sut.listGazeteers(rec1)).containsExactly(gaz1, gaz2);
        
        // Remove first gazeteer
        sut.deleteGazeteers(gaz1);
        
        assertThat(sut.listGazeteers(rec1)).containsExactly(gaz2);
    }
    
    @Test
    public void thatUpdatingGazeteerWorks() throws Exception
    {
        Gazeteer gaz = new Gazeteer("foo", rec1);
        sut.createOrUpdateGazeteer(gaz);

        assertThat(sut.listGazeteers(rec1)).extracting(Gazeteer::getName).containsExactly("foo");
        
        gaz.setName("bar");
        sut.createOrUpdateGazeteer(gaz);
        
        assertThat(sut.listGazeteers(rec1)).extracting(Gazeteer::getName).containsExactly("bar");
    }
    
    @Test
    public void thatImportGazeteerWorks() throws Exception
    {
        Gazeteer gaz = new Gazeteer("gaz", rec1);
        sut.createOrUpdateGazeteer(gaz);
        
        File input = new File("src/test/resources/gazeteers/gaz1.txt");
        
        // Check that import works
        try (InputStream is = new FileInputStream(input)) {
            sut.importGazeteerFile(gaz, is);
        }
        
        File gazFile = sut.getGazeteerFile(gaz);
        assertThat(gazFile.exists()).isTrue();
        assertThat(contentOf(sut.getGazeteerFile(gaz)))
                .isEqualToNormalizingNewlines(contentOf(input));
        
        // Check that gazeteer file has been deleted along with the entity
        sut.deleteGazeteers(gaz);
        
        assertThat(gazFile.exists()).isFalse();
    }
    
    @After
    public void tearDown() throws Exception
    {
        testEntityManager.clear();
    }
    
    @SpringBootConfiguration
    @EnableAutoConfiguration 
    public static class SpringConfig {
        
    }
}
