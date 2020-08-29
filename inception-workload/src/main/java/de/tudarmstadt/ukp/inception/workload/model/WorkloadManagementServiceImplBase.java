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

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;

@Component(WorkloadManagementService.SERVICE_NAME)
public class WorkloadManagementServiceImplBase implements WorkloadManagementService
{
    private final EntityManager entityManager;

    @Autowired
    public WorkloadManagementServiceImplBase(
        EntityManager aEntityManager)
    {
        entityManager = aEntityManager;
    }

    @Override
    @Transactional
    public String getOrCreateExtensionPoint(Project aProject) {
        try {
            String result = entityManager.createQuery(
                "SELECT extensionPointID " +
                    "FROM WorkloadManagerTraits " +
                    "WHERE project = :projectID", String.class)
                .setParameter("projectID", aProject).getSingleResult();
            return result;
        } catch (NoResultException e) {
            createDefaultEntry(aProject,"StaticWorkloadExtension",null);
            return "StaticWorkloadExtension";
        }
    }

    @Override
    @Transactional
    public String getOrCreateTraits(Project aProject) {
        try {
            String result = entityManager.createQuery(
                "SELECT extensionPointID " +
                    "FROM WorkloadManagerTraits " +
                    "WHERE project = :projectID", String.class)
                .setParameter("projectID", aProject).getSingleResult();
            return result;
        } catch (NoResultException e) {
            createDefaultEntry(aProject,"StaticWorkloadExtension",null);
            return null;
        }
    }

    @Override
    @Transactional
    public void setExtensionPoint(String aExtensionPointID, Project aProject) {
        entityManager.createQuery(
            "UPDATE WorkloadManagerTraits " +
                "SET extensionPointID = :extensionPointID " +
                "WHERE project = :projectID")
            .setParameter("extensionPointID", aExtensionPointID)
            .setParameter("projectID", aProject).executeUpdate();
    }

    @Override
    @Transactional
    public void setTraits(String aTraits, Project aProject) {
        entityManager.createQuery(
            "UPDATE WorkloadManagerTraits " +
                "SET traits = :traits " +
                "WHERE project = :projectID")
            .setParameter("traits", aTraits)
            .setParameter("projectID", aProject).executeUpdate();
    }

    private void createDefaultEntry(Project aProject, String aExtensionPoint, String aTraits)
    {
        entityManager.persist(new WorkloadManagerTraits(aProject,aExtensionPoint,aTraits));
    }
}
