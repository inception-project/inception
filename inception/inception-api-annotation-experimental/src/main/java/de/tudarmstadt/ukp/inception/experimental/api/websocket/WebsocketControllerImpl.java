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

import org.apache.uima.cas.CAS;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;
import de.tudarmstadt.ukp.inception.experimental.api.message.AnnotationMessage;
import de.tudarmstadt.ukp.inception.experimental.api.message.NewDocumentMessage;
import de.tudarmstadt.ukp.inception.experimental.api.message.ViewportMessage;

@Controller
@ConditionalOnProperty(prefix = "ui.editor.experimental", name = "enabled", havingValue = "true")
public class WebsocketControllerImpl
    implements WebsocketController
{
    private final DocumentService documentService;
    private final ProjectService projectService;
    private final UserDao userDao;
    private final RepositoryProperties repositoryProperties;
    private final SimpMessagingTemplate simpMessagingTemplate;

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

    private static final String SERVER_RECEIVE_CLIENT_NEW_ANNOTATION = "/new_annotation_by_client/";
    private static final String SERVER_RECEIVE_CLIENT_DELETE_ANNOTATION = "/delete_annotation_by_client";

    private static final String SERVER_SEND_CLIENT_UPDATE_ANNOTATION = "/topic/annotation_update_for_clients/";

    /**
     * -----------------------------------------------------
     **/

    public WebsocketControllerImpl(ProjectService aProjectService, DocumentService aDocumentService,
            UserDao aUserDao, RepositoryProperties aRepositoryProperties,
            SimpMessagingTemplate aSimpMessagingTemplate)
    {
        this.projectService = aProjectService;
        this.documentService = aDocumentService;
        this.userDao = aUserDao;
        this.repositoryProperties = aRepositoryProperties;
        this.simpMessagingTemplate = aSimpMessagingTemplate;
    }

    // ----------- ERROR HANDLER ------------- //
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Throwable aException)
    {
        return aException.getMessage();
    }
    // ---------------------------------------- //

    /**
     * ------------- PUB / SUB HANDLING -------------
     **/

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_NEW_DOCUMENT)
    public void handleDocumentRequest(Message<String> aMessage) throws IOException
    {
        System.out.println("RECEIVED NEW DOCUMENT BY CLIENT, Message: " + aMessage);
        String[] data = purifyData(aMessage.getPayload());
        CAS cas = getCasForDocument(data[0], data[1], "Doc4");

        NewDocumentMessage newDocumentMessage = new NewDocumentMessage();
        newDocumentMessage.setName("Doc4");
        String[] sentences = cas.getDocumentText().split("/.");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Integer.parseInt(data[2]); i++) {
            sb.append(sentences[i]);
        }
        newDocumentMessage.setViewportText(sb.toString());
        String msg = JSONUtil.toJsonString(newDocumentMessage);
        simpMessagingTemplate.convertAndSend(SERVER_SEND_CLIENT_NEW_DOCUMENT + data[0], msg);
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_NEW_VIEWPORT)
    public void handleViewportRequest(Message<String> aMessage) throws IOException
    {
        System.out.println("RECEIVED NEW VIEWPORT BY CLIENT, Message: " + aMessage);
        String[] data = purifyData(aMessage.getPayload());
        CAS cas = getCasForDocument(data[0], data[1], data[2]);
        ViewportMessage viewportMessage = new ViewportMessage(Integer.parseInt(data[3]), Integer.parseInt(data[4]));
        String[] sentences = cas.getDocumentText().split("/.");
        StringBuilder sb = new StringBuilder();
        if (sentences.length >= Integer.parseInt(data[4])) {
            for (int i = Integer.parseInt(data[3]); i < Integer.parseInt(data[4]); i++) {
                sb.append(sentences[i]);
            }
            viewportMessage.setText(sb.toString());
            String msg = JSONUtil.toJsonString(viewportMessage);
            simpMessagingTemplate.convertAndSend(SERVER_SEND_CLIENT_NEW_VIEWPORT + data[0], msg);
        } else {
            System.out.println("Requested sentences do not exist in the document");
        }
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_SELECTED_ANNOTATION)
    public void handleSelectAnnotation(Message<String> aMessage) throws IOException
    {
        System.out.println("RECEIVED SELECT_ANNOTATION BY CLIENT, Message: " + aMessage);
        String[] data = purifyData(aMessage.getPayload());
        CAS cas = getCasForDocument(data[0], data[1], data[2]);
        AnnotationMessage annotationMessage = new AnnotationMessage();
        // TODO retrieve desired content and fill AnnotationMessage
        String msg = JSONUtil.toJsonString(annotationMessage);
        simpMessagingTemplate.convertAndSend(SERVER_SEND_CLIENT_SELECTED_ANNOTATION + data[0],
                msg);
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_NEW_ANNOTATION)
    public void handleNewAnnotation(Message<String> aMessage) throws IOException
    {
        System.out.println("RECEIVED NEW ANNOTATION BY CLIENT");
        String[] data = purifyData(aMessage.getPayload());
        CAS cas = getCasForDocument(data[0], data[1], data[2]);

        // TODO cas.createAnnotation()
        AnnotationMessage annotationMessage = new AnnotationMessage();
        // TODO retrieve desired content and fill AnnotationMessage
        String msg = JSONUtil.toJsonString(annotationMessage);
        simpMessagingTemplate.convertAndSend(
            SERVER_SEND_CLIENT_UPDATE_ANNOTATION, msg);
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_DELETE_ANNOTATION)
    public void handleDeleteAnnotation(Message<String> aMessage) throws IOException
    {
        System.out.println("RECEIVED DELETE ANNOTATION BY CLIENT");
        String[] data = purifyData(aMessage.getPayload());
        CAS cas = getCasForDocument(data[0], data[1], data[2]);

        // TODO cas.deleteAnnotation()
        AnnotationMessage annotationMessage = new AnnotationMessage();
        // TODO retrieve desired content and fill AnnotationMessage
        String msg = JSONUtil.toJsonString(annotationMessage);
        simpMessagingTemplate.convertAndSend(
            SERVER_SEND_CLIENT_UPDATE_ANNOTATION,
                msg);

    }

    /**
     * -----------------------------------------------
     **/

    // --------------- SUPPORT METHODS ---------------- //

    @Override
    public String[] purifyData(String aPayload) {
        String[] spliced = aPayload.replace("\"", "").replace("{", "").replace("}", "").split(",");
        for (int i = 0; i < spliced.length; i++) {
            spliced[i] = spliced[i].split(":")[1];
        }
        return spliced;
    }

    @Override
    public CAS getCasForDocument(String aUser, String aProject, String aDocument)
    {
        try {
            CasStorageSession.open();
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
            Project project = projectService.getProject(aProject);
            SourceDocument sourceDocument = documentService.getSourceDocument(project, aDocument);
            CAS cas = documentService.readAnnotationCas(sourceDocument, aUser);
            CasStorageSession.get().close();
            return cas;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
