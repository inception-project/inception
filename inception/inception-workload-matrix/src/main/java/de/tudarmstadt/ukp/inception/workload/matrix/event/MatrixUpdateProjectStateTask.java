package de.tudarmstadt.ukp.inception.workload.matrix.event;

import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.NEW;
import static de.tudarmstadt.ukp.inception.workload.matrix.MatrixWorkloadExtension.MATRIX_WORKLOAD_MANAGER_EXTENSION_ID;

import java.util.Arrays;
import java.util.Objects;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.event.ProjectStateChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.project.SourceDocumentStateStats;
import de.tudarmstadt.ukp.inception.scheduling.DebouncingTask;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

public class MatrixUpdateProjectStateTask
    extends DebouncingTask
{
    private @PersistenceContext EntityManager entityManager;
    private @Autowired ProjectService projectService;
    private @Autowired ApplicationEventPublisher applicationEventPublisher;
    private @Autowired WorkloadManagementService workloadManagementService;

    public MatrixUpdateProjectStateTask(Project aProject, String aTrigger)
    {
        super(aProject, aTrigger, 30_000l);
    }

    @Override
    public void run()
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

        if (!MATRIX_WORKLOAD_MANAGER_EXTENSION_ID.equals(workloadManagementService
                .loadOrCreateWorkloadManagerConfiguration(project).getType())) {
        }

        // This query is better because we do not inject strings into the query string, but it
        // does not work on HSQLDB (on MySQL it seems to work).
        // See: https://github.com/webanno/webanno/issues/1011
        // String query =
        // "SELECT new " + SourceDocumentStateStats.class.getName() + "(" +
        // "COUNT(*) AS num, " +
        // "SUM(CASE WHEN state = :an THEN 1 ELSE 0 END), " +
        // "SUM(CASE WHEN (state = :aip OR state is NULL) THEN 1 ELSE 0 END), " +
        // "SUM(CASE WHEN state = :af THEN 1 ELSE 0 END), " +
        // "SUM(CASE WHEN state = :cip THEN 1 ELSE 0 END), " +
        // "SUM(CASE WHEN state = :cf THEN 1 ELSE 0 END)) " +
        // "FROM SourceDocument " +
        // "WHERE project = :project";
        //
        // SourceDocumentStateStats stats = entityManager.createQuery(
        // query, SourceDocumentStateStats.class)
        // .setParameter("project", aProject)
        // .setParameter("an", SourceDocumentState.NEW)
        // .setParameter("aip", SourceDocumentState.ANNOTATION_IN_PROGRESS)
        // .setParameter("af", SourceDocumentState.ANNOTATION_FINISHED)
        // .setParameter("cip", SourceDocumentState.CURATION_IN_PROGRESS)
        // .setParameter("cf", SourceDocumentState.CURATION_FINISHED)
        // .getSingleResult();

        // @formatter:off
        String query = 
                "SELECT new " + SourceDocumentStateStats.class.getName() + "(" +
                "COUNT(*), " +
                "SUM(CASE WHEN state = '" + NEW.getId() + "'  THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN (state = '" + ANNOTATION_IN_PROGRESS.getId() + 
                        "' OR state is NULL) THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN state = '" + ANNOTATION_FINISHED.getId() + 
                        "'  THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN state = '" + CURATION_IN_PROGRESS.getId() + 
                        "' THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN state = '" + CURATION_FINISHED.getId() + "'  THEN 1 ELSE 0 END)) " +
                "FROM SourceDocument " + 
                "WHERE project = :project";
        // @formatter:on

        SourceDocumentStateStats stats = entityManager
                .createQuery(query, SourceDocumentStateStats.class).setParameter("project", project)
                .getSingleResult();

        ProjectState oldState = project.getState();

        // We had some strange reports about being unable to calculate the project state, so to
        // be better able to debug this, we add some more detailed information to the exception
        // message here.
        try {
            project.setState(stats.getProjectState());
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

        projectService.updateProject(project);
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
        MatrixUpdateProjectStateTask task = (MatrixUpdateProjectStateTask) o;
        return getProject().equals(task.getProject());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getProject());
    }
}
