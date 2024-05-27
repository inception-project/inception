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
package de.tudarmstadt.ukp.inception.externalsearch.pubannotation;

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
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.config.PubAnnotationDocumentRepositoryAutoConfiguration;
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.traits.PubAnnotationProviderTraits;
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.traits.PubAnnotationProviderTraitsEditor;
import de.tudarmstadt.ukp.inception.externalsearch.pubmed.entrez.EntrezClient;

/**
 * Support for PubAnnotation.
 * <p>
 * This class is exposed as a Spring Component via
 * {@link PubAnnotationDocumentRepositoryAutoConfiguration#pubAnnotationProviderFactory}.
 * </p>
 */
@Order(100)
public class PubAnnotationProviderFactory
    implements BeanNameAware, ExternalSearchProviderFactory<PubAnnotationProviderTraits>
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final EntrezClient entrezClient;

    private String beanName;

    public PubAnnotationProviderFactory(EntrezClient aEntrezClient)
    {
        entrezClient = aEntrezClient;
    }

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
        return "PubAnnotation";
    }

    @Override
    public ExternalSearchProvider<PubAnnotationProviderTraits> getNewExternalSearchProvider()
    {
        return new PubAnnotationProvider(entrezClient);
    }

    @Override
    public Panel createTraitsEditor(String aId, IModel<DocumentRepository> aDocumentRepository)
    {
        return new PubAnnotationProviderTraitsEditor(aId, aDocumentRepository);
    }

    @Override
    public PubAnnotationProviderTraits readTraits(DocumentRepository aDocumentRepository)
    {
        PubAnnotationProviderTraits traits = null;
        try {
            traits = fromJsonString(PubAnnotationProviderTraits.class,
                    aDocumentRepository.getProperties());
        }
        catch (IOException e) {
            log.error("Error while reading traits", e);
        }

        if (traits == null) {
            traits = new PubAnnotationProviderTraits();
        }

        return traits;
    }

    @Override
    public void writeTraits(DocumentRepository aDocumentRepository,
            PubAnnotationProviderTraits aProperties)
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
