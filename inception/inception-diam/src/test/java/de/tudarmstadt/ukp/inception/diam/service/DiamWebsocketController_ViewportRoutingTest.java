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
import static de.tudarmstadt.ukp.inception.diam.service.DiamWebsocketController.FORMAT_LEGACY;
import static de.tudarmstadt.ukp.inception.websocket.config.WebsocketConfig.WS_ENDPOINT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.apache.tomcat.websocket.Constants.WS_AUTHENTICATION_PASSWORD;
import static org.apache.tomcat.websocket.Constants.WS_AUTHENTICATION_USER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
import org.springframework.core.annotation.Order;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.config.AnnotationAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.PreRenderer;
import de.tudarmstadt.ukp.clarin.webanno.diag.config.CasDoctorAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.ExtensiblePermissionEvaluator;
import de.tudarmstadt.ukp.clarin.webanno.security.InceptionDaoAuthenticationProvider;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.InceptionSecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.text.config.TextFormatsAutoConfiguration;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageSession;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.diam.messages.MViewportInit;
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
import de.tudarmstadt.ukp.inception.support.findbugs.SuppressFBWarnings;
import de.tudarmstadt.ukp.inception.support.logging.Logging;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationContextProvider;
import de.tudarmstadt.ukp.inception.websocket.config.WebsocketAutoConfiguration;
import de.tudarmstadt.ukp.inception.websocket.config.WebsocketConfig;
import de.tudarmstadt.ukp.inception.websocket.config.WebsocketSecurityConfig;
import de.tudarmstadt.ukp.inception.websocket.config.stomp.LambdaStompFrameHandler;
import de.tudarmstadt.ukp.inception.websocket.config.stomp.LoggingStompSessionHandlerAdapter;
import jakarta.persistence.EntityManager;

@SpringBootTest( //
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, //
        properties = { //
                "spring.main.banner-mode=off", //
                "websocket.enabled=true" })
@SpringBootApplication( //
        exclude = { //
                LiquibaseAutoConfiguration.class })
@ImportAutoConfiguration({ //
        PreferencesServiceAutoConfig.class, //
        CasDoctorAutoConfiguration.class, //
        RenderingAutoConfig.class, //
        InceptionSecurityAutoConfiguration.class, //
        SecurityAutoConfiguration.class, //
        WebsocketAutoConfiguration.class, //
        ProjectServiceAutoConfiguration.class, //
        DocumentServiceAutoConfiguration.class, //
        CasStorageServiceAutoConfiguration.class, //
        RepositoryAutoConfiguration.class, //
        AnnotationSchemaServiceAutoConfiguration.class, //
        AnnotationAutoConfiguration.class, //
        TextFormatsAutoConfiguration.class, //
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
    private static @TempDir File repositoryDir;
    private static User user;
    private static Project testProject;
    private static SourceDocument testDocument;
    private static AnnotationDocument testAnnotationDocument;

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

        setupOnce();
    }

    void setupOnce() throws Exception
    {
        if (testProject != null) {
            return;
        }

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
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
    @Test
    public void thatViewportBasedMessageRoutingWorks() throws Exception
    {
        var subscriptionDone = new CountDownLatch(2);
        var initDone = new CountDownLatch(2);

        var vpd1 = new ViewportDefinition(testAnnotationDocument, 10, 20, FORMAT_LEGACY);
        var vpd2 = new ViewportDefinition(testAnnotationDocument, 30, 40, FORMAT_LEGACY);

        var sessionHandler1 = new SessionHandler(subscriptionDone, initDone, vpd1);
        var sessionHandler2 = new SessionHandler(subscriptionDone, initDone, vpd2);

        // try {
        var session1 = stompClient.connect(websocketUrl, sessionHandler1).get(1000, SECONDS);
        var session2 = stompClient.connect(websocketUrl, sessionHandler2).get(1000, SECONDS);
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
            try {
                session1.disconnect();
            }
            catch (Exception e) {
                // Ignore exceptions during disconnect
            }
            try {
                session2.disconnect();
            }
            catch (Exception e) {
                // Ignore exceptions during disconnect
            }
        }
    }

    private static class SessionHandler
        extends LoggingStompSessionHandlerAdapter
    {
        private final CountDownLatch subscriptionDoneLatch;
        private final CountDownLatch initDoneLatch;
        private final ViewportDefinition vpd;

        private final List<String> received = new ArrayList<>();

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
            received.add(aPayload.getBegin() + "-" + aPayload.getEnd());
        }

        public List<String> getRecieved()
        {
            return received;
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
            var authProvider = new InceptionDaoAuthenticationProvider();
            authProvider.setUserDetailsService(aUserDetailsManager);
            authProvider.setPasswordEncoder(aEncoder);
            return authProvider;
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

        @Order(100)
        @Bean
        public SecurityFilterChain wsFilterChain(HttpSecurity aHttp) throws Exception
        {
            aHttp.securityMatcher(WebsocketConfig.WS_ENDPOINT);
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
