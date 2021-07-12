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

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.inception.log.EventRepository;
import de.tudarmstadt.ukp.inception.log.adapter.EventLoggingAdapter;
import de.tudarmstadt.ukp.inception.websocket.model.LoggedEventMessage;

@Service
public class LoggedEventMessageServiceImpl implements LoggedEventMessageService
{
    private final ProjectService projectService;
    private final DocumentService docService;
    private final EventRepository eventRepo;
    private final List<EventLoggingAdapter<?>> eventAdapters;
    
    public LoggedEventMessageServiceImpl(@Lazy @Autowired List<EventLoggingAdapter<?>> aAdapters,
            @Autowired DocumentService aDocService, @Autowired ProjectService aProjectService,
            @Autowired EventRepository aEventRepository)
    {
        eventAdapters = aAdapters;
        docService = aDocService;
        projectService = aProjectService;
        eventRepo = aEventRepository;
    }
    
    public List<LoggedEventMessage> getMostRecentLoggedEvents(Set<String> aFilteredEvents, int aMaxEvents)
    {
        List<LoggedEventMessage> recentEvents = eventRepo
                .listFilteredRecentActivity(aFilteredEvents, aMaxEvents).stream()
                .map(event -> createLoggedEventMessage(event.getUser(), event.getProject(),
                        event.getCreated(), event.getEvent(), event.getDocument()))
                .collect(toList());
        return recentEvents;
    }
    
    @Override
    public LoggedEventMessage applicationEventToLoggedEventMessage(ApplicationEvent aEvent) {
        EventLoggingAdapter<ApplicationEvent> adapter = getSpecificAdapter(aEvent);

        if (adapter == null) {
            return null;
        }

        // FIXME: Why are we fetching the annotator user here? Why is the actual user not ok?
        String user = adapter.getAnnotator(aEvent);
        if (user == null) {
            user = adapter.getUser(aEvent);
        }

        LoggedEventMessage eventMsg = createLoggedEventMessage(user, adapter.getProject(aEvent),
                adapter.getCreated(aEvent), adapter.getEvent(aEvent), adapter.getDocument(aEvent));
        return eventMsg;
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
        LoggedEventMessage eventMsg = new LoggedEventMessage(aUsername, projectName, docName,
                aCreated, aEventType);
        return eventMsg;
    }
    
    @SuppressWarnings("unchecked")
    private EventLoggingAdapter<ApplicationEvent> getSpecificAdapter(ApplicationEvent aEvent)
    {
        Optional<EventLoggingAdapter<?>> eventAdapter = eventAdapters.stream() //
                .filter(adapter -> adapter.accepts(aEvent))
                .findFirst();

        return (EventLoggingAdapter<ApplicationEvent>) eventAdapter.orElse(null);
    }

}
