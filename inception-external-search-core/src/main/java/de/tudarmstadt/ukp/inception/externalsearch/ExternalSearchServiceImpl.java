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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;

@Component(ExternalSearchService.SERVICE_NAME)
public class ExternalSearchServiceImpl
    implements ExternalSearchService
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private @PersistenceContext EntityManager entityManager;

    private @Autowired AnnotationSchemaService annotationSchemaService;
    private @Autowired DocumentService documentService;
    private @Autowired ProjectService projectService;
    private @Autowired ExternalSearchProviderRegistry externalSearchProviderRegistry;

    // Index factory
    private ExternalSearchProviderFactory externalSearchProviderFactory;
    private String externalSearchProviderFactoryName = "elasticSearchProviderFactory";

    // FIXME REC: We should not need a static map for these providers. If we need to hold on to 
    // a provider for a longer time (e.g. to support paging), we need to find another way to handle
    // this.
    // The indexes for each project
    private static Map<Long, ExternalSearchProvider> searchProviders;

    @Value(value = "${repository.path}")
    private String dir;

    public ExternalSearchServiceImpl()
    {
        searchProviders = new HashMap<>();
    }

    private ExternalSearchProvider getExternalSearchProviderByProject(Project aProject)
    {
        if (!searchProviders.containsKey(aProject.getId())) {
            externalSearchProviderFactory = externalSearchProviderRegistry
                    .getExternalSearchProviderFactory(externalSearchProviderFactoryName);

            searchProviders.put(aProject.getId(),
                    externalSearchProviderFactory.getNewExternalSearchProvider(aProject,
                            annotationSchemaService, documentService, projectService, dir));
        }

        return searchProviders.get(aProject.getId());
    }

    @Override
    public List<ExternalSearchResult> query(User aUser, DocumentRepository aDocumentRepository,
            String aQuery)
    {
        ExternalSearchProvider provider = getExternalSearchProviderByProject(
                aDocumentRepository.getProject());

        if (provider.isConnected()) {

            log.debug("Running query: {}", aQuery);

            Object properties = externalSearchProviderFactory.readTraits(aDocumentRepository);

            List<ExternalSearchResult> results = provider.executeQuery(properties, aUser,
                    aQuery, null, null);

            return results;
        }
        else {
            return null;
        }

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
    public ExternalSearchResult getDocumentById(User aUser, DocumentRepository aDocumentRepository,
            String aId)
    {
        ExternalSearchProvider provider = getExternalSearchProviderByProject(
                aDocumentRepository.getProject());

        if (provider.isConnected()) {

            Object properties = externalSearchProviderFactory.readTraits(aDocumentRepository);

            ExternalSearchResult result = provider.getDocumentById(properties, aId);
            
            return result;
        }
        else {
            return null;
        }
    }

}
