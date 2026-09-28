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

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;

import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskHandle;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.project.export.ProjectExportService;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

/**
 * Checks that fetching the export log requires being a manager of the project the export belongs
 * to. The run ID is a random UUID, but it is not a secret - it travels in the download URL and
 * therefore through browser history, referrer headers and proxy logs - so the endpoint must not
 * rely on it being unguessable.
 */
class ExportServiceControllerImplLogAccessTest
{
    private static final String RUN_ID = "0a5bd1a4-4d3e-4e4a-9b0e-2b9b6ac0d001";
    private static final String LOG_URL = ExportServiceController.BASE_URL + "/" + RUN_ID + "/log";

    private @Mock UserDao userService;
    private @Mock ProjectService projectService;
    private @Mock ProjectExportService projectExportService;

    private AutoCloseable mocks;
    private MockMvc mockMvc;

    private User user;
    private Project project;

    @BeforeEach
    void setup()
    {
        mocks = MockitoAnnotations.openMocks(this);

        user = new User("user", ROLE_USER);

        project = new Project("test-project");
        project.setId(1l);

        var monitor = new ProjectExportTaskMonitor(project, new ProjectExportTaskHandle(RUN_ID),
                "test-export", "test-export");
        monitor.addMessage(LogMessage.info(this, "Exported content for source document [%s]",
                "patient-notes.txt"));

        when(projectExportService.getTaskMonitor(any())).thenReturn(monitor);
        when(projectService.getProject(project.getId())).thenReturn(project);
        when(userService.getCurrentUser()).thenReturn(user);

        mockMvc = standaloneSetup(
                new ExportServiceControllerImpl(userService, projectService, projectExportService))
                        .build();
    }

    @AfterEach
    void tearDown() throws Exception
    {
        mocks.close();
    }

    @Test
    void thatLogAccessWithoutProjectPermissionIsRejected() throws Exception
    {
        when(projectService.hasRole(user, project, MANAGER)).thenReturn(false);
        when(userService.isAdministrator(user)).thenReturn(false);

        mockMvc.perform(get(LOG_URL)) //
                .andExpect(status().isNotFound());
    }

    @Test
    void thatLogAccessWithManagerPermissionIsAccepted() throws Exception
    {
        when(projectService.hasRole(user, project, MANAGER)).thenReturn(true);

        var response = mockMvc.perform(get(LOG_URL)) //
                .andExpect(status().isOk()) //
                .andReturn().getResponse().getContentAsString();

        assertThat(response).contains("Exported content for source document");
    }

    @Test
    void thatLogAccessAsAdministratorIsAccepted() throws Exception
    {
        when(projectService.hasRole(user, project, MANAGER)).thenReturn(false);
        when(userService.isAdministrator(user)).thenReturn(true);

        mockMvc.perform(get(LOG_URL)) //
                .andExpect(status().isOk());
    }

    @Test
    void thatLogAccessForUnknownRunIdIsRejected() throws Exception
    {
        when(projectExportService.getTaskMonitor(any())).thenReturn(null);

        mockMvc.perform(get(LOG_URL)) //
                .andExpect(status().isNotFound());
    }
}
