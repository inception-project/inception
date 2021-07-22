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
package de.tudarmstadt.ukp.clarin.webanno.export.model;

import java.util.Date;

public class ExportedUser
{
    private String username;
    private String uiName;
    private String email;
    private boolean enabled;
    private Date lastLogin;
    private Date created;
    private Date updated;

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String aUsername)
    {
        username = aUsername;
    }

    public String getUiName()
    {
        return uiName;
    }

    public void setUiName(String aUiName)
    {
        uiName = aUiName;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String aEmail)
    {
        email = aEmail;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean aEnabled)
    {
        enabled = aEnabled;
    }

    public Date getLastLogin()
    {
        return lastLogin;
    }

    public void setLastLogin(Date aLastLogin)
    {
        lastLogin = aLastLogin;
    }

    public Date getCreated()
    {
        return created;
    }

    public void setCreated(Date aCreated)
    {
        created = aCreated;
    }

    public Date getUpdated()
    {
        return updated;
    }

    public void setUpdated(Date aUpdated)
    {
        updated = aUpdated;
    }
}
