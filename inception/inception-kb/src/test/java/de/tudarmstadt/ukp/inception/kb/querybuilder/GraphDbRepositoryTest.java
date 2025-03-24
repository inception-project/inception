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

import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilderLocalTestScenarios.buildSparqlRepository;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofMinutes;
import static java.util.Arrays.asList;
import static org.apache.http.entity.ContentType.APPLICATION_OCTET_STREAM;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.wicket.util.io.IOUtils;
import org.eclipse.rdf4j.repository.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import de.tudarmstadt.ukp.inception.kb.IriConstants;
import de.tudarmstadt.ukp.inception.kb.RepositoryType;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilderLocalTestScenarios.Scenario;

@Testcontainers(disabledWithoutDocker = true)
public class GraphDbRepositoryTest
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String GRAPHDB_IMAGE = "ontotext/graphdb:10.7.0";
    private static final int GRAPHDB_PORT = 7200;

    @Container
    private static final GenericContainer<?> GRAPH_DB = new GenericContainer<>(GRAPHDB_IMAGE) //
            .withExposedPorts(GRAPHDB_PORT) //
            .withLogConsumer(new Slf4jLogConsumer(LOG)) //
            .waitingFor(Wait.forHttp("/rest/repositories").forPort(GRAPHDB_PORT)
                    .withStartupTimeout(ofMinutes(2)));

    private static boolean repoCreated = false;

    private Repository repository;
    private KnowledgeBase kb;

    @BeforeEach
    public void setUp() throws Exception
    {
        assertThat(GRAPH_DB.isRunning()).isTrue();

        var repositoryId = "test";

        var baseUrl = "http://" + GRAPH_DB.getHost() + ":" + GRAPH_DB.getMappedPort(GRAPHDB_PORT);

        if (!repoCreated) {
            createRepository(baseUrl, "test");
            repoCreated = true;
        }

        kb = new KnowledgeBase();
        kb.setDefaultLanguage("en");
        kb.setType(RepositoryType.REMOTE);
        // kb.setFullTextSearchIri(IriConstants.FTS_GRAPHDB.stringValue());
        kb.setFullTextSearchIri(IriConstants.FTS_NONE.stringValue());
        kb.setMaxResults(100);

        SPARQLQueryBuilderLocalTestScenarios.initRdfsMapping(kb);

        repository = buildSparqlRepository(
                "http://" + GRAPH_DB.getHost() + ":" + GRAPH_DB.getMappedPort(GRAPHDB_PORT)
                        + "/repositories/" + repositoryId,
                "http://" + GRAPH_DB.getHost() + ":" + GRAPH_DB.getMappedPort(GRAPHDB_PORT)
                        + "/repositories/" + repositoryId + "/statements");

        try (var conn = repository.getConnection()) {
            conn.clear();
        }
    }

    private static List<Arguments> tests() throws Exception
    {
        var exclusions = asList();

        return SPARQLQueryBuilderLocalTestScenarios.tests().stream() //
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

    private static void createRepository(String baseUrl, String repositoryId)
        throws IOException, URISyntaxException
    {
        var config = TEMPLATE //
                .replace("${REPOSITORY_ID}", repositoryId);

        var entity = MultipartEntityBuilder.create() //
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE) //
                .addBinaryBody("config", config.getBytes(UTF_8), APPLICATION_OCTET_STREAM,
                        "config.ttl") //
                .build();

        var request = RequestBuilder.post(new URI(baseUrl + "/rest/repositories")) //
                .setEntity(entity) //
                .build();

        // Send the request and get the response
        var client = HttpClientBuilder.create().build();
        var response = client.execute(request);

        // Print the response status and body
        var statusCode = response.getStatusLine().getStatusCode();
        if (statusCode < 200 || statusCode >= 300) {
            LOG.error("Response status code: {}", statusCode);
            LOG.error("Response body: {}", IOUtils.toString(response.getEntity().getContent()));
        }
        else {
            LOG.info("Repository created: [{}]", repositoryId);
        }
    }

    private static final String TEMPLATE = //
            """
            # Example RDF4J configuration template for a GraphDB repository named "wines"

            @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
            @prefix rep: <http://www.openrdf.org/config/repository#> .
            @prefix sail: <http://www.openrdf.org/config/sail#> .
            @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

            <#wines> a rep:Repository;
                rep:repositoryID "${REPOSITORY_ID}";
                rep:repositoryImpl [
                    rep:repositoryType "graphdb:SailRepository";
                    <http://www.openrdf.org/config/repository/sail#sailImpl> [
                        <http://www.ontotext.com/config/graphdb#base-URL> "http://example.org/owlim#";
                        <http://www.ontotext.com/config/graphdb#check-for-inconsistencies> "false";
                        <http://www.ontotext.com/config/graphdb#defaultNS> "";
                        <http://www.ontotext.com/config/graphdb#disable-sameAs> "true";
                        <http://www.ontotext.com/config/graphdb#enable-context-index> "false";
                        <http://www.ontotext.com/config/graphdb#enable-fts-index> "true";
                        <http://www.ontotext.com/config/graphdb#enable-literal-index> "true";
                        <http://www.ontotext.com/config/graphdb#enablePredicateList> "true";
                        <http://www.ontotext.com/config/graphdb#entity-id-size> "32";
                        <http://www.ontotext.com/config/graphdb#entity-index-size> "10000000";
                        <http://www.ontotext.com/config/graphdb#fts-indexes> ("default" "en" "de" "fr" "iri");
                        <http://www.ontotext.com/config/graphdb#fts-iris-index> "none";
                        <http://www.ontotext.com/config/graphdb#fts-string-literals-index> "default";
                        <http://www.ontotext.com/config/graphdb#imports> "";
                        <http://www.ontotext.com/config/graphdb#in-memory-literal-properties> "true";
                        <http://www.ontotext.com/config/graphdb#query-limit-results> "0";
                        <http://www.ontotext.com/config/graphdb#query-timeout> "0";
                        <http://www.ontotext.com/config/graphdb#read-only> "false";
                        <http://www.ontotext.com/config/graphdb#repository-type> "file-repository";
                        <http://www.ontotext.com/config/graphdb#ruleset> "rdfsplus-optimized";
                        <http://www.ontotext.com/config/graphdb#storage-folder> "storage";
                        <http://www.ontotext.com/config/graphdb#throw-QueryEvaluationException-on-timeout> "false";
                        sail:sailType "graphdb:Sail"
                    ]
                ];
                rdfs:label "" .
            """;

}
