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
package de.tudarmstadt.ukp.inception.websocket;

import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_ADMIN;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_USER;
import static de.tudarmstadt.ukp.inception.websocket.config.WebsocketConfig.WS_ENDPOINT;
import static java.lang.invoke.MethodHandles.lookup;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.tomcat.websocket.Constants.WS_AUTHENTICATION_PASSWORD;
import static org.apache.tomcat.websocket.Constants.WS_AUTHENTICATION_USER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import de.tudarmstadt.ukp.clarin.webanno.diag.config.CasDoctorAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.InceptionDaoAuthenticationProvider;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.InceptionSecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.documents.config.DocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.event.DocumentStateChangedEvent;
import de.tudarmstadt.ukp.inception.export.config.DocumentImportExportServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.log.adapter.DocumentStateChangedEventAdapter;
import de.tudarmstadt.ukp.inception.log.config.EventLoggingAutoConfiguration;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.schema.config.AnnotationSchemaServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.support.findbugs.SuppressFBWarnings;
import de.tudarmstadt.ukp.inception.support.logging.Logging;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationContextProvider;
import de.tudarmstadt.ukp.inception.websocket.config.WebsocketAutoConfiguration;
import de.tudarmstadt.ukp.inception.websocket.config.WebsocketSecurityConfig;
import de.tudarmstadt.ukp.inception.websocket.config.stomp.LoggingStompSessionHandlerAdapter;
import de.tudarmstadt.ukp.inception.websocket.model.LoggedEventMessage;
import jakarta.persistence.EntityManager;

@Disabled("Test fails sometimes and module is removed in 38.x")
@SpringBootTest( //
        webEnvironment = RANDOM_PORT, //
        properties = { //
                "server.address=127.0.0.1", //
                "spring.main.banner-mode=off", //
                "websocket.enabled=true", //
                "websocket.logged-events.enabled=true", //
                "event-logging.enabled=true" })
@SpringBootApplication( //
        exclude = { //
                LiquibaseAutoConfiguration.class })
@ImportAutoConfiguration({ //
        CasDoctorAutoConfiguration.class, //
        EventLoggingAutoConfiguration.class, //
        InceptionSecurityAutoConfiguration.class, //
        SecurityAutoConfiguration.class, //
        WebsocketAutoConfiguration.class, //
        WebsocketSecurityConfig.class, //
        ProjectServiceAutoConfiguration.class, //
        DocumentServiceAutoConfiguration.class, //
        CasStorageServiceAutoConfiguration.class, //
        RepositoryAutoConfiguration.class, //
        AnnotationSchemaServiceAutoConfiguration.class, //
        DocumentImportExportServiceAutoConfiguration.class })
@EntityScan({ //
        "de.tudarmstadt.ukp.clarin.webanno.model", //
        "de.tudarmstadt.ukp.clarin.webanno.security.model", //
        "de.tudarmstadt.ukp.inception.log.model" })
public class WebSocketIntegrationTest
{
    private static final Logger LOG = getLogger(lookup().lookupClass());

    private static final String USER = "user";
    private static final String PASS = "pass";

    private WebSocketStompClient stompClient;
    private @LocalServerPort int port;
    private String websocketUrl;
    private WebSocketHttpHeaders headers;
    private StompSession session;

    private @Autowired ApplicationEventPublisher applicationEventPublisher;
    private @Autowired DocumentService documentService;
    private @Autowired ProjectService projectService;
    private @Autowired RepositoryProperties repositoryProperties;
    private @Autowired EntityManager entityManager;
    private @Autowired UserDao userService;

    private static @TempDir File repositoryDir;

    private static User user;
    private static Project project;
    private static SourceDocument testDoc;

    @BeforeEach
    void setup() throws Exception
    {
        websocketUrl = "ws://localhost:" + port + WS_ENDPOINT;

        var wsClient = new StandardWebSocketClient();
        wsClient.setUserProperties(Map.of( //
                WS_AUTHENTICATION_USER_NAME, USER, //
                WS_AUTHENTICATION_PASSWORD, PASS));

        headers = new WebSocketHttpHeaders();
        headers.add("Authorization",
                "Basic " + Base64.getEncoder().encodeToString((USER + ":" + PASS).getBytes()));

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

        // WebsocketSecurityConfig limits access to logged events to the admin user!
        user = new User(USER, ROLE_USER, ROLE_ADMIN);
        user.setPassword(PASS);
        userService.create(user);

        project = new Project("test-project");
        projectService.createProject(project);

        testDoc = new SourceDocument("testDoc", project, "text");
        documentService.createSourceDocument(testDoc);
    }

    @AfterEach
    void tearDown()
    {
        entityManager.clear();
    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
    @Test
    void thatRecentMessageIsReceived() throws Exception
    {
        var receivedMessages = new ArrayList<LoggedEventMessage>();
        var latch = new CountDownLatch(1);
        var sessionHandler = new SessionHandler(latch, receivedMessages);

        session = stompClient.connectAsync(websocketUrl, headers, sessionHandler) //
                .get(5, SECONDS);
        // latch.await(10, SECONDS);

        await().atMost(ofSeconds(20)).alias("Message received ").untilAsserted(() -> {
            assertThat(receivedMessages.size()).isEqualTo(1);
            var msg1 = receivedMessages.get(0);
            assertThat(msg1.getEventType())
                    .isEqualTo(DocumentStateChangedEvent.class.getSimpleName());
        });

        try {
            session.disconnect();
        }
        catch (Exception e) {
            // Ignore exceptions during disconnect
        }
    }

    private class SessionHandler
        extends LoggingStompSessionHandlerAdapter
    {
        private final CountDownLatch latch;
        private final List<LoggedEventMessage> receivedMessages;

        private SessionHandler(CountDownLatch aLatch, List<LoggedEventMessage> aReceivedMessages)
        {
            super(LOG);
            latch = aLatch;
            receivedMessages = aReceivedMessages;
        }

        @Override
        public void afterConnected(StompSession aSession, StompHeaders aConnectedHeaders)
        {
            super.afterConnected(aSession, aConnectedHeaders);
            aSession.subscribe("/topic/loggedEvents", new StompFrameHandler()
            {
                @Override
                public Type getPayloadType(StompHeaders aHeaders)
                {
                    return LoggedEventMessage.class;
                }

                @Override
                public void handleFrame(StompHeaders aHeaders, Object aPayload)
                {
                    receivedMessages.add((LoggedEventMessage) aPayload);
                    latch.countDown();
                }
            });
            applicationEventPublisher.publishEvent(
                    new DocumentStateChangedEvent(new Object(), testDoc, SourceDocumentState.NEW));
        }

        @Override
        public void handleException(StompSession aSession, StompCommand aCommand,
                StompHeaders aHeaders, byte[] aPayload, Throwable aException)
        {
            LOG.error("Exception: {}", aException.getMessage(), aException);
            aException.printStackTrace();
        }

        @Override
        public void handleTransportError(StompSession aSession, Throwable aException)
        {
            LOG.error("Transport error: {}", aException.getMessage(), aException);
            aException.printStackTrace();
        }
    }

    @Configuration
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

        @Bean
        AuthenticationEventPublisher authenticationEventPublisher()
        {
            return new DefaultAuthenticationEventPublisher();
        }

        @Bean
        public DocumentStateChangedEventAdapter documentStateChangedEventAdapter()
        {
            return new DocumentStateChangedEventAdapter();
        }

    }
}
