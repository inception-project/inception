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
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_USER;
import static de.tudarmstadt.ukp.inception.diam.service.DiamWebsocketController.FORMAT_LEGACY;
import static de.tudarmstadt.ukp.inception.diam.service.DiamWebsocketController.X_DIAM_FORMAT;
import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.fromJsonString;
import static de.tudarmstadt.ukp.inception.websocket.config.WebsocketConfig.WS_ENDPOINT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.messaging.simp.SimpMessageHeaderAccessor.DESTINATION_HEADER;
import static org.springframework.messaging.simp.SimpMessageHeaderAccessor.SUBSCRIPTION_ID_HEADER;
import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

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
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.config.AnnotationAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.PreRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.diag.config.CasDoctorAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.InceptionDaoAuthenticationProvider;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.InceptionSecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.text.config.TextFormatsAutoConfiguration;
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
import de.tudarmstadt.ukp.inception.support.logging.Logging;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationContextProvider;
import de.tudarmstadt.ukp.inception.support.test.websocket.WebSocketStompTestClient;
import de.tudarmstadt.ukp.inception.websocket.config.WebsocketAutoConfiguration;
import de.tudarmstadt.ukp.inception.websocket.config.WebsocketSecurityConfig;
import de.tudarmstadt.ukp.inception.workload.config.WorkloadManagementAutoConfiguration;
import jakarta.persistence.EntityManager;

@SpringBootTest( //
        webEnvironment = RANDOM_PORT, //
        properties = { //
                "recommender.enabled=false", //
                "server.address=127.0.0.1", //
                "spring.main.banner-mode=off", //
                "websocket.enabled=true" })
@SpringBootApplication( //
        exclude = { //
                WorkloadManagementAutoConfiguration.class })
@ImportAutoConfiguration({ //
        PreferencesServiceAutoConfig.class, //
        CasDoctorAutoConfiguration.class, //
        RenderingAutoConfig.class, //
        InceptionSecurityAutoConfiguration.class, //
        SecurityAutoConfiguration.class, //
        WebsocketAutoConfiguration.class, //
        WebsocketSecurityConfig.class, //
        ProjectServiceAutoConfiguration.class, //
        DocumentServiceAutoConfiguration.class, //
        CasStorageServiceAutoConfiguration.class, //
        RepositoryAutoConfiguration.class, //
        AnnotationSchemaServiceAutoConfiguration.class, //
        AnnotationAutoConfiguration.class, //
        TextFormatsAutoConfiguration.class, //
        DocumentServiceAutoConfiguration.class, //
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
    private static final String CURATOR_USER = "curator";
    private static final String PASS = "pass";

    private static final AtomicInteger sessionCounter = new AtomicInteger();

    private @LocalServerPort int port;
    private String websocketUrl;

    private @Autowired DiamWebsocketController sut;

    private @Autowired ProjectService projectService;
    private @Autowired DocumentService documentService;
    private @Autowired RepositoryProperties repositoryProperties;
    private @Autowired EntityManager entityManager;
    private @Autowired UserDao userService;
    private @Autowired TestPreRenderer testPreRenderer;

    private static @TempDir File repositoryDir;

    private static User user;
    private static User otherUser;
    private static Project testProject;
    private static AnnotationLayer testLayer;
    private static SourceDocument testDoc;
    private static AnnotationDocument testAnnotationDocument;

    @BeforeEach
    public void setup() throws Exception
    {
        websocketUrl = "ws://localhost:" + port + WS_ENDPOINT;

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

        otherUser = new User(CURATOR_USER, ROLE_USER);
        otherUser.setPassword(PASS);
        userService.create(otherUser);

        testProject = new Project("test-project");
        projectService.createProject(testProject);
        projectService.assignRole(testProject, user, ANNOTATOR);
        projectService.assignRole(testProject, otherUser, CURATOR);

        testLayer = new AnnotationLayer();
        testLayer.setProject(testProject);
        testLayer.setId(1L);

        testDoc = new SourceDocument("testDoc", testProject, "text");
        documentService.createSourceDocument(testDoc);

        testAnnotationDocument = new AnnotationDocument(USER, testDoc);
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
    @Test
    public void thatViewportBasedMessageRoutingWorks() throws Exception
    {
        var vpd1 = ViewportDefinition.builder() //
                .withSessionOwner(USER) //
                .withDocument(testAnnotationDocument) //
                .withRange(10, 20) //
                .withFormat(FORMAT_LEGACY) //
                .build();
        var vpd2 = ViewportDefinition.builder() //
                .withSessionOwner(USER) //
                .withDocument(testAnnotationDocument) //
                .withRange(30, 40) //
                .withFormat(FORMAT_LEGACY) //
                .build();

        testPreRenderer.setRenderFunc((aResponse, aRequest) -> {
            aResponse.add(new VSpan(testLayer, new VID(1),
                    new VRange(aRequest.getWindowBeginOffset(), aRequest.getWindowEndOffset()),
                    emptyMap()));
        });

        try (var client1 = new WebSocketStompTestClient(USER, PASS);
                var client2 = new WebSocketStompTestClient(USER, PASS)) {
            var connection1 = client1.expectSuccessfulConnection().connect(websocketUrl);
            var queueSub1 = client1.subscribe("/topic" + vpd1.getTopic(), Map.of( //
                    X_DIAM_FORMAT, vpd1.getFormat(), //
                    "selector", "headers['" + X_DIAM_FORMAT + "']=='" + vpd1.getFormat() + "'"));
            client1.expectWithHeaders(MViewportInit.class, (message, msg) -> {
                assertThat(msg.getText()).contains("test. This");
                assertThat(message.getHeaders().get(DESTINATION_HEADER))
                        .isEqualTo("/app" + vpd1.getTopic());
            });
            var appSub1 = client1.subscribe("/app" + vpd1.getTopic(), Map.of( //
                    X_DIAM_FORMAT, vpd1.getFormat(), //
                    "selector", "headers['" + X_DIAM_FORMAT + "']=='" + vpd1.getFormat() + "'"));

            var connection2 = client2.expectSuccessfulConnection().connect(websocketUrl);
            var queueSub2 = client2.subscribe("/topic" + vpd2.getTopic(), Map.of( //
                    X_DIAM_FORMAT, vpd2.getFormat(), //
                    "selector", "headers['" + X_DIAM_FORMAT + "']=='" + vpd2.getFormat() + "'"));
            client2.expectWithHeaders(MViewportInit.class, (message, msg) -> {
                assertThat(msg.getText()).contains(". This is ");
                assertThat(message.getHeaders().get(DESTINATION_HEADER))
                        .isEqualTo("/app" + vpd2.getTopic());
            });
            var appSub2 = client2.subscribe("/app" + vpd2.getTopic(), Map.of( //
                    X_DIAM_FORMAT, vpd2.getFormat(), //
                    "selector", "headers['" + X_DIAM_FORMAT + "']=='" + vpd2.getFormat() + "'"));

            assertThat(connection1.getSessionId()).isNotEqualTo(connection2.getSessionId());

            // Verify that the queue and app subscriptions have distinct ids
            assertThat(queueSub1.getSubscriptionId()).isNotNull();
            assertThat(appSub1.getSubscriptionId()).isNotNull();
            assertThat(queueSub1.getSubscriptionId()).isNotEqualTo(appSub1.getSubscriptionId());

            assertThat(queueSub2.getSubscriptionId()).isNotNull();
            assertThat(appSub2.getSubscriptionId()).isNotNull();
            assertThat(queueSub2.getSubscriptionId()).isNotEqualTo(appSub2.getSubscriptionId());

            var expected1 = new MViewportUpdate(12, 15, fromJsonString(
                    "[{\"op\":\"replace\",\"path\":\"/spans/0/vid\",\"value\":\"2\"}]"));
            var expected2 = new MViewportUpdate(15, 35, fromJsonString(
                    "[{\"op\":\"replace\",\"path\":\"/spans/0/vid\",\"value\":\"4\"}]"));
            client1.expectWithHeaders(MViewportUpdate.class, (message, update) -> {
                assertThat(update).isEqualTo(expected1);
                assertThat(message.getHeaders().get(DESTINATION_HEADER))
                        .isEqualTo("/topic" + vpd1.getTopic());
                assertThat(message.getHeaders().get(SUBSCRIPTION_ID_HEADER))
                        .isEqualTo(queueSub1.getSubscriptionId());
            }).expectWithHeaders(MViewportUpdate.class, (message, update) -> {
                assertThat(update).isEqualTo(expected2);
                assertThat(message.getHeaders().get(DESTINATION_HEADER))
                        .isEqualTo("/topic" + vpd1.getTopic());
                assertThat(message.getHeaders().get(SUBSCRIPTION_ID_HEADER))
                        .isEqualTo(queueSub1.getSubscriptionId());
            });

            var expected3 = new MViewportUpdate(31, 33, fromJsonString(
                    "[{\"op\":\"replace\",\"path\":\"/spans/0/vid\",\"value\":\"3\"}]"));
            var expected4 = new MViewportUpdate(15, 35, fromJsonString(
                    "[{\"op\":\"replace\",\"path\":\"/spans/0/vid\",\"value\":\"4\"}]"));
            client2.expectWithHeaders(MViewportUpdate.class, (message, update) -> {
                assertThat(update).isEqualTo(expected3);
                assertThat(message.getHeaders().get(DESTINATION_HEADER))
                        .isEqualTo("/topic" + vpd2.getTopic());
                assertThat(message.getHeaders().get(SUBSCRIPTION_ID_HEADER))
                        .isEqualTo(queueSub2.getSubscriptionId());
            }).expectWithHeaders(MViewportUpdate.class, (message, update) -> {
                assertThat(update).isEqualTo(expected4);
                assertThat(message.getHeaders().get(DESTINATION_HEADER))
                        .isEqualTo("/topic" + vpd2.getTopic());
                assertThat(message.getHeaders().get(SUBSCRIPTION_ID_HEADER))
                        .isEqualTo(queueSub2.getSubscriptionId());
            });

            testPreRenderer.setRenderFunc((aResponse, aRequest) -> {
                aResponse.add(new VSpan(testLayer, new VID(2),
                        new VRange(aRequest.getWindowBeginOffset(), aRequest.getWindowEndOffset()),
                        emptyMap()));
            });
            sut.sendUpdate(testAnnotationDocument, 12, 15);

            testPreRenderer.setRenderFunc((aResponse, aRequest) -> {
                aResponse.add(new VSpan(testLayer, new VID(3),
                        new VRange(aRequest.getWindowBeginOffset(), aRequest.getWindowEndOffset()),
                        emptyMap()));
            });
            sut.sendUpdate(testAnnotationDocument, 31, 33);

            testPreRenderer.setRenderFunc((aResponse, aRequest) -> {
                aResponse.add(new VSpan(testLayer, new VID(4),
                        new VRange(aRequest.getWindowBeginOffset(), aRequest.getWindowEndOffset()),
                        emptyMap()));
            });
            sut.sendUpdate(testAnnotationDocument, 15, 35);

            client1.assertExpectations();
            client2.assertExpectations();
        }
    }

    /**
     * The CAS may be written by a thread that carries no security context at all - most commonly a
     * scheduler thread - the scheduling module does no {@code SecurityContext} propagation, so a
     * bulk prediction or bulk curation task writing a CAS has none.
     * {@link DiamWebsocketController#sendUpdate} runs synchronously on that thread, so anything it
     * derives from {@link SecurityContextHolder} is unavailable there. The subscriber's own render
     * must still be pushed.
     */
    @WithMockUser(username = USER, roles = { "USER" })
    @Test
    public void thatUpdateIsPushedWhenCasIsWrittenWithoutSecurityContext() throws Exception
    {
        var vpd = ViewportDefinition.builder() //
                .withSessionOwner(USER) //
                .withDocument(testAnnotationDocument) //
                .withRange(0, 20) //
                .withFormat(FORMAT_LEGACY) //
                .build();

        testPreRenderer.setRenderFunc((aResponse,
                aRequest) -> aResponse.add(new VSpan(testLayer, new VID(1),
                        new VRange(aRequest.getWindowBeginOffset(), aRequest.getWindowEndOffset()),
                        emptyMap())));

        try (var client = new WebSocketStompTestClient(USER, PASS)) {
            client.expectSuccessfulConnection().connect(websocketUrl);
            client.subscribe("/topic" + vpd.getTopic(), Map.of( //
                    X_DIAM_FORMAT, vpd.getFormat(), //
                    "selector", "headers['" + X_DIAM_FORMAT + "']=='" + vpd.getFormat() + "'"));
            client.expect(MViewportInit.class, msg -> assertThat(msg.getText()).isNotEmpty());
            // The /app subscription is what actually invokes @SubscribeMapping and thereby
            // registers the viewport server-side. Subscribing only to /topic yields a client that
            // is listening but a server that has no viewport to push to.
            client.subscribe("/app" + vpd.getTopic(), Map.of( //
                    X_DIAM_FORMAT, vpd.getFormat(), //
                    "selector", "headers['" + X_DIAM_FORMAT + "']=='" + vpd.getFormat() + "'"));

            var expected = new MViewportUpdate(0, 20, fromJsonString(
                    "[{\"op\":\"replace\",\"path\":\"/spans/0/vid\",\"value\":\"2\"}]"));
            client.expect(MViewportUpdate.class, update -> assertThat(update).isEqualTo(expected));

            testPreRenderer.setRenderFunc((aResponse, aRequest) -> aResponse.add(new VSpan(
                    testLayer, new VID(2),
                    new VRange(aRequest.getWindowBeginOffset(), aRequest.getWindowEndOffset()),
                    emptyMap())));

            // Emulate a scheduler thread (e.g. BulkPredictionTask) writing the CAS: no security
            // context whatsoever. Note that a plain `new Thread(...)` inherits nothing here
            // because MODE_INHERITABLETHREADLOCAL is not configured anywhere in the code base.
            runInThreadWithoutSecurityContext(() -> sut.sendUpdate(testAnnotationDocument, 0, 20));

            client.assertExpectations();
        }
    }

    /**
     * Another user writing the document must not cause <b>their</b> session to be used for
     * rendering the subscriber's viewport - that would render the writer's layer visibility and
     * preferences into the payload pushed to the subscriber. Routine in curation and on shared
     * annotation sets.
     */
    @WithMockUser(username = USER, roles = { "USER" })
    @Test
    public void thatUpdateIsPushedAsSubscriberWhenOtherUserWritesCas() throws Exception
    {
        var vpd = ViewportDefinition.builder() //
                .withSessionOwner(USER) //
                .withDocument(testAnnotationDocument) //
                .withRange(0, 20) //
                .withFormat(FORMAT_LEGACY) //
                .build();

        testPreRenderer.setRenderFunc((aResponse,
                aRequest) -> aResponse.add(new VSpan(testLayer, new VID(1),
                        new VRange(aRequest.getWindowBeginOffset(), aRequest.getWindowEndOffset()),
                        emptyMap())));

        try (var client = new WebSocketStompTestClient(USER, PASS)) {
            client.expectSuccessfulConnection().connect(websocketUrl);
            client.subscribe("/topic" + vpd.getTopic(), Map.of( //
                    X_DIAM_FORMAT, vpd.getFormat(), //
                    "selector", "headers['" + X_DIAM_FORMAT + "']=='" + vpd.getFormat() + "'"));
            client.expect(MViewportInit.class, msg -> assertThat(msg.getText()).isNotEmpty());
            // See the note on the /app subscription in the sibling test above.
            client.subscribe("/app" + vpd.getTopic(), Map.of( //
                    X_DIAM_FORMAT, vpd.getFormat(), //
                    "selector", "headers['" + X_DIAM_FORMAT + "']=='" + vpd.getFormat() + "'"));

            var expected = new MViewportUpdate(0, 20, fromJsonString(
                    "[{\"op\":\"replace\",\"path\":\"/spans/0/vid\",\"value\":\"2\"}]"));
            client.expect(MViewportUpdate.class, update -> assertThat(update).isEqualTo(expected));

            // Capture whose session the render actually ran as.
            var renderedAs = new AtomicReference<String>();
            testPreRenderer.setRenderFunc((aResponse, aRequest) -> {
                var sessionOwner = aRequest.getSessionOwner();
                renderedAs.set(sessionOwner != null ? sessionOwner.getUsername() : null);
                aResponse.add(new VSpan(testLayer, new VID(2),
                        new VRange(aRequest.getWindowBeginOffset(), aRequest.getWindowEndOffset()),
                        emptyMap()));
            });

            runInThreadAs(CURATOR_USER, () -> sut.sendUpdate(testAnnotationDocument, 0, 20));

            client.assertExpectations();

            assertThat(renderedAs.get()) //
                    .as("render must run as the subscriber, not as the user who wrote the CAS") //
                    .isEqualTo(USER);
        }
    }

    /**
     * A curator and an annotator viewing the <b>same</b> annotator document must each get their own
     * viewport and their own render. (An annotator may only view their own document, so the
     * realistic two-viewer case is a curator or manager alongside the annotator - see
     * {@code DocumentAccessImpl.canViewAnnotationDocument}.)
     * <p>
     * The CAS is viewer-independent, but the render is not: suggestions are fetched per session
     * owner, colours come from the viewer's preferences, and the viewer's hidden layers decide what
     * is serialized at all. The topic is keyed on the dataOwner, so without the session owner
     * participating in {@link ViewportDefinition#equals} both viewers would collapse onto a single
     * cache entry and whoever subscribed first would govern the render pushed to both.
     */
    @WithMockUser(username = USER, roles = { "USER" })
    @Test
    public void thatEachSessionOwnerGetsItsOwnViewport() throws Exception
    {
        var vpdUser = ViewportDefinition.builder() //
                .withSessionOwner(USER) //
                .withDocument(testAnnotationDocument) //
                .withRange(0, 20) //
                .withFormat(FORMAT_LEGACY) //
                .build();
        var vpdOther = ViewportDefinition.builder() //
                .withSessionOwner(CURATOR_USER) //
                .withDocument(testAnnotationDocument) //
                .withRange(0, 20) //
                .withFormat(FORMAT_LEGACY) //
                .build();

        // Same document, same dataOwner, same range, same format - differing only in who is
        // looking. These must be neither the same viewport nor the same topic: updates are JSON
        // patches against a per-viewport baseline, so sharing a topic would have each client apply
        // the other's patch on top of its own document.
        assertThat(vpdUser) //
                .as("viewports of different session owners must not collide") //
                .isNotEqualTo(vpdOther);
        assertThat(vpdUser.getTopic()) //
                .as("viewers must not share a topic - each would apply the other's patch") //
                .isNotEqualTo(vpdOther.getTopic());

        testPreRenderer.setRenderFunc((aResponse,
                aRequest) -> aResponse.add(new VSpan(testLayer, new VID(1),
                        new VRange(aRequest.getWindowBeginOffset(), aRequest.getWindowEndOffset()),
                        emptyMap())));

        try (var client1 = new WebSocketStompTestClient(USER, PASS);
                var client2 = new WebSocketStompTestClient(CURATOR_USER, PASS)) {
            client1.expectSuccessfulConnection().connect(websocketUrl);
            client1.subscribe("/topic" + vpdUser.getTopic(), Map.of( //
                    X_DIAM_FORMAT, FORMAT_LEGACY, //
                    "selector", "headers['" + X_DIAM_FORMAT + "']=='" + FORMAT_LEGACY + "'"));
            client1.expect(MViewportInit.class, msg -> assertThat(msg.getText()).isNotEmpty());
            client1.subscribe("/app" + vpdUser.getTopic(), Map.of( //
                    X_DIAM_FORMAT, FORMAT_LEGACY, //
                    "selector", "headers['" + X_DIAM_FORMAT + "']=='" + FORMAT_LEGACY + "'"));

            client2.expectSuccessfulConnection().connect(websocketUrl);
            client2.subscribe("/topic" + vpdOther.getTopic(), Map.of( //
                    X_DIAM_FORMAT, FORMAT_LEGACY, //
                    "selector", "headers['" + X_DIAM_FORMAT + "']=='" + FORMAT_LEGACY + "'"));
            client2.expect(MViewportInit.class, msg -> assertThat(msg.getText()).isNotEmpty());
            client2.subscribe("/app" + vpdOther.getTopic(), Map.of( //
                    X_DIAM_FORMAT, FORMAT_LEGACY, //
                    "selector", "headers['" + X_DIAM_FORMAT + "']=='" + FORMAT_LEGACY + "'"));

            // Each client must receive EXACTLY ONE update - its own. Two viewports on one shared
            // topic would deliver both diffs to both clients, and since the client applies every
            // diff it receives (DiamWebsocketImpl), the second would be applied on top of an
            // already-patched document and corrupt it.
            var expected = new MViewportUpdate(0, 20, fromJsonString(
                    "[{\"op\":\"replace\",\"path\":\"/spans/0/vid\",\"value\":\"2\"}]"));
            client1.expectWithHeaders(MViewportUpdate.class, (message, update) -> {
                assertThat(update).isEqualTo(expected);
                assertThat(message.getHeaders().get(DESTINATION_HEADER))
                        .isEqualTo("/topic" + vpdUser.getTopic());
            });
            client2.expectWithHeaders(MViewportUpdate.class, (message, update) -> {
                assertThat(update).isEqualTo(expected);
                assertThat(message.getHeaders().get(DESTINATION_HEADER))
                        .isEqualTo("/topic" + vpdOther.getTopic());
            });

            // Collect the session owner of every render triggered by the update below. With one
            // viewport per session owner we must see BOTH users represented.
            var renderedAs = ConcurrentHashMap.<String> newKeySet();
            testPreRenderer.setRenderFunc((aResponse, aRequest) -> {
                var sessionOwner = aRequest.getSessionOwner();
                renderedAs.add(sessionOwner != null ? sessionOwner.getUsername() : "<null>");
                aResponse.add(new VSpan(testLayer, new VID(2),
                        new VRange(aRequest.getWindowBeginOffset(), aRequest.getWindowEndOffset()),
                        emptyMap()));
            });

            runInThreadWithoutSecurityContext(() -> sut.sendUpdate(testAnnotationDocument, 0, 20));

            await("both viewports rendered") //
                    .atMost(ofSeconds(30)) //
                    .until(() -> renderedAs.size() >= 2);

            assertThat(renderedAs) //
                    .as("each session owner must get their own render") //
                    .containsExactlyInAnyOrder(USER, CURATOR_USER);

            // assertExpectations() also fails on any *extra* message beyond those queued above -
            // which is precisely what a shared topic would produce.
            client1.assertExpectations();
            client2.assertExpectations();
        }
    }

    /**
     * The session owner is part of the destination and is therefore client-supplied. Subscribing to
     * another user's viewport topic must be rejected: that topic carries renders made with the
     * other user's layer visibility and preferences.
     * <p>
     * Note that the surrounding {@code canViewAnnotationDocument} rule guards the <b>data owner</b>
     * and so happily permits a curator subscribing to an annotator's document - only the
     * session-owner check in the handler stops them subscribing <i>as</i> that annotator.
     * <p>
     * This drives the handler directly rather than through a STOMP client: the rejection closes the
     * connection before any subscription is established, so there is nothing for the client to
     * observe the error frame on.
     */
    @WithMockUser(username = CURATOR_USER, roles = { "USER" })
    @Test
    public void thatSubscribingToAnotherUsersViewportIsRejected() throws Exception
    {
        var headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.SUBSCRIBE);
        headerAccessor.setNativeHeader(X_DIAM_FORMAT, FORMAT_LEGACY);

        // CURATOR_USER is allowed to view USER's document, but must not pose as USER.
        assertThatExceptionOfType(AccessDeniedException.class) //
                .isThrownBy(() -> sut.onSubscribeToViewport(headerAccessor,
                        () -> CURATOR_USER /* principal */, testProject.getId(), testDoc.getId(),
                        USER /* dataOwner */, USER /* sessionOwner */, 0, 20)) //
                .withMessageContaining(CURATOR_USER) //
                .withMessageContaining(USER);
    }

    /**
     * The handler-level check in {@link #thatSubscribingToAnotherUsersViewportIsRejected()} only
     * covers {@code /app} - {@code @SubscribeMapping} is dispatched for the application prefix
     * only. The updates themselves are pushed to {@code /topic}, so subscribing there must be
     * barred by the STOMP security rules as well: otherwise a curator can skip the {@code /app}
     * subscription entirely and just listen in on the annotator's topic.
     */
    @WithMockUser(username = CURATOR_USER, roles = { "USER" })
    @Test
    public void thatSubscribingToAnotherUsersViewportTopicIsRejected() throws Exception
    {
        var vpd = ViewportDefinition.builder() //
                .withSessionOwner(USER) //
                .withDocument(testAnnotationDocument) //
                .withRange(0, 20) //
                .withFormat(FORMAT_LEGACY) //
                .build();

        // CURATOR_USER may view USER's document - canViewAnnotationDocument alone would let this
        // through. Only the session-owner part of the rule stops it.
        try (var client = new WebSocketStompTestClient(CURATOR_USER, PASS)) {
            client.expectSuccessfulConnection().connect(websocketUrl);

            client.expectError(
                    "Failed to send message to ExecutorSubscribableChannel[clientInboundChannel]");
            client.subscribeWithoutWaiting("/topic" + vpd.getTopic(), Map.of( //
                    X_DIAM_FORMAT, FORMAT_LEGACY));

            client.assertExpectations();
        }
    }

    /**
     * The counterpart to {@link #thatSubscribingToAnotherUsersViewportIsRejected()}: subscribing to
     * one's own viewport must of course still work.
     */
    @WithMockUser(username = USER, roles = { "USER" })
    @Test
    public void thatSubscribingToOwnViewportIsPermitted() throws Exception
    {
        testPreRenderer.setRenderFunc((aResponse,
                aRequest) -> aResponse.add(new VSpan(testLayer, new VID(1),
                        new VRange(aRequest.getWindowBeginOffset(), aRequest.getWindowEndOffset()),
                        emptyMap())));

        var headerAccessor = subscribeHeaders();

        try {
            var result = sut.onSubscribeToViewport(headerAccessor, () -> USER /* principal */,
                    testProject.getId(), testDoc.getId(), USER /* dataOwner */,
                    USER /* sessionOwner */, 0, 20);

            assertThat(result).as("subscribing to one's own viewport returns a render").isNotNull();
        }
        finally {
            unsubscribe(headerAccessor);
        }
    }

    /**
     * When the CAS is written on a thread that already has an MDC - i.e. a request thread, which
     * the servlet filter has set up - the update rendering must leave that MDC as it found it.
     * {@code AfterCasWrittenEvent} is published synchronously from {@code writeAnnotationCas}, so
     * the request carries on afterwards and would otherwise continue without a repository path.
     */
    @WithMockUser(username = USER, roles = { "USER" })
    @Test
    public void thatSendUpdateRestoresTheCallersMdc() throws Exception
    {
        testPreRenderer.setRenderFunc((aResponse,
                aRequest) -> aResponse.add(new VSpan(testLayer, new VID(1),
                        new VRange(aRequest.getWindowBeginOffset(), aRequest.getWindowEndOffset()),
                        emptyMap())));

        // A viewport must actually be registered, otherwise sendUpdate matches nothing, the
        // MDC-handling code never runs, and the assertion below would hold trivially.
        var headerAccessor = subscribeHeaders();
        sut.onSubscribeToViewport(headerAccessor, () -> USER, testProject.getId(), testDoc.getId(),
                USER, USER, 0, 20);

        var callersRepositoryPath = "/some/caller/owned/path";
        var previous = MDC.get(Logging.KEY_REPOSITORY_PATH);
        MDC.put(Logging.KEY_REPOSITORY_PATH, callersRepositoryPath);
        try {
            sut.sendUpdate(testAnnotationDocument, 0, 20);

            assertThat(MDC.get(Logging.KEY_REPOSITORY_PATH)) //
                    .as("sendUpdate must leave the caller's MDC untouched") //
                    .isEqualTo(callersRepositoryPath);
        }
        finally {
            unsubscribe(headerAccessor);

            if (previous != null) {
                MDC.put(Logging.KEY_REPOSITORY_PATH, previous);
            }
            else {
                MDC.remove(Logging.KEY_REPOSITORY_PATH);
            }
        }
    }

    /**
     * Headers for driving {@link DiamWebsocketController#onSubscribeToViewport} directly. The
     * session and subscription ids matter: without them the viewport records a subscription with a
     * {@code null} session id, which lives on in the shared viewport cache and makes
     * {@code ViewportState.removeSubscriptionsBySession} throw when any later test disconnects.
     */
    private SimpMessageHeaderAccessor subscribeHeaders()
    {
        var headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.SUBSCRIBE);
        headerAccessor.setNativeHeader(X_DIAM_FORMAT, FORMAT_LEGACY);
        headerAccessor.setSessionId("test-session-" + sessionCounter.incrementAndGet());
        headerAccessor.setSubscriptionId("test-subscription");
        return headerAccessor;
    }

    /**
     * Drop the viewport registered via {@link #subscribeHeaders()} again so it cannot emit updates
     * into subsequent tests - the viewport cache is shared across all tests in this class.
     */
    private void unsubscribe(SimpMessageHeaderAccessor aHeaderAccessor)
    {
        var message = MessageBuilder.createMessage(new byte[0],
                aHeaderAccessor.getMessageHeaders());
        sut.onSessionUnsubscribe(new SessionUnsubscribeEvent(this, message));
    }

    private void runInThreadWithoutSecurityContext(Runnable aRunnable) throws Exception
    {
        runInThread(aRunnable, null);
    }

    private void runInThreadAs(String aUsername, Runnable aRunnable) throws Exception
    {
        runInThread(aRunnable, aUsername);
    }

    private void runInThread(Runnable aRunnable, String aUsername) throws Exception
    {
        var error = new AtomicReference<Throwable>();
        var thread = new Thread(() -> {
            try {
                SecurityContextHolder.clearContext();
                if (aUsername != null) {
                    var context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(
                            new UsernamePasswordAuthenticationToken(aUsername, "N/A", emptyList()));
                    SecurityContextHolder.setContext(context);
                }
                aRunnable.run();
            }
            catch (Throwable e) {
                error.set(e);
            }
            finally {
                SecurityContextHolder.clearContext();
            }
        });
        thread.start();
        thread.join();

        if (error.get() != null) {
            throw new AssertionError("Write thread failed", error.get());
        }
    }

    static private class TestPreRenderer
        implements PreRenderer
    {
        private BiConsumer<VDocument, RenderRequest> renderFunc;

        @Override
        public String getId()
        {
            return "TestPreRenderer";
        }

        @Override
        public void render(VDocument aResponse, RenderRequest aRequest)
        {
            if (renderFunc == null) {
                throw new IllegalStateException("renderFunc not set");
            }
            renderFunc.accept(aResponse, aRequest);
        }

        public void setRenderFunc(BiConsumer<VDocument, RenderRequest> aRenderFunc)
        {
            renderFunc = aRenderFunc;
        }
    }

    @SpringBootConfiguration
    public static class WebsocketBrokerTestConfig
    {
        @Bean
        public AuthenticationEventPublisher authenticationEventPublisher(
                ApplicationEventPublisher publisher)
        {
            return new DefaultAuthenticationEventPublisher(publisher);
        }

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
                @Lazy UserDetailsService aUserDetailsManager)
        {
            var authProvider = new InceptionDaoAuthenticationProvider(aUserDetailsManager);
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

        @Primary
        @Bean
        public TestPreRenderer testPreRenderer()
        {
            return new TestPreRenderer();
        }
    }
}
