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
package de.tudarmstadt.ukp.inception.kb.querybuilder;

import static de.tudarmstadt.ukp.inception.kb.IriConstants.FTS_FUSEKI;
import static de.tudarmstadt.ukp.inception.kb.http.PerThreadSslCheckingHttpClientUtils.restoreSslVerification;
import static de.tudarmstadt.ukp.inception.kb.http.PerThreadSslCheckingHttpClientUtils.suspendSslVerification;
import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilderTest.buildSparqlRepository;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.text.EntityDefinition;
import org.apache.jena.query.text.TextDatasetFactory;
import org.apache.jena.query.text.TextIndex;
import org.apache.jena.query.text.TextIndexConfig;
import org.apache.jena.query.text.TextIndexLucene;
import org.apache.jena.tdb.TDBFactory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.tudarmstadt.ukp.inception.kb.RepositoryType;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilderTest.Scenario;

public class FusekiRepositoryTest
{
    private @TempDir Path temp;
    private FusekiServer fusekiServer;
    private Repository repository;
    private KnowledgeBase kb;

    @BeforeEach
    public void setUp(TestInfo aTestInfo) throws Exception
    {
        String methodName = aTestInfo.getTestMethod().map(Method::getName).orElse("<unknown>");
        System.out.printf("\n=== %s === %s =====================\n", methodName,
                aTestInfo.getDisplayName());

        suspendSslVerification();

        // Local Fuseki in-memory story
        fusekiServer = FusekiServer.create() //
                .add("/fuseki", createFusekiFTSDataset()) //
                .build();
        fusekiServer.start();

        kb = new KnowledgeBase();
        kb.setDefaultLanguage("en");
        kb.setType(RepositoryType.REMOTE);
        kb.setFullTextSearchIri(FTS_FUSEKI.stringValue());
        kb.setMaxResults(100);

        SPARQLQueryBuilderTest.initRdfsMapping(kb);

        repository = buildSparqlRepository(
                "http://localhost:" + fusekiServer.getPort() + "/fuseki");

        try (RepositoryConnection conn = repository.getConnection()) {
            conn.clear();
        }
    }

    @AfterEach
    public void tearDown()
    {
        restoreSslVerification();

        fusekiServer.stop();
    }

    private static List<Arguments> tests() throws Exception
    {
        // These require additional configuration in Fuseki FTS
        var exclusions = asList( //
                "thatMatchingAgainstAdditionalSearchPropertiesWorks", //
                "testWithLabelMatchingExactlyAnyOf_subproperty", //
                "testWithLabelStartingWith_OLIA");

        return SPARQLQueryBuilderTest.tests().stream() //
                .filter(scenario -> !exclusions.contains(scenario.name))
                .map(scenario -> Arguments.of(scenario.name, scenario))
                .collect(Collectors.toList());
    }

    @ParameterizedTest(name = "{index}: test {0}")
    @MethodSource("tests")
    public void runTests(String aScenarioName, Scenario aScenario) throws Exception
    {
        aScenario.implementation.accept(repository, kb);
    }

    /**
     * Creates a dataset description with FTS support for the RDFS label property.
     * 
     * @throws IOException
     *             if there was an I/O-level problem
     */
    Dataset createFusekiFTSDataset() throws IOException
    {
        Dataset ds1 = TDBFactory.createDataset();
        Directory dir = new MMapDirectory(temp);
        EntityDefinition eDef = new EntityDefinition("iri", "text");
        eDef.setPrimaryPredicate(org.apache.jena.vocabulary.RDFS.label);
        TextIndexConfig tidxCfg = new TextIndexConfig(eDef);
        tidxCfg.setValueStored(true);
        tidxCfg.setMultilingualSupport(true);
        TextIndex tidx = new TextIndexLucene(dir, tidxCfg);
        Dataset ds = TextDatasetFactory.create(ds1, tidx);
        return ds;
    }
}
