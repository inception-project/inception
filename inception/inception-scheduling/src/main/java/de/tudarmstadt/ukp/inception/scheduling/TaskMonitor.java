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

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class TaskMonitor
    implements Monitor
{
    private static final String ROOT_UNIT = "";

    private final Deque<LogMessage> messages = new ConcurrentLinkedDeque<>();

    private final TaskHandle handle;
    private final Project project;
    private final String user;
    private final String title;
    private final String type;

    private final long createTime;
    private long startTime = -1;
    private long endTime = -1;

    private final List<MutableProgress> progresses = new ArrayList<>();

    private TaskState state = NOT_STARTED;

    private final boolean cancellable;
    private boolean cancelled = false;
    private boolean destroyed = false;

    private MonitorUpdate updater;

    public TaskMonitor(Task aTask)
    {
        handle = aTask.getHandle();
        type = aTask.getType();
        user = aTask.getUser().map(User::getUsername).orElse(null);
        project = aTask.getProject();
        title = aTask.getTitle();
        createTime = currentTimeMillis();
        cancellable = aTask.isCancellable();
        updater = new MonitorUpdate()
        {
            @Override
            public MonitorUpdate setState(TaskState aState)
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

            @Override
            public MonitorUpdate addMessage(LogMessage aMessage)
            {
                // Avoid repeating the same message over for different users
                if (!messages.contains(aMessage)) {
                    messages.add(aMessage);
                }
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
    public synchronized List<Progress> getProgressList()
    {
        return progresses.stream() //
                .map(p -> new Progress(p.unit, p.progress, p.maxProgress)) //
                .toList();
    }

    @Override
    public synchronized ProgressScope openScope(String aUnit, int aMaxProgress)
    {
        var scope = new MutableProgress(aUnit);
        scope.maxProgress = aMaxProgress;
        progresses.add(scope);
        return scope;
    }

    private synchronized void closeScope(ProgressScope aScope)
    {
        progresses.remove(aScope);
    }

    @Override
    public void update(Consumer<MonitorUpdate> aUpdate)
    {
        aUpdate.accept(updater);
        commit();
    }

    @Override
    public synchronized int getProgress()
    {
        if (progresses.isEmpty()) {
            return 0;
        }

        var p = progresses.get(0);
        return p.progress;
    }

    @Override
    public synchronized int getMaxProgress()
    {
        if (progresses.isEmpty()) {
            return 0;
        }

        var p = progresses.get(0);
        return p.maxProgress;
    }

    @Deprecated
    public synchronized Progress toProgress()
    {
        if (progresses.isEmpty()) {
            return new Progress("", 0, 0);
        }

        var p = progresses.get(0);
        return new Progress(ROOT_UNIT, p.progress, p.maxProgress);
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

    @Override
    public boolean isCancelled()
    {
        return cancelled;
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

    protected void commit()
    {
        // Nothing by default
    }

    private class MutableProgress
        implements ProgressScope
    {
        private final String unit;
        private final ProgressUpdate progressUpdater;
        private int progress = 0;
        private int maxProgress = 0;

        public MutableProgress(String aUnit)
        {
            unit = aUnit;
            progressUpdater = new ProgressUpdate()
            {
                @Override
                public ProgressUpdate setProgress(int aProgress)
                {
                    progress = aProgress;
                    return this;
                }

                @Override
                public ProgressUpdate setMaxProgress(int aMaxProgress)
                {
                    maxProgress = aMaxProgress;
                    return this;
                }

                @Override
                public ProgressUpdate addMessage(LogMessage aMessage)
                {
                    // Avoid repeating the same message over for different users
                    if (!messages.contains(aMessage)) {
                        messages.add(aMessage);
                    }
                    return this;
                }

                @Override
                public ProgressUpdate increment()
                {
                    progress++;

                    return this;
                }

                @Override
                public ProgressUpdate increment(int aIncrement)
                {
                    progress += aIncrement;

                    return this;
                }
            };
        }

        @Override
        public int getProgress()
        {
            return progress;
        }

        @Override
        public void update(Consumer<ProgressUpdate> aUpdater)
        {
            aUpdater.accept(progressUpdater);
            commit();
        }

        @Override
        public void close()
        {
            closeScope(this);
        }
    }
}
