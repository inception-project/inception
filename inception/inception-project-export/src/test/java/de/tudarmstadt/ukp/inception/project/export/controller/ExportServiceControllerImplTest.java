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
package de.tudarmstadt.ukp.inception.project.export.controller;

import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_USER;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.inception.websocket.config.WebsocketConfig.WS_ENDPOINT;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.tomcat.websocket.Constants.WS_AUTHENTICATION_PASSWORD;
import static org.apache.tomcat.websocket.Constants.WS_AUTHENTICATION_USER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import java.io.File;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.EntityManager;

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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.converter.GenericMessageConverter;
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
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.config.DocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.project.export.config.ProjectExportServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.schema.config.AnnotationSchemaServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.support.findbugs.SuppressFBWarnings;
import de.tudarmstadt.ukp.inception.websocket.config.WebsocketAutoConfiguration;
import de.tudarmstadt.ukp.inception.websocket.config.WebsocketConfig;
import de.tudarmstadt.ukp.inception.websocket.config.WebsocketSecurityConfig;

@SpringBootTest( //
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, //
        properties = { //
                "spring.main.banner-mode=off", //
                "websocket.enabled=true" })
@SpringBootApplication( //
        exclude = { //
                LiquibaseAutoConfiguration.class })
@ImportAutoConfiguration({ //
        SecurityAutoConfiguration.class, //
        WebsocketSecurityConfig.class, //
        WebsocketAutoConfiguration.class, //
        ProjectServiceAutoConfiguration.class, //
        ProjectExportServiceAutoConfiguration.class, //
        DocumentServiceAutoConfiguration.class, //
        CasStorageServiceAutoConfiguration.class, //
        RepositoryAutoConfiguration.class, //
        AnnotationSchemaServiceAutoConfiguration.class })
@EntityScan({ //
        "de.tudarmstadt.ukp.clarin.webanno.model", //
        "de.tudarmstadt.ukp.clarin.webanno.security.model", //
        "de.tudarmstadt.ukp.inception.log.model" })
class ExportServiceControllerImplTest
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

    // temporarily store data for test project
    private static @TempDir File repositoryDir;
    private static Project testProject;
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
        stompClient.setMessageConverter(new GenericMessageConverter());

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
        projectService.revokeRole(testProject, user, PermissionLevel.MANAGER);

        CountDownLatch responseRecievedLatch = new CountDownLatch(1);
        AtomicBoolean messageRecieved = new AtomicBoolean(false);
        AtomicBoolean errorRecieved = new AtomicBoolean(false);

        SessionHandler sessionHandler = new SessionHandler(responseRecievedLatch, messageRecieved,
                errorRecieved);

        StompSession session = stompClient.connect(websocketUrl, sessionHandler).get(1, SECONDS);

        responseRecievedLatch.await(20, SECONDS);
        try {
            session.disconnect();
        }
        catch (Exception e) {
            // Ignore exceptions during disconnect
        }

        assertThat(messageRecieved).isFalse();
        assertThat(sessionHandler.errorMsg).containsIgnoringCase("access is denied");
        assertThat(errorRecieved).isTrue();
    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
    @Test
    void thatSubscriptionWithProjectPermissionIsAccepted() throws Exception
    {
        projectService.assignRole(testProject, user, PermissionLevel.MANAGER);

        CountDownLatch responseRecievedLatch = new CountDownLatch(1);
        AtomicBoolean messageRecieved = new AtomicBoolean(false);
        AtomicBoolean errorRecieved = new AtomicBoolean(false);

        SessionHandler sessionHandler = new SessionHandler(responseRecievedLatch, messageRecieved,
                errorRecieved);

        StompSession session = stompClient.connect(websocketUrl, sessionHandler).get(5, SECONDS);

        responseRecievedLatch.await(20, SECONDS);
        session.disconnect();

        assertThat(messageRecieved).isTrue();
        assertThat(sessionHandler.errorMsg).isNull();
        assertThat(errorRecieved).isFalse();
    }

    private final class SessionHandler
        extends StompSessionHandlerAdapter
    {
        private final AtomicBoolean errorRecieved;
        private final AtomicBoolean messageRecieved;
        private final CountDownLatch responseRecievedLatch;

        private String errorMsg;

        private SessionHandler(CountDownLatch aResponseRecievedLatch,
                AtomicBoolean aMessageRecieved, AtomicBoolean aErrorRecieved)
        {
            responseRecievedLatch = aResponseRecievedLatch;
            messageRecieved = aMessageRecieved;
            errorRecieved = aErrorRecieved;
        }

        @Override
        public void afterConnected(StompSession aSession, StompHeaders aConnectedHeaders)
        {
            aSession.subscribe("/app" + NS_PROJECT + "/" + testProject.getId() + "/exports",
                    new StompFrameHandler()
                    {
                        @Override
                        public Type getPayloadType(StompHeaders aHeaders)
                        {
                            return Object.class;
                        }

                        @Override
                        public void handleFrame(StompHeaders aHeaders, Object aPayload)
                        {
                            LOG.info("GOT MESSAGE: {}", aPayload);
                            responseRecievedLatch.countDown();
                            messageRecieved.set(true);
                        }
                    });
        }

        @Override
        public void handleFrame(StompHeaders aHeaders, Object aPayload)
        {
            LOG.error("Error: {}", aHeaders.get("message"));
            errorMsg = aHeaders.getFirst("message");
            errorRecieved.set(true);
            responseRecievedLatch.countDown();
        }

        @Override
        public void handleException(StompSession aSession, StompCommand aCommand,
                StompHeaders aHeaders, byte[] aPayload, Throwable aException)
        {
            LOG.error("Exception: {}", aException.getMessage(), aException);
            errorMsg = aException.getMessage();
            errorRecieved.set(true);
            responseRecievedLatch.countDown();
        }

        @Override
        public void handleTransportError(StompSession aSession, Throwable aException)
        {
            LOG.error("Transport error: {}", aException.getMessage(), aException);
            errorMsg = aException.getMessage();
            errorRecieved.set(true);
            responseRecievedLatch.countDown();
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
