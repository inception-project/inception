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

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.log.adapter.EventLoggingAdapter;
import de.tudarmstadt.ukp.inception.log.adapter.GenericEventAdapter;
import de.tudarmstadt.ukp.inception.websocket.model.LoggedEventMessage;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link WebsocketConfig#loggedEventMessageService()}.
 * </p>
 */
public class LoggedEventMessageServiceImpl implements LoggedEventMessageService
{
    private static final String QUEUE_LOGGEDEVENTS = "/queue/loggedevents";
    private SimpMessagingTemplate msgTemplate;
    private List<EventLoggingAdapter<?>> eventAdapters;
    private ProjectService projectService;
    private DocumentService docService;
    
    private static final String SYSTEM_USER = "<SYSTEM>";
    
    
    public LoggedEventMessageServiceImpl(@Autowired SimpMessagingTemplate aMsgTemplate, 
            @Lazy @Autowired List<EventLoggingAdapter<?>> aAdapters, 
            @Autowired DocumentService aDocService, @Autowired ProjectService aProjectService) {
        msgTemplate = aMsgTemplate;
        eventAdapters = aAdapters;
        docService = aDocService;
        projectService = aProjectService;
    }
    
    @EventListener
    @Override
    public void onApplicationEvent(ApplicationEvent aEvent) {
        EventLoggingAdapter<ApplicationEvent> adapter = getSpecificAdapter(aEvent);
        if (adapter == null) {
            return;
        }
        
        // FIXME: it makes not that much sense to send events only to the users that created them
        // better if users could subscribe to a topic taking the project as a parameter 
        // and we would send to this topic here
        String username = adapter.getAnnotator(aEvent);
        if (username == null) {
            username = adapter.getUser(aEvent);
        }
        if (username.equals(SYSTEM_USER)) {
            return;
        }
        
        long projectId = adapter.getProject(aEvent);
        Project project = projectService.getProject(projectId);
        SourceDocument doc = docService.getSourceDocument(projectId, adapter.getDocument(aEvent));
        LoggedEventMessage eventMsg = new LoggedEventMessage(username,
                project.getName(), doc.getName(), adapter.getCreated(aEvent));
        eventMsg.setEventMsg(adapter.getEvent(aEvent));
        
        msgTemplate.convertAndSendToUser(username, QUEUE_LOGGEDEVENTS, eventMsg);
    }

    @SuppressWarnings("unchecked")
    private EventLoggingAdapter<ApplicationEvent> getSpecificAdapter(ApplicationEvent aEvent)
    {
        Optional<EventLoggingAdapter<?>> eventAdapter = eventAdapters.stream().filter(
                adapter -> !(adapter instanceof GenericEventAdapter) && adapter.accepts(aEvent))
                .findFirst();
        if (eventAdapter.isEmpty()) {
            return null;
        }
        return (EventLoggingAdapter<ApplicationEvent>) eventAdapter.get();
    }

    @Override
    public String getTopicChannel()
    {
        return QUEUE_LOGGEDEVENTS;
    }
    
}
