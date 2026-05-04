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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.users;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = UserSelectionPanelConfigurationImpl.PROPERTY_PREFIX)
public class UserSelectionPanelConfigurationImpl
    implements UserSelectionPanelConfiguration
{
    public static final String PROPERTY_PREFIX = "user-selection";

    /**
     * Whether the list of users shown in the users tab of the project settings is restricted. If
     * enabled, the full name of a user has to be entered into the input field before the user can
     * be added. If disabled, it is possible to see all enabled users and to add any of them to the
     * project.
     */
    private boolean hideUsers;

    @Override
    public boolean isHideUsers()
    {
        return hideUsers;
    }

    public void setHideUsers(boolean aHideUsers)
    {
        hideUsers = aHideUsers;
    }
}
