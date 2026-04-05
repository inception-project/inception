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

import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.fromJsonString;
import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.toJsonString;
import static de.tudarmstadt.ukp.inception.support.logging.Logging.KEY_REPOSITORY_PATH;
import static de.tudarmstadt.ukp.inception.support.logging.Logging.KEY_USERNAME;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.PARAM_DOCUMENT;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.PARAM_PROJECT;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.PARAM_USER;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.TOPIC_ELEMENT_DOCUMENT;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.TOPIC_ELEMENT_PROJECT;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.TOPIC_ELEMENT_USER;
import static java.lang.Integer.MAX_VALUE;
import static org.springframework.messaging.simp.SimpMessageType.MESSAGE;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.security.Principal;
import java.time.Duration;

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

import com.flipkart.zjsonpatch.Jackson3JsonDiff;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences.UserPreferencesService;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.diam.messages.MViewportInit;
import de.tudarmstadt.ukp.inception.diam.messages.MViewportUpdate;
import de.tudarmstadt.ukp.inception.diam.model.websocket.ViewportDefinition;
import de.tudarmstadt.ukp.inception.diam.model.websocket.ViewportState;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.documents.event.AfterCasWrittenEvent;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.recommendation.api.event.TransientAnnotationStateChangedEvent;
import de.tudarmstadt.ukp.inception.rendering.pipeline.RenderingPipeline;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.serialization.VDocumentSerializerExtensionPoint;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import jakarta.persistence.NoResultException;
import jakarta.servlet.ServletContext;
import tools.jackson.databind.JsonNode;

/**
 * Differential INCEpTION Annotation Messaging (DIAM) protocol controller.
 */
@ConditionalOnWebApplication
@ConditionalOnExpression("${websocket.enabled:true}")
@Controller
public class DiamWebsocketController
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String X_DIAM_EXTENSIONS = "X-DIAM-EXTENSIONS";
    public static final String X_DIAM_FORMAT = "X-DIAM-FORMAT";

    public static final String FORMAT_LEGACY = "legacy";

    public static final String PARAM_FROM = "from";
    public static final String PARAM_TO = "to";
    public static final String PARAM_FORMAT = "format";

    public static final PropertyPlaceholderHelper PLACEHOLDER_RESOLVER = new PropertyPlaceholderHelper(
            "{", "}", null, '\\', false);

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
    private final RenderingPipeline renderingPipeline;
    private final DocumentService documentService;
    private final RepositoryProperties repositoryProperties;
    private final AnnotationSchemaService schemaService;
    private final ProjectService projectService;
    private final UserDao userRepository;
    private final VDocumentSerializerExtensionPoint vDocumentSerializerExtensionPoint;
    private final UserPreferencesService userPreferencesService;
    private final ConstraintsService constraintsService;

    private final LoadingCache<ViewportDefinition, ViewportState> activeViewports;

    public DiamWebsocketController(SimpMessagingTemplate aMsgTemplate,
            RenderingPipeline aRenderingPipeline, DocumentService aDocumentService,
            RepositoryProperties aRepositoryProperties, AnnotationSchemaService aSchemaService,
            ProjectService aProjectService, UserDao aUserRepository,
            VDocumentSerializerExtensionPoint aVDocumentSerializerExtensionPoint,
            UserPreferencesService aUserPreferencesService, ServletContext aServletContext,
            ConstraintsService aConstraintsService)
    {
        msgTemplate = aMsgTemplate;
        renderingPipeline = aRenderingPipeline;
        documentService = aDocumentService;
        repositoryProperties = aRepositoryProperties;
        schemaService = aSchemaService;
        projectService = aProjectService;
        userRepository = aUserRepository;
        vDocumentSerializerExtensionPoint = aVDocumentSerializerExtensionPoint;
        userPreferencesService = aUserPreferencesService;
        constraintsService = aConstraintsService;

        activeViewports = Caffeine.newBuilder() //
                .expireAfterAccess(Duration.ofMinutes(aServletContext.getSessionTimeout())) //
                .build(this::initState);
    }

    @EventListener
    public void onSessionDisconnect(SessionDisconnectEvent aEvent)
    {
        if (LOG.isTraceEnabled()) {
            var user = aEvent.getUser();
            LOG.trace("Unsubscribing [{}]({}:*) from all viewports",
                    user != null ? user.getName() : "anonymous", aEvent.getSessionId());
        }

        activeViewports.asMap().entrySet().stream() //
                .filter(e -> {
                    e.getValue().removeSubscriptionsBySession(aEvent.getSessionId());
                    return !e.getValue().hasSubscriptions();
                }) //
                .forEach(e -> closeViewport(e.getKey()));
    }

    @EventListener
    public void onSessionUnsubscribe(SessionUnsubscribeEvent aEvent)
    {
        var headers = SimpMessageHeaderAccessor.wrap(aEvent.getMessage());

        var sessionId = headers.getSessionId();
        var subscriptionId = headers.getSubscriptionId();

        if (LOG.isTraceEnabled()) {
            var user = aEvent.getUser();
            LOG.trace("Unsubscribing [{}]({}:{})", user != null ? user.getName() : "anonymous",
                    sessionId, subscriptionId);
        }

        activeViewports.asMap().entrySet().stream() //
                .filter(e -> {
                    e.getValue().removeSubscription(sessionId, subscriptionId);
                    return !e.getValue().hasSubscriptions();
                }) //
                .forEach(e -> closeViewport(e.getKey()));
    }

    private void closeViewport(ViewportDefinition aVpd)
    {
        LOG.trace("Closing viewport {}", aVpd);
        activeViewports.invalidate(aVpd);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onAfterCasWritten(AfterCasWrittenEvent aEvent)
    {
        sendUpdate(aEvent.getDocument());
    }

    @EventListener
    public void onTransientAnnotationStateChanged(TransientAnnotationStateChangedEvent aEvent)
    {
        var doc = aEvent.getDocument();
        sendUpdate(doc, AnnotationSet.forUser(aEvent.getUser()), 0, MAX_VALUE);
    }

    @SubscribeMapping(DOCUMENT_VIEWPORT_TOPIC_TEMPLATE)
    public JsonNode onSubscribeToViewport(SimpMessageHeaderAccessor aHeaderAccessor,
            Principal aPrincipal, //
            @DestinationVariable(PARAM_PROJECT) long aProjectId,
            @DestinationVariable(PARAM_DOCUMENT) long aDocumentId,
            @DestinationVariable(PARAM_USER) String aDataOwner,
            @DestinationVariable(PARAM_FROM) int aViewportBegin,
            @DestinationVariable(PARAM_TO) int aViewportEnd)
        throws IOException
    {
        var project = getProject(aProjectId);

        try (var session = CasStorageSession.open()) {
            MDC.put(KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
            MDC.put(KEY_USERNAME, aPrincipal.getName());

            var format = aHeaderAccessor.getFirstNativeHeader(X_DIAM_FORMAT);
            if (format == null) {
                throw new IllegalArgumentException("Missing required websocket header ["
                        + X_DIAM_FORMAT + "] specifying the DIAM format.");
            }

            var enabledExtensions = fromJsonString(String[].class,
                    aHeaderAccessor.getFirstNativeHeader(X_DIAM_EXTENSIONS), new String[] {});

            var vpd = ViewportDefinition.builder() //
                    .withProjectId(aProjectId) //
                    .withDocumentId(aDocumentId) //
                    .withDataOwner(aDataOwner) //
                    .withRange(aViewportBegin, aViewportEnd) //
                    .withFormat(format) //
                    .enabledExtensions(enabledExtensions) //
                    .build();

            // Ensure that the viewport is registered
            var vps = activeViewports.get(vpd);

            LOG.trace("Subscribing [{}]({}:{}) to {}", aPrincipal.getName(),
                    aHeaderAccessor.getSessionId(), aHeaderAccessor.getSubscriptionId(), vpd);
            vps.addSubscription(aHeaderAccessor.getSessionId(),
                    aHeaderAccessor.getSubscriptionId());

            var json = render(project, vpd);
            vps.setJson(json);
            return json;
        }
        finally {
            MDC.remove(KEY_REPOSITORY_PATH);
            MDC.remove(KEY_USERNAME);
        }
    }

    private JsonNode render(Project aProject, ViewportDefinition aVpd) throws IOException
    {
        var doc = documentService.getSourceDocument(aProject.getId(), aVpd.getDocumentId());
        var sessionOwner = userRepository.getCurrentUsername();
        var dataOwner = userRepository.getUserOrCurationUser(aVpd.getUser());

        var constraints = constraintsService.getMergedConstraints(aProject);

        var cas = documentService.readAnnotationCas(doc, AnnotationSet.forUser(aVpd.getUser()));

        var prefs = userPreferencesService.loadPreferences(doc.getProject(), sessionOwner,
                Mode.ANNOTATION);

        var layers = schemaService.listSupportedLayers(aProject).stream()
                .filter(AnnotationLayer::isEnabled) //
                .filter(l -> !prefs.getHiddenAnnotationLayerIds().contains(l.getId())) //
                .toList();

        var allLayers = schemaService.listAnnotationLayer(aProject);

        var request = RenderRequest.builder() //
                .withSessionOwner(userRepository.getCurrentUser()) //
                .withDocument(doc, dataOwner) //
                .withConstraints(constraints) //
                .withWindow(aVpd.getBegin(), aVpd.getEnd()) //
                .withCas(cas) //
                .withVisibleLayers(layers) //
                .withAllLayers(allLayers) //
                .withEnabledExtensions(aVpd.getEnabledExtensions()) //
                .build();

        var vdoc = renderingPipeline.render(request);

        if (FORMAT_LEGACY.equals(aVpd.getFormat())) {
            return JSONUtil.getObjectMapper().valueToTree(new MViewportInit(vdoc));
        }

        var serializer = vDocumentSerializerExtensionPoint.getExtension(aVpd.getFormat())
                .orElseThrow(() -> new IllegalStateException(
                        "Unsupported format [" + aVpd.getFormat() + "]"));

        return JSONUtil.getObjectMapper().valueToTree(serializer.render(vdoc, request));
    }

    private ViewportState initState(ViewportDefinition aVpd)
    {
        return new ViewportState(aVpd);
    }

    private void sendUpdate(AnnotationDocument aDoc)
    {
        sendUpdate(aDoc.getDocument(), aDoc.getAnnotationSet(), 0, MAX_VALUE);
    }

    void sendUpdate(AnnotationDocument aDoc, int aUpdateBegin, int aUpdateEnd)
    {
        sendUpdate(aDoc.getDocument(), aDoc.getAnnotationSet(), aUpdateBegin, aUpdateEnd);
    }

    private void sendUpdate(SourceDocument aDoc, AnnotationSet aSet, int aUpdateBegin,
            int aUpdateEnd)
    {
        activeViewports.asMap().entrySet().stream() //
                .filter(e -> e.getKey().matches(aDoc.getId(), aSet, aUpdateBegin, aUpdateEnd)) //
                .forEach(e -> sendUpdate(e.getKey(), e.getValue(), aDoc, aUpdateBegin, aUpdateEnd));
    }

    private void sendUpdate(ViewportDefinition vpd, ViewportState vps, SourceDocument aDoc,
            int aUpdateBegin, int aUpdateEnd)
    {
        if (vps.getJson() == null) {
            LOG.trace("Not sending update for document {} - viewport not initialized yet", aDoc);
            return;
        }

        // MDC.put(KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

        try (var session = CasStorageSession.openNested()) {
            var project = projectService.getProject(vpd.getProjectId());
            var newJson = render(project, vpd);

            if (newJson.equals(vps.getJson())) {
                LOG.trace("Not sending update for document {} - no changes", aDoc);
                return;
            }

            LOG.trace("Sending update for document {} - {}", aDoc, vpd);

            var diff = Jackson3JsonDiff.asJson(vps.getJson(), newJson);

            vps.setJson(newJson);

            var sha = SimpMessageHeaderAccessor.create(MESSAGE);
            sha.setHeader(X_DIAM_FORMAT, vpd.getFormat());
            sha.setHeader(X_DIAM_EXTENSIONS,
                    toJsonString(vpd.getEnabledExtensions().stream().sorted().toList()));
            sha.setLeaveMutable(true);

            msgTemplate.convertAndSend("/topic" + vpd.getTopic(),
                    new MViewportUpdate(aUpdateBegin, aUpdateEnd, diff), sha.getMessageHeaders());
        }
        catch (Exception ex) {
            LOG.error("Unable to render update", ex);
        }

        // finally {
        // MDC.remove(KEY_REPOSITORY_PATH);
        // }
    }

    private Project getProject(long aProjectId) throws AccessDeniedException
    {
        // Get current user - this will throw an exception if the current user does not exit
        var user = userRepository.getCurrentUser();

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
