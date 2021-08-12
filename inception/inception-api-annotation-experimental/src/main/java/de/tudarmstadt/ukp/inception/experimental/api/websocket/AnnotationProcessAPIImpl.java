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
package de.tudarmstadt.ukp.inception.experimental.api.websocket;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences.UserPreferencesService;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.experimental.api.AnnotationSystemAPIImpl;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.DocumentRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.ViewportRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.arc.CreateArcRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.arc.DeleteArcRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.arc.SelectArcRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.arc.UpdateArcRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.span.CreateSpanRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.span.DeleteSpanRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.span.SelectSpanRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.span.UpdateSpanRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.DocumentResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.ErrorMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.ViewportResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.arc.CreateArcMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.arc.DeleteArcMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.arc.SelectArcResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.arc.UpdateArcMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.span.CreateSpanMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.span.DeleteSpanMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.span.SelectSpanResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.span.UpdateSpanMessage;

@Controller
@ConditionalOnProperty(prefix = "websocket", name = "enabled", havingValue = "true")
public class AnnotationProcessAPIImpl
    implements AnnotationProcessAPI
{

    private static final Logger LOG = getLogger(MethodHandles.lookup().lookupClass());

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final AnnotationSystemAPIImpl annotationSystemAPIImpl;

    /**
     * ----------------- PUB / SUB CHANNELS ---------------
     **/
    // NEXT DOCUMENT
    private static final String SERVER_RECEIVE_DOCUMENT_REQUEST = "/document_request";
    private static final String SERVER_SEND_DOCUMENT_REQUEST = "/queue/document/";

    // VIEWPORT
    private static final String SERVER_RECEIVE_VIEWPORT_REQUEST = "/viewport_request";
    private static final String SERVER_SEND_VIEWPORT_REQUEST = "/queue/viewport/";

    // SELECT
    private static final String SERVER_RECEIVE_SELECTED_SPAN = "/select_span";
    private static final String SERVER_RECEIVE_SELECTED_ARC = "/select_arc";

    private static final String SERVER_SEND_SELECTED_SPAN = "/queue/selected_span/";
    private static final String SERVER_SEND_SELECTED_ARC = "/queue/selected_arc/";

    // CREATE
    private static final String SERVER_RECEIVE_CREATE_SPAN = "/create_span";
    private static final String SERVER_RECEIVE_CREATE_ARC = "/create_arc";

    private static final String SERVER_SEND_CREATE_SPAN = "/topic/span_create/";
    private static final String SERVER_SEND_CREATE_ARC = "/topic/arc_create/";

    // DELETE
    private static final String SERVER_RECEIVE_DELETE_SPAN = "/delete_span";
    private static final String SERVER_RECEIVE_DELETE_ARC = "/delete_arc";

    private static final String SERVER_SEND_DELETE_SPAN = "/topic/span_delete/";
    private static final String SERVER_SEND_DELETE_ARC = "/topic/arc_delete/";

    // UPDATE
    private static final String SERVER_RECEIVE_UPDATE_SPAN = "/update_span";
    private static final String SERVER_RECEIVE_UPDATE_ARC = "/update_arc";

    private static final String SERVER_SEND_UPDATE_SPAN = "/topic/span_update/";
    private static final String SERVER_SEND_UPDATE_ARC = "/topic/arc_update/";

    // ERROR
    private static final String SERVER_SEND_CLIENT_ERROR_MESSAGE = "/queue/error_message/";

    public AnnotationProcessAPIImpl(ProjectService aProjectService,
            DocumentService aDocumentService, UserDao aUserDao,
            RepositoryProperties aRepositoryProperties,
            SimpMessagingTemplate aSimpMessagingTemplate,
            AnnotationSchemaService aAnnotationSchemaService,
            ColoringService aColoringService, UserPreferencesService aUserPreferencesService)
    {
        this.simpMessagingTemplate = aSimpMessagingTemplate;
        this.annotationSystemAPIImpl = new AnnotationSystemAPIImpl(aProjectService,
                aDocumentService, aUserDao, aRepositoryProperties, this, aAnnotationSchemaService,
                aColoringService, aUserPreferencesService);
    }

    /**
     * ------------- PUB / SUB HANDLING -------------
     **/

    @Override
    @MessageMapping(SERVER_RECEIVE_DOCUMENT_REQUEST)
    public void receiveDocumentRequest(Message<String> aMessage) throws IOException
    {
        LOG.debug("RECEIVED DOCUMENT REQUEST BY CLIENT, Message: " + aMessage);
        annotationSystemAPIImpl.handleDocumentRequest(
                JSONUtil.fromJsonString(DocumentRequest.class, aMessage.getPayload()));

    }

    @Override
    public void sendDocumentResponse(DocumentResponse aDocumentResponse, String aUser)
        throws IOException
    {
        LOG.debug(
                "SENDING NOW DOCUMENT TO CLIENT " + aDocumentResponse.getViewportText());
        simpMessagingTemplate.convertAndSend(SERVER_SEND_DOCUMENT_REQUEST + aUser,
                JSONUtil.toJsonString(aDocumentResponse));
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_VIEWPORT_REQUEST)
    public void receiveViewportRequest(Message<String> aMessage) throws IOException
    {
        LOG.debug("RECEIVED VIEWPORT REQUEST BY CLIENT, Message: " + aMessage);
        annotationSystemAPIImpl.handleViewportRequest(
                JSONUtil.fromJsonString(ViewportRequest.class, aMessage.getPayload()));
    }

    @Override
    public void sendViewportResponse(ViewportResponse aViewportResponse, String aUser)
        throws IOException
    {
        LOG.debug("SENDING NOW VIEWPORT TO CLIENT: " + aViewportResponse.getViewportText());

        simpMessagingTemplate.convertAndSend(SERVER_SEND_VIEWPORT_REQUEST + aUser,
                JSONUtil.toJsonString(aViewportResponse));
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_SELECTED_SPAN)
    public void receiveSelectSpanRequest(Message<String> aMessage) throws IOException
    {
        LOG.debug("RECEIVED SELECT_SPAN BY CLIENT, Message: " + aMessage);
        annotationSystemAPIImpl.handleSelectSpan(
                JSONUtil.fromJsonString(SelectSpanRequest.class, aMessage.getPayload()));
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_SELECTED_ARC)
    public void receiveSelectArcRequest(Message<String> aMessage) throws IOException
    {
        LOG.debug("RECEIVED SELECT_ARC BY CLIENT, Message: " + aMessage);
        annotationSystemAPIImpl.handleSelectArc(
                JSONUtil.fromJsonString(SelectArcRequest.class, aMessage.getPayload()));
    }

    @Override
    public void sendSelectSpanResponse(SelectSpanResponse aSelectSpanResponse, String aUser)
        throws IOException
    {
        LOG.debug("SENDING NOW SPAN SELECT TO CLIENT: " + aUser);
        simpMessagingTemplate.convertAndSend(SERVER_SEND_SELECTED_SPAN + aUser,
                JSONUtil.toJsonString(aSelectSpanResponse));
    }

    @Override
    public void sendSelectArcResponse(SelectArcResponse aSelectArcResponse,
                                           String aUser)
        throws IOException
    {
        LOG.debug("SENDING NOW ARC SELECT TO CLIENT: " + aUser);
        simpMessagingTemplate.convertAndSend(SERVER_SEND_SELECTED_ARC + aUser,
                JSONUtil.toJsonString(aSelectArcResponse));
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_UPDATE_SPAN)
    public void receiveUpdateSpanRequest(Message<String> aMessage) throws Exception
    {
        LOG.debug("RECEIVED UPDATE SPAN BY CLIENT, Message: " + aMessage);
        annotationSystemAPIImpl.handleUpdateSpan(
                JSONUtil.fromJsonString(UpdateSpanRequest.class, aMessage.getPayload()));
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_UPDATE_ARC)
    public void receiveUpdateArcRequest(Message<String> aMessage) throws Exception
    {
        LOG.debug("RECEIVED UPDATE ARC BY CLIENT, Message: " + aMessage);
        annotationSystemAPIImpl.handleUpdateArc(
                JSONUtil.fromJsonString(UpdateArcRequest.class, aMessage.getPayload()));
    }

    @Override
    public void sendUpdateSpan(UpdateSpanMessage aUpdateSpanMessage,
                                             String aProjectID, String aDocumentID, String aViewport)
        throws IOException
    {
        LOG.debug("SENDING NOW SPAN UPDATE TO CLIENTS listening to: "
                + SERVER_SEND_UPDATE_SPAN + aProjectID + "/" + aDocumentID + "/"
                + aViewport);
        simpMessagingTemplate.convertAndSend(SERVER_SEND_UPDATE_SPAN + aProjectID + "/"
                + aDocumentID + "/" + aViewport, JSONUtil.toJsonString(aUpdateSpanMessage));
    }

    @Override
    public void sendUpdateArc(UpdateArcMessage aUpdateArcMessage,
                                           String aProjectID, String aDocumentID, String aViewport)
        throws IOException
    {
        LOG.debug("SENDING NOW ARC UPDATE TO CLIENTS listening to: "
                + SERVER_SEND_UPDATE_ARC + aProjectID + "/" + aDocumentID + "/"
                + aViewport);
        simpMessagingTemplate.convertAndSend(SERVER_SEND_UPDATE_ARC + aProjectID + "/"
                + aDocumentID + "/" + aViewport, JSONUtil.toJsonString(aUpdateArcMessage));
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CREATE_SPAN)
    public void receiveCreateSpanRequest(Message<String> aMessage) throws IOException
    {
        LOG.debug("RECEIVED CREATE SPAN BY CLIENT, Message: " + aMessage);
        annotationSystemAPIImpl.handleCreateSpan(
                JSONUtil.fromJsonString(CreateSpanRequest.class, aMessage.getPayload()));
    }

    @Override
    public void sendCreateSpan(CreateSpanMessage aCreateSpanMessage,
                                             String aProjectID, String aDocumentID, String aViewport)
        throws IOException
    {
        LOG.debug("SENDING NOW CREATE SPAN TO CLIENTS listening to: "
                + SERVER_SEND_CREATE_SPAN + aProjectID + "/" + aDocumentID + "/"
                + aViewport);
        simpMessagingTemplate.convertAndSend(SERVER_SEND_CREATE_SPAN + aProjectID + "/"
                + aDocumentID + "/" + aViewport, JSONUtil.toJsonString(aCreateSpanMessage));
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CREATE_ARC)
    public void receiveCreateArcRequest(Message<String> aMessage) throws IOException
    {
        LOG.debug("RECEIVED CREATE ARC BY CLIENT, Message: " + aMessage);
        annotationSystemAPIImpl.handleCreateArc(
                JSONUtil.fromJsonString(CreateArcRequest.class, aMessage.getPayload()));
    }

    @Override
    public void sendCreateArc(CreateArcMessage aCreateArcMessage,
                                           String aProjectID, String aDocumentID, String aViewport)
        throws IOException
    {
        LOG.debug("SENDING NOW CREATE ARC TO CLIENTS listening to: "
                + SERVER_SEND_CREATE_ARC + aProjectID + "/" + aDocumentID + "/"
                + aViewport);
        simpMessagingTemplate.convertAndSend(
            SERVER_SEND_CREATE_ARC + aProjectID + "/" + aDocumentID + "/" + aViewport,
                JSONUtil.toJsonString(aCreateArcMessage));
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_DELETE_SPAN)
    public void receiveDeleteSpanRequest(Message<String> aMessage) throws IOException
    {
        LOG.debug("RECEIVED DELETE SPAN BY CLIENT");
        annotationSystemAPIImpl.handleDeleteSpan(
                JSONUtil.fromJsonString(DeleteSpanRequest.class, aMessage.getPayload()));
    }

    @Override
    public void sendDeleteSpan(DeleteSpanMessage aDeleteSpanMessage,
                                             String aProjectID, String aDocumentID, String aViewport)
        throws IOException
    {
        LOG.debug("SENDING NOW DELETE SPAN TO CLIENTS listening to: "
                + SERVER_SEND_DELETE_SPAN + aProjectID + "/" + aDocumentID + "/"
                + aViewport);
        simpMessagingTemplate.convertAndSend(SERVER_SEND_DELETE_SPAN + aProjectID + "/"
                + aDocumentID + "/" + aViewport, JSONUtil.toJsonString(aDeleteSpanMessage));
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_DELETE_ARC)
    public void receiveDeleteArcRequest(Message<String> aMessage) throws IOException
    {
        LOG.debug("RECEIVED DELETE ARC BY CLIENT");
        annotationSystemAPIImpl.handleDeleteArc(
                JSONUtil.fromJsonString(DeleteArcRequest.class, aMessage.getPayload()));
    }

    @Override
    public void sendDeleteArc(DeleteArcMessage aDeleteArcMessage,
                                           String aProjectID, String aDocumentID, String aViewport)
        throws IOException
    {
        LOG.debug("SENDING NOW DELETE ARC TO CLIENTS listening to: "
                + SERVER_SEND_DELETE_ARC + aProjectID + "/" + aDocumentID + "/"
                + aViewport);
        simpMessagingTemplate.convertAndSend(SERVER_SEND_DELETE_ARC + aProjectID + "/"
                + aDocumentID + "/" + aViewport, JSONUtil.toJsonString(aDeleteArcMessage));
    }

    @Override
    public void sendErrorMessage(ErrorMessage aErrorMessage, String aUser) throws IOException
    {
        LOG.debug("SENDING NOW ERROR MESSAGE TO CLIENT: " + aUser);
        simpMessagingTemplate.convertAndSend(SERVER_SEND_CLIENT_ERROR_MESSAGE + aUser,
                JSONUtil.toJsonString(aErrorMessage));

    }
}
