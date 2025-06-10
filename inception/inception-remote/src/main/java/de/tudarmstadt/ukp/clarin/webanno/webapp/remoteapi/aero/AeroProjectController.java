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

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RMessageLevel.INFO;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectState;
import de.tudarmstadt.ukp.clarin.webanno.model.ScriptDirection;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.exception.IllegalNameException;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.exception.ObjectExistsException;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.exception.UnsupportedFormatException;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RProject;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model.RResponse;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.config.RemoteApiAutoConfiguration;
import de.tudarmstadt.ukp.inception.project.export.ProjectExportService;
import de.tudarmstadt.ukp.inception.project.export.ProjectImportExportUtils;
import de.tudarmstadt.ukp.inception.remoteapi.Controller_ImplBase;
import de.tudarmstadt.ukp.inception.support.io.ZipUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link RemoteApiAutoConfiguration#aeroRemoteApiController}.
 * </p>
 */
@Tag(name = "Project Management", description = "Management of projects.")
@ConditionalOnExpression("false") // Auto-configured - avoid package scanning
@Controller
@RequestMapping(AeroProjectController.API_BASE)
public class AeroProjectController
    extends Controller_ImplBase
{
    private final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @Autowired ProjectExportService exportService;

    @Operation(summary = "List the projects accessible by the authenticated user")
    @GetMapping(value = ("/" + PROJECTS), produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<RResponse<List<RProject>>> list() throws Exception
    {
        // Get current user - this will throw an exception if the current user does not exit
        var sessionOwner = getSessionOwner();

        // Get projects with permission
        var accessibleProjects = projectService.listAccessibleProjects(sessionOwner);

        // Collect all the projects
        var projectList = new ArrayList<RProject>();
        for (var project : accessibleProjects) {
            projectList.add(new RProject(project));
        }
        return ResponseEntity.ok(new RResponse<>(projectList));
    }

    @Operation(summary = "Create a new project")
    @PostMapping(//
            value = ("/" + PROJECTS), //
            consumes = { ALL_VALUE, MULTIPART_FORM_DATA_VALUE }, //
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<RResponse<RProject>> create( //
            @RequestParam(PARAM_NAME) //
            @Schema(description = """
                    URL slug of the project. This is used to create a URL for the project.
                    """) //
            String aSlug, //
            @RequestParam(PARAM_TITLE) //
            @Schema(description = """
                    Name of the project. It not specified, the slug will be used as the name.
                    """) //
            Optional<String> aName, //
            @RequestParam(PARAM_CREATOR) //
            @Schema(description = """
                    Username of the user on whose behalf the project is created.
                    This user gets full initial permissions on the created project.
                    If this parameter is not set, the authenticated user will be used as the creator.
                    This option is only usable by instance administrators.
                    """) //
            Optional<String> aCreator, //
            UriComponentsBuilder aUcb)
        throws Exception
    {
        // Get current user - this will throw an exception if the current user does not exit
        var sessionOwner = getSessionOwner();

        // Check for the access
        assertPermission(
                "User [" + sessionOwner.getUsername() + "] is not allowed to create projects",
                userRepository.isProjectCreator(sessionOwner)
                        || userRepository.isAdministrator(sessionOwner));

        // Check if the user can create projects for another user
        assertPermission(
                "User [" + sessionOwner.getUsername()
                        + "] is not allowed to create projects for user ["
                        + aCreator.orElse("<unspecified>") + "]",
                userRepository.isAdministrator(sessionOwner) || (aCreator.isPresent()
                        && aCreator.get().equals(sessionOwner.getUsername())));

        // Existing project
        if (projectService.existsProjectWithSlug(aSlug)) {
            throw new ObjectExistsException(
                    "A project with URL slug [" + aSlug + "] already exists");
        }

        var projectName = aName.orElse(aSlug);
        if (!Project.isValidProjectName(projectName)) {
            throw new IllegalNameException("Illegal project name [%s]", projectName);
        }

        if (!Project.isValidProjectSlug(aSlug)) {
            throw new IllegalNameException("Illegal project slug [%s]", aSlug);
        }

        // Create the project and initialize tags
        LOG.info("Creating project [{}]", aSlug);
        Project project = new Project();
        project.setSlug(aSlug);
        project.setName(projectName);
        project.setScriptDirection(ScriptDirection.LTR);
        project.setState(ProjectState.NEW);
        projectService.createProject(project);
        projectService.initializeProject(project);

        // Create permission for the project creator
        String owner = aCreator.isPresent() ? aCreator.get() : sessionOwner.getUsername();
        projectService.assignRole(project, owner, MANAGER, CURATOR, ANNOTATOR);

        RResponse<RProject> response = new RResponse<>(new RProject(project));
        return ResponseEntity.created(aUcb.path(API_BASE + "/" + PROJECTS + "/{id}")
                .buildAndExpand(project.getId()).toUri()).body(response);
    }

    @Operation(summary = "Get information about a project")
    @GetMapping(value = ("/" + PROJECTS + "/{" + PARAM_PROJECT_ID
            + "}"), produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<RResponse<RProject>> read(@PathVariable(PARAM_PROJECT_ID) long aProjectId)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        var project = getProject(aProjectId);

        return ResponseEntity.ok(new RResponse<>(new RProject(project)));
    }

    @Operation(summary = "Delete an existing project")
    @DeleteMapping(value = ("/" + PROJECTS + "/{" + PARAM_PROJECT_ID
            + "}"), produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<RResponse<Void>> delete( //
            @PathVariable(PARAM_PROJECT_ID) //
            @Schema(description = """
                    Project identifier.
                    """) //
            long aProjectId)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        var project = getProject(aProjectId);

        projectService.removeProject(project);

        return ResponseEntity.ok(new RResponse<>(INFO, "Project [" + aProjectId + "] deleted."));
    }

    @Operation(summary = "Import a previously exported project")
    @PostMapping(//
            value = ("/" + PROJECTS + "/" + IMPORT), //
            consumes = MULTIPART_FORM_DATA_VALUE, //
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<RResponse<RProject>> upload(
            @RequestParam(name = PARAM_CREATE_MISSING_USERS, defaultValue = "false") //
            @Schema(description = """
                    Whether to create users present in the project but not present in the instance.
                    Created users must be manually enabled and a password set before they can log in.
                    This option is only available to instance administrators.
                    """) //
            boolean aCreateMissingUsers, //
            @RequestParam(name = PARAM_IMPORT_PERMISSIONS, defaultValue = "false") //
            @Schema(description = """
                    Whether to import project permissions.
                    If this option is not enabled, only the importing user will be set up as a project manager.
                    If it is enabled, any projects present in the exported project will be restored.
                    """) //
            boolean aImportPermissions, //
            @RequestPart(PARAM_FILE) //
            MultipartFile aFile)
        throws Exception
    {
        // Get current user - this will throw an exception if the current user does not exit
        var sessionOwner = getSessionOwner();
        var sessionOwnerIsAdministrator = userRepository.isAdministrator(sessionOwner);
        var sessionOwnerIsProjectCreator = userRepository.isProjectCreator(sessionOwner);

        // Check for the access
        assertPermission(
                "User [" + sessionOwner.getUsername() + "] is not allowed to import projects",
                sessionOwnerIsAdministrator || userRepository.isProjectCreator(sessionOwner));

        if (!sessionOwnerIsAdministrator) {
            if (aCreateMissingUsers) {
                assertPermission("User [" + sessionOwner.getUsername()
                        + "] is not allowed to create missing users", false);
            }
            if (aImportPermissions) {
                assertPermission("User [" + sessionOwner.getUsername()
                        + "] is not allowed to import permissions", false);
            }
        }

        // If the current user is an administrator and importing of permissions is *DISABLED*, we
        // configure the current user as a project manager. But if importing of permissions is
        // *ENABLED*, we do not set the admin up as a project manager because we would assume that
        // the admin wants to restore a project (maybe one exported from another instance) and in
        // that case we want to maintain the permissions the project originally had without adding
        // the admin as a manager.
        User manager = null;
        if (sessionOwnerIsAdministrator) {
            if (!aImportPermissions) {
                manager = sessionOwner;
            }
        }
        // If the current user is NOT an admin but a project creator then we assume that the user is
        // importing the project for own use, so we add the user as a project manager.
        else if (sessionOwnerIsProjectCreator) {
            manager = sessionOwner;
        }

        Project importedProject;
        var tempFile = File.createTempFile("inception-project-import", null);
        try (var is = new BufferedInputStream(aFile.getInputStream());
                var os = new FileOutputStream(tempFile);) {
            if (!ZipUtils.isZipStream(is)) {
                throw new UnsupportedFormatException("Invalid ZIP file");
            }

            IOUtils.copyLarge(is, os);

            if (!ProjectImportExportUtils.isValidProjectArchive(tempFile)) {
                throw new UnsupportedFormatException(
                        "Uploaded file is not an INCEpTION/WebAnno project archive");
            }

            var importRequest = ProjectImportRequest.builder() //
                    .withCreateMissingUsers(aCreateMissingUsers) //
                    .withImportPermissions(aImportPermissions) //
                    .withManager(manager) //
                    .build();
            importedProject = exportService.importProject(importRequest, new ZipFile(tempFile));
        }
        finally {
            tempFile.delete();
        }

        return ResponseEntity.ok(new RResponse<>(new RProject(importedProject)));
    }

    @Operation(summary = "Export a project to a ZIP file")
    @GetMapping(value = ("/" + PROJECTS + "/{" + PARAM_PROJECT_ID + "}/" + EXPORT), produces = {
            "application/zip", APPLICATION_JSON_VALUE })
    public ResponseEntity<InputStreamResource> download( //
            @PathVariable(PARAM_PROJECT_ID) //
            @Schema(description = """
                    Project identifier.
                    """) //
            long aProjectId, //
            @RequestParam(value = PARAM_FORMAT) //
            @Schema(description = """
                    If this parameter is specified, the project export will include a second copy
                    of the data in the specified format in addition to the internal format used
                    by the application. Setting a secondary format can be useful
                    for further processing of the data in external tools, e.g. for training
                    machine learning models.

                    Valid values typically include (unless disabled by the administrator):

                    - `text`: Plain text format (UTF-8).
                    - `xmi`: UIMA CAS XMI (XML 1.0) format.
                    - `jsoncas`: UIMA CAS JSON 0.4.0 format.

                    Additional format identifiers can be found in the format section of the user's guide.
                    """) //
            Optional<String> aFormat)
        throws Exception
    {
        // Get project (this also ensures that it exists and that the current user can access it
        var project = getProject(aProjectId);

        // Check if the format is supported
        if (aFormat.isPresent()) {
            importExportService.getWritableFormatById(aFormat.get())
                    .orElseThrow(() -> new UnsupportedFormatException(
                            "Format [%s] cannot be exported. Exportable formats are %s.",
                            aFormat.get(),
                            importExportService.getWritableFormats().stream()
                                    .map(FormatSupport::getId).sorted().collect(Collectors.toList())
                                    .toString()));
        }

        var request = new FullProjectExportRequest(project, aFormat.orElse(null), true);
        var monitor = new ProjectExportTaskMonitor(project, null, "report-export", "");
        var exportedFile = exportService.exportProject(request, monitor);

        // Turn the file into a resource and auto-delete the file when the resource closes the
        // stream.
        var result = new InputStreamResource(new FileInputStream(exportedFile)
        {
            @Override
            public void close() throws IOException
            {
                super.close();
                FileUtils.forceDelete(exportedFile);
            }
        });

        var httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.valueOf("application/zip"));
        httpHeaders.setContentLength(exportedFile.length());
        httpHeaders.set("Content-Disposition",
                "attachment; filename=\"" + exportedFile.getName() + "\"");

        return new ResponseEntity<>(result, httpHeaders, HttpStatus.OK);
    }
}
