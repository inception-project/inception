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

import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.inception.workload.dynamic.DynamicWorkloadExtension.DYNAMIC_WORKLOAD_MANAGER_EXTENSION_ID;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AnnotationStateChangeEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.inception.workload.dynamic.DynamicWorkloadExtension;
import de.tudarmstadt.ukp.inception.workload.dynamic.trait.DynamicWorkloadTraits;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

/**
 * Watches the state of annotation documents.
 * 
 * <b>Note:</b> This is separate from the {@link DynamicWorkloadDocumentStateWatcher} because we
 * observed that otherwise a trigger of a {@liink DocumentStateChangedEvent} by
 * {@link #recalculateDocumentState} was unable to be caught by the
 * {@link DynamicWorkloadDocumentStateWatcher#onDocumentStateChangeEvent}
 */
public class DynamicWorkloadAnnotationStateWatcher
{
    private @PersistenceContext EntityManager entityManager;
    private final ProjectService projectService;
    private final DocumentService documentService;
    private final WorkloadManagementService workloadManagementService;
    private final DynamicWorkloadExtension dynamicWorkloadExtension;

    public DynamicWorkloadAnnotationStateWatcher(ProjectService aProjectService,
            DocumentService aDocumentService, WorkloadManagementService aWorkloadManagementService,
            DynamicWorkloadExtension aDynamicWorkloadExtension)
    {
        projectService = aProjectService;
        documentService = aDocumentService;
        workloadManagementService = aWorkloadManagementService;
        dynamicWorkloadExtension = aDynamicWorkloadExtension;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void onAnnotationStateChangeEvent(AnnotationStateChangeEvent aEvent)
    {
        recalculateDocumentState(aEvent.getAnnotationDocument());
    }

    private void recalculateDocumentState(AnnotationDocument aAnnotationDocument)
    {
        Project project;
        try {
            project = projectService.getProject(aAnnotationDocument.getProject().getId());
        }
        catch (NoResultException e) {
            // This happens when this method is called as part of deleting an entire project.
            // In such a case, the project may no longer be available, so there is no point in
            // updating its state. So then we do nothing here.
            return;
        }

        if (!DYNAMIC_WORKLOAD_MANAGER_EXTENSION_ID.equals(workloadManagementService
                .loadOrCreateWorkloadManagerConfiguration(project).getType())) {
        }

        SourceDocument doc = documentService.getSourceDocument(project.getId(),
                aAnnotationDocument.getDocument().getId());

        // If the source document is already in curation, we do not touch the state anymore
        if (doc.getState() == CURATION_FINISHED || doc.getState() == CURATION_IN_PROGRESS) {
            return;
        }

        DynamicWorkloadTraits traits = dynamicWorkloadExtension.readTraits(
                workloadManagementService.loadOrCreateWorkloadManagerConfiguration(project));
        int requiredAnnotatorCount = traits.getDefaultNumberOfAnnotations();

        Map<AnnotationDocumentState, Long> stats = documentService
                .getAnnotationDocumentStats(aAnnotationDocument.getDocument());
        long finishedCount = stats.get(AnnotationDocumentState.FINISHED);

        // If enough documents are finished, mark as finished
        if (finishedCount >= requiredAnnotatorCount) {
            documentService.setSourceDocumentState(aAnnotationDocument.getDocument(),
                    ANNOTATION_FINISHED);
        }
        // ... or if nobody has started yet, mark as new
        else if (finishedCount == 0) {
            documentService.setSourceDocumentState(aAnnotationDocument.getDocument(),
                    SourceDocumentState.NEW);
        }
        else {
            documentService.setSourceDocumentState(aAnnotationDocument.getDocument(),
                    SourceDocumentState.ANNOTATION_IN_PROGRESS);
        }
    }
}
