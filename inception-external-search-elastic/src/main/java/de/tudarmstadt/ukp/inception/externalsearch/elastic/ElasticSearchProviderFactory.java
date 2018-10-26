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

import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.fromJsonString;
import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.toJsonString;

import java.io.IOException;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchProvider;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchProviderFactory;
import de.tudarmstadt.ukp.inception.externalsearch.elastic.traits.ElasticSearchProviderTraits;
import de.tudarmstadt.ukp.inception.externalsearch.elastic.traits.ElasticSearchProviderTraitsEditor;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;

@Component
@Order(100)
public class ElasticSearchProviderFactory
    implements BeanNameAware, ExternalSearchProviderFactory<ElasticSearchProviderTraits>
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private String beanName;

    @Override
    public void setBeanName(String aName)
    {
        beanName = aName;
    }

    @Override
    public String getBeanName()
    {
        return beanName;
    }

    @Override
    public String getDisplayName()
    {
        return "Elastic Search";
    }

    @Override
    public ExternalSearchProvider getNewExternalSearchProvider(Project aProject,
            AnnotationSchemaService aAnnotationSchemaService, DocumentService aDocumentService,
            ProjectService aProjectService, String aDir)
    {
        return new ElasticSearchProvider(); 
    }

    @Override
    public Panel createTraitsEditor(String aId, IModel<DocumentRepository> aDocumentRepository)
    {
        return new ElasticSearchProviderTraitsEditor(aId, aDocumentRepository);
    }

    @Override
    public ElasticSearchProviderTraits readTraits(DocumentRepository aDocumentRepository)
    {
        ElasticSearchProviderTraits traits = null;
        try {
            traits = fromJsonString(ElasticSearchProviderTraits.class,
                    aDocumentRepository.getProperties());
        }
        catch (IOException e) {
            log.error("Error while reading traits", e);
        }

        if (traits == null) {
            traits = new ElasticSearchProviderTraits();
        }

        return traits;
    }

    @Override
    public void writeTraits(DocumentRepository aDocumentRepository,
            ElasticSearchProviderTraits aProperties)
    {
        try {
            String json = toJsonString(aProperties);
            aDocumentRepository.setProperties(json);
        }
        catch (IOException e) {
            log.error("Error while writing traits", e);
        }
    }

}
