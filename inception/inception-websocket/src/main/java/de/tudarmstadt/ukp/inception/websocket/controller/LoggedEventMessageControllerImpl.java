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
package de.tudarmstadt.ukp.inception.websocket.controller;

import static org.apache.commons.collections4.SetUtils.unmodifiableSet;

import java.security.Principal;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.access.event.AbstractAuthorizationEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.core.session.SessionCreationEvent;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.support.ServletRequestHandledEvent;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import de.tudarmstadt.ukp.inception.websocket.model.LoggedEventMessage;

@Controller
@ConditionalOnProperty({ "websocket.enabled", "websocket.loggedevent.enabled" })
public class LoggedEventMessageControllerImpl
    implements LoggedEventMessageController
{
    private static final int MAX_EVENTS = 5;
    private static final Set<String> GENERIC_EVENTS = unmodifiableSet( //
            ApplicationEvent.class.getSimpleName(), //
            ApplicationContextEvent.class.getSimpleName(), //
            ServletRequestHandledEvent.class.getSimpleName(), //
            SessionCreationEvent.class.getSimpleName(), //
            SessionDestroyedEvent.class.getSimpleName(), //
            AbstractAuthorizationEvent.class.getSimpleName(), //
            AbstractAuthenticationEvent.class.getSimpleName(), //
            WebServerInitializedEvent.class.getSimpleName(), //
            SessionConnectedEvent.class.getSimpleName(), //
            SessionConnectEvent.class.getSimpleName(), //
            SessionDisconnectEvent.class.getSimpleName(), //
            SessionSubscribeEvent.class.getSimpleName(), //
            SessionUnsubscribeEvent.class.getSimpleName());

    public static final String LOGGED_EVENTS = "/loggedEvents";
    public static final String LOGGED_EVENTS_TOPIC = "/topic" + LOGGED_EVENTS;

    private final SimpMessagingTemplate msgTemplate;
    private final LoggedEventMessageService loggedEventService;

    public LoggedEventMessageControllerImpl(@Autowired SimpMessagingTemplate aMsgTemplate,
            @Autowired LoggedEventMessageService aLoggedEventService)
    {
        msgTemplate = aMsgTemplate;
        loggedEventService = aLoggedEventService;
    }

    @EventListener
    @Override
    public void onApplicationEvent(ApplicationEvent aEvent)
    {
        LoggedEventMessage eventMsg = loggedEventService.applicationEventToLoggedEventMessage(aEvent);
        
        if (eventMsg == null) {
            return;
        }

        // System.out.printf("Sending: %s%n", eventMsg.getEventMsg());
        msgTemplate.convertAndSend(LOGGED_EVENTS_TOPIC, eventMsg);
    }

    @SubscribeMapping(LOGGED_EVENTS)
    @Override
    public List<LoggedEventMessage> getMostRecentLoggedEvents(Principal aPrincipal)
    {
        List<LoggedEventMessage> recentEvents = loggedEventService
                .getMostRecentLoggedEvents(GENERIC_EVENTS, MAX_EVENTS);
        return recentEvents;
    }

    @Override
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Throwable exception)
    {
        return exception.getMessage();
    }
}
