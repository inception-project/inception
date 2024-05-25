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

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class TaskMonitor
{
    private final Deque<LogMessage> messages = new ConcurrentLinkedDeque<>();

    private final TaskHandle handle;
    private final Project project;
    private final String user;
    private final String title;
    private final String type;

    private final long createTime;
    private long startTime = -1;
    private long endTime = -1;

    private int progress = 0;
    private int maxProgress = 0;

    private TaskState state = NOT_STARTED;

    private final boolean cancellable;
    private boolean cancelled = false;
    private boolean destroyed = false;

    public TaskMonitor(Task aTask)
    {
        handle = aTask.getHandle();
        type = aTask.getType();
        user = aTask.getUser().map(User::getUsername).orElse(null);
        project = aTask.getProject();
        title = aTask.getTitle();
        createTime = System.currentTimeMillis();
        cancellable = aTask.isCancellable();
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

    public Project getProject()
    {
        return project;
    }

    public String getTitle()
    {
        return title;
    }

    public String getType()
    {
        return type;
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

    public boolean isDestroyed()
    {
        return destroyed;
    }

    public boolean isCancellable()
    {
        return cancellable;
    }

    public void cancel()
    {
        cancelled = true;
    }

    public boolean isCancelled()
    {
        return cancelled;
    }

    @Deprecated
    public synchronized Progress toProgress()
    {
        return new Progress(maxProgress, progress);
    }
}
