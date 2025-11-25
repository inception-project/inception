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

import static de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RMessageLevel.ERROR;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RMessageLevel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RResponse;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.config.RemoteApiAutoConfiguration;
import de.tudarmstadt.ukp.inception.project.api.ProjectAccess;
import de.tudarmstadt.ukp.inception.remoteapi.Controller_ImplBase;
import de.tudarmstadt.ukp.inception.remoteapi.next.model.RUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link RemoteApiAutoConfiguration#userController}.
 * </p>
 */
@Tag(name = "User Management (non-AERO)", description = "Management of project-bound users.")
@ConditionalOnExpression("false") // Auto-configured - avoid package scanning
@Controller
@RequestMapping(UserController.API_BASE)
public class UserController
    extends Controller_ImplBase
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @Autowired ProjectAccess projectAccess;

    @Operation(summary = "List project-bound users in the given project")
    @GetMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + USERS, //
            produces = { APPLICATION_JSON_VALUE })
    public ResponseEntity<RResponse<List<RUser>>> list( //
            @PathVariable(PARAM_PROJECT_ID) //
            @Schema(description = """
                    Project identifier.
                    """) //
            long aProjectId)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        var project = getProject(aProjectId);

        var sessionOwner = getSessionOwner();

        // Check for the access
        projectAccess.assertCanManageProjectBoundUsers(sessionOwner, project);

        var users = projectService.listProjectBoundUsers(project).stream() //
                .map(RUser::new) //
                .collect(toList());

        return ResponseEntity.ok(new RResponse<>(users));
    }

    @Operation(summary = "Create project-bound user in the given project")
    @PostMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + USERS, //
            produces = { APPLICATION_JSON_VALUE })
    public ResponseEntity<RResponse<RUser>> create( //
            @PathVariable(PARAM_PROJECT_ID) //
            @Schema(description = """
                    Project identifier.
                    """) //
            long aProjectId, //
            @RequestParam(PARAM_NAME) //
            @Schema(description = """
                    Display name.
                    """) //
            String aDisplayName)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        var project = getProject(aProjectId);

        var sessionOwner = getSessionOwner();

        // Check for the access
        projectAccess.assertCanManageProjectBoundUsers(sessionOwner, project);

        if (projectService.getProjectBoundUser(project, aDisplayName).isPresent()) {
            return ResponseEntity.status(CONFLICT)
                    .body(new RResponse<>(ERROR, "User with display name [" + aDisplayName
                            + "] already exists in project [" + aProjectId + "]."));
        }

        var user = projectService.createProjectBoundUser(project, aDisplayName);

        return ResponseEntity.status(CREATED).body(new RResponse<>(new RUser(user)));
    }

    @Operation(summary = "Create project-bound user in the given project")
    @DeleteMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + USERS, //
            produces = { APPLICATION_JSON_VALUE })
    public ResponseEntity<RResponse<RUser>> delete( //
            @PathVariable(PARAM_PROJECT_ID) //
            @Schema(description = """
                    Project identifier.
                    """) //
            long aProjectId, //
            @RequestParam(PARAM_NAME) //
            @Schema(description = """
                    Display name.
                    """) //
            String aDisplayName)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        var project = getProject(aProjectId);

        var sessionOwner = getSessionOwner();

        // Check for the access
        projectAccess.assertCanManageProjectBoundUsers(sessionOwner, project);

        var user = projectService.getProjectBoundUser(project, aDisplayName);
        if (!user.isPresent()) {
            return ResponseEntity.status(NOT_FOUND)
                    .body(new RResponse<>(ERROR, "User with display name [" + aDisplayName
                            + "] already exists in project [" + aProjectId + "]."));
        }

        projectService.deleteProjectBoundUser(project, user.get());

        return ResponseEntity.ok()
                .body(new RResponse<>(RMessageLevel.INFO, "User [" + aDisplayName + "] deleted"));
    }
}
