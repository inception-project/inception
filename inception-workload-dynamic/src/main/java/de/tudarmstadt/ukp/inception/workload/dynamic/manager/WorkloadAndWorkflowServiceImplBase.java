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
package de.tudarmstadt.ukp.inception.workload.dynamic.manager;

import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

@Component(WorkloadAndWorkflowService.SERVICE_NAME)
public class WorkloadAndWorkflowServiceImplBase implements WorkloadAndWorkflowService
{
    private final EntityManager entityManager;
    private final ProjectService projectService;

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
        List<String> result = entityManager.createQuery(
            "SELECT workflowmanager " +
                "FROM WorkloadAndWorkflow " +
                "WHERE project = :projectID", String.class)
            .setParameter("projectID", aProject).getResultList();

        if (result.isEmpty()) {
            entityManager.persist(new WorkloadAndWorkflow(
                aProject,"Default workload manager","Default monitoring page","detault",6));
            return "default";
        } else {
            return result.get(0);
        }
    }

    @Override
    @Transactional
    public String getWorkloadManager(Project aProject)
    {
        List<String> result = entityManager.createQuery(
            "SELECT workloadmanager " +
            "FROM WorkloadAndWorkflow " +
            "WHERE project = :projectID", String.class)
            .setParameter("projectID", aProject).getResultList();

        if (result.isEmpty()) {
            entityManager.persist(new WorkloadAndWorkflow(
                aProject,"Default workload manager","Default monitoring page","detault",6));
            return "default";
        } else {
            return result.get(0);
        }
    }

    @Override
    @Transactional
    public Integer getDefaultAnnotations(Project aProject)
    {
        List<Integer> result = entityManager.createQuery(
            "SELECT defaultnumberofannotations " +
                "FROM WorkloadAndWorkflow " +
                "WHERE project = :projectID", Integer.class)
            .setParameter("projectID", aProject).getResultList();

        if (result.isEmpty()) {
            entityManager.persist(new WorkloadAndWorkflow(
                aProject,"Default workload manager","Default monitoring page","detault",6));
            return 6;
        } else {
            return result.get(0);
        }
    }

    @Override
    @Transactional
    public String getWorkflowType(Project aProject)
    {
        List<String> result = entityManager.createQuery(
            "SELECT workflow " +
                "FROM WorkloadAndWorkflow " +
                "WHERE project = :projectID", String.class)
            .setParameter("projectID", aProject).getResultList();

        if (result.isEmpty()) {
            entityManager.persist(new WorkloadAndWorkflow(
                aProject,"Default workload manager","Default monitoring page","detault",6));
            return "default";
        } else {
            return result.get(0);
        }
    }

    @Override
    @Transactional
    public void setWorkflowManager(String aType, Project aProject)
    {
        entityManager.createQuery(
            "UPDATE WorkloadAndWorkflow " +
                "SET workflowmanager = :workflowManager " +
                "WHERE project = :projectID")
            .setParameter("workflowManager", aType)
            .setParameter("projectID", aProject).executeUpdate();
    }

    @Override
    @Transactional
    public void setWorkloadManager(String aType, Project aProject)
    {
        entityManager.createQuery(
            "UPDATE WorkloadAndWorkflow " +
                "SET workloadmanager = :workloadManager " +
                "WHERE project = :projectID")
            .setParameter("workloadManager", aType)
            .setParameter("projectID", aProject).executeUpdate();
    }

    @Override
    @Transactional
    public void setWorkflowType(String aType, Project aProject)
    {
        entityManager.createQuery(
            "UPDATE WorkloadAndWorkflow " +
                "SET workflow = :workflow " +
                "WHERE project = :projectID")
            .setParameter("workflow", aType)
            .setParameter("projectID", aProject).executeUpdate();
    }

    @Override
    @Transactional
    public void setDefaultAnnotations(int aDefaultAnnotationsNumber, Project aProject)
    {
        entityManager.createQuery(
            "UPDATE WorkloadAndWorkflow " +
                "SET defaultnumberofannotations = :aDefaultAnnotationsNumber " +
                "WHERE project = :projectID")
            .setParameter("aDefaultAnnotationsNumber", aDefaultAnnotationsNumber)
            .setParameter("projectID", aProject).executeUpdate();
    }
}
