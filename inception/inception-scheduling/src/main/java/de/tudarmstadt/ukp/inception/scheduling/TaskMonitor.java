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
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class TaskMonitor
    implements Monitor
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

    private MonitorUpdater updater;

    public TaskMonitor(Task aTask)
    {
        handle = aTask.getHandle();
        type = aTask.getType();
        user = aTask.getUser().map(User::getUsername).orElse(null);
        project = aTask.getProject();
        title = aTask.getTitle();
        createTime = currentTimeMillis();
        cancellable = aTask.isCancellable();
        updater = new MonitorUpdater()
        {
            @Override
            public MonitorUpdater setProgress(int aProgress)
            {
                progress = aProgress;
                return this;
            }

            @Override
            public MonitorUpdater setMaxProgress(int aMaxProgress)
            {
                maxProgress = aMaxProgress;
                return this;
            }

            @Override
            public MonitorUpdater addMessage(LogMessage aMessage)
            {
                // Avoid repeating the same message over for different users
                if (!messages.contains(aMessage)) {
                    messages.add(aMessage);
                }
                return this;
            }

            @Override
            public MonitorUpdater increment()
            {
                progress++;

                return this;
            }

            @Override
            public MonitorUpdater setState(TaskState aState)
            {
                if (state == NOT_STARTED && aState != NOT_STARTED) {
                    startTime = currentTimeMillis();
                }

                if (asList(COMPLETED, CANCELLED, FAILED).contains(aState)) {
                    endTime = currentTimeMillis();
                }

                state = aState;
                return this;
            }
        };
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

    @Override
    public synchronized int getProgress()
    {
        return progress;
    }

    @Override
    public int getMaxProgress()
    {
        return maxProgress;
    }

    public Deque<LogMessage> getMessages()
    {
        return messages;
    }

    @Override
    public synchronized void update(Consumer<MonitorUpdater> aUpdater)
    {
        aUpdater.accept(updater);
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

    @Override
    public boolean isCancelled()
    {
        return cancelled;
    }

    @Deprecated
    public synchronized Progress toProgress()
    {
        return new Progress(progress, maxProgress);
    }

    @Override
    public long getDuration()
    {
        if (startTime < 0) {
            return -1;
        }

        if (endTime > 0) {
            return endTime - startTime;
        }

        return currentTimeMillis() - startTime;
    }
}
