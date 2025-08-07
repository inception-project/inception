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
package de.tudarmstadt.ukp.inception.externalsearch;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.externalsearch.config.ExternalSearchAutoConfiguration;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

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
    @SuppressWarnings("javadoc")
    public ExternalSearchServiceImpl(ExternalSearchProviderRegistry aExternalSearchProviderRegistry,
            EntityManager aEntityManager)
    {
        externalSearchProviderRegistry = aExternalSearchProviderRegistry;
        entityManager = aEntityManager;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public List<ExternalSearchResult> query(User aUser, DocumentRepository aRepository,
            String aQuery)
        throws IOException
    {
        log.debug("Running query: {}", aQuery);

        ExternalSearchProviderFactory<?> factory = externalSearchProviderRegistry
                .getExternalSearchProviderFactory(aRepository.getType());

        ExternalSearchProvider provider = factory.getNewExternalSearchProvider();

        var traits = factory.readTraits(aRepository);

        List<ExternalSearchResult> results = provider.executeQuery(aRepository, traits, aQuery);

        return results;
    }

    @Override
    @Transactional
    public List<DocumentRepository> listDocumentRepositories(Project aProject)
    {
        String query = String.join("\n", //
                "FROM DocumentRepository", //
                "WHERE project = :project", //
                "ORDER BY name ASC");

        return entityManager.createQuery(query, DocumentRepository.class)
                .setParameter("project", aProject) //
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsEnabledDocumentRepository(Project aProject)
    {
        var query = String.join("\n", //
                "SELECT COUNT(*)", //
                "FROM DocumentRepository", //
                "WHERE project = :project");

        return entityManager.createQuery(query, Long.class) //
                .setParameter("project", aProject) //
                .getSingleResult() > 0;
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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public ExternalSearchResult getDocumentResult(DocumentRepository aRepository,
            String aCollectionId, String aDocumentId)
        throws IOException
    {
        ExternalSearchProviderFactory<?> factory = externalSearchProviderRegistry
                .getExternalSearchProviderFactory(aRepository.getType());

        ExternalSearchProvider provider = factory.getNewExternalSearchProvider();

        Object traits = factory.readTraits(aRepository);

        return provider.getDocumentResult(aRepository, traits, aCollectionId, aDocumentId);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public String getDocumentText(DocumentRepository aRepository, String aCollectionId,
            String aDocumentId)
        throws IOException
    {
        ExternalSearchProviderFactory<?> factory = externalSearchProviderRegistry
                .getExternalSearchProviderFactory(aRepository.getType());

        ExternalSearchProvider provider = factory.getNewExternalSearchProvider();

        Object traits = factory.readTraits(aRepository);

        return provider.getDocumentText(aRepository, traits, aCollectionId, aDocumentId);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public InputStream getDocumentAsStream(DocumentRepository aRepository, String aCollectionId,
            String aDocumentId)
        throws IOException
    {
        ExternalSearchProviderFactory<?> factory = externalSearchProviderRegistry
                .getExternalSearchProviderFactory(aRepository.getType());

        ExternalSearchProvider provider = factory.getNewExternalSearchProvider();

        Object traits = factory.readTraits(aRepository);

        return provider.getDocumentAsStream(aRepository, traits, aCollectionId, aDocumentId);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public String getDocumentFormat(DocumentRepository aRepository, String aCollectionId,
            String aDocumentId)
        throws IOException
    {
        ExternalSearchProviderFactory<?> factory = externalSearchProviderRegistry
                .getExternalSearchProviderFactory(aRepository.getType());

        ExternalSearchProvider provider = factory.getNewExternalSearchProvider();

        Object traits = factory.readTraits(aRepository);

        return provider.getDocumentFormat(aRepository, traits, aCollectionId, aDocumentId);
    }
}
