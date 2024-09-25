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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.tudarmstadt.ukp.inception.preferences.PreferenceValue;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AnnotationSidebarState
    implements PreferenceValue
{
    private static final long serialVersionUID = -5212679894035839772L;

    private String selectedTab;
    private boolean expanded = true;

    public void setSelectedTab(String aFactoryId)
    {
        selectedTab = aFactoryId;
    }

    public String getSelectedTab()
    {
        return selectedTab;
    }

    public void setExpanded(boolean aExpanded)
    {
        expanded = aExpanded;
    }

    public boolean isExpanded()
    {
        return expanded;
    }
}
