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
package de.tudarmstadt.ukp.inception.diam.service;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_USER;
import static de.tudarmstadt.ukp.inception.diam.service.DiamWebsocketController.FORMAT_LEGACY;
import static de.tudarmstadt.ukp.inception.websocket.config.WebsocketConfig.WS_ENDPOINT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import java.io.File;
import java.lang.invoke.MethodHandles;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.config.AnnotationAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.PreRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.diag.config.CasDoctorAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.InceptionDaoAuthenticationProvider;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.InceptionSecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.text.config.TextFormatsAutoConfiguration;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.diam.messages.MViewportUpdate;
import de.tudarmstadt.ukp.inception.diam.model.websocket.ViewportDefinition;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.documents.config.DocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.export.config.DocumentImportExportServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.preferences.config.PreferencesServiceAutoConfig;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.rendering.config.RenderingAutoConfig;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VRange;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VSpan;
import de.tudarmstadt.ukp.inception.schema.config.AnnotationSchemaServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.support.logging.Logging;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationContextProvider;
import de.tudarmstadt.ukp.inception.support.test.websocket.WebSocketStompTestClient;
import de.tudarmstadt.ukp.inception.websocket.config.WebsocketAutoConfiguration;
import de.tudarmstadt.ukp.inception.websocket.config.WebsocketSecurityConfig;
import de.tudarmstadt.ukp.inception.workload.config.WorkloadManagementAutoConfiguration;
import jakarta.persistence.EntityManager;

@SpringBootTest( //
        webEnvironment = RANDOM_PORT, //
        properties = { //
                "recommender.enabled=false", //
                "server.address=127.0.0.1", //
                "spring.main.banner-mode=off", //
                "websocket.enabled=true" })
@SpringBootApplication( //
        exclude = { //
                LiquibaseAutoConfiguration.class, //
                WorkloadManagementAutoConfiguration.class })
@ImportAutoConfiguration({ //
        PreferencesServiceAutoConfig.class, //
        CasDoctorAutoConfiguration.class, //
        RenderingAutoConfig.class, //
        InceptionSecurityAutoConfiguration.class, //
        SecurityAutoConfiguration.class, //
        WebsocketAutoConfiguration.class, //
        WebsocketSecurityConfig.class, //
        ProjectServiceAutoConfiguration.class, //
        DocumentServiceAutoConfiguration.class, //
        CasStorageServiceAutoConfiguration.class, //
        RepositoryAutoConfiguration.class, //
        AnnotationSchemaServiceAutoConfiguration.class, //
        AnnotationAutoConfiguration.class, //
        TextFormatsAutoConfiguration.class, //
        DocumentServiceAutoConfiguration.class, //
        DocumentImportExportServiceAutoConfiguration.class })
@EntityScan({ //
        "de.tudarmstadt.ukp.inception.preferences.model", //
        "de.tudarmstadt.ukp.clarin.webanno.model", //
        "de.tudarmstadt.ukp.clarin.webanno.security.model", //
        "de.tudarmstadt.ukp.inception.log.model" })
public class DiamWebsocketController_ViewportRoutingTest
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String USER = "user";
    private static final String PASS = "pass";

    private @LocalServerPort int port;
    private String websocketUrl;

    private @Autowired DiamWebsocketController sut;

    private @Autowired ProjectService projectService;
    private @Autowired DocumentService documentService;
    private @Autowired RepositoryProperties repositoryProperties;
    private @Autowired EntityManager entityManager;
    private @Autowired UserDao userService;

    private static @TempDir File repositoryDir;

    private static User user;
    private static Project testProject;
    private static SourceDocument testDoc;
    private static AnnotationDocument testAnnotationDocument;

    @BeforeEach
    public void setup() throws Exception
    {
        websocketUrl = "ws://localhost:" + port + WS_ENDPOINT;

        setupOnce();
    }

    void setupOnce() throws Exception
    {
        if (testProject != null) {
            return;
        }

        repositoryProperties.setPath(repositoryDir);
        MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

        user = new User(USER, ROLE_USER);
        user.setPassword(PASS);
        userService.create(user);

        testProject = new Project("test-project");
        projectService.createProject(testProject);
        projectService.assignRole(testProject, user, ANNOTATOR);

        testDoc = new SourceDocument("testDoc", testProject, "text");
        documentService.createSourceDocument(testDoc);

        testAnnotationDocument = new AnnotationDocument(USER, testDoc);
        documentService.createOrUpdateAnnotationDocument(testAnnotationDocument);

        try (var session = CasStorageSession.open()) {
            documentService.uploadSourceDocument(
                    toInputStream("This is a test. ".repeat(10).trim(), UTF_8),
                    testAnnotationDocument.getDocument());
        }
    }

    @AfterEach
    public void tearDown()
    {
        entityManager.clear();
    }

    @WithMockUser(username = "user", roles = { "USER" })
    @Test
    public void thatViewportBasedMessageRoutingWorks() throws Exception
    {
        var emptyListNode = JsonNodeFactory.instance.arrayNode();

        var vpd1 = new ViewportDefinition(testAnnotationDocument, 10, 20, FORMAT_LEGACY);
        var vpd2 = new ViewportDefinition(testAnnotationDocument, 30, 40, FORMAT_LEGACY);

        try (var client1 = new WebSocketStompTestClient(USER, PASS);
                var client2 = new WebSocketStompTestClient(USER, PASS)) {
            client1.expectSuccessfulConnection().connect(websocketUrl);
            client1.subscribe("/topic" + vpd1.getTopic());
            client1.expect(new MViewportUpdate(0, 0, null)).subscribe("/app" + vpd1.getTopic());

            client2.expectSuccessfulConnection().connect(websocketUrl);
            client2.subscribe("/topic" + vpd2.getTopic());
            client2.expect(new MViewportUpdate(0, 0, null)).subscribe("/app" + vpd2.getTopic());

            client1.expect(new MViewportUpdate(12, 15, emptyListNode))
                    .expect(new MViewportUpdate(15, 35, emptyListNode));
            client2.expect(new MViewportUpdate(31, 33, emptyListNode))
                    .expect(new MViewportUpdate(15, 35, emptyListNode));

            sut.sendUpdate(testAnnotationDocument, 12, 15);
            sut.sendUpdate(testAnnotationDocument, 31, 33);
            sut.sendUpdate(testAnnotationDocument, 15, 35);

            client1.assertExpectations();
            client2.assertExpectations();
        }
    }

    @SpringBootConfiguration
    public static class WebsocketBrokerTestConfig
    {
        @Bean
        public ChannelInterceptor csrfChannelInterceptor()
        {
            // Disable CSRF
            return new ChannelInterceptor()
            {
            };
        }

        @Bean
        public ApplicationContextProvider applicationContextProvider()
        {
            return new ApplicationContextProvider();
        }

        @Bean
        public DaoAuthenticationProvider authenticationProvider(PasswordEncoder aEncoder,
                @Lazy UserDetailsManager aUserDetailsManager)
        {
            var authProvider = new InceptionDaoAuthenticationProvider();
            authProvider.setUserDetailsService(aUserDetailsManager);
            authProvider.setPasswordEncoder(aEncoder);
            return authProvider;
        }

        @Order(100)
        @Bean
        public SecurityFilterChain wsFilterChain(HttpSecurity aHttp) throws Exception
        {
            aHttp.securityMatcher(WS_ENDPOINT);
            aHttp.authorizeHttpRequests(rules -> rules //
                    .requestMatchers("/**").authenticated() //
                    .anyRequest().denyAll());
            aHttp.sessionManagement(session -> session //
                    .sessionCreationPolicy(STATELESS));
            aHttp.httpBasic(withDefaults());
            return aHttp.build();
        }

        @Primary
        @Bean
        public PreRenderer testPreRenderer()
        {
            return new PreRenderer()
            {
                @Override
                public String getId()
                {
                    return "TestPreRenderer";
                }

                @Override
                public void render(VDocument aResponse, RenderRequest aRequest)
                {
                    var layer = new AnnotationLayer();
                    layer.setId(1l);
                    aResponse.add(
                            new VSpan(layer, new VID(1), new VRange(aRequest.getWindowBeginOffset(),
                                    aRequest.getWindowEndOffset()), emptyMap()));
                }
            };
        }
    }
}
