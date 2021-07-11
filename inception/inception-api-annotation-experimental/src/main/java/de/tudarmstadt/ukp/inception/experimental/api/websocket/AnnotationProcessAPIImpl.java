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

import java.io.IOException;
import java.util.Arrays;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.experimental.api.AnnotationSystemAPIImpl;
import de.tudarmstadt.ukp.inception.experimental.api.AnnotationSystemAPIService;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.CreateAnnotationRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.DeleteAnnotationRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.NewDocumentRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.NewViewportRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.SelectAnnotationRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.UpdateAnnotationRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.CreateAnnotationResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.DeleteAnnotationResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.ErrorMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.NewDocumentResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.NewViewportResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.SelectAnnotationResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.UpdateAnnotationResponse;

@Controller
@ConditionalOnProperty(prefix = "websocket", name = "enabled", havingValue = "true")
public class AnnotationProcessAPIImpl
    implements AnnotationProcessAPI
{
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final AnnotationSystemAPIImpl annotationSystemAPIImpl;

    /**
     * ----------------- PUB / SUB CHANNELS ---------------
     **/
    private static final String SERVER_RECEIVE_CLIENT_NEW_DOCUMENT = "/new_document_by_client";
    private static final String SERVER_SEND_CLIENT_NEW_DOCUMENT = "/queue/new_document_for_client/";

    private static final String SERVER_RECEIVE_CLIENT_NEW_VIEWPORT = "/new_viewport_by_client";
    private static final String SERVER_SEND_CLIENT_NEW_VIEWPORT = "/queue/new_viewport_for_client/";

    private static final String SERVER_RECEIVE_CLIENT_SELECTED_ANNOTATION = "/select_annotation_by_client";
    private static final String SERVER_SEND_CLIENT_SELECTED_ANNOTATION = "/queue/selected_annotation_for_client/";

    private static final String SERVER_RECEIVE_CLIENT_NEW_ANNOTATION = "/new_annotation_by_client";
    private static final String SERVER_RECEIVE_CLIENT_DELETE_ANNOTATION = "/delete_annotation_by_client";
    private static final String SERVER_RECEIVE_CLIENT_UPDATE_ANNOTATION = "/update_annotation_by_client";

    private static final String SERVER_SEND_CLIENT_UPDATE_ANNOTATION = "/topic/annotation_update_for_clients/";

    private static final String SERVER_SEND_CLIENT_ERROR_MESSAGE = "/queue/error_message/";

    public AnnotationProcessAPIImpl(ProjectService aProjectService,
            DocumentService aDocumentService, UserDao aUserDao,
            RepositoryProperties aRepositoryProperties,
            SimpMessagingTemplate aSimpMessagingTemplate,
            AnnotationSchemaService aAnnotationSchemaService,
            AnnotationSystemAPIService aAnnotationSystemAPIService)
    {
        this.simpMessagingTemplate = aSimpMessagingTemplate;
        this.annotationSystemAPIImpl = new AnnotationSystemAPIImpl(aProjectService,
                aDocumentService, aUserDao, aRepositoryProperties, this, aAnnotationSchemaService,
                aAnnotationSystemAPIService);
    }

    /**
     * ------------- PUB / SUB HANDLING -------------
     **/

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_NEW_DOCUMENT)
    public void receiveNewDocumentRequest(Message<String> aMessage) throws IOException
    {
        System.out.println("RECEIVED NEW DOCUMENT BY CLIENT, Message: " + aMessage);
        annotationSystemAPIImpl.handleNewDocument(
                JSONUtil.fromJsonString(NewDocumentRequest.class, aMessage.getPayload()));

    }

    @Override
    public void sendNewDocumentResponse(NewDocumentResponse aNewDocumentResponse, String aUser)
        throws IOException
    {
        System.out.println("SENDING NOW DOCUMENT UPDATE TO CLIENT "
                + Arrays.toString(aNewDocumentResponse.getViewportText()));
        simpMessagingTemplate.convertAndSend(SERVER_SEND_CLIENT_NEW_DOCUMENT + aUser,
                JSONUtil.toJsonString(aNewDocumentResponse));
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_NEW_VIEWPORT)
    public void receiveNewViewportRequest(Message<String> aMessage) throws IOException
    {
        System.out.println("RECEIVED NEW VIEWPORT BY CLIENT, Message: " + aMessage);
        annotationSystemAPIImpl.handleNewViewport(
                JSONUtil.fromJsonString(NewViewportRequest.class, aMessage.getPayload()));
    }

    @Override
    public void sendNewViewportResponse(NewViewportResponse aNewViewportResponse, String aUser)
        throws IOException
    {
        System.out.println("SENDING NOW VIEWPORT TO CLIENT: "
                + Arrays.toString(aNewViewportResponse.getViewportText()));

        simpMessagingTemplate.convertAndSend(SERVER_SEND_CLIENT_NEW_VIEWPORT + aUser,
                JSONUtil.toJsonString(aNewViewportResponse));
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_SELECTED_ANNOTATION)
    public void receiveSelectAnnotationRequest(Message<String> aMessage) throws IOException
    {
        System.out.println("RECEIVED SELECT_ANNOTATION BY CLIENT, Message: " + aMessage);
        annotationSystemAPIImpl.handleSelectAnnotation(
                JSONUtil.fromJsonString(SelectAnnotationRequest.class, aMessage.getPayload()));
    }

    @Override
    public void sendSelectAnnotationResponse(SelectAnnotationResponse aSelectAnnotationResponse,
            String aUser)
        throws IOException
    {
        System.out.println("SENDING NOW ANNOTATION SELECT TO CLIENT: " + aUser);
        simpMessagingTemplate.convertAndSend(SERVER_SEND_CLIENT_SELECTED_ANNOTATION + aUser,
                JSONUtil.toJsonString(aSelectAnnotationResponse));
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_UPDATE_ANNOTATION)
    public void receiveUpdateAnnotationRequest(Message<String> aMessage) throws IOException
    {
        System.out.println("RECEIVED UPDATE ANNOTATION BY CLIENT, Message: " + aMessage);
        annotationSystemAPIImpl.handleUpdateAnnotation(
                JSONUtil.fromJsonString(UpdateAnnotationRequest.class, aMessage.getPayload()));
    }

    @Override
    public void sendUpdateAnnotationResponse(UpdateAnnotationResponse aUpdateAnnotationResponse,
            String aProjectID, String aDocumentID, String aViewport)
        throws IOException
    {
        System.out.println("SENDING NOW ANNOTATION UPDATE TO CLIENTS listening to: "
                + SERVER_SEND_CLIENT_UPDATE_ANNOTATION + aProjectID + "/" + aDocumentID + "/"
                + aViewport);
        simpMessagingTemplate.convertAndSend(SERVER_SEND_CLIENT_UPDATE_ANNOTATION + aProjectID + "/"
                + aDocumentID + "/" + aViewport, JSONUtil.toJsonString(aUpdateAnnotationResponse));
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_NEW_ANNOTATION)
    public void receiveCreateAnnotationRequest(Message<String> aMessage) throws IOException
    {
        System.out.println("RECEIVED NEW ANNOTATION BY CLIENT, Message: " + aMessage);
        annotationSystemAPIImpl.handleCreateAnnotation(
                JSONUtil.fromJsonString(CreateAnnotationRequest.class, aMessage.getPayload()));
    }

    @Override
    public void sendCreateAnnotationResponse(CreateAnnotationResponse aCreateAnnotationResponse,
            String aProjectID, String aDocumentID, String aViewport)
        throws IOException
    {
        System.out.println("SENDING NOW CREATE ANNOTATION TO CLIENTS listening to: "
                + SERVER_SEND_CLIENT_UPDATE_ANNOTATION + aProjectID + "/" + aDocumentID + "/"
                + aViewport);
        simpMessagingTemplate.convertAndSend(SERVER_SEND_CLIENT_UPDATE_ANNOTATION + aProjectID + "/"
                + aDocumentID + "/" + aViewport, JSONUtil.toJsonString(aCreateAnnotationResponse));
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_DELETE_ANNOTATION)
    public void receiveDeleteAnnotationRequest(Message<String> aMessage) throws IOException
    {
        System.out.println("RECEIVED DELETE ANNOTATION BY CLIENT");
        annotationSystemAPIImpl.handleDeleteAnnotation(
                JSONUtil.fromJsonString(DeleteAnnotationRequest.class, aMessage.getPayload()));
    }

    @Override
    public void sendDeleteAnnotationResponse(DeleteAnnotationResponse aDeleteAnnotationResponse,
            String aProjectID, String aDocumentID, String aViewport)
        throws IOException
    {
        System.out.println("SENDING NOW DELETE ANNOTATION TO CLIENTS listening to: "
                + SERVER_SEND_CLIENT_UPDATE_ANNOTATION + aProjectID + "/" + aDocumentID + "/"
                + aViewport);
        simpMessagingTemplate.convertAndSend(SERVER_SEND_CLIENT_UPDATE_ANNOTATION + aProjectID + "/"
                + aDocumentID + "/" + aViewport, JSONUtil.toJsonString(aDeleteAnnotationResponse));
    }

    @Override
    public void sendErrorMessage(ErrorMessage aErrorMessage, String aUser) throws IOException
    {
        System.out.println("SENDING NOW ERROR MESSAGE TO CLIENT: " + aUser);
        simpMessagingTemplate.convertAndSend(SERVER_SEND_CLIENT_ERROR_MESSAGE + aUser,
                JSONUtil.toJsonString(aErrorMessage));

    }
}
