/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import static org.apache.commons.lang3.Validate.notNull;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public abstract class Task
    implements Runnable
{
    private final static AtomicInteger nextId = new AtomicInteger(1);
    
    private final User user;
    private final Project project;
    private final String trigger;
    private final int id;

    public Task(User aUser, Project aProject, String aTrigger)
    {
        notNull(aUser);
        notNull(aProject);
        notNull(aTrigger);
        
        user = aUser;
        project = aProject;
        trigger = aTrigger;
        id = nextId.getAndIncrement();
    }

    public User getUser()
    {
        return user;
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getName());
        sb.append('{');
        sb.append("user=").append(user.getUsername());
        sb.append(", project=").append(project.getName());
        sb.append(", trigger=\"").append(trigger);
        sb.append("\"}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Task task = (Task) o;
        return user.equals(task.user) && project.equals(task.project);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, project);
    }
}
