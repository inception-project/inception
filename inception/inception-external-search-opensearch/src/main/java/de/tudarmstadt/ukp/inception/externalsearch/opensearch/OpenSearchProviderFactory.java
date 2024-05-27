/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
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
package de.tudarmstadt.ukp.inception.externalsearch.opensearch;

import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.fromJsonString;
import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.toJsonString;

import java.io.IOException;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchProvider;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchProviderFactory;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;
import de.tudarmstadt.ukp.inception.externalsearch.opensearch.config.OpenSearchDocumentRepositoryAutoConfiguration;
import de.tudarmstadt.ukp.inception.externalsearch.opensearch.traits.OpenSearchProviderTraits;
import de.tudarmstadt.ukp.inception.externalsearch.opensearch.traits.OpenSearchProviderTraitsEditor;

/**
 * Support for OpenSearch-based document repositories.
 * <p>
 * This class is exposed as a Spring Component via
 * {@link OpenSearchDocumentRepositoryAutoConfiguration#openSearchProviderFactory}.
 * </p>
 */
@Order(100)
public class OpenSearchProviderFactory
    implements BeanNameAware, ExternalSearchProviderFactory<OpenSearchProviderTraits>
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
        return "OpenSearch";
    }

    @Override
    public ExternalSearchProvider<OpenSearchProviderTraits> getNewExternalSearchProvider()
    {
        return new OpenSearchProvider();
    }

    @Override
    public Panel createTraitsEditor(String aId, IModel<DocumentRepository> aDocumentRepository)
    {
        return new OpenSearchProviderTraitsEditor(aId, aDocumentRepository);
    }

    @Override
    public OpenSearchProviderTraits readTraits(DocumentRepository aDocumentRepository)
    {
        OpenSearchProviderTraits traits = null;
        try {
            traits = fromJsonString(OpenSearchProviderTraits.class,
                    aDocumentRepository.getProperties());
        }
        catch (IOException e) {
            log.error("Error while reading traits", e);
        }

        if (traits == null) {
            traits = new OpenSearchProviderTraits();
        }

        return traits;
    }

    @Override
    public void writeTraits(DocumentRepository aDocumentRepository,
            OpenSearchProviderTraits aProperties)
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
