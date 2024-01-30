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

import static java.util.stream.Collectors.toList;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.scheduling.controller.SchedulerController;
import de.tudarmstadt.ukp.inception.scheduling.controller.model.MTaskStateUpdate;

@Controller
@RequestMapping(SchedulerController.BASE_URL)
@ConditionalOnProperty(prefix = "websocket", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulerControllerImpl
    implements SchedulerController
{
    private final SchedulingService schedulingService;

    @Autowired
    public SchedulerControllerImpl(SchedulingService aSchedulingService)
    {
        schedulingService = aSchedulingService;
    }

    @SubscribeMapping(SchedulerController.BASE_TOPIC + "/tasks")
    public List<MTaskStateUpdate> getCurrentTaskStates(Principal user) throws AccessDeniedException
    {
        return schedulingService.getAllTasks().stream() //
                .map(t -> t.getMonitor()) //
                .filter(t -> user != null && t.getUser().equals(user.getName())) //
                .map(MTaskStateUpdate::new) //
                .collect(toList());
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Throwable exception)
    {
        return exception.getMessage();
    }
}
