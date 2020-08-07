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

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import static de.tudarmstadt.ukp.inception.workload.dynamic.api.WorkloadConst.*;

@Component(WorkloadAndWorkflowService.SERVICE_NAME)
public class WorkloadAndWorkflowServiceImplBase implements WorkloadAndWorkflowService
{
    private final EntityManager entityManager;
    private final ProjectService projectService;

    @Autowired
    public WorkloadAndWorkflowServiceImplBase(
        EntityManager aEntityManager, ProjectService aProjectService)
    {
        entityManager = aEntityManager;
        projectService = aProjectService;
    }

    @Override
    @Transactional
    public String getWorkflowManager(Project aProject)
    {
        try {
            String result = entityManager.createQuery(
                "SELECT workflowManagerType " +
                    "FROM WorkloadAssignment " +
                    "WHERE project = :projectID", String.class)
                .setParameter("projectID", aProject).getSingleResult();
            return result;
        } catch (NoResultException e) {
            createDefaultEntry(aProject);
            return DEFAULT_WORKFLOW;
        }
    }

    @Override
    @Transactional
    public String getWorkloadManager(Project aProject)
    {
        try {
            String result = entityManager.createQuery(
                "SELECT workloadManagerType " +
                    "FROM WorkloadAssignment " +
                    "WHERE project = :projectID", String.class)
                .setParameter("projectID", aProject).getSingleResult();
            return result;
        } catch (NoResultException e) {
            createDefaultEntry(aProject);
            return DEFAULT_MONITORING;
        }
    }

    @Override
    @Transactional
    public Integer getDefaultNumberOfAnnotations(Project aProject)
    {
        try {
            Integer result = entityManager.createQuery(
                "SELECT numberOfAnnotations " +
                    "FROM WorkloadAssignment " +
                    "WHERE project = :projectID", Integer.class)
                .setParameter("projectID", aProject).getSingleResult();
            return result;
        } catch (NoResultException e) {
            createDefaultEntry(aProject);
            return 3;
        }
    }

    @Override
    @Transactional
    public String getWorkflowType(Project aProject)
    {
        try {
            String result = entityManager.createQuery(
                "SELECT workflow " +
                    "FROM WorkloadAssignment " +
                    "WHERE project = :projectID", String.class)
                .setParameter("projectID", aProject).getSingleResult();
            return result;
        } catch (NoResultException e) {
            createDefaultEntry(aProject);
            return DEFAULT_WORKFLOW_TYPE;
        }
    }

    @Override
    @Transactional
    public void setWorkflowManager(String aType, Project aProject)
    {
        entityManager.createQuery(
            "UPDATE WorkloadAssignment " +
                "SET workflowManagerType = :workflowManager " +
                "WHERE project = :projectID")
            .setParameter("workflowManager", aType)
            .setParameter("projectID", aProject).executeUpdate();
    }

    @Override
    @Transactional
    public void setWorkloadManager(String aType, Project aProject)
    {
        entityManager.createQuery(
            "UPDATE WorkloadAssignment " +
                "SET workloadManagerType = :workloadManager " +
                "WHERE project = :projectID")
            .setParameter("workloadManager", aType)
            .setParameter("projectID", aProject).executeUpdate();
    }

    @Override
    @Transactional
    public void setWorkflowType(String aType, Project aProject)
    {
        entityManager.createQuery(
            "UPDATE WorkloadAssignment " +
                "SET workflow = :workflow " +
                "WHERE project = :projectID")
            .setParameter("workflow", aType)
            .setParameter("projectID", aProject).executeUpdate();
    }

    @Override
    @Transactional
    public void setDefaultNumberOfAnnotations(int aDefaultAnnotationsNumber, Project aProject)
    {
        entityManager.createQuery(
            "UPDATE WorkloadAssignment " +
                "SET numberOfAnnotations = :aDefaultAnnotationsNumber " +
                "WHERE project = :projectID")
            .setParameter("aDefaultAnnotationsNumber", aDefaultAnnotationsNumber)
            .setParameter("projectID", aProject).executeUpdate();
    }

    @Transactional
    public void createDefaultEntry(Project aProject)
    {
        entityManager.persist(new WorkloadAssignment(
            aProject,DEFAULT_WORKFLOW,DEFAULT_MONITORING,
            DEFAULT_WORKFLOW_TYPE,3));
    }
}
