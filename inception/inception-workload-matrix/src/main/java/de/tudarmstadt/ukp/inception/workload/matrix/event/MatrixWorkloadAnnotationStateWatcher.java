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
package de.tudarmstadt.ukp.inception.workload.matrix.event;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IGNORE;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.inception.workload.matrix.MatrixWorkloadExtension.MATRIX_WORKLOAD_MANAGER_EXTENSION_ID;

import java.util.List;

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
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

/**
 * Watches the state of annotation documents.
 * 
 * <b>Note:</b> This is separate from the {@link MatrixWorkloadDocumentStateWatcher} because we
 * observed that otherwise a trigger of a {@liink DocumentStateChangedEvent} by
 * {@link #recalculateDocumentState} was unable to be caught by the
 * {@link MatrixWorkloadDocumentStateWatcher#onDocumentStateChangeEvent}
 */
public class MatrixWorkloadAnnotationStateWatcher
{
    private @PersistenceContext EntityManager entityManager;
    private final ProjectService projectService;
    private final DocumentService documentService;
    private final WorkloadManagementService workloadManagementService;

    public MatrixWorkloadAnnotationStateWatcher(ProjectService aProjectService,
            DocumentService aDocumentService, WorkloadManagementService aWorkloadManagementService)
    {
        projectService = aProjectService;
        documentService = aDocumentService;
        workloadManagementService = aWorkloadManagementService;
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

        if (!MATRIX_WORKLOAD_MANAGER_EXTENSION_ID.equals(workloadManagementService
                .loadOrCreateWorkloadManagerConfiguration(project).getType())) {
        }

        SourceDocument doc = documentService.getSourceDocument(project.getId(),
                aAnnotationDocument.getDocument().getId());

        // If the source document is already in curation, we do not touch the state anymore
        if (doc.getState() == CURATION_FINISHED || doc.getState() == CURATION_IN_PROGRESS) {
            return;
        }

        List<AnnotationDocument> annDocs = documentService
                .listAnnotationDocuments(aAnnotationDocument.getDocument());

        long ignoreCount = annDocs.stream().filter(adoc -> adoc.getState() == IGNORE).count();
        long finishedCount = annDocs.stream().filter(adoc -> adoc.getState() == FINISHED).count();

        long newCount = annDocs.stream()
                .filter(adoc -> adoc.getState() == AnnotationDocumentState.NEW) //
                .count();

        // If all documents are ignored or finished, we set the source document to finished
        if ((finishedCount + ignoreCount) == annDocs.size()) {
            documentService.setSourceDocumentState(aAnnotationDocument.getDocument(),
                    ANNOTATION_FINISHED);
        }
        // ... or we set it to new if there is at least one new document and the others are ignored
        else if ((newCount + ignoreCount) == annDocs.size()) {
            documentService.setSourceDocumentState(aAnnotationDocument.getDocument(),
                    SourceDocumentState.NEW);
        }
        else {
            documentService.setSourceDocumentState(aAnnotationDocument.getDocument(),
                    SourceDocumentState.ANNOTATION_IN_PROGRESS);
        }
    }
}
