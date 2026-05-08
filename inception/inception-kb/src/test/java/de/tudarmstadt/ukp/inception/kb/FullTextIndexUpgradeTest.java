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

import static de.tudarmstadt.ukp.inception.kb.reification.Reification.NONE;
import static java.nio.file.Files.newInputStream;
import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryPropertiesImpl;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseProperties;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBasePropertiesImpl;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Transactional
@DataJpaTest( //
        showSql = false, //
        properties = { //
                "spring.main.banner-mode=off" })
@EnableAutoConfiguration
@EntityScan(basePackages = { //
        "de.tudarmstadt.ukp.inception.kb.model", //
        "de.tudarmstadt.ukp.clarin.webanno.model" })
public class FullTextIndexUpgradeTest
{
    static final String REF_DIR = "src/test/resources/KnowledgeBaseUpgradeTest";

    private @PersistenceContext EntityManager entityManager;
    private @TempDir File temp;

    private Project project;
    private RepositoryPropertiesImpl repoProperties;
    private KnowledgeBaseProperties kbProperties;
    private KnowledgeBase kb;

    private KnowledgeBaseServiceImpl sut;

    @BeforeEach
    void setup()
    {
        kb = new KnowledgeBase();
        kb.setName("test");
        kb.setProject(project);
        kb.setType(RepositoryType.LOCAL);
        kb.setClassIri(RDFS.CLASS.stringValue());
        kb.setSubclassIri(RDFS.SUBCLASSOF.stringValue());
        kb.setTypeIri(RDF.TYPE.stringValue());
        kb.setLabelIri(RDFS.LABEL.stringValue());
        kb.setPropertyTypeIri(RDF.PROPERTY.stringValue());
        kb.setDescriptionIri(RDFS.COMMENT.stringValue());
        kb.setFullTextSearchIri(IriConstants.FTS_RDF4J_LUCENE.stringValue());
        kb.setPropertyLabelIri(RDFS.LABEL.stringValue());
        kb.setPropertyDescriptionIri(RDFS.COMMENT.stringValue());
        kb.setSubPropertyIri(RDFS.SUBPROPERTYOF.stringValue());
        kb.setRootConcepts(new ArrayList<>());
        kb.setReification(NONE);
        kb.setMaxResults(1000);

        project = new Project("test");
        entityManager.persist(project);

        repoProperties = new RepositoryPropertiesImpl();
        kbProperties = new KnowledgeBasePropertiesImpl();
        repoProperties.setPath(temp);
    }

    @AfterEach
    public void tearDown() throws Exception
    {
        entityManager.clear();
        sut.destroy();
    }

    /**
     * To regenerate reference data for a given Lucene version:
     * <ol>
     * <li>Check out a project revision that uses the desired Lucene version (e.g. the commit
     * immediately before the Lucene 10 upgrade for Lucene 9 reference data).</li>
     * <li>Un-disable this test and run it.</li>
     * <li>Copy the resulting index from the {@link TempDir} (the path is logged at INFO; or set a
     * breakpoint and inspect {@code temp}) into
     * {@code src/test/resources/KnowledgeBaseUpgradeTest/lucene-X.Y.Z/}.</li>
     * </ol>
     */
    @Disabled("Only needed when we want to create new reference data")
    @Test
    void createKnowledgeBase() throws Exception
    {
        sut = new KnowledgeBaseServiceImpl(repoProperties, kbProperties, entityManager);

        sut.registerKnowledgeBase(kb, sut.getNativeConfig());

        try (var is = newInputStream(Paths
                .get("src/test/resources/turtle/data_labels_and_descriptions_with_language.ttl"))) {
            sut.importData(kb, "data.ttl", is);
        }
    }

    // Lucene 10 can only read indexes from the previous major (Lucene 9).
    // Indexes from Lucene 8.x and earlier can no longer be opened directly and
    // require a two-step migration through an intermediate Lucene 9 build.
    // The lucene-7.7.3 and lucene-8.11.3 reference data are kept for history.

    @Test
    void thatExistingLucene9KnowledgeBaseCanBeRead() throws Exception
    {
        copyDirectory(new File(REF_DIR, "lucene-9.12.3"), temp);

        sut = new KnowledgeBaseServiceImpl(repoProperties, kbProperties, entityManager);

        kb.setRepositoryId("pid-1-kbid-");
        sut.reconfigureLocalKnowledgeBase(kb);

        List<KBHandle> results;
        try (var conn = sut.getConnection(kb)) {
            var prefLabels = SPARQLQueryBuilder.forItems(kb).resolvePrefLabelProperties(conn);
            var builder = SPARQLQueryBuilder //
                    .forItems(kb) //
                    .withPrefLabelProperties(prefLabels) //
                    .withLabelStartingWith("Green Go");
            results = builder.asHandles(conn, true);
        }

        assertThat(results) //
                .extracting(KBHandle::getIdentifier) //
                .containsExactly(
                        "http://www.ukp.informatik.tu-darmstadt.de/inception/1.0#green-goblin");
    }

    @Test
    void thatExistingLucene9KnowledgeBaseCanBeUpdated() throws Exception
    {
        copyDirectory(new File(REF_DIR, "lucene-9.12.3"), temp);

        sut = new KnowledgeBaseServiceImpl(repoProperties, kbProperties, entityManager);

        kb.setRepositoryId("pid-1-kbid-");
        sut.reconfigureLocalKnowledgeBase(kb);

        try (var is = newInputStream(
                Paths.get("src/test/resources/turtle/data_additional_search_properties.ttl"))) {
            sut.importData(kb, "data.ttl", is);
        }

        List<KBHandle> results;
        try (var conn = sut.getConnection(kb)) {
            var prefLabels = SPARQLQueryBuilder.forItems(kb).resolvePrefLabelProperties(conn);
            var builder = SPARQLQueryBuilder //
                    .forItems(kb) //
                    .withPrefLabelProperties(prefLabels) //
                    .withLabelContainingAnyOf("Green", "case");
            results = builder.asHandles(conn, true);
        }

        assertThat(results) //
                .extracting(KBHandle::getIdentifier) //
                .containsExactly( //
                        "http://www.ukp.informatik.tu-darmstadt.de/inception/1.0#example",
                        "http://www.ukp.informatik.tu-darmstadt.de/inception/1.0#green-goblin",
                        "http://www.ukp.informatik.tu-darmstadt.de/inception/1.0#lucky-green");
    }

    @SpringBootConfiguration
    static class SpringConfig
    {
        // No content
    }
}
