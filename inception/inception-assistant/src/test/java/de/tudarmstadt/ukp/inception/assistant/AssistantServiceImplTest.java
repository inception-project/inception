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
package de.tudarmstadt.ukp.inception.assistant;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_USER;
import static de.tudarmstadt.ukp.inception.websocket.config.WebsocketConfig.WS_ENDPOINT;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
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
import org.springframework.core.annotation.Order;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import de.tudarmstadt.ukp.clarin.webanno.diag.config.CasDoctorAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.InceptionDaoAuthenticationProvider;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.assistant.model.MTextMessage;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.documents.config.DocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.export.config.DocumentImportExportServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.log.config.EventLoggingAutoConfiguration;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.search.config.SearchServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.support.logging.Logging;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationContextProvider;
import de.tudarmstadt.ukp.inception.support.test.websocket.WebSocketStompTestClient;
import de.tudarmstadt.ukp.inception.websocket.config.WebsocketAutoConfiguration;
import de.tudarmstadt.ukp.inception.websocket.config.WebsocketSecurityConfig;
import jakarta.persistence.EntityManager;

@SpringBootTest( //
        webEnvironment = RANDOM_PORT, //
        properties = { //
                "server.address=127.0.0.1", //
                "spring.main.banner-mode=off", //
                "assistant.enabled=true", //
                "websocket.enabled=true" })
@SpringBootApplication( //
        exclude = { //
                LiquibaseAutoConfiguration.class, //
                SearchServiceAutoConfiguration.class, //
                EventLoggingAutoConfiguration.class })
@ImportAutoConfiguration({ //
        SecurityAutoConfiguration.class, //
        WebsocketAutoConfiguration.class, //
        WebsocketSecurityConfig.class, //
        ProjectServiceAutoConfiguration.class, //
        CasStorageServiceAutoConfiguration.class, //
        RepositoryAutoConfiguration.class, //
        CasDoctorAutoConfiguration.class, //
        DocumentImportExportServiceAutoConfiguration.class, //
        DocumentServiceAutoConfiguration.class })
@EntityScan({ //
        "de.tudarmstadt.ukp.clarin.webanno.model", //
        "de.tudarmstadt.ukp.clarin.webanno.security.model" })
class AssistantServiceImplTest
{
    private static final Logger LOG = getLogger(lookup().lookupClass());

    private static final String USER = "user";
    private static final String PASS = "pass";

    private @LocalServerPort int port;
    private String websocketUrl;

    private @Autowired ProjectService projectService;
    private @Autowired RepositoryProperties repositoryProperties;
    private @Autowired EntityManager entityManager;
    private @Autowired UserDao userService;
    private @Autowired AssistantServiceImpl assistantService;

    private static @TempDir File repositoryDir;

    private static User user;
    private static Project project;

    @BeforeEach
    void setup() throws Exception
    {
        websocketUrl = "ws://localhost:" + port + WS_ENDPOINT;

        setupOnce();
    }

    private void setupOnce() throws IOException
    {
        if (project != null) {
            return;
        }

        repositoryProperties.setPath(repositoryDir);
        MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

        user = new User(USER, ROLE_USER);
        user.setPassword(PASS);
        userService.create(user);

        project = new Project("test-project");
        projectService.createProject(project);
    }

    @AfterEach
    void tearDown()
    {
        entityManager.clear();
    }

    @Test
    void thatSubscriptionWithoutProjectPermissionIsRejected() throws Exception
    {
        projectService.revokeRole(project, user, MANAGER);

        var channel = "/topic" + AssistantWebsocketController.getChannel(project);

        try (var client = new WebSocketStompTestClient(USER, PASS)) {
            client.expectSuccessfulConnection().connect(websocketUrl);
            client.expectError(
                    "Failed to send message to ExecutorSubscribableChannel[clientInboundChannel]")
                    .subscribe(channel);
        }
    }

    @Test
    void thatSubscriptionWithProjectPermissionIsAccepted() throws Exception
    {
        projectService.assignRole(project, user, MANAGER);

        var channel = "/topic" + AssistantWebsocketController.getChannel(project);

        var message = MTextMessage.builder() //
                .withRole("assistant") //
                .withMessage("Test message") //
                .build();

        try (var client = new WebSocketStompTestClient(USER, PASS)) {
            client.expectSuccessfulConnection().connect(websocketUrl);
            client.subscribe(channel);
            client.expect(message).perform(() -> {
                assistantService.dispatchMessage(user.getUsername(), project, message);
            });
        }
    }

    @Test
    void thatMessageSentIsRecieved() throws Exception
    {
        projectService.assignRole(project, user, ANNOTATOR);

        var broadcastChannel = "/topic" + AssistantWebsocketController.getChannel(project);

        var message = MTextMessage.builder() //
                .withRole("assistant") //
                .withMessage("Test message") //
                .build();

        try (var client = new WebSocketStompTestClient(USER, PASS)) {
            client.expectSuccessfulConnection().connect(websocketUrl);
            client.subscribe(broadcastChannel);
            client.expect(message).perform(() -> {
                client.send(broadcastChannel, message);
            });
        }
    }

    @SpringBootConfiguration
    public static class SpringConfig
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
    }
}
