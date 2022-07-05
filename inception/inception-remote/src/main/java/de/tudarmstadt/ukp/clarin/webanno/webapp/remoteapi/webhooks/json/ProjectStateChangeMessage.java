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
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.json;

import static de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.AeroRemoteApiController.projectStateToString;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import de.tudarmstadt.ukp.clarin.webanno.api.event.ProjectStateChangedEvent;

public class ProjectStateChangeMessage
{
    private long timestamp;

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
        timestamp = aEvent.getTimestamp();

        projectId = aEvent.getProject().getId();
        projectName = aEvent.getProject().getName();

        projectState = projectStateToString(aEvent.getNewState());
        projectPreviousState = projectStateToString(aEvent.getPreviousState());
    }

    public long getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(long aTimestamp)
    {
        timestamp = aTimestamp;
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

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.JSON_STYLE) //
                .append("timestamp", timestamp) //
                .append("projectId", projectId) //
                .append("projectName", projectName) //
                .append("projectPreviousState", projectPreviousState) //
                .append("projectState", projectState) //
                .toString();
    }
}
