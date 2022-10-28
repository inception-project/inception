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
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.tomcat.websocket.Constants.WS_AUTHENTICATION_PASSWORD;
import static org.apache.tomcat.websocket.Constants.WS_AUTHENTICATION_USER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
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
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.event.DocumentStateChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.ExtensiblePermissionEvaluator;
import de.tudarmstadt.ukp.clarin.webanno.security.InceptionDaoAuthenticationProvider;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.ApplicationContextProvider;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.config.DocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.export.config.DocumentImportExportServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.log.adapter.DocumentStateChangedEventAdapter;
import de.tudarmstadt.ukp.inception.schema.config.AnnotationSchemaServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.support.findbugs.SuppressFBWarnings;
import de.tudarmstadt.ukp.inception.websocket.config.WebsocketAutoConfiguration;
import de.tudarmstadt.ukp.inception.websocket.config.WebsocketConfig;
import de.tudarmstadt.ukp.inception.websocket.config.WebsocketSecurityConfig;
import de.tudarmstadt.ukp.inception.websocket.model.LoggedEventMessage;

@SpringBootTest( //
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, //
        properties = { //
                "spring.main.banner-mode=off", //
                "websocket.enabled=true", //
                "websocket.logged-events.enabled=true", //
                "event-logging.enabled=true" })
@SpringBootApplication( //
        exclude = { //
                LiquibaseAutoConfiguration.class, //
                SecurityAutoConfiguration.class })
@ImportAutoConfiguration({ //
        WebsocketAutoConfiguration.class, //
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
    private static final String USER = "user";
    private static final String PASS = "pass";

    private WebSocketStompClient stompClient;
    private @LocalServerPort int port;
    private String websocketUrl;
    private StompSession session;

    private @Autowired ApplicationEventPublisher applicationEventPublisher;
    private @Autowired DocumentService docService;
    private @Autowired ProjectService projectService;
    private @Autowired RepositoryProperties repositoryProperties;
    private @Autowired EntityManager entityManager;
    private @Autowired UserDao userService;

    @TempDir
    File repositoryDir;

    private User user;
    private Project testProject;
    private SourceDocument testDoc;

    @BeforeEach
    public void setup() throws IOException
    {
        // create websocket client
        websocketUrl = "ws://localhost:" + port + WS_ENDPOINT;
        var wsClient = new StandardWebSocketClient();
        wsClient.setUserProperties(Map.of( //
                WS_AUTHENTICATION_USER_NAME, USER, //
                WS_AUTHENTICATION_PASSWORD, PASS));
        stompClient = new WebSocketStompClient(wsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        createTestdata();
    }

    @AfterEach
    public void tearDown()
    {
        entityManager.clear();
    }

    private void createTestdata() throws IOException
    {
        repositoryProperties.setPath(repositoryDir);
        MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

        // WebsocketSecurityConfig limits access to logged events to the admin user!
        user = new User(USER, ROLE_USER, ROLE_ADMIN);
        user.setPassword(PASS);
        userService.create(user);

        testProject = new Project("test-project");
        testDoc = new SourceDocument("testDoc", testProject, "text");
        projectService.createProject(testProject);
        docService.createSourceDocument(testDoc);
    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
    @Test
    public void thatRecentMessageIsReceived()
        throws InterruptedException, ExecutionException, TimeoutException
    {
        List<LoggedEventMessage> receivedMessages = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        SessionHandler sessionHandler = new SessionHandler(latch, receivedMessages);

        session = stompClient.connect(websocketUrl, sessionHandler).get(5, SECONDS);
        latch.await(10, SECONDS);
        session.disconnect();

        assertThat(receivedMessages.size()).isEqualTo(1);
        LoggedEventMessage msg1 = receivedMessages.get(0);
        assertThat(msg1.getEventType()).isEqualTo(DocumentStateChangedEvent.class.getSimpleName());
    }

    private final class SessionHandler
        extends StompSessionHandlerAdapter
    {
        private final CountDownLatch latch;
        private final List<LoggedEventMessage> receivedMessages;

        private SessionHandler(CountDownLatch aLatch, List<LoggedEventMessage> aReceivedMessages)
        {
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
            System.out.println("StompSessionHandler: " + aException);
            aException.printStackTrace();
        }

        @Override
        public void handleTransportError(StompSession aSession, Throwable aException)
        {
            System.out.println("TransportError: " + aException);
            aException.printStackTrace();
        }
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

    @Configuration
    public static class SpringConfig
    {
        @Bean
        public DocumentStateChangedEventAdapter documentStateChangedEventAdapter()
        {
            return new DocumentStateChangedEventAdapter();
        }

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
            aHttp.authorizeRequests() //
                    .antMatchers("/**").authenticated() //
                    .anyRequest().denyAll();
            aHttp.sessionManagement() //
                    .sessionCreationPolicy(STATELESS);
            aHttp.httpBasic();
            return aHttp.build();
        }
    }
}
