/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.inception.externalsearch;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.externalsearch.config.ExternalSearchAutoConfiguration;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;

/**
 * Implementation of the external search service API.
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ExternalSearchAutoConfiguration#externalSearchService}.
 * </p>
 */
public class ExternalSearchServiceImpl
    implements ExternalSearchService
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private @PersistenceContext EntityManager entityManager;

    private final ExternalSearchProviderRegistry externalSearchProviderRegistry;

    @Autowired
    public ExternalSearchServiceImpl(ExternalSearchProviderRegistry aExternalSearchProviderRegistry)
    {
        externalSearchProviderRegistry = aExternalSearchProviderRegistry;
    }

    /**
     * For testing.
     */
    public ExternalSearchServiceImpl(ExternalSearchProviderRegistry aExternalSearchProviderRegistry,
            EntityManager aEntityManager)
    {
        externalSearchProviderRegistry = aExternalSearchProviderRegistry;
        entityManager = aEntityManager;
    }

    @Override
    public List<ExternalSearchResult> query(User aUser, DocumentRepository aRepository,
            String aQuery)
        throws IOException
    {
        log.debug("Running query: {}", aQuery);
        
        ExternalSearchProviderFactory factory = externalSearchProviderRegistry
                .getExternalSearchProviderFactory(aRepository.getType());
        
        ExternalSearchProvider provider = factory.getNewExternalSearchProvider();

        Object traits = factory.readTraits(aRepository);

        List<ExternalSearchResult> results = provider.executeQuery(aRepository, traits, aQuery);

        return results;
    }

    @Override
    @Transactional
    public List<DocumentRepository> listDocumentRepositories(Project aProject)
    {
        List<DocumentRepository> settings = entityManager
                .createQuery("FROM DocumentRepository WHERE project = :project ORDER BY name ASC",
                        DocumentRepository.class)
                .setParameter("project", aProject).getResultList();
        return settings;
    }

    @Override
    @Transactional
    public void createOrUpdateDocumentRepository(DocumentRepository aDocumentRepository)
    {
        if (aDocumentRepository.getId() == null) {
            entityManager.persist(aDocumentRepository);
        }
        else {
            entityManager.merge(aDocumentRepository);
        }        
    }

    @Override
    @Transactional
    public void deleteDocumentRepository(DocumentRepository aDocumentRepository)
    {
        DocumentRepository settings = aDocumentRepository;

        if (!entityManager.contains(settings)) {
            settings = entityManager.merge(settings);
        }

        entityManager.remove(settings);
    }
    
    @Override
    @Transactional
    public DocumentRepository getRepository(long aId)
    {
        return entityManager.find(DocumentRepository.class, aId);
    }

    @Override
    public ExternalSearchResult getDocumentResult(DocumentRepository aRepository,
            String aCollectionId, String aDocumentId)
        throws IOException
    {
        ExternalSearchProviderFactory factory = externalSearchProviderRegistry
                .getExternalSearchProviderFactory(aRepository.getType());

        ExternalSearchProvider provider = factory.getNewExternalSearchProvider();

        Object traits = factory.readTraits(aRepository);

        return provider.getDocumentResult(aRepository, traits, aCollectionId, aDocumentId);
    }
    
    @Override
    public String getDocumentText(DocumentRepository aRepository, String aCollectionId,
            String aDocumentId)
        throws IOException
    {
        ExternalSearchProviderFactory factory = externalSearchProviderRegistry
                .getExternalSearchProviderFactory(aRepository.getType());

        ExternalSearchProvider provider = factory.getNewExternalSearchProvider();

        Object traits = factory.readTraits(aRepository);

        return provider.getDocumentText(aRepository, traits, aCollectionId, aDocumentId);
    }

    @Override
    public InputStream getDocumentAsStream(DocumentRepository aRepository, String aCollectionId,
            String aDocumentId)
        throws IOException
    {
        ExternalSearchProviderFactory factory = externalSearchProviderRegistry
                .getExternalSearchProviderFactory(aRepository.getType());

        ExternalSearchProvider provider = factory.getNewExternalSearchProvider();

        Object traits = factory.readTraits(aRepository);

        return provider.getDocumentAsStream(aRepository, traits, aCollectionId, aDocumentId);
    }

    @Override
    public String getDocumentFormat(DocumentRepository aRepository, String aCollectionId,
            String aDocumentId)
        throws IOException
    {
        ExternalSearchProviderFactory factory = externalSearchProviderRegistry
                .getExternalSearchProviderFactory(aRepository.getType());

        ExternalSearchProvider provider = factory.getNewExternalSearchProvider();

        Object traits = factory.readTraits(aRepository);

        return provider.getDocumentFormat(aRepository, traits, aCollectionId, aDocumentId);
    }
}
