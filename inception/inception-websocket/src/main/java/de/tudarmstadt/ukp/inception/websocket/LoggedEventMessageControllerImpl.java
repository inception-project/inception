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
package de.tudarmstadt.ukp.inception.websocket;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.SetUtils.unmodifiableSet;

import java.security.Principal;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Lazy;
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

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.inception.log.EventRepository;
import de.tudarmstadt.ukp.inception.log.adapter.EventLoggingAdapter;
import de.tudarmstadt.ukp.inception.log.adapter.GenericEventAdapter;
import de.tudarmstadt.ukp.inception.websocket.model.LoggedEventMessage;

@Controller
@ConditionalOnProperty({ "websocket.enabled", "websocket.loggedevent.enabled" })
public class LoggedEventMessageControllerImpl
    implements LoggedEventMessageController
{
    private static final int MAX_EVENTS = 5;
    private static final Set<String> GENRIC_EVENTS = unmodifiableSet( //
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
    private final List<EventLoggingAdapter<?>> eventAdapters;
    private final ProjectService projectService;
    private final DocumentService docService;
    private final EventRepository eventRepo;

    public LoggedEventMessageControllerImpl(@Autowired SimpMessagingTemplate aMsgTemplate,
            @Lazy @Autowired List<EventLoggingAdapter<?>> aAdapters,
            @Autowired DocumentService aDocService, @Autowired ProjectService aProjectService,
            @Autowired EventRepository aEventRepository)
    {
        msgTemplate = aMsgTemplate;
        eventAdapters = aAdapters;
        docService = aDocService;
        projectService = aProjectService;
        eventRepo = aEventRepository;
    }

    @EventListener
    @Override
    public void onApplicationEvent(ApplicationEvent aEvent)
    {
        EventLoggingAdapter<ApplicationEvent> adapter = getSpecificAdapter(aEvent);

        // If no adapter could be found, check if the generic adapter applies
        if (adapter == null && GenericEventAdapter.INSTANCE.accepts(aEvent)) {
            adapter = GenericEventAdapter.INSTANCE;
        }

        if (adapter == null) {
            return;
        }

        // FIXME: Why are we fetching the annotator user here? Why is the actual user not ok?
        String user = adapter.getAnnotator(aEvent);
        if (user == null) {
            user = adapter.getUser(aEvent);
        }

        LoggedEventMessage eventMsg = createLoggedEventMessage(user, adapter.getProject(aEvent),
                adapter.getCreated(aEvent), adapter.getEvent(aEvent), adapter.getDocument(aEvent));

        // System.out.printf("Sending: %s%n", eventMsg.getEventMsg());

        msgTemplate.convertAndSend(LOGGED_EVENTS_TOPIC, eventMsg);
    }

    @SuppressWarnings("unchecked")
    private EventLoggingAdapter<ApplicationEvent> getSpecificAdapter(ApplicationEvent aEvent)
    {
        Optional<EventLoggingAdapter<?>> eventAdapter = eventAdapters.stream() //
                .filter(adapter -> !(adapter instanceof GenericEventAdapter)
                        && adapter.accepts(aEvent))
                .findFirst();

        return (EventLoggingAdapter<ApplicationEvent>) eventAdapter.orElse(null);
    }

    @SubscribeMapping(LOGGED_EVENTS)
    @Override
    public List<LoggedEventMessage> getMostRecentLoggedEvents(Principal aPrincipal)
    {
        List<LoggedEventMessage> recentEvents = eventRepo
                .listFilteredRecentActivity(GENRIC_EVENTS, MAX_EVENTS).stream()
                .map(event -> createLoggedEventMessage(event.getUser(), event.getProject(),
                        event.getCreated(), event.getEvent(), event.getDocument()))
                .collect(toList());
        return recentEvents;
    }

    private LoggedEventMessage createLoggedEventMessage(String aUsername, long aProjectId,
            Date aCreated, String aEvent, long aDocId)
    {
        String projectName = null;
        String docName = null;
        if (aProjectId > -1) {
            projectName = projectService.getProject(aProjectId).getName();
            if (aDocId > -1) {
                docName = docService.getSourceDocument(aProjectId, aDocId).getName();
            }
        }
        LoggedEventMessage eventMsg = new LoggedEventMessage(aUsername, projectName, docName,
                aCreated);
        eventMsg.setEventMsg(aEvent);
        return eventMsg;
    }

    @Override
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Throwable exception)
    {
        return exception.getMessage();
    }
}
