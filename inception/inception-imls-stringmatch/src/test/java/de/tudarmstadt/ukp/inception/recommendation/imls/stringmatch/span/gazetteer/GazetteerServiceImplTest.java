/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
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
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazetteer;

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
import java.util.ArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryPropertiesImpl;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazetteer.model.Gazetteer;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazetteer.model.GazetteerEntry;
import de.tudarmstadt.ukp.inception.support.WebAnnoConst;
import de.tudarmstadt.ukp.inception.support.logging.Logging;
import jakarta.persistence.EntityManager;

@DataJpaTest( //
        showSql = false, //
        properties = { //
                "spring.main.banner-mode=off" })
@Transactional
@EntityScan(basePackages = { "de.tudarmstadt.ukp.inception", "de.tudarmstadt.ukp.clarin.webanno" })
public class GazetteerServiceImplTest
{
    @TempDir
    public File temporaryFolder;

    @Autowired
    private TestEntityManager testEntityManager;

    private GazetteerServiceImpl sut;

    private Project project;
    private AnnotationLayer spanLayer;
    private AnnotationFeature spanFeat1;
    private Recommender rec1;

    @BeforeEach
    public void setup()
    {
        EntityManager em = testEntityManager.getEntityManager();

        RepositoryProperties repoProps = new RepositoryPropertiesImpl();
        repoProps.setPath(temporaryFolder);
        MDC.put(Logging.KEY_REPOSITORY_PATH, repoProps.getPath().toString());

        sut = new GazetteerServiceImpl(repoProps, em);

        project = new Project();
        project.setName("test");
        em.persist(project);

        spanLayer = new AnnotationLayer("span", "span", WebAnnoConst.SPAN_TYPE, project, false,
                TOKENS, NO_OVERLAP);
        em.persist(spanLayer);

        spanFeat1 = new AnnotationFeature(project, spanLayer, "feat1", "feat1", TYPE_NAME_STRING);
        em.persist(spanFeat1);

        rec1 = new Recommender("rec1", spanLayer);
        rec1.setFeature(spanFeat1);
        em.persist(rec1);
    }

    @AfterEach
    public void tearDown() throws Exception
    {
        testEntityManager.clear();
    }

    @Test
    public void thatCreateListAndDeleteGazetteerWorks() throws Exception
    {
        // Add first gazetteer
        Gazetteer gaz1 = new Gazetteer("gaz1", rec1);
        sut.createOrUpdateGazetteer(gaz1);

        assertThat(gaz1.getId())
                .describedAs("After saving the gazetter to the DB, the ID should be set")
                .isNotNull();
        assertThat(sut.existsGazetteer(gaz1.getRecommender(), gaz1.getName()))
                .describedAs("Gazetteers can be found using existsGazetteer").isTrue();
        assertThat(sut.listGazetteers(rec1))
                .describedAs("All gazetteers that have been created are returned by listGazetteers")
                .containsExactly(gaz1);

        // Add second gazetteer
        Gazetteer gaz2 = new Gazetteer("gaz2", rec1);
        sut.createOrUpdateGazetteer(gaz2);

        assertThat(gaz2.getId())
                .describedAs("After saving the gazetter to the DB, the ID should be set")
                .isNotNull();
        assertThat(sut.existsGazetteer(gaz2.getRecommender(), gaz2.getName()))
                .describedAs("Gazetteers can be found using existsGazetteer").isTrue();
        assertThat(sut.listGazetteers(rec1))
                .describedAs("All gazetteers that have been created are returned by listGazetteers")
                .containsExactly(gaz1, gaz2);

        // Remove first gazetteer
        sut.deleteGazetteers(gaz1);

        assertThat(sut.listGazetteers(rec1))
                .describedAs("A deleted gazetteer no longer returned by listGazetteers")
                .containsExactly(gaz2);
    }

    @Test
    public void thatUpdatingGazetteerWorks() throws Exception
    {
        Gazetteer gaz = new Gazetteer("foo", rec1);
        sut.createOrUpdateGazetteer(gaz);

        assertThat(sut.listGazetteers(rec1))
                .describedAs("Name of the gazetteer has the initial value")
                .extracting(Gazetteer::getName).containsExactly("foo");

        gaz.setName("bar");
        sut.createOrUpdateGazetteer(gaz);

        assertThat(sut.listGazetteers(rec1))
                .describedAs("Name of the gazetteer has the updated value")
                .extracting(Gazetteer::getName).containsExactly("bar");
    }

    @Test
    public void thatImportGazetteerWorks() throws Exception
    {
        var gaz = new Gazetteer("gaz", rec1);
        sut.createOrUpdateGazetteer(gaz);

        var input = new File("src/test/resources/gazetteers/gaz1.txt");

        // Check that import works
        try (var is = new FileInputStream(input)) {
            sut.importGazetteerFile(gaz, is);
        }

        var gazFile = sut.getGazetteerFile(gaz);
        assertThat(gazFile.exists()).describedAs("Gazetteer data has been imported").isTrue();
        assertThat(contentOf(sut.getGazetteerFile(gaz)))
                .isEqualToNormalizingNewlines(contentOf(input));

        // Check that imported file matches the expectations
        var gazData = sut.readGazetteerFile(gaz);
        assertThat(gazData).containsExactlyInAnyOrder(new GazetteerEntry("John", "PER"),
                new GazetteerEntry("London", "LOC"), new GazetteerEntry("London", "GPE"),
                new GazetteerEntry("ACME", "ORG"));

        // Check that gazetteer file has been deleted along with the entity
        sut.deleteGazetteers(gaz);

        assertThat(gazFile.exists()).describedAs("Gazetteer data has been deleted").isFalse();
    }

    @Test
    public void thatGazetteerCommentLineIsIgnored() throws Exception
    {
        var gaz = new Gazetteer("gaz", rec1);

        var gazetteer = String.join("\n", "# This is a comment", "John\tPER");

        var data = new ArrayList<GazetteerEntry>();
        sut.parseGazetteer(gaz, toInputStream(gazetteer, UTF_8), data);

        assertThat(data).containsExactlyInAnyOrder(new GazetteerEntry("John", "PER"));
    }

    @Test
    public void thatGazetteerWithExtraColumnsCanBeRead() throws Exception
    {
        var gaz = new Gazetteer("gaz", rec1);

        var data = new ArrayList<GazetteerEntry>();

        var gazetteer1 = String.join("\n", "Bill\tPER", "John PER");

        assertThatExceptionOfType(IOException.class)
                .describedAs("Line without tab generated exception")
                .isThrownBy(() -> sut.parseGazetteer(gaz, toInputStream(gazetteer1, UTF_8), data))
                .withMessageContaining("Unable to parse line 2");

        var gazetteer2 = "Bill\tPER\t40";

        sut.parseGazetteer(gaz, toInputStream(gazetteer2, UTF_8), data);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    public static class SpringConfig
    {
        // No content
    }
}
