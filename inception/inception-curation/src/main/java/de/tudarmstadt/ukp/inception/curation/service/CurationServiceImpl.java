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
package de.tudarmstadt.ukp.inception.curation.service;

import static java.util.Objects.isNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.curation.config.CurationServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeStrategy;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeStrategyFactory;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeStrategyFactoryExtensionPoint;
import de.tudarmstadt.ukp.inception.curation.model.CurationWorkflow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link CurationServiceAutoConfiguration#curationService}.
 * </p>
 */
public class CurationServiceImpl
    implements CurationService
{
    private final EntityManager entityManager;
    private final MergeStrategyFactoryExtensionPoint mergeStrategyFactoryExtensionPoint;

    @Autowired
    public CurationServiceImpl(EntityManager aEntityManager,
            MergeStrategyFactoryExtensionPoint aMergeStrategyFactoryExtensionPoint)
    {
        entityManager = aEntityManager;
        mergeStrategyFactoryExtensionPoint = aMergeStrategyFactoryExtensionPoint;
    }

    @Override
    @Transactional
    public void createOrUpdateCurationWorkflow(CurationWorkflow aCurationWorkflow)
    {
        if (isNull(aCurationWorkflow.getId())) {
            entityManager.persist(aCurationWorkflow);
        }
        else {
            entityManager.merge(aCurationWorkflow);
        }
    }

    @Override
    @Transactional
    public CurationWorkflow readOrCreateCurationWorkflow(Project aProject)
    {
        CurationWorkflow result;
        try {
            var query = "FROM CurationWorkflow WHERE project = :project";

            result = entityManager.createQuery(query, CurationWorkflow.class) //
                    .setParameter("project", aProject) //
                    .getSingleResult();
        }
        catch (NoResultException e) {
            result = new CurationWorkflow(aProject,
                    mergeStrategyFactoryExtensionPoint.getDefault().getId(), null);
            entityManager.persist(result);
        }

        return result;
    }

    @Override
    @Transactional
    public MergeStrategy getDefaultMergeStrategy(Project aProject)
    {
        return getMergeStrategy(readOrCreateCurationWorkflow(aProject));
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public MergeStrategy getMergeStrategy(CurationWorkflow aCurationWorkflow)
    {
        var factory = getMergeStrategyFactory(aCurationWorkflow);
        return factory.makeStrategy(factory.readTraits(aCurationWorkflow));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public MergeStrategyFactory getMergeStrategyFactory(CurationWorkflow aCurationWorkflow)
    {
        return mergeStrategyFactoryExtensionPoint //
                .getExtension(aCurationWorkflow.getMergeStrategy())
                .orElseGet(mergeStrategyFactoryExtensionPoint::getDefault);
    }
}
