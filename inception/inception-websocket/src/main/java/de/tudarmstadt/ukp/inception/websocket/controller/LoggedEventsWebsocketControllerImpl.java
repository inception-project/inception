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

import static java.util.stream.Collectors.toList;

import java.security.Principal;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.inception.log.EventRepository;
import de.tudarmstadt.ukp.inception.log.adapter.EventLoggingAdapterRegistry;
import de.tudarmstadt.ukp.inception.websocket.model.LoggedEventMessage;

@Controller
@ConditionalOnExpression("${websocket.enabled:true} and ${websocket.logged-events.enabled:false}")
public class LoggedEventsWebsocketControllerImpl
    implements LoggedEventsWebsocketController
{
    private static final int MAX_EVENTS = 5;

    public static final String LOGGED_EVENTS = "/loggedEvents";
    public static final String LOGGED_EVENTS_TOPIC = "/topic" + LOGGED_EVENTS;

    private final SimpMessagingTemplate msgTemplate;
    private final ProjectService projectService;
    private final DocumentService docService;
    private final EventRepository eventRepo;
    private final EventLoggingAdapterRegistry adapterRegistry;

    @Autowired
    public LoggedEventsWebsocketControllerImpl(SimpMessagingTemplate aMsgTemplate,
            EventLoggingAdapterRegistry aAdapterRegistry, DocumentService aDocService,
            ProjectService aProjectService, EventRepository aEventRepository)
    {
        msgTemplate = aMsgTemplate;
        adapterRegistry = aAdapterRegistry;
        docService = aDocService;
        projectService = aProjectService;
        eventRepo = aEventRepository;
    }

    @EventListener
    @Override
    public void onApplicationEvent(ApplicationEvent aEvent)
    {
        adapterRegistry.getAdapter(aEvent)
                .map(a -> createLoggedEventMessage(a.getUser(aEvent), a.getProject(aEvent),
                        a.getCreated(aEvent), a.getEvent(aEvent), a.getDocument(aEvent)))
                .ifPresent(eventMsg -> msgTemplate.convertAndSend(LOGGED_EVENTS_TOPIC, eventMsg));
    }

    @SubscribeMapping(LOGGED_EVENTS)
    @Override
    public List<LoggedEventMessage> getMostRecentLoggedEvents(Principal aPrincipal)
    {
        List<LoggedEventMessage> recentEvents = getMostRecentLoggedEvents(aPrincipal.getName(),
                MAX_EVENTS);
        return recentEvents;
    }

    @Override
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Throwable exception)
    {
        return exception.getMessage();
    }

    private List<LoggedEventMessage> getMostRecentLoggedEvents(String aUsername, int aMaxEvents)
    {
        return eventRepo.listRecentActivity(aUsername, aMaxEvents).stream()
                .map(event -> createLoggedEventMessage(event.getUser(), event.getProject(),
                        event.getCreated(), event.getEvent(), event.getDocument()))
                .collect(toList());
    }

    private LoggedEventMessage createLoggedEventMessage(String aUsername, long aProjectId,
            Date aCreated, String aEventType, long aDocId)
    {
        String projectName = null;
        String docName = null;

        if (aProjectId > -1) {
            projectName = projectService.getProject(aProjectId).getName();

            if (aDocId > -1) {
                docName = docService.getSourceDocument(aProjectId, aDocId).getName();
            }
        }

        return new LoggedEventMessage(aUsername, projectName, docName, aCreated, aEventType);
    }
}
