/*
 * Copyright 2020
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.workload.model;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.workload.config.WorkloadManagementAutoConfiguration;
import de.tudarmstadt.ukp.inception.workload.extension.WorkloadManagerExtensionPoint;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link WorkloadManagementAutoConfiguration#workloadManagementService}
 * </p>
 * Implementation of the WorkloadManagementService Interface. Very important class to get all the
 * fast DB lookups which are required.
 */
public class WorkloadManagementServiceImpl
    implements WorkloadManagementService
{
    private final EntityManager entityManager;
    private final WorkloadManagerExtensionPoint<Project> workloadManagerExtensionPoint;

    @Autowired
    public WorkloadManagementServiceImpl(EntityManager aEntityManager,
            WorkloadManagerExtensionPoint<Project> aWorkloadManagerExtensionPoint)
    {
        entityManager = aEntityManager;
        workloadManagerExtensionPoint = aWorkloadManagerExtensionPoint;
    }

    /**
     * Returns for a given project a WorkloadManager Object. Also applicable for older INCEpTION
     * version where the workload feature was not present. Also, if no entity can be found, a new
     * entry will be created and returned.
     */
    @Override
    @Transactional
    public WorkloadManager getOrCreateWorkloadManagerConfiguration(Project aProject)
    {
        WorkloadManager result;
        try {
            result = entityManager
                    .createQuery("SELECT wm " + "FROM WorkloadManager wm "
                            + "WHERE wm.project = :projectID", WorkloadManager.class)
                    .setParameter("projectID", aProject).getSingleResult();

            // If the workload strategy set for this project is not there anymore, use the strategy
            // with the lowest order
            if (workloadManagerExtensionPoint.getExtension(result.getType()) == null) {
                result.setType(workloadManagerExtensionPoint.getDefault().getId());
                entityManager.persist(result);
            }
        }
        catch (NoResultException e) {
            result = new WorkloadManager(aProject,
                    workloadManagerExtensionPoint.getDefault().getId(), null);
            entityManager.persist(result);
        }

        return result;
    }

    /**
     * Simply updates the DB entry with a new workload manager extension type.
     */
    @Override
    @Transactional
    public void setWorkloadManagerConfiguration(String aExtensionPointID, Project aProject)
    {
        entityManager
                .createQuery("UPDATE WorkloadManager " + "SET workloadType = :extensionPointID "
                        + "WHERE project = :projectID")
                .setParameter("extensionPointID", aExtensionPointID)
                .setParameter("projectID", aProject).executeUpdate();
    }

    /**
     * As with the other setWorkloadManagerConfiguration method, this also simply updates the DB
     * entry. However, this method also updates the traits of a WorkloadManager entity
     */
    @Override
    @Transactional
    public void setWorkloadManagerConfiguration(String aExtensionPointID, String aTraits,
            Project aProject)
    {
        entityManager
                .createQuery("UPDATE WorkloadManager "
                        + "SET workloadType = :extensionPointID, traits = :traits "
                        + "WHERE project = :projectID")
                .setParameter("extensionPointID", aExtensionPointID).setParameter("traits", aTraits)
                .setParameter("projectID", aProject).executeUpdate();
    }

    /**
     * This method is a fast DB search to get all USERS (List) for a specific SourceDocument in a
     * specific Project for a certain State.
     */
    @Override
    @Transactional
    public List<User> getUsersForSpecificDocumentAndState(AnnotationDocumentState aState,
            SourceDocument aSourceDocument, Project aProject)
    {
        return entityManager
                .createQuery("SELECT user FROM User user, AnnotationDocument anno "
                        + "WHERE anno.project = :projectID " + "AND anno.name = :document "
                        + "AND anno.state = :state", User.class)
                .setParameter("projectID", aProject)
                .setParameter("document", aSourceDocument.getName()).setParameter("state", aState)
                .getResultList();
    }

    /**
     * This method is a fast DB search to get a TOTAL NUMBER (Long) of Users working on a specific
     * SourceDocument in a specific Project.
     */
    @Override
    @Transactional
    public Long getAmountOfUsersWorkingOnADocument(SourceDocument aDocument, Project aProject)
    {
        return entityManager
                .createQuery(
                        " SELECT COUNT(anno) " + "FROM AnnotationDocument anno "
                                + "WHERE anno.name = :name " + "AND anno.project = :project ",
                        Long.class)
                .setParameter("name", aDocument.getName()).setParameter("project", aProject)
                .getSingleResult();
    }

    /**
     * This method is a fast DB search to get all ANNOTATION DOCUMENTS (List) for a specific User in
     * a specific Project with a specific State.
     */
    @Override
    @Transactional
    public List<AnnotationDocument> getAnnotationDocumentListForUserWithState(Project aProject,
            User aUser, AnnotationDocumentState aState)
    {
        return entityManager.createQuery("SELECT anno "
                + "FROM SourceDocument source LEFT JOIN AnnotationDocument anno ON "
                + "source.project = :project AND anno.user = :user AND anno.name = source.name AND anno.state = :state",
                AnnotationDocument.class).setParameter("project", aProject)
                .setParameter("state", aState).setParameter("user", aUser.getUsername())
                .getResultList();
    }

}
