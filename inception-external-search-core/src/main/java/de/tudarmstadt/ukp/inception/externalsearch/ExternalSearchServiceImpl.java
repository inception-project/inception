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
    private String externalSearchProviderFactoryName = "ElasticSearchProviderFactory";

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
    public List<ExternalSearchResult> query(User aUser, Project aProject, String aQuery)
    {
        ExternalSearchProvider provider = getExternalSearchProviderByProject(aProject);

        if (provider.isConnected()) {

            log.debug("Running query: {}", aQuery);

            List<ExternalSearchResult> results = provider.executeQuery(aUser, aQuery, null, null);

            for (ExternalSearchResult result : results) {
                String title = result.getDocumentTitle();
            }
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

}
