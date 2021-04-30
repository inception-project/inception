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
package de.tudarmstadt.ukp.clarin.webanno.api.dao.event;

import static java.time.Duration.ofSeconds;

import java.util.Arrays;
import java.util.Objects;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.SourceDocumentStateStats;
import de.tudarmstadt.ukp.clarin.webanno.api.event.ProjectStateChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.scheduling.DebouncingTask;

public class UpdateProjectStateTask
    extends DebouncingTask
{
    private @PersistenceContext EntityManager entityManager;
    private @Autowired ProjectService projectService;
    private @Autowired DocumentService documentService;
    private @Autowired ApplicationEventPublisher applicationEventPublisher;

    public UpdateProjectStateTask(Project aProject, String aTrigger)
    {
        super(aProject, aTrigger, ofSeconds(3));
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

        ProjectState oldState = project.getState();

        // We had some strange reports about being unable to calculate the project state, so to
        // be better able to debug this, we add some more detailed information to the exception
        // message here.
        try {
            project.setState(stats.getProjectState());
            projectService.updateProject(project);
        }
        catch (IllegalStateException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("\nDetailed document states in project [" + project.getName() + "]("
                    + project.getId() + "):\n");
            String detailQuery = "SELECT id, name, state FROM " + SourceDocument.class.getName()
                    + " WHERE project = :project";
            Query q = entityManager.createQuery(detailQuery).setParameter("project", project);
            for (Object res : q.getResultList()) {
                sb.append("- ");
                sb.append(Arrays.toString((Object[]) res));
                sb.append('\n');
            }
            IllegalStateException ne = new IllegalStateException(e.getMessage() + sb, e.getCause());
            ne.setStackTrace(e.getStackTrace());
            throw ne;
        }

        if (!Objects.equals(oldState, project.getState())) {
            applicationEventPublisher
                    .publishEvent(new ProjectStateChangedEvent(this, project, oldState));
        }
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
}
