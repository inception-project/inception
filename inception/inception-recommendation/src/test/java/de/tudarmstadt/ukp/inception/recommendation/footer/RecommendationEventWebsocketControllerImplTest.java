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
package de.tudarmstadt.ukp.inception.recommendation.footer;

import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_USER;
import static de.tudarmstadt.ukp.inception.websocket.config.WebsocketConfig.WS_ENDPOINT;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.tomcat.websocket.Constants.WS_AUTHENTICATION_PASSWORD;
import static org.apache.tomcat.websocket.Constants.WS_AUTHENTICATION_USER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import java.io.File;
import java.util.Map;

import javax.persistence.EntityManager;

import org.awaitility.Awaitility;
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
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.ExtensiblePermissionEvaluator;
import de.tudarmstadt.ukp.clarin.webanno.security.InceptionDaoAuthenticationProvider;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.ApplicationContextProvider;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommenderTaskNotificationEvent;
import de.tudarmstadt.ukp.inception.schema.config.AnnotationSchemaServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.support.findbugs.SuppressFBWarnings;
import de.tudarmstadt.ukp.inception.support.test.websocket.WebSocketSessionTestHandler;
import de.tudarmstadt.ukp.inception.websocket.config.WebsocketAutoConfiguration;
import de.tudarmstadt.ukp.inception.websocket.config.WebsocketConfig;
import de.tudarmstadt.ukp.inception.websocket.config.WebsocketSecurityConfig;

@SpringBootTest( //
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, //
        properties = { //
                "spring.main.banner-mode=off", //
                "websocket.enabled=true", //
                "websocket.recommender-events.enabled=true" })
@SpringBootApplication( //
        exclude = { //
                LiquibaseAutoConfiguration.class })
@ImportAutoConfiguration({ //
        SecurityAutoConfiguration.class, //
        WebsocketSecurityConfig.class, //
        WebsocketAutoConfiguration.class, //
        ProjectServiceAutoConfiguration.class, //
        RepositoryAutoConfiguration.class, //
        AnnotationSchemaServiceAutoConfiguration.class })
@EntityScan({ //
        "de.tudarmstadt.ukp.clarin.webanno.model", //
        "de.tudarmstadt.ukp.clarin.webanno.security.model", //
        "de.tudarmstadt.ukp.inception.log.model" })
class RecommendationEventWebsocketControllerImplTest
{
    private static final Logger LOG = getLogger(lookup().lookupClass());

    private static final String USER = "user";
    private static final String PASS = "pass";

    private WebSocketStompClient stompClient;
    private @LocalServerPort int port;
    private String websocketUrl;

    private @Autowired ProjectService projectService;
    private @Autowired RepositoryProperties repositoryProperties;
    private @Autowired EntityManager entityManager;
    private @Autowired UserDao userService;
    private @Autowired ApplicationEventPublisher appEventPublisher;

    // temporarily store data for test project
    private static @TempDir File repositoryDir;
    private static Project project;
    private static User user;

    @BeforeEach
    void setup() throws Exception
    {
        // create websocket client
        websocketUrl = "ws://localhost:" + port + WS_ENDPOINT;

        var wsClient = new StandardWebSocketClient();
        wsClient.setUserProperties(Map.of( //
                WS_AUTHENTICATION_USER_NAME, USER, //
                WS_AUTHENTICATION_PASSWORD, PASS));
        stompClient = new WebSocketStompClient(wsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        setupOnce();
    }

    void setupOnce() throws Exception
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

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
    @Test
    void thatSubscriptionWithoutProjectPermissionIsRejected() throws Exception
    {
        projectService.revokeRole(project, user, PermissionLevel.MANAGER);

        var channel = "/topic" + RecommendationEventWebsocketControllerImpl.getChannel(project,
                user.getUsername());
        var sessionHandler = WebSocketSessionTestHandler.builder() //
                .subscribe(channel) //
                .afterConnected(this::sendTestMessage) //
                .expect(RRecommenderLogMessage.class, (headers, msg) -> {
                    assertThat(msg.getMessage()).isEqualTo("Test message");
                }) //
                .build();

        StompSession session = stompClient.connect(websocketUrl, sessionHandler).get(5, SECONDS);
        Awaitility.await().atMost(20, SECONDS).until(sessionHandler::messagesProcessed);
        try {
            session.disconnect();
        }
        catch (Exception e) {
            // Ignore exceptions during disconnect
        }

        sessionHandler
                .assertError(msg -> assertThat(msg).containsIgnoringCase("AccessDeniedException"));
    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
    @Test
    void thatSubscriptionAsOtherUserIsRejected() throws Exception
    {
        var channel = "/topic" + RecommendationEventWebsocketControllerImpl.getChannel(project,
                "USER_WITHOUT_ACCESS");
        var sessionHandler = WebSocketSessionTestHandler.builder() //
                .subscribe(channel) //
                .afterConnected(this::sendTestMessage) //
                .expect(RRecommenderLogMessage.class, (headers, msg) -> {
                    assertThat(msg.getMessage()).isEqualTo("Test message");
                }) //
                .build();

        StompSession session = stompClient.connect(websocketUrl, sessionHandler).get(5, SECONDS);
        Awaitility.await().atMost(20, SECONDS).until(sessionHandler::messagesProcessed);
        try {
            session.disconnect();
        }
        catch (Exception e) {
            // Ignore exceptions during disconnect
        }

        sessionHandler
                .assertError(msg -> assertThat(msg).containsIgnoringCase("AccessDeniedException"));
    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
    @Test
    void thatSubscriptionWithProjectPermissionIsAccepted() throws Exception
    {
        projectService.assignRole(project, user, PermissionLevel.MANAGER);

        var channel = "/topic" + RecommendationEventWebsocketControllerImpl.getChannel(project,
                user.getUsername());
        var sessionHandler = WebSocketSessionTestHandler.builder() //
                .subscribe(channel) //
                .afterConnected(this::sendTestMessage)
                .expect(RRecommenderLogMessage.class, (headers, msg) -> {
                    assertThat(msg.getMessage()).isEqualTo("Test message");
                }) //
                .build();

        StompSession session = stompClient.connect(websocketUrl, sessionHandler).get(5, SECONDS);
        Awaitility.await().atMost(20, SECONDS).until(sessionHandler::messagesProcessed);
        session.disconnect();

        sessionHandler.assertSuccess();
    }

    private void sendTestMessage()
    {
        appEventPublisher.publishEvent(
                RecommenderTaskNotificationEvent.builder(this, project, user.getUsername()) //
                        .withMessage(LogMessage.info(this, "Test message")) //
                        .build());
    }

    @Configuration
    public static class WebsocketSecurityTestConfig
        extends WebsocketSecurityConfig
    {
        @Autowired
        public WebsocketSecurityTestConfig(ApplicationContext aContext,
                ExtensiblePermissionEvaluator aPermissionEvaluator)
        {
            super(aContext, aPermissionEvaluator);
        }
    }

    @SpringBootConfiguration
    public static class WebsocketBrokerTestConfig
    {
        @Bean
        public ApplicationContextProvider applicationContextProvider()
        {
            return new ApplicationContextProvider();
        }

        @Bean(name = "authenticationProvider")
        public DaoAuthenticationProvider internalAuthenticationProvider(PasswordEncoder aEncoder,
                @Lazy UserDetailsManager aUserDetailsManager)
        {
            DaoAuthenticationProvider authProvider = new InceptionDaoAuthenticationProvider();
            authProvider.setUserDetailsService(aUserDetailsManager);
            authProvider.setPasswordEncoder(aEncoder);
            return authProvider;
        }

        @Order(100)
        @Bean
        public SecurityFilterChain wsFilterChain(HttpSecurity aHttp) throws Exception
        {
            aHttp.antMatcher(WebsocketConfig.WS_ENDPOINT);
            aHttp.authorizeHttpRequests() //
                    .requestMatchers("/**").authenticated() //
                    .anyRequest().denyAll();
            aHttp.sessionManagement() //
                    .sessionCreationPolicy(STATELESS);
            aHttp.httpBasic();
            return aHttp.build();
        }
    }
}
