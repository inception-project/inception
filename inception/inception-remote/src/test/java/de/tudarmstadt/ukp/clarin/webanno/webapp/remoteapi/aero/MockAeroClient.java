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

import static de.tudarmstadt.ukp.inception.remoteapi.Controller_ImplBase.API_BASE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.io.File;
import java.io.UnsupportedEncodingException;

import org.apache.commons.io.FileUtils;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import de.tudarmstadt.ukp.inception.annotation.storage.OpenCasStorageSessionForRequestFilter;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.support.logging.LoggingFilter;

class MockAeroClient
{
    private MockMvc mvc;
    private String username;
    private String[] roles;

    MockAeroClient(WebApplicationContext aContext, String aUser, String... aRoles)
    {
        var repositoryProperties = aContext.getBean(RepositoryProperties.class);
        mvc = MockMvcBuilders //
                .webAppContextSetup(aContext) //
                // .alwaysDo(print()) //
                .apply(SecurityMockMvcConfigurers.springSecurity()) //
                .addFilters(new LoggingFilter(repositoryProperties.getPath().toString())) //
                .addFilters(new OpenCasStorageSessionForRequestFilter()) //
                .build();
        username = aUser;
        roles = aRoles;
    }

    MockAeroClient(MockMvc aMvc, String aUser, String... aRoles)
    {
        mvc = aMvc;
        username = aUser;
        roles = aRoles;
    }

    ResultActions importTextDocument(Object aProjectId, String aName, String aContent)
        throws Exception
    {
        return mvc.perform(multipart(API_BASE + "/projects/" + aProjectId + "/documents")
                .file("content", aContent.getBytes("UTF-8")) //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)) //
                .param("name", aName) //
                .param("format", "text"));
    }

    ResultActions exportTextDocument(Object aProjectId, long aDocId) throws Exception
    {
        return mvc.perform(get(API_BASE + "/projects/" + aProjectId + "/documents/" + aDocId)
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)) //
                .param("format", "text"));
    }

    ResultActions deleteDocument(Object aProjectId, long aDocId) throws Exception
    {
        return mvc.perform(delete(API_BASE + "/projects/" + aProjectId + "/documents/" + aDocId) //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)));
    }

    ResultActions listDocuments(Object aProjectId) throws Exception
    {
        return mvc.perform(get(API_BASE + "/projects/" + aProjectId + "/documents") //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)));
    }

    ResultActions createProject(String aSlug) throws Exception
    {
        return mvc.perform(post(API_BASE + "/projects") //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)) //
                .param("slug", aSlug));
    }

    ResultActions createProject(String aSlug, String aTitle) throws Exception
    {
        return mvc.perform(post(API_BASE + "/projects") //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)) //
                .param("slug", aSlug) //
                .param("title", aTitle));
    }

    /**
     * Create a project using the deprecated {@code name} parameter instead of {@code slug}.
     */
    ResultActions createProjectUsingNameParameter(String aSlug) throws Exception
    {
        return mvc.perform(post(API_BASE + "/projects") //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)) //
                .param("name", aSlug));
    }

    ResultActions createProjectUsingSlugAndNameParameters(String aSlug, String aName)
        throws Exception
    {
        return mvc.perform(post(API_BASE + "/projects") //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)) //
                .param("slug", aSlug) //
                .param("name", aName));
    }

    ResultActions createProjectWithoutSlugOrName() throws Exception
    {
        return mvc.perform(post(API_BASE + "/projects") //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)));
    }

    ResultActions readProject(Object aProjectId) throws Exception
    {
        return mvc.perform(get(API_BASE + "/projects/" + aProjectId) //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)));
    }

    ResultActions deleteProject(Object aProjectId) throws Exception
    {
        return mvc.perform(delete(API_BASE + "/projects/" + aProjectId) //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)));
    }

    ResultActions listProjects() throws Exception
    {
        return mvc.perform(get(API_BASE + "/projects") //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)));
    }

    ResultActions exportProject(Object aProjectId) throws Exception
    {
        return mvc.perform(get(API_BASE + "/projects/" + aProjectId + "/export.zip") //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)));
    }

    ResultActions importProject(File aExportFile, boolean aCreateMissingUsers,
            boolean aImportPermissions)
        throws Exception
    {
        var data = FileUtils.readFileToByteArray(aExportFile);
        return mvc.perform(multipart(API_BASE + "/projects/import") //
                .file("file", data) //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)) //
                .param("createMissingUsers", Boolean.toString(aCreateMissingUsers)) //
                .param("importPermissions", Boolean.toString(aImportPermissions)));
    }

    ResultActions createAnnotations(Object aProjectId, long aDocId, String aUser, String aContent)
        throws Exception
    {
        return createAnnotations(aProjectId, aDocId, aUser, aContent, null);
    }

    ResultActions createAnnotations(Object aProjectId, long aDocId, String aUser, String aContent,
            String aState)
        throws Exception
    {
        var url = API_BASE + "/projects/" + aProjectId + "/documents/" + aDocId + "/annotations/"
                + aUser;
        return mvc.perform(multipart(url) //
                .file("content", aContent.getBytes("UTF-8")) //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)) //
                .param("format", "text") //
                .param("state", aState));
    }

    ResultActions updateAnnotationState(Object aProjectId, int aDocId, String aUser, String aState)
        throws Exception
    {
        var url = API_BASE + "/projects/" + aProjectId + "/documents/" + aDocId + "/annotations/"
                + aUser + "/state";
        return mvc.perform(post(url) //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)) //
                .param("state", aState));
    }

    ResultActions listAnnotations(Object aProjectId, long aDocId) throws Exception
    {
        var url = API_BASE + "/projects/" + aProjectId + "/documents/" + aDocId + "/annotations";
        return mvc.perform(get(url) //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)));
    }

    ResultActions importCurations(Object aProjectId, long aDocId, String aContent, String aState)
        throws Exception, UnsupportedEncodingException
    {
        var url = API_BASE + "/projects/" + aProjectId + "/documents/" + aDocId + "/curation";
        return mvc.perform(multipart(url) //
                .file("content", aContent.getBytes("UTF-8")) //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)) //
                .param("format", "text") //
                .param("state", aState));
    }

    ResultActions deleteCurations(Object aProjectId, long aDocId) throws Exception
    {
        var url = API_BASE + "/projects/" + aProjectId + "/documents/" + aDocId + "/curation";
        return mvc.perform(delete(url) //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)));
    }

    ResultActions grantProjectRole(Object aProjectId, String aUser, String... aRoles)
        throws Exception
    {
        return mvc.perform(post(API_BASE + "/projects/" + aProjectId + "/permissions/" + aUser) //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)) //
                .param("roles", aRoles));
    }

    ResultActions grantProjectRoleWithoutRoles(Object aProjectId, String aUser) throws Exception
    {
        return mvc.perform(post(API_BASE + "/projects/" + aProjectId + "/permissions/" + aUser) //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)));
    }

    ResultActions listPermissionsForUser(Object aProjectId, String aUser) throws Exception
    {
        return mvc.perform(get(API_BASE + "/projects/" + aProjectId + "/permissions/" + aUser) //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)));
    }

    ResultActions listPermissionsForProject(Object aProjectId) throws Exception
    {
        return mvc.perform(get(API_BASE + "/projects/" + aProjectId + "/permissions") //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)));
    }

    ResultActions revokeProjectRole(Object aProjectId, String aUser, String... aRoles)
        throws Exception
    {
        return mvc.perform(delete(API_BASE + "/projects/" + aProjectId + "/permissions/" + aUser) //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)) //
                .param("roles", aRoles));
    }

    ResultActions revokeProjectRoleWithoutRoles(Object aProjectId, String aUser) throws Exception
    {
        return mvc.perform(delete(API_BASE + "/projects/" + aProjectId + "/permissions/" + aUser) //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)));
    }

    ResultActions listTasks(Object aProjectId) throws Exception
    {
        return mvc.perform(get(API_BASE + "/projects/" + aProjectId + "/tasks") //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)));
    }

    ResultActions cancelTask(Object aProjectId, long aTaskId) throws Exception
    {
        return mvc.perform(delete(API_BASE + "/projects/" + aProjectId + "/tasks/" + aTaskId) //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)));
    }

    ResultActions listUsers(Object aProjectId) throws Exception
    {
        return mvc.perform(get(API_BASE + "/projects/" + aProjectId + "/users") //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)));
    }

    ResultActions createUser(Object aProjectId, String aDisplayName) throws Exception
    {
        return mvc.perform(post(API_BASE + "/projects/" + aProjectId + "/users") //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)) //
                .param("name", aDisplayName));
    }

    ResultActions deleteUser(Object aProjectId, String aDisplayName) throws Exception
    {
        return mvc.perform(delete(API_BASE + "/projects/" + aProjectId + "/users") //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)) //
                .param("name", aDisplayName));
    }
}
