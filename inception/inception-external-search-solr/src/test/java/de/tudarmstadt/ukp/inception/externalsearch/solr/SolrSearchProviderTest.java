/*
 * Copyright 2020
 * ENP-China, Aix-Marseille University
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
package de.tudarmstadt.ukp.inception.externalsearch.solr;

import static java.time.Duration.ofMinutes;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;
import de.tudarmstadt.ukp.inception.externalsearch.solr.traits.SolrSearchProviderTraits;

@Testcontainers(disabledWithoutDocker = true)
class SolrSearchProviderTest
{
    private static final String CORE = "core";
    private static final int SOLR_PORT = 8983;
    private static final String SOLR_VERSION = System.getProperty("inception.solr.version",
            "10.0.0");

    @Container
    private static final GenericContainer<?> SOLR = new GenericContainer<>("solr:" + SOLR_VERSION) //
            .withExposedPorts(SOLR_PORT) //
            .withCopyFileToContainer(MountableFile.forClasspathResource("/solr/core/conf"),
                    "/opt/solr/server/solr/configsets/inception/conf") //
            .withCommand("solr-precreate", CORE, "/opt/solr/server/solr/configsets/inception") //
            .waitingFor(Wait.forHttp("/solr/" + CORE + "/admin/ping").forPort(SOLR_PORT)
                    .withStartupTimeout(ofMinutes(2)));

    private SolrSearchProvider sut;
    private DocumentRepository repo;
    private SolrSearchProviderTraits traits;

    @BeforeAll
    static void seedCore() throws Exception
    {
        try (var client = newClient()) {
            addDoc(client, "1", "Title 1", "Here goes the document text.");
            addDoc(client, "2", "Title 2", "Here goes the document text.");
            addDoc(client, "3", "Title 3", "Here goes the document text.");
        }
    }

    @BeforeEach
    void setup() throws Exception
    {
        sut = new SolrSearchProvider();

        repo = new DocumentRepository("test", null);

        traits = new SolrSearchProviderTraits();
        traits.setRemoteUrl(solrUrl());
        traits.setIndexName(CORE);
        traits.setSearchPath("/select");
        traits.setDefaultField("text");
        traits.setTextField("text");
    }

    @Test
    void thatQueryWorks() throws Exception
    {
        var query = "document";

        assertThat(sut.executeQuery(repo, traits, query)) //
                .extracting(ExternalSearchResult::getDocumentId) //
                .containsExactlyInAnyOrder("1", "2", "3");
    }

    @Test
    void thatDocumentTextCanBeRetrieved() throws Exception
    {
        assertThat(sut.getDocumentText(repo, traits, traits.getIndexName(), "1"))
                .isEqualTo("Here goes the document text.");
    }

    @Test
    void randomOrderWork() throws Exception
    {
        var query = "*";
        traits.setRandomOrder(true);

        traits.setSeed(2);
        List<ExternalSearchResult> results = sut.executeQuery(repo, traits, query);
        var result1 = results.get(0).getDocumentTitle();

        traits.setSeed(3);
        results = sut.executeQuery(repo, traits, query);
        var result2 = results.get(0).getDocumentTitle();

        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    void highlightingWork() throws Exception
    {
        var query = "document";
        traits.setResultSize(5);

        var results = sut.executeQuery(repo, traits, query);

        assertThat(results.get(0).getHighlights().get(0).getHighlight()).isNotNull();
    }

    private static String solrUrl()
    {
        return "http://" + SOLR.getHost() + ":" + SOLR.getMappedPort(SOLR_PORT) + "/solr";
    }

    private static SolrClient newClient()
    {
        return new HttpJdkSolrClient.Builder(solrUrl())
                .withConnectionTimeout(10000, TimeUnit.MILLISECONDS)
                .withIdleTimeout(60000, TimeUnit.MILLISECONDS).build();
    }

    private static void addDoc(SolrClient client, String id, String title, String text)
        throws SolrServerException, IOException
    {
        var doc = new SolrInputDocument();
        doc.addField("id", id);
        doc.addField("title", title);
        doc.addField("text", text);
        client.add(CORE, doc);
        client.commit(CORE);
    }
}
