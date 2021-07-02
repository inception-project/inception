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
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.experimental.api.AnnotationSystemAPIImpl;
import de.tudarmstadt.ukp.inception.experimental.api.AnnotationSystemAPIService;
import de.tudarmstadt.ukp.inception.experimental.api.message.AnnotationMessage;
import de.tudarmstadt.ukp.inception.experimental.api.message.ClientMessage;
import de.tudarmstadt.ukp.inception.experimental.api.message.DocumentMessage;
import de.tudarmstadt.ukp.inception.experimental.api.message.ViewportMessage;

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
    private static final String SERVER_SEND_CLIENT_CONNECTION_MESSAGE = "/queue/connection_message/";

    private static final String SERVER_RECEIVE_CLIENT_NEW_DOCUMENT = "/new_document_by_client";
    private static final String SERVER_SEND_CLIENT_NEW_DOCUMENT = "/queue/new_document_for_client/";

    private static final String SERVER_RECEIVE_CLIENT_NEW_VIEWPORT = "/new_viewport_by_client";
    private static final String SERVER_SEND_CLIENT_NEW_VIEWPORT = "/queue/new_viewport_for_client/";

    private static final String SERVER_RECEIVE_CLIENT_SELECTED_ANNOTATION = "/select_annotation_by_client";
    private static final String SERVER_SEND_CLIENT_SELECTED_ANNOTATION = "/queue/selected_annotation_for_client/";

    private static final String SERVER_RECEIVE_CLIENT_NEW_ANNOTATION = "/new_annotation_by_client";
    private static final String SERVER_RECEIVE_CLIENT_DELETE_ANNOTATION = "/delete_annotation_by_client";

    private static final String SERVER_SEND_CLIENT_UPDATE_ANNOTATION = "/topic/annotation_update_for_clients/";

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

    // ----------- ERROR HANDLER ------------- //
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Throwable aException)
    {
        return aException.getMessage();
    }

    /**
     * ------------- PUB / SUB HANDLING -------------
     **/

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_NEW_DOCUMENT)
    public void handleReceiveDocumentRequest(Message<String> aMessage) throws IOException
    {
        System.out.println("RECEIVED NEW DOCUMENT BY CLIENT, Message: " + aMessage);
        annotationSystemAPIImpl.handleDocument(
                JSONUtil.fromJsonString(ClientMessage.class, aMessage.getPayload()));

    }

    @Override
    public void handleSendDocumentRequest(DocumentMessage aDocumentMessage, String aUser)
        throws IOException
    {
        System.out.println("SENDING NOW DOCUMENT UPDATE TO CLIENT "
                + Arrays.toString(aDocumentMessage.getViewportText()));
        simpMessagingTemplate.convertAndSend(SERVER_SEND_CLIENT_NEW_DOCUMENT + aUser,
                JSONUtil.toJsonString(aDocumentMessage));
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_NEW_VIEWPORT)
    public void handleReceiveViewportRequest(Message<String> aMessage) throws IOException
    {
        System.out.println("RECEIVED NEW VIEWPORT BY CLIENT, Message: " + aMessage);
        annotationSystemAPIImpl.handleViewport(
                JSONUtil.fromJsonString(ClientMessage.class, aMessage.getPayload()));
    }

    @Override
    public void handleSendViewportRequest(ViewportMessage aViewportMessage, String aUser)
        throws IOException
    {
        System.out.println("SENDING NOW VIEWPORT TO CLIENT: "
                + Arrays.toString(aViewportMessage.getViewportText()));

        simpMessagingTemplate.convertAndSend(SERVER_SEND_CLIENT_NEW_VIEWPORT + aUser,
                JSONUtil.toJsonString(aViewportMessage));
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_SELECTED_ANNOTATION)
    public void handleReceiveSelectAnnotation(Message<String> aMessage) throws IOException
    {
        System.out.println("RECEIVED SELECT_ANNOTATION BY CLIENT, Message: " + aMessage);
        annotationSystemAPIImpl.handleSelectAnnotation(
                JSONUtil.fromJsonString(ClientMessage.class, aMessage.getPayload()));
    }

    @Override
    public void handleSendSelectAnnotation(AnnotationMessage aAnnotationMessage, String aUser)
        throws IOException
    {
        System.out.println("SENDING NOW ANNOTATION SELECT TO CLIENT: " + aUser);
        simpMessagingTemplate.convertAndSend(SERVER_SEND_CLIENT_SELECTED_ANNOTATION + aUser,
                JSONUtil.toJsonString(aAnnotationMessage));
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_NEW_ANNOTATION)
    public void handleReceiveCreateAnnotation(Message<String> aMessage) throws IOException
    {
        System.out.println("RECEIVED NEW ANNOTATION BY CLIENT, Message: " + aMessage);
        annotationSystemAPIImpl.handleCreateAnnotation(
                JSONUtil.fromJsonString(ClientMessage.class, aMessage.getPayload()));
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_DELETE_ANNOTATION)
    public void handleReceiveDeleteAnnotation(Message<String> aMessage) throws IOException
    {
        System.out.println("RECEIVED DELETE ANNOTATION BY CLIENT");
        annotationSystemAPIImpl.handleDeleteAnnotation(
                JSONUtil.fromJsonString(ClientMessage.class, aMessage.getPayload()));
    }

    @Override
    public void handleSendUpdateAnnotation(AnnotationMessage aAnnotationMessage, String aProjectID,
            String aDocumentID, String aViewport)
        throws IOException
    {
        System.out.println("SENDING NOW ANNOTATION UPDATE TO CLIENTS to: " + SERVER_SEND_CLIENT_UPDATE_ANNOTATION + aProjectID + "/"
                            + aDocumentID + "/" + aViewport);
        simpMessagingTemplate.convertAndSend(SERVER_SEND_CLIENT_UPDATE_ANNOTATION + aProjectID + "/"
                + aDocumentID + "/" + aViewport, JSONUtil.toJsonString(aAnnotationMessage));
    }
}
