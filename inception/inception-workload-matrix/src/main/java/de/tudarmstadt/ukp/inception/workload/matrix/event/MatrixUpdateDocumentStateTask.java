package de.tudarmstadt.ukp.inception.workload.matrix.event;

import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.inception.workload.matrix.MatrixWorkloadExtension.MATRIX_WORKLOAD_MANAGER_EXTENSION_ID;

import java.util.Map;
import java.util.Objects;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.inception.scheduling.DebouncingTask;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

public class MatrixUpdateDocumentStateTask
    extends DebouncingTask
{
    private @PersistenceContext EntityManager entityManager;
    private @Autowired ProjectService projectService;
    private @Autowired DocumentService documentService;
    private @Autowired WorkloadManagementService workloadManagementService;

    private final SourceDocument document;

    public MatrixUpdateDocumentStateTask(SourceDocument aDocument, String aTrigger)
    {
        super(aDocument.getProject(), aTrigger, 15_000l);
        document = aDocument;
    }

    @Override
    public void run()
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

        if (!MATRIX_WORKLOAD_MANAGER_EXTENSION_ID.equals(workloadManagementService
                .loadOrCreateWorkloadManagerConfiguration(project).getType())) {
        }

        // Get the latest state
        SourceDocument doc = documentService.getSourceDocument(project.getId(), document.getId());

        // If the source document is already in curation, we do not touch the state anymore
        if (doc.getState() == CURATION_FINISHED || doc.getState() == CURATION_IN_PROGRESS) {
            return;
        }

        int annotatorCount = projectService.listProjectUsersWithPermissions(project).size();

        Map<AnnotationDocumentState, Long> stats = documentService.getAnnotationDocumentStats(doc);
        long ignoreCount = stats.get(AnnotationDocumentState.IGNORE);
        long finishedCount = stats.get(AnnotationDocumentState.FINISHED);
        long newCount = stats.get(AnnotationDocumentState.NEW);

        // If all documents are ignored or finished, we set the source document to finished
        if ((finishedCount + ignoreCount) == annotatorCount) {
            documentService.setSourceDocumentState(doc, ANNOTATION_FINISHED);
        }
        // ... or we set it to new if there is at least one new document and the others are ignored
        else if ((newCount + ignoreCount) == annotatorCount) {
            documentService.setSourceDocumentState(doc, SourceDocumentState.NEW);
        }
        else {
            documentService.setSourceDocumentState(doc, SourceDocumentState.ANNOTATION_IN_PROGRESS);
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
        MatrixUpdateDocumentStateTask task = (MatrixUpdateDocumentStateTask) o;
        return document.equals(task.document) && getProject().equals(task.getProject());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(document, getProject());
    }
}
