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
package de.tudarmstadt.ukp.inception.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("security.oauth2.roles")
public class SecurityOAuthRolesPropertiesImpl
    implements SecurityOAuthRolesProperties
{
    private boolean enabled = false;
    private String claim = "groups";
    private String admin;
    private String user;
    private String projectCreator;
    private String remote;

    @Override
    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean aEnabled)
    {
        enabled = aEnabled;
    }

    @Override
    public String getClaim()
    {
        return claim;
    }

    public void setClaim(String aClaim)
    {
        claim = aClaim;
    }

    @Override
    public String getAdmin()
    {
        return admin;
    }

    public void setAdmin(String aAdmin)
    {
        admin = aAdmin;
    }

    @Override
    public String getUser()
    {
        return user;
    }

    public void setUser(String aUser)
    {
        user = aUser;
    }

    @Override
    public String getProjectCreator()
    {
        return projectCreator;
    }

    public void setProjectCreator(String aProjectCreator)
    {
        projectCreator = aProjectCreator;
    }

    @Override
    public String getRemote()
    {
        return remote;
    }

    public void setRemote(String aRemote)
    {
        remote = aRemote;
    }
}
