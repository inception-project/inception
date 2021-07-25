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
import de.tudarmstadt.ukp.inception.experimental.api.AnnotationSystemAPIService;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.NewDocumentRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.NewViewportRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.relation.AllRelationRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.relation.CreateRelationRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.relation.DeleteRelationRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.relation.SelectRelationRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.relation.UpdateRelationRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.span.AllSpanRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.span.CreateSpanRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.span.DeleteSpanRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.span.SelectSpanRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.span.UpdateSpanRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.ErrorMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.NewDocumentResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.NewViewportResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.relation.AllRelationResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.relation.CreateRelationResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.relation.DeleteRelationResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.relation.SelectRelationResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.relation.UpdateRelationResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.span.AllSpanResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.span.CreateSpanResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.span.DeleteSpanResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.span.SelectSpanResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.span.UpdateSpanResponse;

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
    private static final String SERVER_RECEIVE_CLIENT_NEW_DOCUMENT = "/new_document_from_client";
    private static final String SERVER_SEND_CLIENT_NEW_DOCUMENT = "/queue/new_document_for_client/";

    // VIEWPORT
    private static final String SERVER_RECEIVE_CLIENT_NEW_VIEWPORT = "/new_viewport_from_client";
    private static final String SERVER_SEND_CLIENT_NEW_VIEWPORT = "/queue/new_viewport_for_client/";

    // SELECT
    private static final String SERVER_RECEIVE_CLIENT_SELECTED_ANNOTATION = "/select_annotation_from_client";
    private static final String SERVER_RECEIVE_CLIENT_SELECTED_RELATION = "/select_relation_from_client";

    private static final String SERVER_SEND_CLIENT_SELECTED_ANNOTATION = "/queue/selected_annotation_for_client/";
    private static final String SERVER_SEND_CLIENT_SELECTED_RELATION = "/queue/selected_relation_for_client/";

    // CREATE
    private static final String SERVER_RECEIVE_CLIENT_NEW_ANNOTATION = "/new_annotation_from_client";
    private static final String SERVER_RECEIVE_CLIENT_NEW_RELATION = "/new_relation_from_client";

    private static final String SERVER_SEND_CLIENT_NEW_ANNOTATION = "/topic/span_create_for_clients/";
    private static final String SERVER_SEND_CLIENT_NEW_RELATION = "/topic/relation_create_for_clients/";

    // DELETE
    private static final String SERVER_RECEIVE_CLIENT_DELETE_ANNOTATION = "/delete_annotation_from_client";
    private static final String SERVER_RECEIVE_CLIENT_DELETE_RELATION = "/delete_relation_from_client";

    private static final String SERVER_SEND_CLIENT_DELETE_ANNOTATION = "/topic/span_delete_for_clients/";
    private static final String SERVER_SEND_CLIENT_DELETE_RELATION = "/topic/relation_delete_for_clients/";

    // UPDATE
    private static final String SERVER_RECEIVE_CLIENT_UPDATE_ANNOTATION = "/update_annotation_from_client";
    private static final String SERVER_RECEIVE_CLIENT_UPDATE_RELATION = "/update_relation_from_client";

    private static final String SERVER_SEND_CLIENT_UPDATE_ANNOTATION = "/topic/span_update_for_clients/";
    private static final String SERVER_SEND_CLIENT_UPDATE_RELATION = "/topic/relation_update_for_clients/";

    // ALL SPANS
    private static final String SERVER_RECEIVE_CLIENT_ALL_SPANS = "/all_spans_from_client";
    private static final String SERVER_SEND_CLIENT_ALL_SPANS = "/topic/all_spans_for_client";

    // ALL RELATIONS
    private static final String SERVER_RECEIVE_CLIENT_ALL_RELATIONS = "/all_relations_from_client";
    private static final String SERVER_SEND_CLIENT_ALL_RELATIONS = "/topic/all_relation_for_client";

    // ERROR
    private static final String SERVER_SEND_CLIENT_ERROR_MESSAGE = "/queue/error_message/";

    public AnnotationProcessAPIImpl(ProjectService aProjectService,
            DocumentService aDocumentService, UserDao aUserDao,
            RepositoryProperties aRepositoryProperties,
            SimpMessagingTemplate aSimpMessagingTemplate,
            AnnotationSchemaService aAnnotationSchemaService,
            AnnotationSystemAPIService aAnnotationSystemAPIService,
            ColoringService aColoringService, UserPreferencesService aUserPreferencesService)
    {
        this.simpMessagingTemplate = aSimpMessagingTemplate;
        this.annotationSystemAPIImpl = new AnnotationSystemAPIImpl(aProjectService,
                aDocumentService, aUserDao, aRepositoryProperties, this, aAnnotationSchemaService,
                aAnnotationSystemAPIService, aColoringService, aUserPreferencesService);
    }

    /**
     * ------------- PUB / SUB HANDLING -------------
     **/

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_NEW_DOCUMENT)
    public void receiveNewDocumentRequest(Message<String> aMessage) throws IOException
    {
        LOG.debug("RECEIVED NEW DOCUMENT BY CLIENT, Message: " + aMessage);
        annotationSystemAPIImpl.handleNewDocument(
                JSONUtil.fromJsonString(NewDocumentRequest.class, aMessage.getPayload()));

    }

    @Override
    public void sendNewDocumentResponse(NewDocumentResponse aNewDocumentResponse, String aUser)
        throws IOException
    {
        LOG.debug(
                "SENDING NOW DOCUMENT UPDATE TO CLIENT " + aNewDocumentResponse.getViewportText());
        simpMessagingTemplate.convertAndSend(SERVER_SEND_CLIENT_NEW_DOCUMENT + aUser,
                JSONUtil.toJsonString(aNewDocumentResponse));
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_NEW_VIEWPORT)
    public void receiveNewViewportRequest(Message<String> aMessage) throws IOException
    {
        LOG.debug("RECEIVED NEW VIEWPORT BY CLIENT, Message: " + aMessage);
        annotationSystemAPIImpl.handleNewViewport(
                JSONUtil.fromJsonString(NewViewportRequest.class, aMessage.getPayload()));
    }

    @Override
    public void sendNewViewportResponse(NewViewportResponse aNewViewportResponse, String aUser)
        throws IOException
    {
        LOG.debug("SENDING NOW VIEWPORT TO CLIENT: " + aNewViewportResponse.getViewportText());

        simpMessagingTemplate.convertAndSend(SERVER_SEND_CLIENT_NEW_VIEWPORT + aUser,
                JSONUtil.toJsonString(aNewViewportResponse));
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_SELECTED_ANNOTATION)
    public void receiveSelectAnnotationRequest(Message<String> aMessage) throws IOException
    {
        LOG.debug("RECEIVED SELECT_ANNOTATION BY CLIENT, Message: " + aMessage);
        annotationSystemAPIImpl.handleSelectSpan(
                JSONUtil.fromJsonString(SelectSpanRequest.class, aMessage.getPayload()));
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_SELECTED_RELATION)
    public void receiveSelectRelationRequest(Message<String> aMessage) throws IOException
    {
        LOG.debug("RECEIVED SELECT_RELATION BY CLIENT, Message: " + aMessage);
        annotationSystemAPIImpl.handleSelectRelation(
                JSONUtil.fromJsonString(SelectRelationRequest.class, aMessage.getPayload()));
    }

    @Override
    public void sendSelectAnnotationResponse(SelectSpanResponse aSelectSpanResponse, String aUser)
        throws IOException
    {
        LOG.debug("SENDING NOW ANNOTATION SELECT TO CLIENT: " + aUser);
        simpMessagingTemplate.convertAndSend(SERVER_SEND_CLIENT_SELECTED_ANNOTATION + aUser,
                JSONUtil.toJsonString(aSelectSpanResponse));
    }

    @Override
    public void sendSelectRelationResponse(SelectRelationResponse aSelectRelationResponse,
            String aUser)
        throws IOException
    {
        LOG.debug("SENDING NOW ANNOTATION SELECT TO CLIENT: " + aUser);
        simpMessagingTemplate.convertAndSend(SERVER_SEND_CLIENT_SELECTED_RELATION + aUser,
                JSONUtil.toJsonString(aSelectRelationResponse));
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_UPDATE_ANNOTATION)
    public void receiveUpdateAnnotationRequest(Message<String> aMessage) throws IOException
    {
        LOG.debug("RECEIVED UPDATE ANNOTATION BY CLIENT, Message: " + aMessage);
        annotationSystemAPIImpl.handleUpdateSpan(
                JSONUtil.fromJsonString(UpdateSpanRequest.class, aMessage.getPayload()));
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_UPDATE_RELATION)
    public void receiveUpdateRelationRequest(Message<String> aMessage) throws IOException
    {
        LOG.debug("RECEIVED UPDATE RELATION BY CLIENT, Message: " + aMessage);
        annotationSystemAPIImpl.handleUpdateRelation(
                JSONUtil.fromJsonString(UpdateRelationRequest.class, aMessage.getPayload()));
    }

    @Override
    public void sendUpdateAnnotationResponse(UpdateSpanResponse aUpdateSpanResponse,
            String aProjectID, String aDocumentID, String aViewport)
        throws IOException
    {
        LOG.debug("SENDING NOW ANNOTATION UPDATE TO CLIENTS listening to: "
                + SERVER_SEND_CLIENT_UPDATE_ANNOTATION + aProjectID + "/" + aDocumentID + "/"
                + aViewport);
        simpMessagingTemplate.convertAndSend(SERVER_SEND_CLIENT_UPDATE_ANNOTATION + aProjectID + "/"
                + aDocumentID + "/" + aViewport, JSONUtil.toJsonString(aUpdateSpanResponse));
    }

    @Override
    public void sendUpdateRelationResponse(UpdateRelationResponse aUpdateRelationResponse,
            String aProjectID, String aDocumentID, String aViewport)
        throws IOException
    {
        LOG.debug("SENDING NOW RELATION UPDATE TO CLIENTS listening to: "
                + SERVER_SEND_CLIENT_UPDATE_RELATION + aProjectID + "/" + aDocumentID + "/"
                + aViewport);
        simpMessagingTemplate.convertAndSend(SERVER_SEND_CLIENT_UPDATE_RELATION + aProjectID + "/"
                + aDocumentID + "/" + aViewport, JSONUtil.toJsonString(aUpdateRelationResponse));
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_NEW_ANNOTATION)
    public void receiveCreateAnnotationRequest(Message<String> aMessage) throws IOException
    {
        LOG.debug("RECEIVED NEW ANNOTATION BY CLIENT, Message: " + aMessage);
        annotationSystemAPIImpl.handleCreateSpan(
                JSONUtil.fromJsonString(CreateSpanRequest.class, aMessage.getPayload()));
    }

    @Override
    public void sendCreateAnnotationResponse(CreateSpanResponse aCreateSpanResponse,
            String aProjectID, String aDocumentID, String aViewport)
        throws IOException
    {
        LOG.debug("SENDING NOW CREATE ANNOTATION TO CLIENTS listening to: "
                + SERVER_SEND_CLIENT_NEW_ANNOTATION + aProjectID + "/" + aDocumentID + "/"
                + aViewport);
        simpMessagingTemplate.convertAndSend(SERVER_SEND_CLIENT_NEW_ANNOTATION + aProjectID + "/"
                + aDocumentID + "/" + aViewport, JSONUtil.toJsonString(aCreateSpanResponse));
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_NEW_RELATION)
    public void receiveCreateRelationRequest(Message<String> aMessage) throws IOException
    {
        LOG.debug("RECEIVED NEW RELATION BY CLIENT, Message: " + aMessage);
        annotationSystemAPIImpl.handleCreateRelation(
                JSONUtil.fromJsonString(CreateRelationRequest.class, aMessage.getPayload()));
    }

    @Override
    public void sendCreateRelationResponse(CreateRelationResponse aCreateRelationResponse,
            String aProjectID, String aDocumentID, String aViewport)
        throws IOException
    {
        LOG.debug("SENDING NOW CREATE RELATION TO CLIENTS listening to: "
                + SERVER_SEND_CLIENT_NEW_RELATION + aProjectID + "/" + aDocumentID + "/"
                + aViewport);
        simpMessagingTemplate.convertAndSend(
                SERVER_SEND_CLIENT_NEW_RELATION + aProjectID + "/" + aDocumentID + "/" + aViewport,
                JSONUtil.toJsonString(aCreateRelationResponse));
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_DELETE_ANNOTATION)
    public void receiveDeleteAnnotationRequest(Message<String> aMessage) throws IOException
    {
        LOG.debug("RECEIVED DELETE ANNOTATION BY CLIENT");
        annotationSystemAPIImpl.handleDeleteSpan(
                JSONUtil.fromJsonString(DeleteSpanRequest.class, aMessage.getPayload()));
    }

    @Override
    public void sendDeleteAnnotationResponse(DeleteSpanResponse aDeleteSpanResponse,
            String aProjectID, String aDocumentID, String aViewport)
        throws IOException
    {
        LOG.debug("SENDING NOW DELETE ANNOTATION TO CLIENTS listening to: "
                + SERVER_SEND_CLIENT_DELETE_ANNOTATION + aProjectID + "/" + aDocumentID + "/"
                + aViewport);
        simpMessagingTemplate.convertAndSend(SERVER_SEND_CLIENT_DELETE_ANNOTATION + aProjectID + "/"
                + aDocumentID + "/" + aViewport, JSONUtil.toJsonString(aDeleteSpanResponse));
    }

    @Override

    @MessageMapping(SERVER_RECEIVE_CLIENT_DELETE_RELATION)
    public void receiveDeleteRelationRequest(Message<String> aMessage) throws IOException
    {
        LOG.debug("RECEIVED DELETE RELATION BY CLIENT");
        annotationSystemAPIImpl.handleDeleteRelation(
                JSONUtil.fromJsonString(DeleteRelationRequest.class, aMessage.getPayload()));
    }

    @Override
    public void sendDeleteRelationResponse(DeleteRelationResponse aDeleteRelationResponse,
            String aProjectID, String aDocumentID, String aViewport)
        throws IOException
    {
        LOG.debug("SENDING NOW DELETE RELATION TO CLIENTS listening to: "
                + SERVER_SEND_CLIENT_DELETE_RELATION + aProjectID + "/" + aDocumentID + "/"
                + aViewport);
        simpMessagingTemplate.convertAndSend(SERVER_SEND_CLIENT_DELETE_RELATION + aProjectID + "/"
                + aDocumentID + "/" + aViewport, JSONUtil.toJsonString(aDeleteRelationResponse));
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_ALL_SPANS)
    public void receiveAllSpansRequest(Message<String> aMessage) throws IOException
    {
        LOG.debug("RECEIVED ALL SPANS BY CLIENT");
        annotationSystemAPIImpl.handleAllSpans(
                JSONUtil.fromJsonString(AllSpanRequest.class, aMessage.getPayload()));
    }

    @Override
    public void sendAllSpansResponse(AllSpanResponse aAllSpanResponse, String aUser)
        throws IOException
    {
        LOG.debug("SENDING NOW ALL SPANS TO CLIENT: " + aUser);
        simpMessagingTemplate.convertAndSend(SERVER_SEND_CLIENT_ALL_SPANS + aUser,
                JSONUtil.toJsonString(aAllSpanResponse));
    }

    @Override

    @MessageMapping(SERVER_RECEIVE_CLIENT_ALL_RELATIONS)
    public void receiveAllRelationsRequest(Message<String> aMessage) throws IOException
    {
        LOG.debug("RECEIVED ALL SPANS BY CLIENT");
        annotationSystemAPIImpl.handleAllRelations(
                JSONUtil.fromJsonString(AllRelationRequest.class, aMessage.getPayload()));
    }

    @Override
    public void sendAllRelationsResponse(AllRelationResponse aAllRelationResponse, String aUser)
        throws IOException
    {
        LOG.debug("SENDING NOW ANNOTATION SELECT TO CLIENT: " + aUser);
        simpMessagingTemplate.convertAndSend(SERVER_SEND_CLIENT_ALL_RELATIONS + aUser,
                JSONUtil.toJsonString(aAllRelationResponse));
    }

    @Override
    public void sendErrorMessage(ErrorMessage aErrorMessage, String aUser) throws IOException
    {
        LOG.debug("SENDING NOW ERROR MESSAGE TO CLIENT: " + aUser);
        simpMessagingTemplate.convertAndSend(SERVER_SEND_CLIENT_ERROR_MESSAGE + aUser,
                JSONUtil.toJsonString(aErrorMessage));

    }
}
