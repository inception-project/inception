package de.tudarmstadt.ukp.inception.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.event.DocumentStateChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;
import de.tudarmstadt.ukp.inception.websocket.model.LoggedEventMessage;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "websocket.enabled=true", "websocket.loggedevent.enabled=true"})
@SpringBootApplication(exclude = { LiquibaseAutoConfiguration.class, SecurityAutoConfiguration.class })
@ContextConfiguration(classes = WebSocketTestConfig.class)
public class WebSocketIntegrationTest
{
    private WebSocketStompClient webSocketClient;
    private @LocalServerPort int port;
    private String websocketUrl;
    private StompSession session;
    
    private @Autowired ApplicationEventPublisher applicationEventPublisher;
    private @Autowired DocumentService docService;
    private @Autowired ProjectService projectService;
    private @Autowired RepositoryProperties repositoryProperties;
    private @Autowired EntityManager entityManager;
    // temporarily store data for test project
    @TempDir
    File repositoryDir;
    
    private User testAdmin;
    private Project testProject;
    private SourceDocument testDoc;
    
    @BeforeEach
    public void setup() throws IOException
    {
        // create websocket client
        websocketUrl = "ws://localhost:" + port + "/ws-endpoint";
        webSocketClient = new WebSocketStompClient(new StandardWebSocketClient());
        webSocketClient.setMessageConverter(new MappingJackson2MessageConverter());
        createTestdata();
    }
    
    @AfterEach
    public void tearDown() {
        entityManager.clear();
    }

    private void createTestdata() throws IOException
    {
        repositoryProperties.setPath(repositoryDir);
        MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
        
        testAdmin = new User("testAdmin", Role.ROLE_USER, Role.ROLE_ADMIN);
        testProject = new Project("testProject");
        testDoc = new SourceDocument("testDoc", testProject, "text");
        projectService.createProject(testProject);
        docService.createSourceDocument(testDoc);
    }
    
    @Test
    public void thatRecentMessageIsReceived() throws InterruptedException, ExecutionException, TimeoutException
    {
        List<LoggedEventMessage> receivedMessages = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        StompSessionHandlerAdapter sessionHandler = new StompSessionHandlerAdapter() {

            @Override
            public void afterConnected(StompSession aSession, StompHeaders aConnectedHeaders)
            {
                super.afterConnected(aSession, aConnectedHeaders);
                System.out.println("Connected to websocket");
                aSession.subscribe("topic/loggedEvents", new StompFrameHandler()
                {
                    @Override
                    public Type getPayloadType(StompHeaders aHeaders)
                    {
                        return LoggedEventMessage.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders aHeaders, Object aPayload)
                    {
                        System.out.println("Subscription payload: " + aPayload);
                        receivedMessages.add((LoggedEventMessage) aPayload);
                        latch.countDown();
                    }
                });                
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
        latch.await(5, TimeUnit.SECONDS);
        applicationEventPublisher.publishEvent(
                new DocumentStateChangedEvent(new Object(), testDoc, SourceDocumentState.NEW));
        latch.await(5, TimeUnit.SECONDS);
        session.disconnect();
        
        assertThat(receivedMessages.size()).isEqualTo(1);
        LoggedEventMessage msg1 = receivedMessages.get(0);
        assertThat(msg1.getEventMsg()).isEqualTo(DocumentStateChangedEvent.class.getSimpleName());    
    }

}
