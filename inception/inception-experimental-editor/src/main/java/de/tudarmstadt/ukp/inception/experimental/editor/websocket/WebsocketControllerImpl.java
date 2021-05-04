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
package de.tudarmstadt.ukp.inception.experimental.editor.websocket;

import static org.apache.commons.collections4.SetUtils.unmodifiableSet;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

import de.tudarmstadt.ukp.inception.htmleditor.annotatorjs.model.Annotation;

@Controller
public class WebsocketControllerImpl implements WebsocketController
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Set<String> genericEvents = unmodifiableSet();

    private List<String> connectedClientId = new ArrayList<>();

    /** ----------------- PUB / SUB CHANNELS --------------- **/

    private static final String SERVER_RECEIVE_CLIENT_SELECTED_ANNOTATION = "/select_annotation_by_client";
    private static final String SERVER_SEND_CLIENT_SELECTED_ANNOTATION = "/queue/selected_annotation_for_client";

    private static final String SERVER_RECEIVE_CLIENT_NEW_ANNOTATION = "/new_annotation_by_client";
    private static final String SERVER_SEND_CLIENT_NEW_ANNOTATION = "/topic/new_annotation_for_client";

    private static final String SERVER_RECEIVE_CLIENT_DELETE_ANNOTATION = "/delete_annotation_by_client";
    private static final String SERVER_SEND_CLIENT_DELETE_ANNOTATION = "/topic/delete_annotation_for_client";

    /** ----------------------------------------------------- **/

    @EventListener
    public void connectionEstablished(SessionConnectedEvent aSce)
    {
        MessageHeaders msgHeaders = aSce.getMessage().getHeaders();
        Principal princ = (Principal) msgHeaders.get("simpUser");
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(aSce.getMessage());
        List<String> nativeHeaders = sha.getNativeHeader("userId");
        if (nativeHeaders != null)
        {
            String userId = nativeHeaders.get(0);
            connectedClientId.add(userId);
        }
        else
        {
            String userId = princ.getName();
            connectedClientId.add(userId);
        }
    }


    // ------- TO BE REMOVED, INFO ONLY --------- //

    @EventListener
    @Override
    public void onApplicationEvent(ApplicationEvent aEvent) {
        System.out.println("---- EVENT ---- : " + aEvent);
    }

    // ------------------------------------------- //


    // ----------- ERROR HANDLER ------------- //

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Throwable aException)
    {
        return aException.getMessage();
    }

    // ---------------------------------------- //


    /** ------------- PUB / SUB HANDLING ------------- **/

    @MessageMapping(SERVER_RECEIVE_CLIENT_SELECTED_ANNOTATION)
    @SendTo(SERVER_SEND_CLIENT_SELECTED_ANNOTATION)
    public Annotation getSelectedAnnotationByClient(Message<String> aMessage) throws Exception {
        System.out.println("RECEIVED SELECT_ANNOTATION BY CLIENT, Message: " + aMessage);
        return new Annotation();
    }

    @MessageMapping(SERVER_RECEIVE_CLIENT_NEW_ANNOTATION)
    @SendTo(SERVER_SEND_CLIENT_NEW_ANNOTATION)
    public Annotation getNewAnnotationByClient(Message<String> aMessage) throws Exception {
        System.out.println("RECEIVED NEW ANNOTATION BY CLIENT, Message: " + aMessage);
        return new Annotation();
    }

    @MessageMapping(SERVER_RECEIVE_CLIENT_DELETE_ANNOTATION)
    @SendTo(SERVER_SEND_CLIENT_DELETE_ANNOTATION)
    public Annotation getDeleteAnnotationByClient(Message<String> aMessage) throws Exception {
        System.out.println("RECEIVED NEW ANNOTATION BY CLIENT, Message: " + aMessage);
        return new Annotation();
    }

    /** ----------------------------------------------- **/




    // --------------- GETTER AND SETTER ------------------ //

    public List<String> getConnectedClientId()
    {
        return connectedClientId;
    }
    public void setConnectedClientId(List<String> connectedClientId)
    {
        this.connectedClientId = connectedClientId;
    }

    // ---------------------------------------------------- //
}
