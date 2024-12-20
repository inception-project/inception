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
package de.tudarmstadt.ukp.clarin.webanno.api.export;

import static de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskState.CANCELLED;
import static de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskState.COMPLETED;
import static de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskState.FAILED;
import static de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskState.NOT_STARTED;
import static java.util.Arrays.asList;

import java.io.File;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class ProjectExportTaskMonitor
{
    private final Deque<LogMessage> messages = new ConcurrentLinkedDeque<>();

    private final ProjectExportTaskHandle handle;
    private final long projectId;
    private final String title;
    private final String filenamePrefix;

    private long createTime;
    private long startTime = -1;
    private long endTime = -1;
    private int progress = 0;
    private ProjectExportTaskState state = NOT_STARTED;
    private File exportedFile;
    private String url;

    private boolean destroyed = false;

    public ProjectExportTaskMonitor(Project aProject, ProjectExportTaskHandle aHandle,
            String aTitle, String aFilenamePrefix)
    {
        projectId = aProject.getId();
        handle = aHandle;
        title = aTitle;
        filenamePrefix = aFilenamePrefix;
    }

    public long getProjectId()
    {
        return projectId;
    }

    public ProjectExportTaskHandle getHandle()
    {
        return handle;
    }

    public synchronized ProjectExportTaskState getState()
    {
        return state;
    }

    public String getTitle()
    {
        return title;
    }

    public synchronized void setState(ProjectExportTaskState aState)
    {
        state = aState;

        if (state == NOT_STARTED && aState != NOT_STARTED) {
            startTime = System.currentTimeMillis();
        }

        if (asList(COMPLETED, CANCELLED, FAILED).contains(aState)) {
            endTime = System.currentTimeMillis();
        }
    }

    public synchronized long getCreateTime()
    {
        return createTime;
    }

    public synchronized void setCreateTime(long aCreateTime)
    {
        createTime = aCreateTime;
    }

    public synchronized long getStartTime()
    {
        return startTime;
    }

    public synchronized long getEndTime()
    {
        return endTime;
    }

    public synchronized int getProgress()
    {
        return progress;
    }

    public synchronized void setStateAndProgress(ProjectExportTaskState aState, int aProgress)
    {
        setState(aState);
        setProgress(aProgress);
    }

    public synchronized void setProgress(int aProgress)
    {
        progress = aProgress;
    }

    public void addMessage(LogMessage aMessage)
    {
        // Avoid repeating the same message over for different users
        if (!messages.contains(aMessage)) {
            messages.add(aMessage);
        }
    }

    public Deque<LogMessage> getMessages()
    {
        return messages;
    }

    public synchronized File getExportedFile()
    {
        return exportedFile;
    }

    public String getExportedFilenamePrefix()
    {
        return filenamePrefix;
    }

    public synchronized void setExportedFile(File aExportedFile)
    {
        exportedFile = aExportedFile;
    }

    public synchronized void setUrl(String aUrl)
    {
        url = aUrl;
    }

    public synchronized String getUrl()
    {
        return url;
    }

    public synchronized void destroy()
    {
        if (!destroyed) {
            destroyed = true;
            onDestroy();
        }
    }

    protected void onDestroy()
    {
        // By default do nothing
    }

    protected boolean isDestroyed()
    {
        return destroyed;
    }
}
