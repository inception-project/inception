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

import static de.tudarmstadt.ukp.inception.kb.RepositoryType.REMOTE;
import static de.tudarmstadt.ukp.inception.support.SettingsUtil.PROP_VERSION;
import static de.tudarmstadt.ukp.inception.support.SettingsUtil.getVersionProperties;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;

import com.sun.net.httpserver.HttpServer;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryPropertiesImpl;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBasePropertiesImpl;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.support.config.InstanceMetadataPropertiesImpl;

@Transactional
@DataJpaTest( //
        showSql = false, //
        properties = { //
                "spring.main.banner-mode=off", //
                "spring.application.name=INCEpTION" })
public class KnowledgeBaseServiceUserAgentTest
{
    private static final String EMPTY_RESULT = """
            <?xml version="1.0"?>
            <sparql xmlns="http://www.w3.org/2005/sparql-results#">
              <head><variable name="s"/></head>
              <results/>
            </sparql>""";

    private static @TempDir File repoPath;

    private @Autowired TestEntityManager testEntityManager;
    private @Autowired KnowledgeBaseServiceImpl sut;
    private @Autowired InstanceMetadataPropertiesImpl instanceMetadataProperties;

    private HttpServer endpoint;
    private AtomicReference<String> receivedUserAgent;
    private KnowledgeBase kb;

    @BeforeEach
    void setUp() throws Exception
    {
        receivedUserAgent = new AtomicReference<>();
        endpoint = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        endpoint.createContext("/sparql", exchange -> {
            receivedUserAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
            var body = EMPTY_RESULT.getBytes(UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/sparql-results+xml");
            exchange.sendResponseHeaders(200, body.length);
            try (var os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        endpoint.start();

        var project = new Project();
        project.setName("Test project");
        testEntityManager.persist(project);

        kb = new KnowledgeBase();
        kb.setProject(project);
        kb.setName("Remote KB");
        kb.setType(REMOTE);
        kb.setReadOnly(true);
        sut.registerKnowledgeBase(kb, sut.getRemoteConfig(
                "http://localhost:" + endpoint.getAddress().getPort() + "/sparql"));
    }

    @AfterEach
    void tearDown()
    {
        instanceMetadataProperties.setOperatorContact(null);
        endpoint.stop(0);
        testEntityManager.clear();
    }

    @Test
    void thatUserAgentIdentifiesApplicationAndVersion() throws Exception
    {
        query();

        assertThat(receivedUserAgent.get()).isEqualTo("INCEpTION/" + version());
    }

    @Test
    void thatUserAgentIncludesOperatorContact() throws Exception
    {
        instanceMetadataProperties.setOperatorContact("https://annotation.example.org/contact");

        query();

        assertThat(receivedUserAgent.get())
                .isEqualTo("INCEpTION/" + version() + " (https://annotation.example.org/contact)");
    }

    private void query()
    {
        try (var conn = sut.getConnection(kb)) {
            conn.prepareTupleQuery("SELECT ?s WHERE { ?s ?p ?o } LIMIT 1").evaluate().close();
        }
    }

    private static String version()
    {
        return getVersionProperties().getProperty(PROP_VERSION, "unknown");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = { //
            "de.tudarmstadt.ukp.inception.kb.model", //
            "de.tudarmstadt.ukp.clarin.webanno.model" })
    public static class SpringConfig
    {
        @Bean
        InstanceMetadataPropertiesImpl instanceMetadataProperties()
        {
            return new InstanceMetadataPropertiesImpl();
        }

        @Bean
        KnowledgeBaseServiceImpl knowledgeBaseService(
                InstanceMetadataPropertiesImpl aInstanceMetadataProperties)
        {
            var repoProps = new RepositoryPropertiesImpl();
            repoProps.setPath(repoPath);
            // The entity manager is injected by Spring via @PersistenceContext. Creating the
            // service as a bean is what makes Spring resolve its @Value fields.
            return new KnowledgeBaseServiceImpl(repoProps, new KnowledgeBasePropertiesImpl(),
                    aInstanceMetadataProperties, null);
        }
    }
}
