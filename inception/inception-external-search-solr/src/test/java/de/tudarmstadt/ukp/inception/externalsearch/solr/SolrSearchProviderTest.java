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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.embedded.JettyConfig;
import org.apache.solr.embedded.JettySolrRunner;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;
import de.tudarmstadt.ukp.inception.externalsearch.solr.traits.SolrSearchProviderTraits;

/**
 * The test class SolrSearchProviderTest is used to test functionalities. By default this class use
 * the collection given in the first Solr tutorial : techproducts In order to test your own
 * collection, setup the name of the collection, names of the fields and provide an existing id for
 * the method 'thatDocumentTextCanBeRetrieved()'
 */
class SolrSearchProviderTest
{
    private static JettySolrRunner solrRunner;

    private SolrSearchProvider sut;
    private DocumentRepository repo;
    private SolrSearchProviderTraits traits;

    @BeforeAll
    static void startServer(@TempDir Path aTemp) throws Exception
    {
        var origSolrHome = new File("src/test/resources/solr");
        var tempSolrHome = aTemp.toFile();
        var tempSolrData = aTemp.resolve("data").toFile();
        FileUtils.copyDirectory(origSolrHome, tempSolrHome);
        var jettyConfig = JettyConfig.builder() //
                .setContext("") //
                .setPort(0) //
                .stopAtShutdown(true) //
                .build();
        var nodeProperties = new Properties();
        nodeProperties.setProperty("solr.data.dir", tempSolrData.getCanonicalPath());
        nodeProperties.setProperty("coreRootDirectory", tempSolrHome.toString());
        nodeProperties.setProperty("configSetBaseDir", tempSolrHome.toString());
        System.setProperty("solr.directoryFactory", "solr.RAMDirectoryFactory");
        System.setProperty("jetty.testMode", "true");
        solrRunner = new JettySolrRunner(tempSolrHome.toString(), nodeProperties, jettyConfig);
        solrRunner.start();
        solrRunner.getCoreContainer().create("core", Map.of());

        try (var client = solrRunner.newClient()) {
            addDoc(client, "1", "Title 1", "Here goes the document text.");
            addDoc(client, "2", "Title 2", "Here goes the document text.");
            addDoc(client, "3", "Title 3", "Here goes the document text.");
        }
    }

    @AfterAll()
    static void shutDown() throws Exception
    {
        solrRunner.stop();
    }

    @BeforeEach
    void setup() throws Exception
    {
        sut = new SolrSearchProvider();

        repo = new DocumentRepository("test", null);

        traits = new SolrSearchProviderTraits();
        traits.setRemoteUrl("http://localhost:" + solrRunner.getLocalPort());
        traits.setIndexName("core");
        traits.setSearchPath("/select");
        traits.setDefaultField("text");
        traits.setTextField("text");
    }

    private static void addDoc(SolrClient client, String id, String title, String text)
        throws SolrServerException, IOException
    {
        final SolrInputDocument doc = new SolrInputDocument();
        doc.addField("id", id);
        doc.addField("title", title);
        doc.addField("text", text);
        client.add("core", doc);
        client.commit("core");
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
    public void thatDocumentTextCanBeRetrieved() throws Exception
    {
        assertThat(sut.getDocumentText(repo, traits, traits.getIndexName(), "1"))
                .isEqualTo("Here goes the document text.");
    }

    @Test
    public void randomOrderWork() throws Exception
    {
        var query = "*";
        traits.setRandomOrder(true);

        traits.setSeed(2);
        List<ExternalSearchResult> results = sut.executeQuery(repo, traits, query);
        // System.out.println(results.get(0).getDocumentTitle());
        var result1 = results.get(0).getDocumentTitle();

        traits.setSeed(3);
        results = sut.executeQuery(repo, traits, query);
        // System.out.println(results.get(0).getDocumentTitle());
        var result2 = results.get(0).getDocumentTitle();

        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    public void highlightingWork() throws Exception
    {
        var query = "document";
        traits.setResultSize(5);

        var results = sut.executeQuery(repo, traits, query);
        // System.out.println(results.get(0).getHighlights().get(0).getHighlight());

        assertThat(results.get(0).getHighlights().get(0).getHighlight()).isNotNull();
    }
}
