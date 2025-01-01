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
package de.tudarmstadt.ukp.inception.workload.task;

import static de.tudarmstadt.ukp.inception.scheduling.MatchResult.NO_MATCH;
import static de.tudarmstadt.ukp.inception.scheduling.MatchResult.UNQUEUE_EXISTING_AND_QUEUE_THIS;
import static java.time.Duration.ofSeconds;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.scheduling.DebouncingTask;
import de.tudarmstadt.ukp.inception.scheduling.MatchResult;
import de.tudarmstadt.ukp.inception.scheduling.MatchableTask;
import de.tudarmstadt.ukp.inception.scheduling.Task;
import de.tudarmstadt.ukp.inception.workload.extension.WorkloadManagerExtension;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;
import jakarta.persistence.NoResultException;

public class RecalculateProjectStateTask
    extends DebouncingTask
    implements MatchableTask

{
    public static final String TYPE = "RecalculateProjectStateTask";

    private @Autowired ProjectService projectService;
    private @Autowired WorkloadManagementService workloadService;

    public RecalculateProjectStateTask(Builder<? extends Builder<?>> aBuilder)
    {
        super(aBuilder.withType(TYPE));
    }

    @Override
    public String getTitle()
    {
        return "Updating project state...";
    }

    @Override
    public void execute()
    {
        Project project;
        try {
            project = projectService.getProject(getProject().getId());
        }
        catch (NoResultException e) {
            // This happens when this method is called as part of deleting an entire project.
            // In such a case, the project may no longer be available, so there is no point in
            // updating its state. So then we do nothing here.
            return;
        }

        WorkloadManagerExtension<?> ext = workloadService.getWorkloadManagerExtension(project);
        ext.recalculate(project);
    }

    @Override
    public MatchResult matches(Task aTask)
    {
        // If a recalculation task for a project is coming in, we can throw out any scheduled tasks
        // for updating in the project.
        if (aTask instanceof RecalculateProjectStateTask
                || aTask instanceof UpdateProjectStateTask) {
            if (Objects.equals(getProject().getId(), aTask.getProject().getId())) {
                return UNQUEUE_EXISTING_AND_QUEUE_THIS;
            }
        }

        return NO_MATCH;
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
        RecalculateProjectStateTask task = (RecalculateProjectStateTask) o;
        return getProject().equals(task.getProject());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getProject());
    }

    public static Builder<Builder<?>> builder()
    {
        return new Builder<>();
    }

    public static class Builder<T extends Builder<?>>
        extends DebouncingTask.Builder<T>
    {
        protected Builder()
        {
            withDebounceDelay(ofSeconds(3));
        }

        public RecalculateProjectStateTask build()
        {
            return new RecalculateProjectStateTask(this);
        }
    }
}
