/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.chains;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.model.WeblichtChain;

@Component
public class WeblichtChainServiceImpl
    implements WeblichtChainService
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @PersistenceContext
    private EntityManager entityManager;
    
    private final RepositoryProperties repositoryProperties;
    
    @Autowired
    public WeblichtChainServiceImpl(RepositoryProperties aRepositoryProperties)
    {
        repositoryProperties = aRepositoryProperties;
    }    

    public WeblichtChainServiceImpl(RepositoryProperties aRepositoryProperties,
            EntityManager aEntityManager)
    {
        this(aRepositoryProperties);
        entityManager = aEntityManager;
    }
    
    @Override
    @Transactional
    public Optional<WeblichtChain> getChain(Recommender aRecommender)
    {
        String query = String.join("\n", 
                "FROM WeblichtChain",
                "WHERE recommender = :recommender ",
                "ORDER BY name ASC");
        
        return entityManager
                .createQuery(query, WeblichtChain.class)
                .setParameter("recommender", aRecommender)
                .getResultList().stream().findFirst();
    }

    @Override
    @Transactional
    public void createOrUpdateChain(WeblichtChain aGazeteer)
    {
        if (aGazeteer.getId() == null) {
            entityManager.persist(aGazeteer);
            
            try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                    String.valueOf(aGazeteer.getRecommender().getProject().getId()))) {
                log.info("Created chain [{}] for recommender [{}]({}) in project [{}]({})",
                        aGazeteer.getName(), aGazeteer.getRecommender().getName(),
                        aGazeteer.getRecommender().getId(),
                        aGazeteer.getRecommender().getProject().getName(),
                        aGazeteer.getRecommender().getProject().getId());
            }
        }
        else {
            entityManager.merge(aGazeteer);
            
            try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                    String.valueOf(aGazeteer.getRecommender().getProject().getId()))) {
                log.info("Updated chain [{}] for recommender [{}]({}) in project [{}]({})",
                        aGazeteer.getName(), aGazeteer.getRecommender().getName(),
                        aGazeteer.getRecommender().getId(),
                        aGazeteer.getRecommender().getProject().getName(),
                        aGazeteer.getRecommender().getProject().getId());
            }
        }
    }

    @Override
    @Transactional
    public void importChainFile(WeblichtChain aGazeteer, InputStream aStream) throws IOException
    {
        File gazFile = getChainFile(aGazeteer);
        
        if (!gazFile.getParentFile().exists()) {
            gazFile.getParentFile().mkdirs();
        }
        
        try (OutputStream os = new FileOutputStream(gazFile)) {
            IOUtils.copyLarge(aStream, os);
        }
    }

    @Override
    public File getChainFile(WeblichtChain aChain) throws IOException
    {
        return repositoryProperties.getPath().toPath()
                .resolve("project")
                .resolve(String.valueOf(aChain.getRecommender().getProject().getId()))
                .resolve("weblicht_chains")
                .resolve(aChain.getId() + ".xml")
                .toFile();
    }

    @Override
    @Transactional
    public void deleteChain(WeblichtChain aChain) throws IOException
    {
        entityManager.remove(
                entityManager.contains(aChain) ? aChain : entityManager.merge(aChain));
        
        File gaz = getChainFile(aChain);
        if (gaz.exists()) {
            gaz.delete();
        }
        
        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aChain.getRecommender().getProject().getId()))) {
            log.info("Removed chain [{}] from recommender [{}]({}) in project [{}]({})",
                    aChain.getName(), aChain.getRecommender().getName(),
                    aChain.getRecommender().getId(),
                    aChain.getRecommender().getProject().getName(),
                    aChain.getRecommender().getProject().getId());
        }
    }
    
    @Override
    @Transactional
    public boolean existsChain(Recommender aRecommender)
    {
        Validate.notNull(aRecommender, "Recommender must be specified");
        
        String query = 
                "SELECT COUNT(*) " +
                "FROM WeblichtChain " + 
                "WHERE recommender = :recommender";
        
        long count = entityManager.createQuery(query, Long.class)
            .setParameter("recommender", aRecommender)
            .getSingleResult();

        return count > 0;
    }
}
