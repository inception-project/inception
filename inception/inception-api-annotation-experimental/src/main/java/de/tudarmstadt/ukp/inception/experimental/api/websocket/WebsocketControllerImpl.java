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
import org.apache.uima.cas.CASException;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

import com.google.gson.Gson;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.htmleditor.annotatorjs.model.Annotation;

@Controller
public class WebsocketControllerImpl
    implements WebsocketController
{
    private final DocumentService documentService;
    private final ProjectService projectService;
    private final UserDao userDao;

    // GSON for JSON Message transfer
    Gson g = new Gson();

    /** ----------------- PUB / SUB CHANNELS --------------- **/
    private static final String SERVER_RECEIVE_CLIENT_NEW_DOCUMENT = "/new_document_by_client";
    private static final String SERVER_SEND_CLIENT_NEW_DOCUMENT = "/queue/new_document_for_client/{aClient}";

    private static final String SERVER_RECEIVE_CLIENT_NEW_VIEWPORT = "/new_viewport_by_client";
    private static final String SERVER_SEND_CLIENT_NEW_VIEWPORT = "/queue/new_viewport_for_client/{aClient}";

    private static final String SERVER_RECEIVE_CLIENT_SELECTED_ANNOTATION = "/select_annotation_by_client";
    private static final String SERVER_SEND_CLIENT_SELECTED_ANNOTATION = "/queue/selected_annotation_for_client/{aClient}";

    private static final String SERVER_RECEIVE_CLIENT_NEW_ANNOTATION = "/new_annotation_by_client";
    private static final String SERVER_SEND_CLIENT_NEW_ANNOTATION = "/topic/annotation_created_for_clients/{aProject}/{aDocument}/{aViewportPosition}";

    private static final String SERVER_RECEIVE_CLIENT_DELETE_ANNOTATION = "/delete_annotation_by_client";
    private static final String SERVER_SEND_CLIENT_DELETE_ANNOTATION = "/topic/annotation_deleted_for_clients/{aProject}/{aDocument}/{aViewportPosition}";
    /** ----------------------------------------------------- **/

    public WebsocketControllerImpl(ProjectService aProjectService, DocumentService aDocumentService,
            UserDao aUserDao)
    {
        this.projectService = aProjectService;
        this.documentService = aDocumentService;
        this.userDao = aUserDao;
    }

    // ------- TO BE REMOVED, INFO ONLY --------- //
    @EventListener
    public void connectionEstablished(SessionConnectedEvent aSce) throws IOException, CASException
    {
        System.out.println("CONNECTION ESTABLISHED");
    }

    // ------- TO BE REMOVED, INFO ONLY --------- //
    @EventListener
    @Override
    public void onApplicationEvent(ApplicationEvent aEvent)
    {
        System.out.println("---- EVENT ---- : " + aEvent);
    }


    // ----------- ERROR HANDLER ------------- //
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Throwable aException)
    {
        return aException.getMessage();
    }
    // ---------------------------------------- //



    /** ------------- PUB / SUB HANDLING ------------- **/

    @MessageMapping(SERVER_RECEIVE_CLIENT_NEW_DOCUMENT)
    @Override
    public void handleNewDocument(Message<String> aMessage)
    {
        System.out.println("RECEIVED NEW DOCUMENT BY CLIENT, Message: " + aMessage);
    }

    @Override
    @SendTo(SERVER_SEND_CLIENT_NEW_DOCUMENT)
    public String publishNewDocument(@DestinationVariable String aClient, String aText)
    {
        System.out.println(
                "PUBLISHING NOW NEW DOCUMENT FOR CLIENT: " + aClient + ", Message: " + aText);
        return aText;
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_NEW_VIEWPORT)
    public void handleRequestViewport(Message<String> aMessage)
    {
        System.out.println("RECEIVED NEW VIEWPORT BY CLIENT, Message: " + aMessage);
    }

    @Override
    @SendTo(SERVER_SEND_CLIENT_NEW_VIEWPORT)
    public String publishRequestViewport(@DestinationVariable String aClient, String aText)
    {
        System.out.println(
                "PUBLISHING NOW NEW VIEWPORT FOR CLIENT: " + aClient + ", Message: " + aText);
        return aText;
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_SELECTED_ANNOTATION)
    public void handleSelectAnnotation(Message<String> aMessage)
    {
        System.out.println("RECEIVED SELECT_ANNOTATION BY CLIENT, Message: " + aMessage);
        publishSelectAnnotation("admin", new Annotation());
    }

    @Override
    @SendTo(SERVER_SEND_CLIENT_SELECTED_ANNOTATION)
    public Annotation publishSelectAnnotation(@DestinationVariable String aClient, Annotation aAnnotation)
    {
        System.out.println("PUBLISHING NOW SELECT ANNOTATION FOR CLIENT: " + aClient + ", Message: "
                + aAnnotation);
        return aAnnotation;
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_NEW_ANNOTATION)
    @SendTo(SERVER_SEND_CLIENT_NEW_ANNOTATION)
    public String handleNewAnnotation(Message<String> aMessage)
    {
        System.out.println("RECEIVED NEW ANNOTATION BY CLIENT");
        System.out.println(aMessage.getPayload());
        CAS cas = getCasForDocument("Annotation Study", "Doc4", "admin");

        return "CREATED";
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_DELETE_ANNOTATION)
    @SendTo(SERVER_SEND_CLIENT_DELETE_ANNOTATION)
    public String handleDeleteAnnotation(Message<String> aMessage)
    {
        CAS cas = getCasForDocument((String) aMessage.getHeaders().get("project"),
                (String) aMessage.getHeaders().get("document"),
                (String) aMessage.getHeaders().get("user"));
        return "DELETED";
    }

    /** ----------------------------------------------- **/

    // --------------- SUPPORT METHODS ---------------- //
    @Override
    public CAS getCasForDocument(String aProject, String aDocument, String aUser)
    {
        try {
            Project project = projectService.getProject(aProject);
            SourceDocument sourceDocument = documentService.getSourceDocument(project, aDocument);
            return documentService.readAnnotationCas(sourceDocument, aUser);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
