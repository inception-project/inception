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
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.experimental.api.AnnotationSystemAPIImpl;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.DeleteAnnotationRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.DocumentRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.UpdateFeaturesRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.create.CreateArcRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.create.CreateSpanRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.AdviceMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.DeleteAnnotationMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.DocumentMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.UpdateFeatureMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.create.ArcCreatedMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.create.SpanCreatedMessage;

/**
 * Implementation of the Interface AnnotationProcessAPI within that package.
 * <p>
 * In order to activate this class, add 'websocket.enabled = true' in the application.yml file, @see
 * 'inception-app-webapp/src/main/resources/application.yml'.
 * <p>
 * For further details
 *
 * @see AnnotationProcessAPI interface class .
 **/
@Controller
@ConditionalOnProperty(prefix = "websocket", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AnnotationProcessAPIImpl
    implements AnnotationProcessAPI
{

    private static final Logger LOG = getLogger(MethodHandles.lookup().lookupClass());

    private static double create = 0;
    private double begin = 0;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final AnnotationSystemAPIImpl annotationSystemAPIImpl;

    /**
     * -------------------- PUBLISH / SUBSCRIBE CHANNELS ---------------------------
     * <p>
     * Easy Extensible: Simply follow the currently used approach to add a new send / receive topic.
     * <p>
     * For further details look into the README file for the annotation API in
     * 'src/main/ts/main/README.md' README file to see the streamlined process for extensions.
     **/
    // NEXT DOCUMENT
    private static final String SERVER_RECEIVE_DOCUMENT_REQUEST = "/document_request";
    private static final String SERVER_SEND_DOCUMENT_REQUEST = "/queue/document/";

    // CREATE
    private static final String SERVER_RECEIVE_CREATE_SPAN = "/span_create";
    private static final String SERVER_RECEIVE_CREATE_ARC = "/arc_create";

    private static final String SERVER_SEND_CREATE_SPAN = "/topic/span_create/";
    private static final String SERVER_SEND_CREATE_ARC = "/topic/arc_create/";

    // DELETE
    private static final String SERVER_RECEIVE_DELETE_ANNOTATION = "/annotation_delete";
    private static final String SERVER_SEND_DELETE_ANNOTATION = "/topic/annotation_delete/";

    // UPDATE
    private static final String SERVER_RECEIVE_UPDATE_FEATURES = "/features_update";
    private static final String SERVER_SEND_UPDATE_FEATURES = "/topic/features_update/";

    // ERROR
    private static final String SERVER_SEND_CLIENT_ERROR_MESSAGE = "/queue/error_message/";

    /**
     * Default Constructor
     *
     * @param aProjectService
     *            The Projectservice obtains projects
     * @param aDocumentService
     *            The Documentservice obtains SourceDocuments for the CAS
     * @param aRepositoryProperties
     *            The RepositoryPropterties are required for the CAS
     * @param aSimpMessagingTemplate
     *            The Messaging Template publish data to specific topics
     * @param aAnnotationSchemaService
     *            The AnnotationSchemaService contains methods which are related to TagSet, Tag and
     *            Type for the annotation Project
     * @param aColoringService
     *            The Coloringservice retrieves the correct color for specific annotations
     */
    public AnnotationProcessAPIImpl(ProjectService aProjectService,
            DocumentService aDocumentService, RepositoryProperties aRepositoryProperties,
            SimpMessagingTemplate aSimpMessagingTemplate,
            AnnotationSchemaService aAnnotationSchemaService, ColoringService aColoringService)
    {
        this.simpMessagingTemplate = aSimpMessagingTemplate;
        this.annotationSystemAPIImpl = new AnnotationSystemAPIImpl(aProjectService,
                aDocumentService, aRepositoryProperties, this, aAnnotationSchemaService,
                aColoringService);
    }

    /**
     * -------------------- PUBLISH / SUBSCRIBE HANDLER METHODS ------------------------
     * <p>
     * For further details see Interface class @see AnnotationProcessAPI.class
     **/

    @Override
    @MessageMapping(SERVER_RECEIVE_DOCUMENT_REQUEST)
    public void receiveDocumentRequest(Message<String> aMessage) throws IOException
    {
        LOG.debug("RECEIVED DOCUMENT REQUEST BY CLIENT, Message: {}", aMessage);
        annotationSystemAPIImpl.handleDocumentRequest(
                JSONUtil.fromJsonString(DocumentRequest.class, aMessage.getPayload()));

    }

    @Override
    public void sendDocumentResponse(DocumentMessage aDocumentResponse, String aUser)
        throws IOException
    {
        LOG.debug("SENDING NOW DOCUMENT TO CLIENT  {} ", aUser);
        simpMessagingTemplate.convertAndSend(SERVER_SEND_DOCUMENT_REQUEST + aUser,
                JSONUtil.toJsonString(aDocumentResponse));
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CREATE_SPAN)
    public void receiveCreateSpanRequest(Message<String> aMessage) throws IOException
    {
        LOG.debug("RECEIVED CREATE SPAN BY CLIENT, Message: {}", aMessage);
        annotationSystemAPIImpl.handleCreateSpan(
                JSONUtil.fromJsonString(CreateSpanRequest.class, aMessage.getPayload()));
    }

    @Override
    public void sendCreateSpan(SpanCreatedMessage aCreateSpanMessage, String aProjectID,
            String aDocumentID)
        throws IOException
    {

        LOG.debug("SENDING NOW CREATE SPAN TO CLIENTS listening to: {}",
                SERVER_SEND_CREATE_SPAN + aProjectID + "/" + aDocumentID);
        simpMessagingTemplate.convertAndSend(
                SERVER_SEND_CREATE_SPAN + aProjectID + "/" + aDocumentID,
                JSONUtil.toJsonString(aCreateSpanMessage));

    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CREATE_ARC)
    public void receiveCreateArcRequest(Message<String> aMessage) throws IOException
    {
        begin = System.currentTimeMillis();
        LOG.debug("RECEIVED CREATE ARC BY CLIENT, Message: {}", aMessage);
        annotationSystemAPIImpl.handleCreateArc(
                JSONUtil.fromJsonString(CreateArcRequest.class, aMessage.getPayload()));
    }

    @Override
    public void sendCreateArc(ArcCreatedMessage aCreateArcMessage, String aProjectID,
            String aDocumentID)
        throws IOException
    {
        LOG.debug("SENDING NOW CREATE ARC TO CLIENTS listening to: {}",
                SERVER_SEND_CREATE_ARC + aProjectID + "/" + aDocumentID);
        simpMessagingTemplate.convertAndSend(
                SERVER_SEND_CREATE_ARC + aProjectID + "/" + aDocumentID,
                JSONUtil.toJsonString(aCreateArcMessage));
        System.out.println("TIME CREATE: " + (System.currentTimeMillis() - begin));
        if (create == 0) {
            create = (System.currentTimeMillis() - begin);
        }
        else {
            create = (create + (System.currentTimeMillis() - begin)) / 2;
        }
        begin = 0;
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_UPDATE_FEATURES)
    public void receiveUpdateFeaturesRequest(Message<String> aMessage) throws Exception
    {
        LOG.debug("RECEIVED UPDATE FEATURES BY CLIENT, Message: {}", aMessage);
        annotationSystemAPIImpl.handleUpdateFeatures(
                JSONUtil.fromJsonString(UpdateFeaturesRequest.class, aMessage.getPayload()));
    }

    @Override
    public void sendUpdateFeatures(UpdateFeatureMessage aUpdateFeaturesMessage, String aProjectID,
            String aDocumentID)
        throws IOException
    {
        LOG.debug("SENDING NOW FEATURES UPDATE TO CLIENTS listening to: {}",
                SERVER_SEND_UPDATE_FEATURES + aProjectID + "/" + aDocumentID);
        simpMessagingTemplate.convertAndSend(
                SERVER_SEND_UPDATE_FEATURES + aProjectID + "/" + aDocumentID,
                JSONUtil.toJsonString(aUpdateFeaturesMessage));
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_DELETE_ANNOTATION)
    public void receiveDeleteAnnotationRequest(Message<String> aMessage) throws IOException
    {
        LOG.debug("RECEIVED UPDATE FEATURES BY CLIENT, Message: {}", aMessage);
        annotationSystemAPIImpl.handleDeleteAnnotation(
                JSONUtil.fromJsonString(DeleteAnnotationRequest.class, aMessage.getPayload()));

    }

    @Override
    public void sendDeleteAnnotation(DeleteAnnotationMessage aDeleteAnnotationMessage,
            String aProjectID, String aDocumentID)
        throws IOException
    {
        LOG.debug("SENDING NOW DELETE ANNOTATION TO CLIENTS listening to: {}",
                SERVER_SEND_DELETE_ANNOTATION + aProjectID + "/" + aDocumentID);
        simpMessagingTemplate.convertAndSend(
                SERVER_SEND_DELETE_ANNOTATION + aProjectID + "/" + aDocumentID,
                JSONUtil.toJsonString(aDeleteAnnotationMessage));
    }

    @Override
    public void sendAdviceMessage(AdviceMessage aAdviceMessage, String aUser) throws IOException
    {
        LOG.debug("SENDING NOW ADVICE MESSAGE TO CLIENT: {}", aUser);
        simpMessagingTemplate.convertAndSend(SERVER_SEND_CLIENT_ERROR_MESSAGE + aUser,
                JSONUtil.toJsonString(aAdviceMessage));
    }
}
