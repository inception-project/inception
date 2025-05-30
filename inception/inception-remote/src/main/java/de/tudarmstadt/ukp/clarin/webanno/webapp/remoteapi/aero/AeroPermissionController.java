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
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RPermission;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RResponse;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.config.RemoteApiAutoConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link RemoteApiAutoConfiguration#aeroPermissionController}.
 * </p>
 */
@Tag(name = "Permission Management", description = "Management of project-level user permissions.")
@ConditionalOnExpression("false") // Auto-configured - avoid package scanning
@Controller
@RequestMapping(AeroPermissionController.API_BASE)
public class AeroPermissionController
    extends AeroController_ImplBase
{
    private final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Operation(summary = "List all permissions in the given project (non-AERO)")
    @GetMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + PERMISSIONS, //
            produces = { APPLICATION_JSON_VALUE })
    public ResponseEntity<RResponse<List<RPermission>>> read(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        var project = getProject(aProjectId);

        var sessionOwner = getSessionOwner();

        // Check for the access
        assertPermission(
                "User [" + sessionOwner.getUsername()
                        + "] is not allowed to list project permissions",
                projectService.hasRole(sessionOwner, project, MANAGER)
                        || userRepository.isAdministrator(sessionOwner));

        var permissions = projectService.listProjectPermissions(project).stream() //
                .map(RPermission::new) //
                .collect(toList());

        return ResponseEntity.ok(new RResponse<>(permissions));
    }

    @Operation(summary = "List permissions for a user in the given project (non-AERO)")
    @GetMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + PERMISSIONS + "/{"
                    + PARAM_ANNOTATOR_ID + "}", //
            produces = { APPLICATION_JSON_VALUE })
    public ResponseEntity<RResponse<List<RPermission>>> read(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId,
            @PathVariable(PARAM_ANNOTATOR_ID) String aSubjectUser)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        var project = getProject(aProjectId);

        var sessionOwner = getSessionOwner();

        // Check for the access
        assertPermission(
                "User [" + sessionOwner.getUsername()
                        + "] is not allowed to list project permissions",
                projectService.hasRole(sessionOwner, project, MANAGER)
                        || userRepository.isAdministrator(sessionOwner));

        var subjectUser = getUser(aSubjectUser);

        var permissions = projectService.listProjectPermissionLevel(subjectUser, project).stream()
                .map(RPermission::new) //
                .collect(toList());

        return ResponseEntity.ok(new RResponse<>(permissions));
    }

    @Operation(summary = "Assign roles to a user in the given project (non-AERO)")
    @PostMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + PERMISSIONS + "/{"
                    + PARAM_ANNOTATOR_ID + "}", //
            produces = { APPLICATION_JSON_VALUE })
    public ResponseEntity<RResponse<List<RPermission>>> create(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId,
            @PathVariable(PARAM_ANNOTATOR_ID) String aSubjectUser,
            @RequestParam(PARAM_ROLES) List<String> aRoles)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        var project = getProject(aProjectId);

        var sessionOwner = getSessionOwner();

        // Check for the access
        assertPermission(
                "User [" + sessionOwner.getUsername()
                        + "] is not allowed to manage project permissions",
                projectService.hasRole(sessionOwner, project, MANAGER)
                        || userRepository.isAdministrator(sessionOwner));

        var subjectUser = getUser(aSubjectUser);

        var roles = aRoles.stream().map(PermissionLevel::valueOf).toArray(PermissionLevel[]::new);

        projectService.assignRole(project, subjectUser, roles);

        var permissions = projectService.listProjectPermissionLevel(subjectUser, project).stream()
                .map(RPermission::new) //
                .collect(toList());

        return ResponseEntity.ok(new RResponse<>(permissions));
    }

    @Operation(summary = "Revoke roles to a user in the given project (non-AERO)")
    @DeleteMapping( //
            value = "/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + PERMISSIONS + "/{"
                    + PARAM_ANNOTATOR_ID + "}", //
            produces = { APPLICATION_JSON_VALUE })
    public ResponseEntity<RResponse<List<RPermission>>> delete(
            @PathVariable(PARAM_PROJECT_ID) long aProjectId,
            @PathVariable(PARAM_ANNOTATOR_ID) String aSubjectUser,
            @RequestParam(PARAM_ROLES) List<String> aRoles)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        var project = getProject(aProjectId);

        var sessionOwner = getSessionOwner();

        // Check for the access
        assertPermission(
                "User [" + sessionOwner.getUsername()
                        + "] is not allowed to manage project permissions",
                projectService.hasRole(sessionOwner, project, MANAGER)
                        || userRepository.isAdministrator(sessionOwner));

        var subjectUser = getUser(aSubjectUser);

        var roles = aRoles.stream() //
                .map(PermissionLevel::valueOf) //
                .toArray(PermissionLevel[]::new);

        projectService.revokeRole(project, subjectUser, roles);

        var permissions = projectService.listProjectPermissionLevel(subjectUser, project).stream()
                .map(RPermission::new) //
                .collect(toList());

        return ResponseEntity.ok(new RResponse<>(permissions));
    }
}
