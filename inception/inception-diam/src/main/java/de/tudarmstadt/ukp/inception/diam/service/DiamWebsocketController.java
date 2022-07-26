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

import static de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging.KEY_REPOSITORY_PATH;
import static de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging.KEY_USERNAME;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.PARAM_DOCUMENT;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.PARAM_PROJECT;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.PARAM_USER;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.TOPIC_ELEMENT_DOCUMENT;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.TOPIC_ELEMENT_PROJECT;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.TOPIC_ELEMENT_USER;
import static java.lang.Integer.MAX_VALUE;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.security.Principal;
import java.time.Duration;
import java.util.List;

import javax.persistence.NoResultException;

import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import com.fasterxml.jackson.databind.JsonNode;
import com.flipkart.zjsonpatch.JsonDiff;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.PreRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterCasWrittenEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageSession;
import de.tudarmstadt.ukp.inception.diam.messages.MViewportInit;
import de.tudarmstadt.ukp.inception.diam.messages.MViewportUpdate;
import de.tudarmstadt.ukp.inception.diam.model.websocket.ViewportDefinition;
import de.tudarmstadt.ukp.inception.diam.model.websocket.ViewportState;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

/**
 * Differential INCEpTION Annotation Messaging (DIAM) protocol controller.
 */
@ConditionalOnWebApplication
@ConditionalOnExpression("${websocket.enabled:true}")
@Controller
public class DiamWebsocketController
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String PARAM_FROM = "from";
    public static final String PARAM_TO = "to";

    public static final PropertyPlaceholderHelper PLACEHOLDER_RESOLVER = new PropertyPlaceholderHelper(
            "{", "}", null, false);

    public static final String DOCUMENT_BASE_TOPIC_TEMPLATE = TOPIC_ELEMENT_PROJECT + "{"
            + PARAM_PROJECT + "}" + TOPIC_ELEMENT_DOCUMENT + "{" + PARAM_DOCUMENT + "}"
            + TOPIC_ELEMENT_USER + "{" + PARAM_USER + "}";

    public static final String DOCUMENT_VIEWPORT_TOPIC_TEMPLATE = //
            DOCUMENT_BASE_TOPIC_TEMPLATE + "/from/{" + PARAM_FROM + "}/to/{" + PARAM_TO + "}";

    // public static final String ANNOTATION_COMMAND_DELETE_TOPIC_TEMPLATE = //
    // DOCUMENT_BASE_TOPIC_TEMPLATE + "/delete";
    //
    public static final String ANNOTATION_COMMAND_CREATE_SPAN_TOPIC_TEMPLATE = //
            DOCUMENT_BASE_TOPIC_TEMPLATE + "/span";

    public static final String ANNOTATION_COMMAND_CREATE_RELATION_TOPIC_TEMPLATE = //
            DOCUMENT_BASE_TOPIC_TEMPLATE + "/relation";

    public static final String ANNOTATION_COMMAND_SELECT_TOPIC_TEMPLATE = //
            DOCUMENT_BASE_TOPIC_TEMPLATE + "/select";

    private final SimpMessagingTemplate msgTemplate;
    private final PreRenderer preRenderer;
    private final DocumentService documentService;
    private final RepositoryProperties repositoryProperties;
    private final AnnotationSchemaService schemaService;
    private final ProjectService projectService;
    private final UserDao userRepository;

    private final LoadingCache<ViewportDefinition, ViewportState> activeViewports;

    public DiamWebsocketController(SimpMessagingTemplate aMsgTemplate, PreRenderer aPreRenderer,
            DocumentService aDocumentService, RepositoryProperties aRepositoryProperties,
            AnnotationSchemaService aSchemaService, ProjectService aProjectService,
            UserDao aUserRepository)
    {
        msgTemplate = aMsgTemplate;
        preRenderer = aPreRenderer;
        documentService = aDocumentService;
        repositoryProperties = aRepositoryProperties;
        schemaService = aSchemaService;
        projectService = aProjectService;
        userRepository = aUserRepository;

        activeViewports = Caffeine.newBuilder() //
                .expireAfterAccess(Duration.ofMinutes(30)) //
                .build(this::initState);
    }

    @EventListener
    public void onSessionDisconnect(SessionDisconnectEvent aEvent)
    {
        log.trace("Unsubscribing {} from all viewports", aEvent.getSessionId());

        activeViewports.asMap().entrySet().stream() //
                .filter(e -> {
                    e.getValue().removeSubscriber(aEvent.getSessionId());
                    return !e.getValue().hasSubscribers();
                }) //
                .forEach(e -> closeViewport(e.getKey()));
    }

    @EventListener
    public void onSessionUnsubscribe(SessionUnsubscribeEvent aEvent)
    {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(aEvent.getMessage());

        String sessionId = headers.getSessionId();
        String subscriptionId = headers.getSubscriptionId();

        log.trace("Unsubscribing {} from subscription {}", sessionId, subscriptionId);

        activeViewports.asMap().entrySet().stream() //
                .filter(e -> {
                    e.getValue().removeSubscription(sessionId, subscriptionId);
                    return !e.getValue().hasSubscribers();
                }) //
                .forEach(e -> closeViewport(e.getKey()));
    }

    private void closeViewport(ViewportDefinition aVpd)
    {
        log.trace("Closing viewport {}", aVpd);
        activeViewports.invalidate(aVpd);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void afterAnnotationUpdate(AfterCasWrittenEvent aEvent)
    {
        sendUpdate(aEvent.getDocument());
    }

    @SubscribeMapping(DOCUMENT_VIEWPORT_TOPIC_TEMPLATE)
    public JsonNode onSubscribeToAnnotationDocument(SimpMessageHeaderAccessor aHeaderAccessor,
            Principal aPrincipal, @DestinationVariable(PARAM_PROJECT) long aProjectId,
            @DestinationVariable(PARAM_DOCUMENT) long aDocumentId,
            @DestinationVariable(PARAM_USER) String aUser,
            @DestinationVariable(PARAM_FROM) int aViewportBegin,
            @DestinationVariable(PARAM_TO) int aViewportEnd)
        throws IOException
    {
        Project project = getProject(aProjectId);

        try (CasStorageSession session = CasStorageSession.open()) {
            MDC.put(KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
            MDC.put(KEY_USERNAME, aPrincipal.getName());

            ViewportDefinition vpd = new ViewportDefinition(aProjectId, aDocumentId, aUser,
                    aViewportBegin, aViewportEnd);

            // Ensure that the viewport is registered
            ViewportState vps = activeViewports.get(vpd);

            log.trace("Subscribing {} to {}", aHeaderAccessor.getSessionId(), vpd.getTopic());
            vps.addSubscription(aHeaderAccessor.getSessionId(),
                    aHeaderAccessor.getSubscriptionId());

            VDocument vdoc = render(project, aDocumentId, aUser, aViewportBegin, aViewportEnd);

            JsonNode newJson = JSONUtil.getObjectMapper().valueToTree(new MViewportInit(vdoc));
            vps.setJson(newJson);

            return newJson;
        }
        finally {
            MDC.remove(KEY_REPOSITORY_PATH);
            MDC.remove(KEY_USERNAME);
        }
    }

    // @MessageMapping(ANNOTATION_COMMAND_DELETE_TOPIC_TEMPLATE)
    // public void onDeleteAnnotationRequest(Principal aPrincipal,
    // @DestinationVariable(PARAM_PROJECT) long aProjectId,
    // @DestinationVariable(PARAM_DOCUMENT) long aDocumentId,
    // @DestinationVariable(PARAM_USER) String aUser, @Header("vid") String aVid)
    // throws IOException
    // {
    // Project project = getProject(aProjectId);
    //
    // try (CasStorageSession session = CasStorageSession.open()) {
    // MDC.put(KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
    // MDC.put(KEY_USERNAME, aPrincipal.getName());
    //
    // VID vid = VID.parse(aVid);
    //
    // SourceDocument doc = documentService.getSourceDocument(project.getId(), aDocumentId);
    // CAS cas = documentService.readAnnotationCas(doc, aUser);
    // AnnotationFS fs = selectAnnotationByAddr(cas, vid.getId());
    // TypeAdapter adapter = schemaService.getAdapter(schemaService.findLayer(project, fs));
    //
    // // FIXME Does not delete/clear up related annotations (relations, slots, etc.)
    // adapter.delete(doc, aUser, cas, vid);
    //
    // documentService.writeAnnotationCas(cas, doc, aUser, true);
    // }
    // finally {
    // MDC.remove(KEY_REPOSITORY_PATH);
    // MDC.remove(KEY_USERNAME);
    // }
    // }

    private VDocument render(Project aProject, long aDocumentId, String aUser, int aViewportBegin,
            int aViewportEnd)
        throws IOException
    {
        SourceDocument doc = documentService.getSourceDocument(aProject.getId(), aDocumentId);
        User user = userRepository.get(aUser);
        CAS cas = documentService.readAnnotationCas(doc, aUser);

        List<AnnotationLayer> layers = schemaService.listSupportedLayers(aProject).stream()
                .filter(AnnotationLayer::isEnabled) //
                .collect(toList());

        RenderRequest request = RenderRequest.builder() //
                .withDocument(doc, user) //
                .withWindow(aViewportBegin, aViewportEnd) //
                .withCas(cas) //
                .withVisibleLayers(layers) //
                .build();

        VDocument vdoc = new VDocument();
        preRenderer.render(vdoc, request);
        return vdoc;
    }

    private ViewportState initState(ViewportDefinition aVpd)
    {
        return new ViewportState(aVpd);
    }

    private void sendUpdate(AnnotationDocument aDoc)
    {
        sendUpdate(aDoc.getProject().getId(), aDoc.getDocument().getId(), aDoc.getUser(), 0,
                MAX_VALUE);
    }

    void sendUpdate(AnnotationDocument aDoc, int aUpdateBegin, int aUpdateEnd)
    {
        sendUpdate(aDoc.getProject().getId(), aDoc.getDocument().getId(), aDoc.getUser(),
                aUpdateBegin, aUpdateEnd);
    }

    private void sendUpdate(long aProjectId, long aDocumentId, String aUser, int aUpdateBegin,
            int aUpdateEnd)
    {
        activeViewports.asMap().entrySet().stream() //
                .filter(e -> e.getKey().matches(aDocumentId, aUser, aUpdateBegin, aUpdateEnd)) //
                .forEach(e -> sendUpdate(e.getKey(), e.getValue(), aProjectId, aDocumentId, aUser,
                        aUpdateBegin, aUpdateEnd));
    }

    private void sendUpdate(ViewportDefinition vpd, ViewportState vps, long aProjectId,
            long aDocumentId, String aUser, int aUpdateBegin, int aUpdateEnd)
    {
        // MDC.put(KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

        try (CasStorageSession session = CasStorageSession.openNested()) {
            Project project = projectService.getProject(vpd.getProjectId());
            VDocument vdoc = render(project, vpd.getDocumentId(), vpd.getUser(), vpd.getBegin(),
                    vpd.getEnd());

            MViewportInit fullRender = new MViewportInit(vdoc);

            JsonNode newJson = JSONUtil.getObjectMapper().valueToTree(fullRender);

            JsonNode diff = JsonDiff.asJson(vps.getJson(), newJson);

            vps.setJson(newJson);

            msgTemplate.convertAndSend("/topic" + vpd.getTopic(),
                    new MViewportUpdate(aUpdateBegin, aUpdateEnd, diff));
        }
        catch (Exception ex) {
            log.error("Unable to render update", ex);
        }

        // finally {
        // MDC.remove(KEY_REPOSITORY_PATH);
        // }
    }

    private Project getProject(long aProjectId) throws AccessDeniedException
    {
        // Get current user - this will throw an exception if the current user does not exit
        User user = userRepository.getCurrentUser();

        // Get project
        Project project;
        try {
            project = projectService.getProject(aProjectId);
        }
        catch (NoResultException e) {
            throw new IllegalArgumentException("Project [" + aProjectId + "] not found.");
        }

        // Check for the access
        assertPermission("User [" + user.getUsername() + "] is not allowed to access project ["
                + aProjectId + "]", projectService.hasAnyRole(user, project));

        return project;
    }

    private void assertPermission(String aMessage, boolean aHasAccess) throws AccessDeniedException
    {
        if (!aHasAccess) {
            throw new AccessDeniedException(aMessage);
        }
    }
}
