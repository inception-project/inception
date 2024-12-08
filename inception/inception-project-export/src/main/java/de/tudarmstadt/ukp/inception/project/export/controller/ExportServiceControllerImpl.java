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
package de.tudarmstadt.ukp.inception.project.export.controller;

import static de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskState.COMPLETED;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static java.time.LocalDateTime.now;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskHandle;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.project.export.ProjectExportService;
import de.tudarmstadt.ukp.inception.project.export.model.MProjectExportStateUpdate;
import de.tudarmstadt.ukp.inception.project.export.model.RExportLogMessage;
import io.swagger.v3.oas.annotations.Operation;

@Controller
@RequestMapping(ExportServiceController.BASE_URL)
@ConditionalOnProperty(prefix = "websocket", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ExportServiceControllerImpl
    implements ExportServiceController
{
    private final UserDao userService;
    private final ProjectService projectService;
    private final ProjectExportService projectExportService;

    @Autowired
    public ExportServiceControllerImpl(UserDao aUserService, ProjectService aProjectService,
            ProjectExportService aProjectExportService)
    {
        userService = aUserService;
        projectService = aProjectService;
        projectExportService = aProjectExportService;
    }

    @SubscribeMapping(NS_PROJECT + "/{projectId}/exports")
    public List<MProjectExportStateUpdate> getCurrentExportStates(
            @DestinationVariable("projectId") long aProjectId)
        throws AccessDeniedException
    {
        Project project = projectService.getProject(aProjectId);

        return projectExportService.listRunningExportTasks(project).stream() //
                .map(taskInfo -> new MProjectExportStateUpdate(taskInfo.getMonitor())) //
                .collect(toList());
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Throwable exception)
    {
        return exception.getMessage();
    }

    @MessageMapping("/export/{runId}/cancel")
    public void cancelDownload(@DestinationVariable("runId") String aRunId)
    {
        ProjectExportTaskHandle handle = new ProjectExportTaskHandle(aRunId);
        ProjectExportTaskMonitor monitor = projectExportService.getTaskMonitor(handle);

        if (monitor == null) {
            return;
        }

        Project project = projectService.getProject(monitor.getProjectId());
        User user = userService.getCurrentUser();
        if (!projectService.hasRole(user, project, PermissionLevel.MANAGER)) {
            return;
        }

        projectExportService.cancelTask(handle);
    }

    @Operation(summary = "Fetch export log messages")
    @GetMapping(value = ("/{runId}/log"), produces = { "application/json" })
    public ResponseEntity<List<RExportLogMessage>> projectExportLog(
            @PathVariable("runId") String aRunId)
        throws Exception
    {
        ProjectExportTaskHandle handle = new ProjectExportTaskHandle(aRunId);
        ProjectExportTaskMonitor monitor = projectExportService.getTaskMonitor(handle);

        if (monitor == null) {
            return new ResponseEntity<>(NOT_FOUND);
        }

        List<RExportLogMessage> result = monitor.getMessages().stream()
                .map(msg -> new RExportLogMessage(msg)).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Download a finished export")
    @GetMapping(value = ("/{runId}/data"), produces = { "application/zip" })
    public ResponseEntity<InputStreamResource> projectExportData(
            @PathVariable("runId") String aRunId)
        throws Exception
    {
        var handle = new ProjectExportTaskHandle(aRunId);
        var monitor = projectExportService.getTaskMonitor(handle);

        if (monitor == null || (monitor.getState() != COMPLETED)) {
            return new ResponseEntity<>(NOT_FOUND);
        }

        // Get project (this also ensures that it exists). Then check user permissions.
        var project = projectService.getProject(monitor.getProjectId());
        var user = userService.getCurrentUser();
        if (!projectService.hasRole(user, project, MANAGER) && !userService.isAdministrator(user)) {
            return new ResponseEntity<>(NOT_FOUND);
        }

        // Turn the file into a resource and auto-delete the file when the resource closes the
        // stream.
        var exportedFile = monitor.getExportedFile();
        var result = new InputStreamResource(new FileInputStream(exportedFile)
        {
            @Override
            public void close() throws IOException
            {
                super.close();
                projectExportService.cancelTask(handle);
            }
        });

        var formattedDateTime = now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss"));
        var filename = monitor.getExportedFilenamePrefix() + "-" + project.getSlug() + "-"
                + formattedDateTime + ".zip";

        var httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.valueOf("application/zip"));
        httpHeaders.setContentLength(exportedFile.length());
        httpHeaders.set("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        return new ResponseEntity<>(result, httpHeaders, OK);
    }
}
