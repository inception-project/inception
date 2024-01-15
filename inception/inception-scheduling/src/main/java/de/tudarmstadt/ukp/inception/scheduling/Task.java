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

import static de.tudarmstadt.ukp.inception.support.logging.Logging.KEY_PROJECT_ID;
import static de.tudarmstadt.ukp.inception.support.logging.Logging.KEY_REPOSITORY_PATH;
import static de.tudarmstadt.ukp.inception.support.logging.Logging.KEY_USERNAME;
import static org.apache.commons.lang3.Validate.notNull;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.MDC;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;

public abstract class Task
    implements Runnable, InitializingBean
{
    private final static AtomicInteger nextId = new AtomicInteger(1);

    private @Autowired RepositoryProperties repositoryProperties;
    private @Autowired(required = false) SimpMessagingTemplate msgTemplate;

    private final TaskHandle handle;
    private final User sessionOwner;
    private final Project project;
    private final String trigger;
    private final int id;

    private TaskMonitor monitor;
    private Task parentTask;

    private boolean cancelled;

    public Task(Project aProject, String aTrigger)
    {
        this(null, aProject, aTrigger);
    }

    public Task(User aSessionOwner, Project aProject, String aTrigger)
    {
        notNull(aProject, "Project must be specified");
        notNull(aTrigger, "Trigger must be specified");

        id = nextId.getAndIncrement();
        handle = new TaskHandle(id);
        sessionOwner = aSessionOwner;
        project = aProject;
        trigger = aTrigger;
    }

    @Override
    public void afterPropertiesSet()
    {
        // For tasks that have a parent task, we use a non-notifying monitor. Also, we do not report
        // such subtasks ia the SchedulerControllerImpl - they are internal.
        if (msgTemplate != null && sessionOwner != null && parentTask == null) {
            monitor = new NotifyingTaskMonitor(handle, sessionOwner.getUsername(), getTitle(),
                    msgTemplate);
        }
        else {
            monitor = new TaskMonitor(handle,
                    sessionOwner != null ? sessionOwner.getUsername() : null, getTitle());
        }
    }

    /**
     * @param aParentTask
     *            parent task for this task.
     */
    public void setParentTask(Task aParentTask)
    {
        parentTask = aParentTask;
    }

    public Task getParentTask()
    {
        return parentTask;
    }

    public String getTitle()
    {
        return getClass().getSimpleName();
    }

    public TaskMonitor getMonitor()
    {
        return monitor;
    }

    public Optional<User> getUser()
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

    public void cancel()
    {
        cancelled = true;
    }

    public boolean isCancelled()
    {
        return cancelled;
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

            monitor.setState(TaskState.RUNNING);
            execute();
            if (monitor.getState() == TaskState.RUNNING) {
                monitor.setState(TaskState.COMPLETED);
            }
        }
        finally {
            destroy();
            MDC.remove(KEY_REPOSITORY_PATH);
            MDC.remove(KEY_USERNAME);
            MDC.remove(KEY_PROJECT_ID);
        }
    }

    public abstract void execute();

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(getName());
        sb.append('{');
        sb.append("user=").append(sessionOwner != null ? sessionOwner.getUsername() : "<SYSTEM>");
        sb.append(", project=").append(project.getName());
        sb.append(", trigger=\"").append(trigger);
        sb.append("\"}");
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
}
