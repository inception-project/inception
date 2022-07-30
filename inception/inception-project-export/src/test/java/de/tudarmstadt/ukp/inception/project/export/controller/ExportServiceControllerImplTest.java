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

import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.inception.websocket.config.WebsocketConfig.WS_ENDPOINT;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.converter.GenericMessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.config.DocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.project.export.config.ProjectExportServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.project.export.model.MProjectExportStateUpdate;
import de.tudarmstadt.ukp.inception.schema.config.AnnotationSchemaServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.support.findbugs.SuppressFBWarnings;
import de.tudarmstadt.ukp.inception.websocket.config.WebsocketAutoConfiguration;
import de.tudarmstadt.ukp.inception.websocket.config.WebsocketConfig;

@SpringBootTest( //
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, //
        properties = { //
                "spring.main.banner-mode=off", //
                "websocket.enabled=true", //
                "websocket.loggedevent.enabled=true", //
                "event-logging.enabled=true" })
@SpringBootApplication( //
        exclude = { //
                LiquibaseAutoConfiguration.class, //
                SecurityAutoConfiguration.class })
@Import({ //
        de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration.class, //
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
public class ExportServiceControllerImplTest
{
    private static final String USER = "user";

    private WebSocketStompClient webSocketClient;
    private @LocalServerPort int port;
    private String websocketUrl;
    private StompSession session;

    private @Autowired SimpMessagingTemplate msgTemplate;
    private @Autowired ProjectService projectService;
    private @Autowired RepositoryProperties repositoryProperties;
    private @Autowired EntityManager entityManager;
    private @Autowired UserDao userService;

    // temporarily store data for test project
    @TempDir
    File repositoryDir;

    private Project testProject;

    @BeforeEach
    public void setup() throws IOException
    {
        // create websocket client
        websocketUrl = "ws://localhost:" + port + WS_ENDPOINT;
        webSocketClient = new WebSocketStompClient(new StandardWebSocketClient());
        // webSocketClient.setMessageConverter(new MappingJackson2MessageConverter());
        webSocketClient.setMessageConverter(new GenericMessageConverter());

        repositoryProperties.setPath(repositoryDir);
        MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

        userService.create(new User(USER));

        testProject = new Project("test-project");
        projectService.createProject(testProject);
    }

    @AfterEach
    public void tearDown()
    {
        entityManager.clear();
    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
    @Test
    public void thatSubscriptionWithoutProjectPermissionIsRejected()
        throws InterruptedException, ExecutionException, TimeoutException
    {
        AtomicBoolean errorRecieved = new AtomicBoolean(false);
        AtomicBoolean messageRecieved = new AtomicBoolean(false);
        CountDownLatch subscriptionDoneLatch = new CountDownLatch(1);
        CountDownLatch messageSentLatch = new CountDownLatch(1);
        StringBuffer errorMessage = new StringBuffer();
        StompSessionHandlerAdapter sessionHandler = new StompSessionHandlerAdapter()
        {
            @Override
            public void afterConnected(StompSession aSession, StompHeaders aConnectedHeaders)
            {
                aSession.subscribe("/user/queue/errors", new StompFrameHandler()
                {
                    @Override
                    public Type getPayloadType(StompHeaders aHeaders)
                    {
                        return Object.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders aHeaders, Object aPayload)
                    {
                        String payload = new String((byte[]) aPayload, StandardCharsets.UTF_8);
                        errorMessage.append(payload);
                        errorRecieved.set(true);
                        subscriptionDoneLatch.countDown();
                    }
                });

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
                                System.out.println(aPayload);
                                messageRecieved.set(true);
                            }
                        });

                MProjectExportStateUpdate msg = new MProjectExportStateUpdate();
                msgTemplate.convertAndSend(
                        "/topic" + NS_PROJECT + "/" + testProject.getId() + "/exports", msg);

                try {
                    Thread.sleep(Duration.ofSeconds(3).toMillis());
                }
                catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                messageSentLatch.countDown();
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
        };

        session = webSocketClient.connect(websocketUrl, sessionHandler).get(1, TimeUnit.SECONDS);
        subscriptionDoneLatch.await(20, TimeUnit.SECONDS);
        messageSentLatch.await(20, TimeUnit.SECONDS);
        session.disconnect();

        assertThat(errorRecieved).isTrue();
        assertThat(messageRecieved).isFalse();
        assertThat(errorMessage.toString().trim()).isEqualTo("Access denied");
    }

    /**
     * Test does not check correct authentication for websocket messages, instead we allow all to
     * test communication assuming an authenticated user
     */
    @Configuration
    public static class WebsocketSecurityTestConfig
        extends AbstractSecurityWebSocketMessageBrokerConfigurer
    {

        @Override
        protected void configureInbound(MessageSecurityMetadataSourceRegistry aMessages)
        {
            aMessages.anyMessage().permitAll();
        }

        @Override
        protected boolean sameOriginDisabled()
        {
            return true;
        }
    }

    @SpringBootConfiguration
    public static class WebsocketBrokerTestConfig
    {
        @Primary
        @Bean
        public WebSocketMessageBrokerConfigurer webSocketMessageBrokerConfigurer()
        {
            return new WebsocketConfig()
            {
                @Override
                public void registerStompEndpoints(StompEndpointRegistry aRegistry)
                {
                    aRegistry.addEndpoint(WS_ENDPOINT)
                            .setHandshakeHandler(new DefaultHandshakeHandler()
                            {
                                @Override
                                protected Principal determineUser(ServerHttpRequest aRequest,
                                        WebSocketHandler aWsHandler,
                                        Map<String, Object> aAttributes)
                                {
                                    return new UsernamePasswordAuthenticationToken(USER, "dummyPw",
                                            emptyList());
                                }
                            });
                }
            };
        }
    }
}
