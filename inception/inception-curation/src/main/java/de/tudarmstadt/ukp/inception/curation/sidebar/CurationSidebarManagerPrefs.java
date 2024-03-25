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
package de.tudarmstadt.ukp.inception.curation.sidebar;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.tudarmstadt.ukp.inception.preferences.Key;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CurationSidebarManagerPrefs
    implements Serializable
{
    private static final long serialVersionUID = -7731274322176414665L;

    public static final Key<CurationSidebarManagerPrefs> KEY_CURATION_SIDEBAR_MANAGER_PREFS = new Key<>(
            CurationSidebarManagerPrefs.class, "annotation/editor/curation-sidebar/manager");

    private boolean autoMergeCurationSidebar = true;

    public boolean isAutoMergeCurationSidebar()
    {
        return autoMergeCurationSidebar;
    }

    public void setAutoMergeCurationSidebar(boolean aAutoMergeCurationSidebar)
    {
        autoMergeCurationSidebar = aAutoMergeCurationSidebar;
    }

}
