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

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import de.tudarmstadt.ukp.inception.experimental.editor.websocket.model.WebsocketMessage;
import de.tudarmstadt.ukp.inception.htmleditor.annotatorjs.model.Annotation;

@Controller
public class WebsocketControllerImpl implements WebsocketController {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Set<String> genericEvents = unmodifiableSet();

    private static final String SELECT_ANNOTATION_BY_CLIENT_EVENT = "/selectEvent";
    private static final String SELECT_ANNOTATION_BY_CLIENT_EVENT_TOPIC =
        "/selectTopic" + SELECT_ANNOTATION_BY_CLIENT_EVENT;


    public WebsocketControllerImpl() {
    }

    @EventListener
    @Override
    public void onApplicationEvent(ApplicationEvent aEvent) {
        System.out.println("---- EVENT ---- : " + aEvent);
    }

    @Override
    public String getTopicChannel() {
        return SELECT_ANNOTATION_BY_CLIENT_EVENT;
    }


    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Throwable exception) {
        return exception.getMessage();
    }

    @MessageMapping("/select_annotation_by_client")
    @SendTo("/selected_annotation")
    public Annotation greeting(WebsocketMessage aMmessage) throws Exception {
        System.out.println("Reveiced SELECT_ANNOTATION BY CLIENT");
        return new Annotation();
    }
}
