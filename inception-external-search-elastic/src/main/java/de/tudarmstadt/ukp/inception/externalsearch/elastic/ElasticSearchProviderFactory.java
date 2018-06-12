/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.inception.externalsearch.elastic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchProvider;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchProviderFactoryImplBase;

@Component("ElasticSearchProviderFactory")
public class ElasticSearchProviderFactory
    extends ExternalSearchProviderFactoryImplBase
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public String getDisplayName()
    {
        return "ElasticSearchProviderFactory";
    }

    @Override
    public int getOrder()
    {
        return 0;
    }

    @Override
    public ExternalSearchProvider getNewExternalSearchProvider(Project aProject,
            AnnotationSchemaService aAnnotationSchemaService, DocumentService aDocumentService,
            ProjectService aProjectService, String aDir)
    {
        ExternalSearchProvider provider = null;
        try {
            provider = new ElasticSearchProvider();
        }
        catch (Exception e) {
            log.error("Unable to get Elastic Search provider", e);
        }
        return provider;
    }

}
