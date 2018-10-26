/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.kb;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;

import javax.persistence.EntityManager;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
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

import de.tudarmstadt.ukp.clarin.webanno.api.dao.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.reification.Reification;
import de.tudarmstadt.ukp.inception.kb.util.TestFixtures;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SpringConfig.class)
@Transactional
@DataJpaTest
public class KnowledgeBaseLocalFullTextSearchIntegrationTest
{
    private static final String PROJECT_NAME = "Test project";
    private static final String KB_NAME = "Test knowledge base";

    public @Rule TemporaryFolder temporaryFolder = new TemporaryFolder();

    private @Autowired TestEntityManager testEntityManager;
    
    private KnowledgeBaseService kbService;

    private KnowledgeBase kb;

    @Before
    public void setUp() throws Exception
    {
        RepositoryProperties repoProps = new RepositoryProperties();
        repoProps.setPath(temporaryFolder.getRoot());
        EntityManager entityManager = testEntityManager.getEntityManager();
        TestFixtures testFixtures = new TestFixtures(testEntityManager);
        kbService = new KnowledgeBaseServiceImpl(repoProps, entityManager);
        Project project = testFixtures.createProject(PROJECT_NAME);
        kb = testFixtures.buildKnowledgeBase(project, KB_NAME, Reification.NONE);
    }

    @Test
    public void thatResourceCanBeFoundByLabel() throws Exception
    {
        kbService.registerKnowledgeBase(kb, kbService.getNativeConfig());
        importKnowledgeBase("data/pets.ttl");
        
        kbService.read(kb, conn -> {
            String QUERY = String.join("\n",
                    "PREFIX search: <http://www.openrdf.org/contrib/lucenesail#>",
                    "SELECT DISTINCT ?s ?text WHERE",
                    "{",
                    "  ?s search:matches [",
                    "    search:query ?query ;",
                    "    search:snippet ?text",
                    "  ]",
                    "  OPTIONAL",
                    "  {",
                    "    ?e2 ?descriptionIri ?description.",
                    "  }",
                    "}");
            TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
            query.setBinding("query", conn.getValueFactory().createLiteral("Animal"));
            TupleQueryResult result = query.evaluate();
            
            assertThat(result.hasNext()).isTrue();
            BindingSet bs = result.next();
            assertThat(bs.getBinding("s").getValue().stringValue()).isEqualTo("http://mbugert.de/pets#animal");
            assertThat(bs.getBinding("text").getValue().stringValue()).isEqualTo("<B>Animal</B>");
            assertThat(result.hasNext()).isFalse();
            
            return null;
        });
    }
    
    // 
//    @Test
//    public void thatResourceCanBeFoundBySubjectIdentifier() throws Exception
//    {
//        kbService.registerKnowledgeBase(kb, kbService.getNativeConfig());
//        importKnowledgeBase("data/pets.ttl");
//        
//        kbService.read(kb, conn -> {
//            String QUERY = String.join("\n",
//                    "PREFIX search: <http://www.openrdf.org/contrib/lucenesail#>",
//                    "SELECT DISTINCT ?s ?text WHERE",
//                    "{",
//                    "  ?s search:matches [",
//                    "    search:query ?query ;",
//                    "    search:snippet ?text",
//                    "  ]",
//                    "  OPTIONAL",
//                    "  {",
//                    "    ?e2 ?descriptionIri ?description.",
//                    "  }",
//                    "}");
//            TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
//            query.setBinding("query", conn.getValueFactory().createLiteral("uri:\"http://mbugert.de/pets#animal\""));
//            TupleQueryResult result = query.evaluate();
//            
//            while (result.hasNext()) {
//                BindingSet bs = result.next();
//                System.out.printf("%s %s %n", bs.getBinding("s"), bs.getBinding("text"));
//            }
//            
//            return null;
//        });
//    }    

    private void importKnowledgeBase(String resourceName) throws Exception
    {
        ClassLoader classLoader = getClass().getClassLoader();
        String fileName = classLoader.getResource(resourceName).getFile();
        try (InputStream is = classLoader.getResourceAsStream(resourceName)) {
            kbService.importData(kb, fileName, is);
        }
    }
}
