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

import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.project.api.ProjectAccess;
import de.tudarmstadt.ukp.inception.scheduling.ProjectTask;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.scheduling.Task;
import de.tudarmstadt.ukp.inception.scheduling.controller.SchedulerController;

@ConditionalOnWebApplication
@RestController
@RequestMapping(SchedulerController.BASE_URL)
public class SchedulerControllerImpl
    implements SchedulerController
{
    private final SchedulingService schedulingService;
    private final UserDao userService;
    private final ProjectAccess projectAccess;

    public SchedulerControllerImpl(SchedulingService aSchedulingService, UserDao aUserDao,
            ProjectAccess aProjectAccess)
    {
        schedulingService = aSchedulingService;
        userService = aUserDao;
        projectAccess = aProjectAccess;
    }

    @PostMapping(//
            value = TASKS + "/{" + PARAM_TASK_ID + "}/" + CANCEL, //
            consumes = { ALL_VALUE }, //
            produces = APPLICATION_JSON_VALUE)
    public void cancelTask(@PathVariable(PARAM_TASK_ID) int aTaskId)
    {
        var sessionOwner = userService.getCurrentUser();

        schedulingService.stopAllTasksMatching(
                t -> t.getId() == aTaskId && canPerformActionOnTask(t, sessionOwner));
    }

    @PostMapping(//
            value = TASKS + "/{" + PARAM_TASK_ID + "}/" + ACKNOWLEDGE, //
            consumes = { ALL_VALUE }, //
            produces = APPLICATION_JSON_VALUE)
    public void acknowledgeResult(@PathVariable(PARAM_TASK_ID) int aTaskId)
    {
        var sessionOwner = userService.getCurrentUser();

        schedulingService.stopAllTasksMatching(
                t -> t.getId() == aTaskId && canPerformActionOnTask(t, sessionOwner));
    }

    private boolean canPerformActionOnTask(Task aTask, User aUser)
    {
        if (aTask instanceof ProjectTask) {
            return projectAccess.canManageProject(String.valueOf(aTask.getProject().getId()));
        }

        return aTask.getUser().filter(aUser::equals).isPresent();
    }
}
