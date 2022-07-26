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
import static de.tudarmstadt.ukp.inception.websocket.config.WebsocketConfig.WS_ENDPOINT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.apache.tomcat.websocket.Constants.WS_AUTHENTICATION_PASSWORD;
import static org.apache.tomcat.websocket.Constants.WS_AUTHENTICATION_USER_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.persistence.EntityManager;
import javax.sql.DataSource;

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
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.PreRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.ExtensiblePermissionEvaluator;
import de.tudarmstadt.ukp.clarin.webanno.security.InceptionDaoAuthenticationProvider;
import de.tudarmstadt.ukp.clarin.webanno.security.OverridableUserDetailsManager;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.ApplicationContextProvider;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;
import de.tudarmstadt.ukp.clarin.webanno.text.config.TextFormatsAutoConfiguration;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageSession;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.diam.messages.MViewportInit;
import de.tudarmstadt.ukp.inception.diam.messages.MViewportUpdate;
import de.tudarmstadt.ukp.inception.diam.model.websocket.ViewportDefinition;
import de.tudarmstadt.ukp.inception.documents.config.DocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.export.config.DocumentImportExportServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VRange;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VSpan;
import de.tudarmstadt.ukp.inception.schema.config.AnnotationSchemaServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.websocket.config.WebsocketAutoConfiguration;
import de.tudarmstadt.ukp.inception.websocket.config.WebsocketSecurityConfig;
import de.tudarmstadt.ukp.inception.websocket.config.stomp.LambdaStompFrameHandler;
import de.tudarmstadt.ukp.inception.websocket.config.stomp.LoggingStompSessionHandlerAdapter;

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
        WebsocketAutoConfiguration.class, //
        ProjectServiceAutoConfiguration.class, //
        DocumentServiceAutoConfiguration.class, //
        CasStorageServiceAutoConfiguration.class, //
        RepositoryAutoConfiguration.class, //
        AnnotationSchemaServiceAutoConfiguration.class, //
        TextFormatsAutoConfiguration.class, //
        DocumentImportExportServiceAutoConfiguration.class })
@EntityScan({ //
        "de.tudarmstadt.ukp.clarin.webanno.model", //
        "de.tudarmstadt.ukp.clarin.webanno.security.model", //
        "de.tudarmstadt.ukp.inception.log.model" })
public class DiamWebsocketController_ViewportRoutingTest
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String USER = "user";
    private static final String PASS = "pass";

    private WebSocketStompClient stompClient;
    private @LocalServerPort int port;
    private String websocketUrl;

    private @Autowired DiamWebsocketController sut;

    private @Autowired ProjectService projectService;
    private @Autowired DocumentService documentService;
    private @Autowired RepositoryProperties repositoryProperties;
    private @Autowired EntityManager entityManager;
    private @Autowired UserDao userService;

    // temporarily store data for test project
    @TempDir
    File repositoryDir;

    private User user;
    private Project testProject;
    private SourceDocument testDocument;
    private AnnotationDocument testAnnotationDocument;

    @BeforeEach
    public void setup() throws Exception
    {
        // create websocket client
        websocketUrl = "ws://localhost:" + port + WS_ENDPOINT;

        StandardWebSocketClient wsClient = new StandardWebSocketClient();
        wsClient.setUserProperties(Map.of( //
                WS_AUTHENTICATION_USER_NAME, USER, //
                WS_AUTHENTICATION_PASSWORD, PASS));
        stompClient = new WebSocketStompClient(wsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        repositoryProperties.setPath(repositoryDir);
        MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

        user = new User(USER, Role.ROLE_USER);
        user.setPassword(PASS);
        userService.create(user);

        testProject = new Project("test-project");
        projectService.createProject(testProject);
        projectService.assignRole(testProject, user, ANNOTATOR);

        testDocument = new SourceDocument("test", testProject, "text");
        documentService.createSourceDocument(testDocument);

        testAnnotationDocument = new AnnotationDocument(USER, testDocument);
        documentService.createAnnotationDocument(testAnnotationDocument);

        try (var session = CasStorageSession.open()) {
            documentService.uploadSourceDocument(toInputStream("This is a test.", UTF_8),
                    testAnnotationDocument.getDocument());
        }
    }

    @AfterEach
    public void tearDown()
    {
        entityManager.clear();
    }

    @Test
    public void thatViewportBasedMessageRoutingWorks()
        throws InterruptedException, ExecutionException, TimeoutException
    {
        CountDownLatch subscriptionDone = new CountDownLatch(2);
        CountDownLatch initDone = new CountDownLatch(2);

        ViewportDefinition vpd1 = new ViewportDefinition(testAnnotationDocument, 10, 20);
        ViewportDefinition vpd2 = new ViewportDefinition(testAnnotationDocument, 30, 40);

        var sessionHandler1 = new SessionHandler(subscriptionDone, initDone, vpd1);
        var sessionHandler2 = new SessionHandler(subscriptionDone, initDone, vpd2);

        // try {
        WebSocketHttpHeaders wsHeaders = new WebSocketHttpHeaders();
        wsHeaders.setBasicAuth(USER, "pass");
        StompSession session1 = stompClient.connect(websocketUrl, sessionHandler1).get(1000,
                SECONDS);
        StompSession session2 = stompClient.connect(websocketUrl, sessionHandler2).get(1000,
                SECONDS);
        // }
        // catch (Exception e) {
        // Thread.sleep(Duration.of(3, ChronoUnit.HOURS).toMillis());
        // }

        try {
            subscriptionDone.await(5, TimeUnit.SECONDS);
            assertThat(subscriptionDone.getCount()).isEqualTo(0);

            initDone.await(5, TimeUnit.SECONDS);
            assertThat(initDone.getCount()).isEqualTo(0);

            sut.sendUpdate(testAnnotationDocument, 12, 15);
            sut.sendUpdate(testAnnotationDocument, 31, 33);
            sut.sendUpdate(testAnnotationDocument, 15, 35);

            Thread.sleep(Duration.of(3, ChronoUnit.SECONDS).toMillis());

            assertThat(sessionHandler1.getRecieved()).containsExactly("12-15", "15-35");
            assertThat(sessionHandler2.getRecieved()).containsExactly("31-33", "15-35");
        }
        finally {
            session2.disconnect();
            session1.disconnect();
        }
    }

    private static class SessionHandler
        extends LoggingStompSessionHandlerAdapter
    {
        private final CountDownLatch subscriptionDoneLatch;
        private final CountDownLatch initDoneLatch;
        private final ViewportDefinition vpd;

        private final List<String> recieved = new ArrayList<>();

        public SessionHandler(CountDownLatch aSubscriptionDoneLatch, CountDownLatch aInitDoneLatch,
                ViewportDefinition aVpd)
        {
            super(LOG);
            subscriptionDoneLatch = aSubscriptionDoneLatch;
            initDoneLatch = aInitDoneLatch;
            vpd = aVpd;
        }

        @Override
        public void afterConnected(StompSession aSession, StompHeaders aConnectedHeaders)
        {
            aSession.subscribe("/app" + vpd.getTopic(),
                    LambdaStompFrameHandler.handleFrame(MViewportInit.class, this::onInit));
            aSession.subscribe("/topic" + vpd.getTopic(),
                    LambdaStompFrameHandler.handleFrame(MViewportUpdate.class, this::onUpdate));
            subscriptionDoneLatch.countDown();
        }

        public void onInit(StompHeaders aHeaders, MViewportInit aPayload)
        {
            initDoneLatch.countDown();
        }

        public void onUpdate(StompHeaders aHeaders, MViewportUpdate aPayload)
        {
            recieved.add(aPayload.getBegin() + "-" + aPayload.getEnd());
        }

        public List<String> getRecieved()
        {
            return recieved;
        }
    }

    // /**
    // * Test does not check correct authentication for websocket messages, instead we allow all to
    // * test communication assuming an authenticated user
    // */
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
                UserDetailsManager aUserDetailsManager)
        {
            DaoAuthenticationProvider authProvider = new InceptionDaoAuthenticationProvider();
            authProvider.setUserDetailsService(aUserDetailsManager);
            authProvider.setPasswordEncoder(aEncoder);
            return authProvider;
        }

        @Bean
        public UserDetailsManager userDetailsService(DataSource aDataSource,
                @Lazy AuthenticationManager aAuthenticationManager)
        {
            OverridableUserDetailsManager manager = new OverridableUserDetailsManager();
            manager.setDataSource(aDataSource);
            manager.setAuthenticationManager(aAuthenticationManager);
            return manager;
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
                    AnnotationLayer layer = new AnnotationLayer();
                    layer.setId(1l);
                    aResponse.add(new VSpan(layer, new VID(1), "dummy",
                            new VRange(aRequest.getWindowBeginOffset(),
                                    aRequest.getWindowEndOffset()),
                            emptyMap()));
                }
            };
        }
    }
}
