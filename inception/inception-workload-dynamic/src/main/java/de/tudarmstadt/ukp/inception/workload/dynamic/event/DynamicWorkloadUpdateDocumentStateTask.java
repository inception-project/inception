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
package de.tudarmstadt.ukp.inception.workload.dynamic.event;

import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.inception.workload.dynamic.DynamicWorkloadExtension.DYNAMIC_WORKLOAD_MANAGER_EXTENSION_ID;
import static java.time.Duration.ofSeconds;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.scheduling.DebouncingTask;
import de.tudarmstadt.ukp.inception.workload.dynamic.DynamicWorkloadExtension;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

public class DynamicWorkloadUpdateDocumentStateTask
    extends DebouncingTask
{
    public static final String TYPE = "DynamicWorkloadUpdateDocumentStateTask";

    private @PersistenceContext EntityManager entityManager;
    private @Autowired ProjectService projectService;
    private @Autowired DocumentService documentService;
    private @Autowired WorkloadManagementService workloadManagementService;
    private @Autowired DynamicWorkloadExtension dynamicWorkloadExtension;

    private final SourceDocument document;

    public DynamicWorkloadUpdateDocumentStateTask(Builder<? extends Builder<?>> aBuilder)
    {
        super(aBuilder //
                .withProject(aBuilder.document.getProject()) //
                .withType(TYPE));
        document = aBuilder.document;
    }

    @Override
    public String getTitle()
    {
        return "Updating document states...";
    }

    public SourceDocument getDocument()
    {
        return document;
    }

    @Override
    public void execute()
    {
        Project project;
        try {
            project = projectService.getProject(document.getProject().getId());
        }
        catch (NoResultException e) {
            // This happens when this method is called as part of deleting an entire project.
            // In such a case, the project may no longer be available, so there is no point in
            // updating its state. So then we do nothing here.
            return;
        }

        // We check this here instead of checking at task submission to avoid hammering the
        // DB if there is a high event frequency
        var workloadManager = workloadManagementService
                .loadOrCreateWorkloadManagerConfiguration(project);
        if (!DYNAMIC_WORKLOAD_MANAGER_EXTENSION_ID.equals(workloadManager.getType())) {
            return;
        }

        // Get the latest state
        var doc = documentService.getSourceDocument(project.getId(), document.getId());

        // If the source document is already in curation, we do not touch the state anymore
        if (doc.getState() == CURATION_FINISHED || doc.getState() == CURATION_IN_PROGRESS) {
            return;
        }

        var traits = dynamicWorkloadExtension.readTraits(workloadManager);
        int requiredAnnotatorCount = traits.getDefaultNumberOfAnnotations();

        dynamicWorkloadExtension.updateDocumentState(doc, requiredAnnotatorCount);
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
        var task = (DynamicWorkloadUpdateDocumentStateTask) o;
        return document.equals(task.document) && getProject().equals(task.getProject());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(document, getProject());
    }

    public static Builder<Builder<?>> builder()
    {
        return new Builder<>();
    }

    public static class Builder<T extends Builder<?>>
        extends DebouncingTask.Builder<T>
    {
        private SourceDocument document;

        protected Builder()
        {
            withDebounceDelay(ofSeconds(2));
        }

        @SuppressWarnings("unchecked")
        public T withDocument(SourceDocument aDocument)
        {
            document = aDocument;
            return (T) this;
        }

        public DynamicWorkloadUpdateDocumentStateTask build()
        {
            return new DynamicWorkloadUpdateDocumentStateTask(this);
        }
    }
}
