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

package de.tudarmstadt.ukp.inception.conceptlinking.service;

import static de.tudarmstadt.ukp.inception.kb.ConceptFeatureValueType.ANY_OBJECT;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.inception.conceptlinking.config.EntityLinkingPropertiesImpl;
import de.tudarmstadt.ukp.inception.conceptlinking.util.TestFixtures;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryPropertiesImpl;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseServiceImpl;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBasePropertiesImpl;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.reification.Reification;

@Transactional
@DataJpaTest(excludeAutoConfiguration = LiquibaseAutoConfiguration.class)
public class ConceptLinkingServiceImplTest
{
    static {
        System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
        System.setProperty("spring.main.banner-mode", "off");
    }

    private static final String PROJECT_NAME = "Test project";
    private static final String KB_NAME = "Test knowledge base";

    @TempDir
    File temporaryFolder;

    @Autowired
    private TestEntityManager testEntityManager;

    private KnowledgeBaseService kbService;
    private ConceptLinkingServiceImpl sut;

    private KnowledgeBase kb;

    @BeforeEach
    public void setUp() throws Exception
    {
        var repoProps = new RepositoryPropertiesImpl();
        var kbProperties = new KnowledgeBasePropertiesImpl();
        repoProps.setPath(temporaryFolder);

        var entityManager = testEntityManager.getEntityManager();
        var testFixtures = new TestFixtures(testEntityManager);
        kbService = new KnowledgeBaseServiceImpl(repoProps, kbProperties, entityManager);

        sut = new ConceptLinkingServiceImpl(kbService, new EntityLinkingPropertiesImpl(), repoProps,
                emptyList());
        sut.afterPropertiesSet();
        sut.init();

        var project = testFixtures.createProject(PROJECT_NAME);
        kb = testFixtures.buildKnowledgeBase(project, KB_NAME, Reification.NONE);
    }

    @Test
    public void thatLuceneSailIndexedConceptIsRetrievableWithFullTextSearch() throws Exception
    {
        kbService.registerKnowledgeBase(kb, kbService.getNativeConfig());
        importKnowledgeBase("data/pets.ttl");

        var handles = sut.disambiguate(kb, null, ANY_OBJECT, "soc", null, 0, null);

        assertThat(handles.stream().map(KBHandle::getName))
                .as("Check whether \"Socke\" has been retrieved.") //
                .contains("Socke");

        kbService.removeKnowledgeBase(kb);
    }

    @Test
    public void thatAddedLuceneSailIndexedConceptIsRetrievableWithFullTextSearch() throws Exception
    {
        kbService.registerKnowledgeBase(kb, kbService.getNativeConfig());
        importKnowledgeBase("data/pets.ttl");

        var concept = new KBConcept();
        concept.setName("manatee");
        kbService.createConcept(kb, concept);

        var handles = sut.disambiguate(kb, null, ANY_OBJECT, "man", null, 0, null);

        assertThat(handles.stream().map(KBHandle::getName))
                .as("Check whether \"manatee\" has been retrieved.") //
                .contains("manatee");

        kbService.removeKnowledgeBase(kb);
    }

    private void importKnowledgeBase(String resourceName) throws Exception
    {
        var classLoader = getClass().getClassLoader();
        var fileName = classLoader.getResource(resourceName).getFile();
        try (var is = classLoader.getResourceAsStream(resourceName)) {
            kbService.importData(kb, fileName, is);
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
