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
package de.tudarmstadt.ukp.inception.externalsearch.opensearch;

import static java.lang.String.join;
import static java.time.Duration.ofMinutes;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;
import de.tudarmstadt.ukp.inception.externalsearch.opensearch.traits.OpenSearchProviderTraits;

@Testcontainers(disabledWithoutDocker = true)
public class OpenSearchProviderTest
{
    private static final String INDEX = "test";
    private static final int OPENSEARCH_PORT = 9200;
    private static final String OPENSEARCH_VERSION = System
            .getProperty("inception.opensearch.version", "3.6.0");

    @Container
    private static final GenericContainer<?> OPENSEARCH = new GenericContainer<>(
            "opensearchproject/opensearch:" + OPENSEARCH_VERSION) //
                    .withExposedPorts(OPENSEARCH_PORT) //
                    .withEnv("discovery.type", "single-node") //
                    .withEnv("plugins.security.disabled", "true") //
                    .withEnv("DISABLE_INSTALL_DEMO_CONFIG", "true") //
                    // Allow tests to run on machines with less than the default 90% high
                    // watermark disk space free.
                    .withEnv("cluster.routing.allocation.disk.threshold_enabled", "false") //
                    .withEnv("OPENSEARCH_JAVA_OPTS", "-Xms512m -Xmx512m") //
                    .waitingFor(Wait.forHttp("/_cluster/health?wait_for_status=yellow")
                            .forPort(OPENSEARCH_PORT).withStartupTimeout(ofMinutes(2)));

    private OpenSearchProvider sut;
    private DocumentRepository repo;
    private OpenSearchProviderTraits traits;

    @BeforeEach
    public void setup() throws Exception
    {
        recreateIndex();
        indexDocument("1", join("\n", //
                "{", //
                "  'metadata': {", //
                "    'language': 'en',", //
                "    'source': 'My favourite document collection',", //
                "    'timestamp': '2011/11/11 11:11',", //
                "    'uri': 'http://the.internet.com/my/document/collection/document1.txt',", //
                "    'title': 'Cool Document Title'", //
                "  },", //
                "  'doc': {", //
                "    'text': 'This is a test document'", //
                "  }", //
                "}").replace('\'', '"'));

        sut = new OpenSearchProvider();

        repo = new DocumentRepository("dummy", null);

        traits = new OpenSearchProviderTraits();
        traits.setRemoteUrl(opensearchUrl());
        traits.setIndexName(INDEX);
        traits.setSearchPath("_search");
    }

    @Test
    public void thatQueryWorks() throws Exception
    {
        List<ExternalSearchResult> results = sut.executeQuery(repo, traits, "document");

        assertThat(results).isNotEmpty();
    }

    @Test
    public void thatDocumentTextCanBeRetrieved() throws Exception
    {
        String documentText = sut.getDocumentText(repo, traits, INDEX, "1");
        assertThat(documentText).isNotNull();
    }

    private static String opensearchUrl()
    {
        return "http://" + OPENSEARCH.getHost() + ":" + OPENSEARCH.getMappedPort(OPENSEARCH_PORT);
    }

    private static void recreateIndex() throws IOException, InterruptedException
    {
        var client = HttpClient.newHttpClient();
        client.send(HttpRequest.newBuilder(URI.create(opensearchUrl() + "/" + INDEX)) //
                .DELETE().build(), BodyHandlers.discarding());
        var response = client.send(HttpRequest.newBuilder(URI.create(opensearchUrl() + "/" + INDEX)) //
                .PUT(BodyPublishers.noBody()).build(), BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException(
                    "Failed to create index: " + response.statusCode() + " " + response.body());
        }
    }

    private static void indexDocument(String id, String body)
        throws IOException, InterruptedException
    {
        var client = HttpClient.newHttpClient();
        var response = client.send(HttpRequest
                .newBuilder(
                        URI.create(opensearchUrl() + "/" + INDEX + "/_doc/" + id + "?refresh=true")) //
                .header("Content-Type", "application/json") //
                .PUT(BodyPublishers.ofString(body)).build(), BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException(
                    "Failed to index document: " + response.statusCode() + " " + response.body());
        }
    }
}
