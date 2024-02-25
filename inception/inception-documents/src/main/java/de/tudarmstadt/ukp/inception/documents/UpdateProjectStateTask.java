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
package de.tudarmstadt.ukp.inception.documents;

import static java.time.Duration.ofSeconds;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.SourceDocumentStateStats;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.scheduling.DebouncingTask;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

public class UpdateProjectStateTask
    extends DebouncingTask
{
    public static final String TYPE = "UpdateProjectStateTask";

    private @PersistenceContext EntityManager entityManager;
    private @Autowired ProjectService projectService;
    private @Autowired DocumentService documentService;

    public UpdateProjectStateTask(Builder<? extends Builder<?>> aBuilder)
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

        SourceDocumentStateStats stats = documentService.getSourceDocumentStats(project);

        projectService.setProjectState(project, stats.getProjectState());
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
        UpdateProjectStateTask task = (UpdateProjectStateTask) o;
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
            withDebounceMillis(ofSeconds(3));
        }

        public UpdateProjectStateTask build()
        {
            return new UpdateProjectStateTask(this);
        }
    }
}
