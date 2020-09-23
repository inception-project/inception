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
package de.tudarmstadt.ukp.inception.workload.dynamic.model;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.workload.dynamic.config.DynamicWorkloadManagerAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link DynamicWorkloadManagerAutoConfiguration#dynamicWorkflowManagementService}.
 * A persistence object for the workflow and workload properties of each project
 * </p>
 */
public class DynamicWorkflowManagementServiceImplBase implements DynamicWorkflowManagementService
{
    private final EntityManager entityManager;

    @Autowired
    public DynamicWorkflowManagementServiceImplBase(EntityManager aEntityManager)
    {
        entityManager = aEntityManager;
    }

    @Override
    @Transactional
    public DynamicWorkflowManager getOrCreateWorkflowEntry(Project aProject) {
        try {
            DynamicWorkflowManager result = entityManager.createQuery(
                "SELECT wm " +
                    "FROM DynamicWorkflowManager wm " +
                    "WHERE wm.project = :projectID", DynamicWorkflowManager.class)
                .setParameter("projectID", aProject).getSingleResult();
            return result;
        } catch (NoResultException e) {
            return createDefaultEntry(aProject);
        }
    }

    @Override
    @Transactional
    public void setWorkflow(String aWorkflow, Project aProject)
    {
        entityManager.createQuery(
            "UPDATE DynamicWorkflowManager " +
                "SET workflow = :workflow " +
                "WHERE project = :projectID")
            .setParameter("workflow", aWorkflow)
            .setParameter("projectID", aProject).executeUpdate();
    }

    @Override
    @Transactional
    public void setDefaultAnnotations(int aDefaultAnnotations, Project aProject)
    {
        entityManager.createQuery(
            "UPDATE DynamicWorkflowManager " +
                "SET defaultAnnotations = :defaultAnnotations " +
                "WHERE project = :projectID")
            .setParameter("defaultAnnotations", aDefaultAnnotations)
            .setParameter("projectID", aProject).executeUpdate();
    }

    private DynamicWorkflowManager createDefaultEntry(Project aProject)
    {
        entityManager.persist(new DynamicWorkflowManager(
            aProject,6,"Default"));
        DynamicWorkflowManager result = entityManager.createQuery(
            "SELECT wm " +
                "FROM  DynamicWorkflowManager wm " +
                "WHERE wm.project = :projectID", DynamicWorkflowManager.class)
            .setParameter("projectID", aProject).getSingleResult();
        return result;
    }
}
