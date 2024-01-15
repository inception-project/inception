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

import java.security.Principal;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.scheduling.controller.SchedulerWebsocketController;
import de.tudarmstadt.ukp.inception.scheduling.controller.model.MTaskStateUpdate;

@Controller
@RequestMapping(SchedulerWebsocketController.BASE_URL)
@ConditionalOnProperty(prefix = "websocket", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulerWebsocketControllerImpl
    implements SchedulerWebsocketController
{
    private final SchedulingService schedulingService;

    @Autowired
    public SchedulerWebsocketControllerImpl(SchedulingService aSchedulingService)
    {
        schedulingService = aSchedulingService;
    }

    @SubscribeMapping(SchedulerWebsocketController.BASE_TOPIC + "/tasks")
    public List<MTaskStateUpdate> getCurrentTaskStates(Principal user) throws AccessDeniedException
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

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Throwable exception)
    {
        return exception.getMessage();
    }
}
