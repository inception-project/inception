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
package de.tudarmstadt.ukp.inception.remoteapi.next;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RMessageLevel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RResponse;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.config.RemoteApiAutoConfiguration;
import de.tudarmstadt.ukp.inception.remoteapi.Controller_ImplBase;
import de.tudarmstadt.ukp.inception.remoteapi.next.model.RTaskState;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.scheduling.TaskAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link RemoteApiAutoConfiguration#aeroTaskController}.
 * </p>
 */
@Tag(name = "Task Management (non-AERO)", description = "Management of long-runnig tasks.")
@ConditionalOnExpression("false") // Auto-configured - avoid package scanning
@Controller
@RequestMapping(TaskController.API_BASE)
public class TaskController
    extends Controller_ImplBase
{
    private @Autowired SchedulingService schedulingService;
    private @Autowired TaskAccess taskAccess;

    @Operation(summary = "List tasks and their respective states.")
    @GetMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + TASKS, //
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<RResponse<List<RTaskState>>> list( //
            @PathVariable(PARAM_PROJECT_ID) //
            @Schema(description = """
                    Project identifier.
                    """) //
            long aProjectId)
        throws Exception
    {
        var project = getProject(aProjectId);
        var sessionOwner = getSessionOwner();

        taskAccess.assertCanManageTasks(sessionOwner, project);

        var tasks = schedulingService.getAllTasks().stream() //
                .filter(task -> task.getSessionOwner().map(sessionOwner::equals).orElse(false)) //
                .map(RTaskState::new) //
                .toList();

        return ResponseEntity.ok(new RResponse<>(tasks));
    }

    @Operation(summary = "Cancel task.")
    @DeleteMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + TASKS + "/{" + PARAM_TASK_ID
                    + "}", //
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<RResponse<Void>> cancel( //
            @PathVariable(PARAM_PROJECT_ID) //
            @Schema(description = """
                    Project identifier.
                    """) //
            long aProjectId, //
            @PathVariable(PARAM_TASK_ID) //
            @Schema(description = """
                    Task identifier.
                    """) //
            long aTaskId)
        throws Exception
    {
        var project = getProject(aProjectId);
        var sessionOwner = getSessionOwner();

        taskAccess.assertCanManageTasks(sessionOwner, project);

        var count = schedulingService.stopAllTasksMatching(
                task -> task.getSessionOwner().map(sessionOwner::equals).orElse(false)
                        && task.getId() == aTaskId);

        if (count == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND) //
                    .body(new RResponse<>(RMessageLevel.ERROR, "Task not found"));
        }

        return ResponseEntity.ok(new RResponse<>(RMessageLevel.INFO, "Task cancelled"));
    }
}
