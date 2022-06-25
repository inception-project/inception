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

import static de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.AeroRemoteApiController.API_BASE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import java.io.UnsupportedEncodingException;

import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.OpenCasStorageSessionForRequestFilter;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LoggingFilter;

public class MockAeroClient
{
    private MockMvc mvc;
    private String username;
    private String[] roles;

    public MockAeroClient(WebApplicationContext aContext, String aUser, String... aRoles)
    {
        var repositoryProperties = aContext.getBean(RepositoryProperties.class);
        mvc = MockMvcBuilders //
                .webAppContextSetup(aContext) //
                .alwaysDo(print()) //
                .apply(SecurityMockMvcConfigurers.springSecurity()) //
                .addFilters(new LoggingFilter(repositoryProperties.getPath().toString())) //
                .addFilters(new OpenCasStorageSessionForRequestFilter()) //
                .build();
        username = aUser;
        roles = aRoles;
    }

    public MockAeroClient(MockMvc aMvc, String aUser, String... aRoles)
    {
        mvc = aMvc;
        username = aUser;
        roles = aRoles;
    }

    ResultActions performImportTextDocument(long aProjectId, String aName, String aContent)
        throws Exception
    {
        return mvc.perform(multipart(API_BASE + "/projects/" + aProjectId + "/documents")
                .file("content", aContent.getBytes("UTF-8")) //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)) //
                .param("name", aName) //
                .param("format", "text"));
    }

    public ResultActions performExportTextDocument(long aProjectId, long aDocId) throws Exception
    {
        return mvc.perform(get(API_BASE + "/projects/" + aProjectId + "/documents/" + aDocId)
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)) //
                .param("format", "text"));
    }

    public ResultActions performDeleteDocument(long aProjectId, long aDocId) throws Exception
    {
        return mvc.perform(delete(API_BASE + "/projects/" + aProjectId + "/documents/" + aDocId) //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)));
    }

    ResultActions performListDocuments(long aProjectId) throws Exception
    {
        return mvc.perform(get(API_BASE + "/projects/" + aProjectId + "/documents") //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)));
    }

    ResultActions performCreateProject(String aName) throws Exception
    {
        return mvc.perform(post(API_BASE + "/projects") //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)) //
                .param("name", aName));
    }

    ResultActions performDeleteProject(long aProjectId) throws Exception
    {
        return mvc.perform(delete(API_BASE + "/projects/" + aProjectId) //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)));
    }

    ResultActions performListProjects() throws Exception
    {
        return mvc.perform(get(API_BASE + "/projects") //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)));
    }

    ResultActions performCreateAnnotations(long aProjectId, long aDocId, String aUser,
            String aContent, String aState)
        throws Exception, UnsupportedEncodingException
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

    ResultActions performListAnnotations(long aProjectId, long aDocId) throws Exception
    {
        var url = API_BASE + "/projects/" + aProjectId + "/documents/" + aDocId + "/annotations";
        return mvc.perform(get(url) //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)));
    }

    ResultActions performImportCurations(long aProjectId, long aDocId, String aContent,
            String aState)
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

    ResultActions performDeleteCurations(long aProjectId, long aDocId) throws Exception
    {
        var url = API_BASE + "/projects/" + aProjectId + "/documents/" + aDocId + "/curation";
        return mvc.perform(delete(url) //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)));
    }

    ResultActions performGrantProjectRole(long aProjectId, String aUser, String... aRoles)
        throws Exception
    {
        return mvc.perform(post(API_BASE + "/projects/" + aProjectId + "/permissions/" + aUser) //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)) //
                .param("roles", aRoles));
    }

    ResultActions performListPermissionsForUser(long aProjectId, String aUser) throws Exception
    {
        return mvc.perform(get(API_BASE + "/projects/" + aProjectId + "/permissions/" + aUser) //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)));
    }

    ResultActions performListPermissionsForProject(long aProjectId) throws Exception
    {
        return mvc.perform(get(API_BASE + "/projects/" + aProjectId + "/permissions") //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)));
    }

    ResultActions performRevokeProjectRole(long aProjectId, String aUser, String... aRoles)
        throws Exception
    {
        return mvc.perform(delete(API_BASE + "/projects/" + aProjectId + "/permissions/" + aUser) //
                .with(csrf().asHeader()) //
                .with(user(username).roles(roles)) //
                .param("roles", aRoles));
    }
}
