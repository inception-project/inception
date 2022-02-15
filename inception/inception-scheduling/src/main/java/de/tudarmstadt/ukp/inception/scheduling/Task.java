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

import static de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging.KEY_PROJECT_ID;
import static de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging.KEY_REPOSITORY_PATH;
import static de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging.KEY_USERNAME;
import static org.apache.commons.lang3.Validate.notNull;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public abstract class Task
    implements Runnable
{
    private final static AtomicInteger nextId = new AtomicInteger(1);

    private @Autowired RepositoryProperties repositoryProperties;

    private final User user;
    private final Project project;
    private final String trigger;
    private final int id;

    public Task(Project aProject, String aTrigger)
    {
        this(null, aProject, aTrigger);
    }

    public Task(User aUser, Project aProject, String aTrigger)
    {
        notNull(aProject, "Project must be specified");
        notNull(aTrigger, "Trigger must be specified");

        user = aUser;
        project = aProject;
        trigger = aTrigger;
        id = nextId.getAndIncrement();
    }

    public Optional<User> getUser()
    {
        return Optional.ofNullable(user);
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

    @Override
    public void run()
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

            execute();
        }
        finally {
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
        sb.append("user=").append(user != null ? user.getUsername() : "<SYSTEM>");
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
        return Objects.equals(user, task.user) && project.equals(task.project);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(user, project);
    }
}
