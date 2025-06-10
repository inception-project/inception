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
package de.tudarmstadt.ukp.inception.ui.scheduling.controller;

import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.PARAM_PROJECT;

import java.security.Principal;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import de.tudarmstadt.ukp.inception.scheduling.ProjectTask;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.scheduling.controller.SchedulerWebsocketController;
import de.tudarmstadt.ukp.inception.scheduling.controller.model.MTaskStateUpdate;
import jakarta.servlet.ServletContext;

@Controller
@RequestMapping(SchedulerWebsocketController.BASE_URL)
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "websocket", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulerWebsocketControllerImpl
    implements SchedulerWebsocketController
{
    private final SchedulingService schedulingService;
    private final SimpMessagingTemplate msgTemplate;

    @Autowired
    public SchedulerWebsocketControllerImpl(SchedulingService aSchedulingService,
            ServletContext aServletContext, SimpMessagingTemplate aMsgTemplate)
    {
        msgTemplate = aMsgTemplate;
        schedulingService = aSchedulingService;
    }

    @SubscribeMapping(USER_TASKS_TOPIC)
    public List<MTaskStateUpdate> onSubscribeToUserTaskUpdates(Principal user)
    {
        return schedulingService.getAllTasks().stream() //
                .filter(t -> t.getParentTask() == null) //
                .map(t -> t.getMonitor()) //
                .filter(Objects::nonNull) //
                .filter(t -> t.getUser() != null) //
                .filter(t -> t.getUser().equals(user.getName())) //
                .map(MTaskStateUpdate::new) //
                .toList();
    }

    @SubscribeMapping(PROJECT_TASKS_TOPIC_TEMPLATE)
    public List<MTaskStateUpdate> onSubscribeToProjectTaskUpdates(
            SimpMessageHeaderAccessor aHeaderAccessor, Principal aPrincipal, //
            @DestinationVariable(PARAM_PROJECT) long aProjectId)
    {
        return schedulingService.getAllTasks().stream() //
                .filter(t -> t.getParentTask() == null) //
                .filter(ProjectTask.class::isInstance) //
                .map(t -> t.getMonitor()) //
                .filter(Objects::nonNull) //
                .filter(t -> t.getProject() != null) //
                .filter(t -> Objects.equals(t.getProject().getId(), aProjectId)) //
                .map(MTaskStateUpdate::new) //
                .toList();
    }

    @Override
    public void dispatch(MTaskStateUpdate aUpdate)
    {
        if (aUpdate.getUsername() != null) {
            msgTemplate.convertAndSendToUser(aUpdate.getUsername(), "/queue" + USER_TASKS_TOPIC,
                    aUpdate);
        }

        if (aUpdate.getProjectId() > 0) {
            var topic = SchedulerWebsocketController
                    .getProjectTaskUpdatesTopic(aUpdate.getProjectId());
            msgTemplate.convertAndSend("/topic" + topic, aUpdate);
        }
    }

    @SendTo(PROJECT_TASKS_TOPIC_TEMPLATE)
    public MTaskStateUpdate send(MTaskStateUpdate aUpdate)
    {
        return aUpdate;
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Throwable exception)
    {
        return exception.getMessage();
    }
}
