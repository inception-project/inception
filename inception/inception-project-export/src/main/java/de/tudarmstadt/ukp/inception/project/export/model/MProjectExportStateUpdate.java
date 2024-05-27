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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskState;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class MProjectExportStateUpdate
{
    private final long timestamp;
    private final String id;
    private final int progress;
    private final ProjectExportTaskState state;
    private final String title;
    private final String url;
    private final int messageCount;

    @JsonInclude(Include.NON_DEFAULT)
    private final boolean removed;

    @JsonInclude(Include.NON_EMPTY)
    private final LogMessage latestMessage;

    public MProjectExportStateUpdate()
    {
        timestamp = System.currentTimeMillis();
        title = null;
        id = null;
        progress = 0;
        state = null;
        url = null;
        messageCount = 0;
        removed = false;
        latestMessage = null;
    }

    public MProjectExportStateUpdate(ProjectExportTaskMonitor aMonitor)
    {
        this(aMonitor, false);
    }

    public MProjectExportStateUpdate(ProjectExportTaskMonitor aMonitor, boolean aRemoved)
    {
        timestamp = System.currentTimeMillis();
        title = aMonitor.getTitle();
        id = aMonitor.getHandle().getRunId();
        progress = aMonitor.getProgress();
        state = aMonitor.getState();
        url = aMonitor.getUrl();
        messageCount = aMonitor.getMessages().size();
        latestMessage = aMonitor.getMessages().peekLast();
        removed = aRemoved;
    }

    public String getTitle()
    {
        return title;
    }

    public String getId()
    {
        return id;
    }

    public int getProgress()
    {
        return progress;
    }

    public ProjectExportTaskState getState()
    {
        return state;
    }

    public String getUrl()
    {
        return url;
    }

    public LogMessage getLatestMessage()
    {
        return latestMessage;
    }

    public int getMessageCount()
    {
        return messageCount;
    }

    public boolean isRemoved()
    {
        return removed;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof MProjectExportStateUpdate)) {
            return false;
        }
        MProjectExportStateUpdate castOther = (MProjectExportStateUpdate) other;
        return new EqualsBuilder().append(timestamp / 1000, castOther.timestamp / 1000)
                .append(progress, castOther.progress).append(state, castOther.state).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(timestamp / 1000).append(progress).append(state)
                .toHashCode();
    }
}
