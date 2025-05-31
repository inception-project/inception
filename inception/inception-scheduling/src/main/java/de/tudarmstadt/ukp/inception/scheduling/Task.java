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
import static de.tudarmstadt.ukp.inception.scheduling.TaskState.RUNNING;
import static de.tudarmstadt.ukp.inception.support.logging.Logging.KEY_PROJECT_ID;
import static de.tudarmstadt.ukp.inception.support.logging.Logging.KEY_REPOSITORY_PATH;
import static de.tudarmstadt.ukp.inception.support.logging.Logging.KEY_USERNAME;
import static org.apache.commons.lang3.Validate.notNull;

import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.scheduling.controller.SchedulerWebsocketController;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public abstract class Task
    implements Runnable, InitializingBean
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final static AtomicInteger nextId = new AtomicInteger(1);

    private @Autowired RepositoryProperties repositoryProperties;
    private @Autowired(required = false) SchedulerWebsocketController schedulerController;

    private final TaskHandle handle;
    private final User sessionOwner;
    private final Project project;
    private final String trigger;
    private final int id;
    private final String type;
    private final boolean cancellable;

    private volatile Thread thread;
    private TaskMonitor monitor;
    private Task parentTask;

    private TaskScope scope;

    protected Task(Builder<? extends Builder<?>> builder)
    {
        notNull(builder.project, "Project must be specified");
        notNull(builder.trigger, "Trigger must be specified");
        notNull(builder.scope, "Scope must be specified");
        notNull(builder.type, "Type must be specified");

        id = nextId.getAndIncrement();
        handle = new TaskHandle(id);
        sessionOwner = builder.sessionOwner;
        project = builder.project;
        trigger = builder.trigger;
        type = builder.type;

        cancellable = builder.cancellable;
        parentTask = builder.parentTask;
        scope = builder.scope;

        if (builder.monitor != null) {
            monitor = builder.monitor.apply(this);
        }
    }

    @Override
    public void afterPropertiesSet()
    {
        if (monitor == null) {
            // For tasks that have a parent task, we use a non-notifying monitor. Also, we do not
            // report such subtasks via the SchedulerControllerImpl - they are internal.
            if (schedulerController != null && parentTask == null) {
                monitor = new NotifyingTaskMonitor(this, schedulerController);
            }
            else {
                monitor = new TaskMonitor(this);
            }
        }
    }

    Thread getThread()
    {
        return thread;
    }

    public boolean isCancellable()
    {
        return cancellable;
    }

    public Task getParentTask()
    {
        return parentTask;
    }

    public String getType()
    {
        return type;
    }

    public TaskHandle getHandle()
    {
        return handle;
    }

    public String getTitle()
    {
        return getClass().getSimpleName();
    }

    public TaskMonitor getMonitor()
    {
        return monitor;
    }

    /**
     * @deprecated Use {@link #getSessionOwner()}
     * @return user who started the task
     */
    @Deprecated
    public Optional<User> getUser()
    {
        return getSessionOwner();
    }

    public Optional<User> getSessionOwner()
    {
        return Optional.ofNullable(sessionOwner);
    }

    public Project getProject()
    {
        return project;
    }

    public String getTrigger()
    {
        return trigger;
    }

    public String getName()
    {
        return getClass().getSimpleName();
    }

    public int getId()
    {
        return id;
    }

    public boolean isReadyToStart()
    {
        return true;
    }

    public TaskScope getScope()
    {
        return scope;
    }

    void destroy()
    {
        if (monitor != null) {
            monitor.destroy();
        }
    }

    @Override
    public final void run()
    {
        try {
            // We are in a new thread. Set up thread-specific MDC
            if (repositoryProperties != null) {
                MDC.put(KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
            }

            getUser().ifPresent(_user -> MDC.put(KEY_USERNAME, _user.getUsername()));

            if (getProject() != null) {
                MDC.put(KEY_PROJECT_ID, String.valueOf(getProject().getId()));
            }

            runSync();
        }
        finally {
            MDC.remove(KEY_REPOSITORY_PATH);
            MDC.remove(KEY_USERNAME);
            MDC.remove(KEY_PROJECT_ID);
        }
    }

    public final void runSync()
    {
        try {
            if (thread != null) {
                throw new IllegalStateException("Task " + this + " already bound to thread "
                        + thread + " when trying to start on thread " + Thread.currentThread());
            }

            thread = Thread.currentThread();
            monitor.update(up -> up.setState(RUNNING));

            execute();

            if (monitor.isCancelled()) {
                monitor.update(up -> up.setState(CANCELLED));
            }
            else if (monitor.getState() == RUNNING) {
                monitor.update(up -> up.setState(COMPLETED));
            }

            if (monitor.getState() == COMPLETED) {
                LOG.debug("Task [{}] completed (trigger: [{}]) in {}ms", getTitle(), getTrigger(),
                        monitor.getDuration());
            }
        }
        catch (Exception e) {
            monitor.update(up -> up.setState(FAILED) //
                    .addMessage(LogMessage.error(this, "Task failed.")));
            LOG.error("Task [{}] failed (trigger: [{}])", getTitle(), getTrigger(), e);
        }
        catch (Throwable e) {
            monitor.update(up -> up.setState(FAILED) //
                    .addMessage(LogMessage.error(this, "Task failed with a serious error.")));
            LOG.error("Task [{}] failed with a serious error (trigger: [{}])", getTitle(),
                    getTrigger(), e);
        }
        finally {
            thread = null;
        }
    }

    public abstract void execute() throws Exception;

    @Override
    public String toString()
    {
        var sb = new StringBuilder(getName());
        sb.append('[');
        sb.append("user=").append(sessionOwner != null ? sessionOwner.getUsername() : "<SYSTEM>");
        sb.append(", project=").append(project.getName());
        sb.append(", trigger=\"").append(trigger);
        if (monitor != null) {
            sb.append(", ");
            sb.append(monitor.getProgress());
            sb.append("/");
            sb.append(monitor.getMaxProgress());
        }
        sb.append("\"]");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Task task = (Task) o;
        return Objects.equals(sessionOwner, task.sessionOwner) && project.equals(task.project);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(sessionOwner, project);
    }

    public static abstract class Builder<T extends Builder<?>>
    {
        protected Function<Task, TaskMonitor> monitor;
        protected User sessionOwner;
        protected Project project;
        protected String trigger;
        protected String type;
        protected boolean cancellable;
        protected Task parentTask;
        protected TaskScope scope = TaskScope.EPHEMERAL;

        protected Builder()
        {
        }

        /**
         * @param aSessionOwner
         *            the user owning the task.
         */
        @SuppressWarnings("unchecked")
        public T withSessionOwner(User aSessionOwner)
        {
            sessionOwner = aSessionOwner;
            return (T) this;
        }

        /**
         * @param aProject
         *            the project on which the task operates.
         */
        @SuppressWarnings("unchecked")
        public T withProject(Project aProject)
        {
            project = aProject;
            return (T) this;
        }

        /**
         * @param aTrigger
         *            the trigger that caused the selection to be scheduled.
         */
        @SuppressWarnings("unchecked")
        public T withTrigger(String aTrigger)
        {
            trigger = aTrigger;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T withType(String aType)
        {
            type = aType;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T withCancellable(boolean aCancellable)
        {
            cancellable = aCancellable;
            return (T) this;
        }

        /**
         * @param aParentTask
         *            parent task for this task.
         */
        @SuppressWarnings("unchecked")
        public T withParentTask(Task aParentTask)
        {
            parentTask = aParentTask;
            return (T) this;
        }

        /**
         * @param aMonitorFactory
         *            function to create monitor for this task.
         */
        @SuppressWarnings("unchecked")
        public T withMonitor(Function<Task, TaskMonitor> aMonitorFactory)
        {
            monitor = aMonitorFactory;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T withScope(TaskScope aScope)
        {
            scope = aScope;
            return (T) this;
        }
    }
}
