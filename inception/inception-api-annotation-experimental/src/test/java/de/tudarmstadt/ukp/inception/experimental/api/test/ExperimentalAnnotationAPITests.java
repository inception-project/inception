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
package de.tudarmstadt.ukp.inception.experimental.api.test;

public class ExperimentalAnnotationAPITests
{
    /*
    private @Mock DocumentService docService;
    private @Mock ProjectService projectService;
    private @Mock EventRepository eventRepository;
    private @Mock UserDao userRepo;
    private List<EventLoggingAdapter<?>> adapters;
    private TestChannel outboundChannel;
    
    private Project testProject;
    private SourceDocument testDoc;
    private User testAdmin;

    
    @BeforeEach
    public void setup() {
        outboundChannel = new TestChannel();
        adapters = asList(new SpanEventAdapter());
        testProject = new Project("testProject");
        testProject.setId(1L);
        testDoc = new SourceDocument("testDoc", testProject, "text");
        testDoc.setId(2L);
        testAdmin = new User("testAdmin", Role.ROLE_USER, Role.ROLE_ADMIN);
        
        when(projectService.getProject(1L)).thenReturn(testProject);
        when(docService.getSourceDocument(1L, 2L)).thenReturn(testDoc);

    }
    
    @Test
    public void thatSpanCreatedEventIsRelayedToUser() {
        sut.onApplicationEvent(new SpanCreatedEvent(getClass(), testDoc, testAdmin.getUsername(), null, null));
        
        List<Message<?>> messages = outboundChannel.getMessages();
        LoggedEventMessage msg = (LoggedEventMessage) messages.get(0).getPayload();
        
        assertThat(messages).hasSize(1);
        assertThat(msg.getDocumentName()).isEqualTo(testDoc.getName());
        assertThat(msg.getProjectName()).isEqualTo(testProject.getName());
        assertThat(msg.getActorName()).isEqualTo(testAdmin.getUsername());
        assertThat(msg.getEventMsg()).isEqualTo(SpanCreatedEvent.class.getSimpleName());

    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {EventLoggingAutoConfiguration.class,
            LiquibaseAutoConfiguration.class})
    @EntityScan(basePackages = { "de.tudarmstadt.ukp.inception.websocket"})
    public static class SpringConfig
    {
    }
    
    public class TestChannel
        extends AbstractMessageChannel
    {

        private List<Message<?>> messages = new ArrayList<>();

        @Override
        protected boolean sendInternal(Message<?> aMessage, long aTimeout)
        {
            messages.add(aMessage);
            return true;
        }

        public List<Message<?>> getMessages()
        {
            return messages;
        }

        public void clearMessages()
        {
            messages.clear();
        }
    }

     */

            /*
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
        };

        session = webSocketClient.connect(websocketUrl, sessionHandler).get(1, TimeUnit.SECONDS);
        latch.await(1, TimeUnit.SECONDS);
        session.disconnect();

        assertThat(receivedMessages.size()).isEqualTo(1);
        LoggedEventMessage msg1 = receivedMessages.get(0);
        assertThat(msg1.getEventMsg()).isEqualTo(DocumentStateChangedEvent.class.getSimpleName());


    }

         */
    
}
