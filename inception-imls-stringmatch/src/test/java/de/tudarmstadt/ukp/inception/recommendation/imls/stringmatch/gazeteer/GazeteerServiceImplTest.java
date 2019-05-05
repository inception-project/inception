/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.gazeteer;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PROJECT_TYPE_ANNOTATION;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.contentOf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.model.Gazeteer;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.model.GazeteerEntry;

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
        
        spanLayer = new AnnotationLayer("span", "span", SPAN_TYPE, project, false, TOKENS,
                NO_OVERLAP);
        em.persist(spanLayer);

        spanFeat1 = new AnnotationFeature(project, spanLayer, "feat1", "feat1", TYPE_NAME_STRING);
        em.persist(spanFeat1);
        
        rec1 = new Recommender("rec1", spanLayer);
        rec1.setFeature(spanFeat1);
        em.persist(rec1);
    }
    
    @After
    public void tearDown() throws Exception
    {
        testEntityManager.clear();
    }

    @Test
    public void thatCreateListAndDeleteGazeteerWorks() throws Exception
    {
        // Add first gazeteer
        Gazeteer gaz1 = new Gazeteer("gaz1", rec1);
        sut.createOrUpdateGazeteer(gaz1);

        assertThat(gaz1.getId())
                .describedAs("After saving the gazetter to the DB, the ID should be set")
                .isNotNull();
        assertThat(sut.existsGazeteer(gaz1.getRecommender(), gaz1.getName()))
                .describedAs("Gazeteers can be found using existsGazeteer")
                .isTrue();
        assertThat(sut.listGazeteers(rec1))
                .describedAs("All gazeteers that have been created are returned by listGazeteers")
                .containsExactly(gaz1);

        // Add second gazeteer
        Gazeteer gaz2 = new Gazeteer("gaz2", rec1);
        sut.createOrUpdateGazeteer(gaz2);

        assertThat(gaz2.getId())
                .describedAs("After saving the gazetter to the DB, the ID should be set")
                .isNotNull();
        assertThat(sut.existsGazeteer(gaz2.getRecommender(), gaz2.getName()))
                .describedAs("Gazeteers can be found using existsGazeteer")
                .isTrue();
        assertThat(sut.listGazeteers(rec1))
                .describedAs("All gazeteers that have been created are returned by listGazeteers")
                .containsExactly(gaz1, gaz2);
        
        // Remove first gazeteer
        sut.deleteGazeteers(gaz1);
        
        assertThat(sut.listGazeteers(rec1))
                .describedAs("A deleted gazeteer no longer returned by listGazeteers")
                .containsExactly(gaz2);
    }
    
    @Test
    public void thatUpdatingGazeteerWorks() throws Exception
    {
        Gazeteer gaz = new Gazeteer("foo", rec1);
        sut.createOrUpdateGazeteer(gaz);

        assertThat(sut.listGazeteers(rec1))
                .describedAs("Name of the gazeteer has the initial value")
                .extracting(Gazeteer::getName)
                .containsExactly("foo");
        
        gaz.setName("bar");
        sut.createOrUpdateGazeteer(gaz);
        
        assertThat(sut.listGazeteers(rec1))
                .describedAs("Name of the gazeteer has the updated value")
                .extracting(Gazeteer::getName)
                .containsExactly("bar");
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
        assertThat(gazFile.exists())
                .describedAs("Gazeteer data has been imported")
                .isTrue();
        assertThat(contentOf(sut.getGazeteerFile(gaz)))
                .isEqualToNormalizingNewlines(contentOf(input));
     
        // Check that imported file matches the expectations
        List<GazeteerEntry> gazData = sut.readGazeteerFile(gaz);
        assertThat(gazData).containsExactlyInAnyOrder(
                new GazeteerEntry("John", "PER"),
                new GazeteerEntry("London", "LOC"),
                new GazeteerEntry("London", "GPE"),
                new GazeteerEntry("ACME", "ORG"));
        
        // Check that gazeteer file has been deleted along with the entity
        sut.deleteGazeteers(gaz);
        
        assertThat(gazFile.exists())
                .describedAs("Gazeteer data has been deleted")
                .isFalse();
    }
    
    @Test
    public void thatGazeteerCommentLineIsIgnored() throws Exception
    {
        Gazeteer gaz = new Gazeteer("gaz", rec1);
        
        String gazeteer = String.join("\n",
                "# This is a comment",
                "John\tPER");
        
        List<GazeteerEntry> data = new ArrayList<>();
        sut.parseGazeteer(gaz, toInputStream(gazeteer, UTF_8), data);
        
        assertThat(data).containsExactlyInAnyOrder(
                new GazeteerEntry("John", "PER"));
    }

    @Test
    public void thatInvalidGazeteerGeneratesException() throws Exception
    {
        Gazeteer gaz = new Gazeteer("gaz", rec1);
        
        List<GazeteerEntry> data = new ArrayList<>();

        String gazeteer1 = String.join("\n",
                "Bill\tPER",
                "John PER");
                
        assertThatExceptionOfType(IOException.class)
                .describedAs("Line without tab generated exception")
                .isThrownBy(() -> sut.parseGazeteer(gaz, toInputStream(gazeteer1, UTF_8), data))
                .withMessageContaining("Unable to parse line 2");
        
        String gazeteer2 = "Bill\tPER\tDUMMY";
                
        assertThatExceptionOfType(IOException.class)
                .describedAs("Line without too many fields")
                .isThrownBy(() -> sut.parseGazeteer(gaz, toInputStream(gazeteer2, UTF_8), data))
                .withMessageContaining("Unable to parse line 1");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration 
    public static class SpringConfig {
        // No content
    }
}
