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

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.scheduling.DebouncingTask;
import de.tudarmstadt.ukp.inception.workload.dynamic.DynamicWorkloadExtension;
import de.tudarmstadt.ukp.inception.workload.dynamic.trait.DynamicWorkloadTraits;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManager;

public class DynamicWorkloadUpdateDocumentStateTask
    extends DebouncingTask
{
    private @PersistenceContext EntityManager entityManager;
    private @Autowired ProjectService projectService;
    private @Autowired DocumentService documentService;
    private @Autowired WorkloadManagementService workloadManagementService;
    private @Autowired DynamicWorkloadExtension dynamicWorkloadExtension;

    private final SourceDocument document;

    public DynamicWorkloadUpdateDocumentStateTask(SourceDocument aDocument, String aTrigger)
    {
        super(aDocument.getProject(), aTrigger, ofSeconds(2));
        document = aDocument;
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
        WorkloadManager workloadManager = workloadManagementService
                .loadOrCreateWorkloadManagerConfiguration(project);
        if (!DYNAMIC_WORKLOAD_MANAGER_EXTENSION_ID.equals(workloadManager.getType())) {
            return;
        }

        // Get the latest state
        SourceDocument doc = documentService.getSourceDocument(project.getId(), document.getId());

        // If the source document is already in curation, we do not touch the state anymore
        if (doc.getState() == CURATION_FINISHED || doc.getState() == CURATION_IN_PROGRESS) {
            return;
        }

        DynamicWorkloadTraits traits = dynamicWorkloadExtension.readTraits(workloadManager);
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
        DynamicWorkloadUpdateDocumentStateTask task = (DynamicWorkloadUpdateDocumentStateTask) o;
        return document.equals(task.document) && getProject().equals(task.getProject());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(document, getProject());
    }
}
