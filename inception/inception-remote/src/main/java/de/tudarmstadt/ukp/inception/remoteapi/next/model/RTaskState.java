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
package de.tudarmstadt.ukp.inception.remoteapi.next.model;

import de.tudarmstadt.ukp.inception.scheduling.Task;
import de.tudarmstadt.ukp.inception.scheduling.TaskState;

public class RTaskState
{
    private final int id;
    private final String type;
    private final String title;
    private final TaskState state;
    private final long createTime;
    private final long endTime;
    private final long duration;
    private final int progress;
    private final int maxProgress;
    private final long startTime;

    public RTaskState(Task aTask)
    {
        id = aTask.getHandle().getId();
        type = aTask.getMonitor().getType();
        title = aTask.getMonitor().getTitle();
        state = aTask.getMonitor().getState();
        createTime = aTask.getMonitor().getCreateTime();
        startTime = aTask.getMonitor().getStartTime();
        endTime = aTask.getMonitor().getEndTime();
        duration = aTask.getMonitor().getDuration();
        progress = aTask.getMonitor().getProgress();
        maxProgress = aTask.getMonitor().getMaxProgress();
    }

    public int getId()
    {
        return id;
    }

    public String getType()
    {
        return type;
    }

    public String getTitle()
    {
        return title;
    }

    public TaskState getState()
    {
        return state;
    }

    public long getCreateTime()
    {
        return createTime;
    }

    public long getDuration()
    {
        return duration;
    }

    public long getStartTime()
    {
        return startTime;
    }

    public long getEndTime()
    {
        return endTime;
    }

    public int getMaxProgress()
    {
        return maxProgress;
    }

    public int getProgress()
    {
        return progress;
    }
}
