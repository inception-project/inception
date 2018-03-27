/*
 * Copyright 2017
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

/**
 * TODO java.lang.IllegalStateException: Unable to find a @SpringBootConfiguration, you need to
 * use @ContextConfiguration or @SpringBootTest(classes=...) with your test
 */
//@RunWith(SpringRunner.class)
//@DataJpaTest
public class KnowledgeBaseServiceImplTest {
    
//    @TestConfiguration
//    static class KnowledgeBaseServiceImplTestContextConfiguration {
//        
//        @Bean
//        public KnowledgeBaseService knowledgeBaseService() {
//            return new KnowledgeBaseServiceImpl();
//        }
//    }
//    
//    @Autowired
//    private KnowledgeBaseService service;    
//    
//    @Autowired
//    private TestEntityManager entityManager;
//    
//    private KnowledgeBase kb1, kb2, kb3;
//    private Project p1, p2;
//    private RepositoryImplConfig cfg1, cfg2, cfg3;
//
//    @Before
//    public void setUp() throws Exception {        
//        kb1 = new KnowledgeBase();
//        kb1.setName("british museum");
//        
//        kb2 = new KnowledgeBase();
//        kb2.setName("dbpedia");
//        
//        kb3 = new KnowledgeBase();
//        kb3.setName("babelnet");
//        
//        p1 = new Project();
//        p1.setId(1);
//        
//        p2 = new Project();
//        p2.setId(2);
//        
//        cfg1 = KnowledgeBases.BRITISH_MUSEUM;
//        cfg2 = KnowledgeBases.DBPEDIA;
//        cfg3 = KnowledgeBases.BABELNET;
//    }
//
//    @After
//    public void tearDown() throws Exception {
//    }
//
//    @Test
//    public void testRegisterRemove() {
//        {
//            // assert that there is no KnowledgeBase present
//            List<KnowledgeBase> kbs = service.getKnowledgeBases(p1);
//            assertTrue(kbs.isEmpty());
//        }       
//        
//        
//        // create a KnowledgeBase, register it
//        kb1.setProject(p1);
//        service.registerKnowledgeBase(kb1, cfg1);
//        
//        {
//            // assert that the KnowledgeBase was registered
//            List<KnowledgeBase> kbs = service.getKnowledgeBases(p1);
//            assertEquals(1, kbs.size());
//            assertTrue(kbs.contains(kb1));
//        }
//        
//        // create a second KnowledgeBase for the same project
//        kb2.setProject(p1);
//        service.registerKnowledgeBase(kb2, cfg2);
//        
//        {
//            // assert that both KnowledgeBases were registered
//            List<KnowledgeBase> kbs = service.getKnowledgeBases(p1);
//            assertEquals(2, kbs.size());
//            assertTrue(kbs.contains(kb1));
//            assertTrue(kbs.contains(kb2));
//        }
//        
//        // remove the first KnowledgeBase
//        service.removeKnowledgeBase(kb1);
//        
//        {
//            // assert that there's only one KnowledgeBase two left
//            List<KnowledgeBase> kbs = service.getKnowledgeBases(p1);
//            assertEquals(1, kbs.size());
//            assertTrue(kbs.contains(kb2));
//        }
//        
//        // register a KnowledgeBase for a different project
//        kb3.setProject(p2);
//        service.registerKnowledgeBase(kb3, cfg3);
//
//        {
//            // assert that the query for p1 only returns kb2
//            List<KnowledgeBase> kbs = service.getKnowledgeBases(p1);
//            assertEquals(1, kbs.size());
//            assertTrue(kbs.contains(kb2));
//        }
//        {
//            // assert that the query for p2 only returns kb3
//            List<KnowledgeBase> kbs = service.getKnowledgeBases(p2);
//            assertEquals(1, kbs.size());
//            assertTrue(kbs.contains(kb3));
//        }
//        
//        service.removeKnowledgeBase(kb2);
//        service.removeKnowledgeBase(kb3);
//        {
//            // assert that there is no KnowledgeBase present
//            List<KnowledgeBase> kbs = service.getKnowledgeBases(p1);
//            assertTrue(kbs.isEmpty());
//        }
//    }
}
