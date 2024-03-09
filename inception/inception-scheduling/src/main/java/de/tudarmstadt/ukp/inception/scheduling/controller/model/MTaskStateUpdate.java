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
package de.tudarmstadt.ukp.inception.scheduling.controller.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import de.tudarmstadt.ukp.inception.scheduling.TaskMonitor;
import de.tudarmstadt.ukp.inception.scheduling.TaskState;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class MTaskStateUpdate
{
    private final long timestamp;
    private final int id;
    private final String type;
    private final int progress;
    private final int maxProgress;
    private final TaskState state;
    private final String title;
    private final int messageCount;

    @JsonInclude(Include.NON_DEFAULT)
    private final boolean cancellable;

    @JsonInclude(Include.NON_DEFAULT)
    private final boolean removed;

    @JsonInclude(Include.NON_EMPTY)
    private final LogMessage latestMessage;

    public MTaskStateUpdate(TaskMonitor aMonitor)
    {
        this(aMonitor, false);
    }

    public MTaskStateUpdate(TaskMonitor aMonitor, boolean aRemoved)
    {
        timestamp = System.currentTimeMillis();
        title = aMonitor.getTitle();
        id = aMonitor.getHandle().getId();
        type = aMonitor.getType();
        progress = aMonitor.getProgress();
        maxProgress = aMonitor.getMaxProgress();
        state = aMonitor.getState();
        messageCount = aMonitor.getMessages().size();
        latestMessage = aMonitor.getMessages().peekLast();
        removed = aRemoved;
        cancellable = aMonitor.isCancellable();
    }

    public String getTitle()
    {
        return title;
    }

    public int getId()
    {
        return id;
    }

    public int getProgress()
    {
        return progress;
    }

    public int getMaxProgress()
    {
        return maxProgress;
    }

    public TaskState getState()
    {
        return state;
    }

    public String getType()
    {
        return type;
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

    public boolean isCancellable()
    {
        return cancellable;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof MTaskStateUpdate)) {
            return false;
        }

        MTaskStateUpdate castOther = (MTaskStateUpdate) other;
        return new EqualsBuilder() //
                .append(timestamp / 1000, castOther.timestamp / 1000) //
                .append(progress, castOther.progress) //
                .append(maxProgress, castOther.maxProgress) //
                .append(state, castOther.state) //
                .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder() //
                .append(timestamp / 1000) //
                .append(progress) //
                .append(maxProgress) //
                .append(state) //
                .toHashCode();
    }
}
