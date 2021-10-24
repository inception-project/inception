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
package de.tudarmstadt.ukp.inception.project.export.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskHandle;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskState;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;

public class ProjectExportState
{
    private final ProjectExportTaskHandle handle;
    private final int progress;
    private final List<LogMessage> messages;
    private final ProjectExportTaskState state;
    private final String title;
    private final String downloadUrl;

    public ProjectExportState(ProjectExportTaskMonitor aMonitor, Collection<LogMessage> aMessages)
    {
        title = aMonitor.getTitle();
        handle = aMonitor.getHandle();
        progress = aMonitor.getProgress();
        messages = new ArrayList<>(aMessages);
        state = aMonitor.getState();
        downloadUrl = aMonitor.getDownloadUrl();
    }

    public String getTitle()
    {
        return title;
    }

    public ProjectExportTaskHandle getHandle()
    {
        return handle;
    }

    public int getProgress()
    {
        return progress;
    }

    public List<LogMessage> getMessages()
    {
        return messages;
    }

    public ProjectExportTaskState getState()
    {
        return state;
    }

    public String getDownloadUrl()
    {
        return downloadUrl;
    }
}
