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
package de.tudarmstadt.ukp.inception.scheduling;

import static de.tudarmstadt.ukp.inception.scheduling.TaskState.CANCELLED;
import static de.tudarmstadt.ukp.inception.scheduling.TaskState.COMPLETED;
import static de.tudarmstadt.ukp.inception.scheduling.TaskState.FAILED;
import static de.tudarmstadt.ukp.inception.scheduling.TaskState.NOT_STARTED;
import static java.util.Arrays.asList;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class TaskMonitor
{
    private final Deque<LogMessage> messages = new ConcurrentLinkedDeque<>();

    private final TaskHandle handle;
    private final String user;
    private final String title;

    private long createTime;
    private long startTime = -1;
    private long endTime = -1;
    private int progress = 0;
    private int maxProgress = 0;
    private TaskState state = NOT_STARTED;

    private boolean destroyed = false;

    public TaskMonitor(TaskHandle aHandle, String aUser, String aTitle)
    {
        handle = aHandle;
        user = aUser;
        title = aTitle;
        createTime = System.currentTimeMillis();
    }

    public TaskHandle getHandle()
    {
        return handle;
    }

    public synchronized TaskState getState()
    {
        return state;
    }

    public String getUser()
    {
        return user;
    }

    public String getTitle()
    {
        return title;
    }

    public synchronized void setState(TaskState aState)
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

    public synchronized void setStateAndProgress(TaskState aState, int aProgress)
    {
        setState(aState);
        setProgress(aProgress);
    }

    public synchronized void setStateAndProgress(TaskState aState, int aProgress, int aMaxProgress)
    {
        setState(aState);
        setProgress(aProgress);
        setMaxProgress(aMaxProgress);
    }

    public synchronized void incrementProgress()
    {
        setProgress(progress + 1);
    }

    public synchronized void setProgress(int aProgress)
    {
        progress = aProgress;
    }

    public int getMaxProgress()
    {
        return maxProgress;
    }

    public synchronized void setMaxProgress(int aMaxProgress)
    {
        maxProgress = aMaxProgress;
    }

    public void addMessage(LogMessage aMessage)
    {
        // Avoid repeating the same message over for different users
        if (!messages.contains(aMessage)) {
            messages.add(aMessage);
        }
    }

    public synchronized void setProgressWithMessage(int aProgress, int aMaxProgress,
            LogMessage aMessage)
    {
        setProgress(aProgress);
        setMaxProgress(aMaxProgress);
        addMessage(aMessage);
    }

    public Deque<LogMessage> getMessages()
    {
        return messages;
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
