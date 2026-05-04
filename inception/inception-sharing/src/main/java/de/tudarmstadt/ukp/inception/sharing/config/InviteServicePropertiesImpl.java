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
package de.tudarmstadt.ukp.inception.sharing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("sharing.invites")
public class InviteServicePropertiesImpl
    implements InviteServiceProperties
{
    /**
     * Whether to enable guest annotators. When enabled, users can be invited via a password-less
     * login. The user logging in chooses a login name and a project-bound password-less account is
     * created for this user. The user can only log in to this account via the invite link. When the
     * project is deleted, all the project-bound accounts are deleted as well.
     */
    private boolean guestsEnabled;

    /**
     * Base URL used to generate invite links, e.g. when running behind a reverse proxy.
     */
    private String inviteBaseUrl;

    @Override
    public boolean isGuestsEnabled()
    {
        return guestsEnabled;
    }

    public void setGuestsEnabled(boolean aGuestsEnabled)
    {
        guestsEnabled = aGuestsEnabled;
    }

    @Override
    public String getInviteBaseUrl()
    {
        return inviteBaseUrl;
    }

    public void setInviteBaseUrl(String aInviteBaseUrl)
    {
        inviteBaseUrl = aInviteBaseUrl;
    }
}
