/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.json;

import de.tudarmstadt.ukp.clarin.webanno.api.event.ProjectStateChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.RemoteApiController2;

public class ProjectStateChangeMessage
{
    private long projectId;
    private String projectName;

    private String projectPreviousState;
    private String projectState;
    
    public ProjectStateChangeMessage()
    {
        // Nothing to do
    }
    
    public ProjectStateChangeMessage(ProjectStateChangedEvent aEvent)
    {
        projectId = aEvent.getProject().getId();
        projectName = aEvent.getProject().getName();
        
        projectState = RemoteApiController2.projectStateToString(aEvent.getNewState());
        projectPreviousState = RemoteApiController2.projectStateToString(aEvent.getPreviousState());
    }

    public long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(long aProjectId)
    {
        projectId = aProjectId;
    }

    public String getProjectName()
    {
        return projectName;
    }

    public void setProjectName(String aProjectName)
    {
        projectName = aProjectName;
    }

    public String getProjectPreviousState()
    {
        return projectPreviousState;
    }

    public void setProjectPreviousState(String aProjectPreviousState)
    {
        projectPreviousState = aProjectPreviousState;
    }

    public String getProjectState()
    {
        return projectState;
    }

    public void setProjectState(String aProjectState)
    {
        projectState = aProjectState;
    }
}
