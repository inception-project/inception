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
package de.tudarmstadt.ukp.inception.workload.model;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IN_PROGRESS;
import static java.util.Arrays.asList;

import java.io.Serializable;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.workload.config.WorkloadManagementAutoConfiguration;
import de.tudarmstadt.ukp.inception.workload.extension.WorkloadManagerExtension;
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
    implements WorkloadManagementService, Serializable
{
    private static final long serialVersionUID = 4019275726336545144L;

    private final EntityManager entityManager;
    private final WorkloadManagerExtensionPoint workloadManagerExtensionPoint;

    @Autowired
    public WorkloadManagementServiceImpl(EntityManager aEntityManager,
            WorkloadManagerExtensionPoint aWorkloadManagerExtensionPoint)
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
    public WorkloadManager loadOrCreateWorkloadManagerConfiguration(Project aProject)
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

    @Override
    public WorkloadManagerExtension<?> getWorkloadManagerExtension(Project aProject)
    {
        WorkloadManager currentWorkload = loadOrCreateWorkloadManagerConfiguration(aProject);
        return workloadManagerExtensionPoint.getExtension(currentWorkload.getType())
                .orElse(workloadManagerExtensionPoint.getDefault());
    }

    /**
     * Saves the configuration in the DB
     */
    @Override
    @Transactional
    public void saveConfiguration(WorkloadManager aManager)
    {
        String query = String.join("\n", //
                "UPDATE WorkloadManager ", //
                "SET workloadType = :workloadType, traits = :traits ", //
                "WHERE project = :projectID");

        entityManager.createQuery(query) //
                .setParameter("workloadType", aManager.getType()) //
                .setParameter("traits", aManager.getTraits()) //
                .setParameter("projectID", aManager.getProject()) //
                .executeUpdate();
    }

    /**
     * This method is a fast DB search to get all USERS (List) for a specific SourceDocument in a
     * specific Project for a certain State.
     */
    @Override
    @Transactional
    public List<AnnotationDocument> listAnnotationDocumentsForSourceDocumentInState(
            SourceDocument aSourceDocument, AnnotationDocumentState aState)
    {
        String query = String.join("\n", //
                "FROM AnnotationDocument", //
                "WHERE document = :document", //
                "AND state = :state");

        return entityManager.createQuery(query, AnnotationDocument.class) //
                .setParameter("document", aSourceDocument) //
                .setParameter("state", aState) //
                .getResultList();
    }

    /**
     * This method is a fast DB search to get a TOTAL NUMBER (Long) of Users working on a specific
     * SourceDocument in a specific Project.
     */
    @Override
    @Transactional
    public Long getNumberOfUsersWorkingOnADocument(SourceDocument aDocument)
    {
        String query = String.join("\n", //
                "SELECT COUNT(*)", //
                "FROM AnnotationDocument", //
                "WHERE document = :document", "AND state IN (:states)");

        return entityManager.createQuery(query, Long.class) //
                .setParameter("document", aDocument) //
                .setParameter("states", asList(IN_PROGRESS, FINISHED)) //
                .getSingleResult();
    }
}
