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
package de.tudarmstadt.ukp.inception.externalsearch.pubmed;

import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.fromJsonString;
import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.toJsonString;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchProvider;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchProviderFactory;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;
import de.tudarmstadt.ukp.inception.externalsearch.pubmed.config.PubMedDocumentRepositoryAutoConfiguration;
import de.tudarmstadt.ukp.inception.externalsearch.pubmed.entrez.EntrezClient;
import de.tudarmstadt.ukp.inception.externalsearch.pubmed.pmcoa.PmcOaClient;
import de.tudarmstadt.ukp.inception.externalsearch.pubmed.traits.PubMedProviderTraits;
import de.tudarmstadt.ukp.inception.externalsearch.pubmed.traits.PubMedProviderTraitsEditor;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

/**
 * Support for PubAnnotation.
 * <p>
 * This class is exposed as a Spring Component via
 * {@link PubMedDocumentRepositoryAutoConfiguration#pubMedCentralProviderFactory}.
 * </p>
 */
@Order(100)
public class PubMedCentralProviderFactory
    implements BeanNameAware, ExternalSearchProviderFactory<PubMedProviderTraits>
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final EntrezClient entrezClient;
    private final PmcOaClient pmcOaClient;
    private final AnnotationSchemaService schemaService;

    private String beanName;

    public PubMedCentralProviderFactory(EntrezClient aEntrezClient, PmcOaClient aPmcOaClient,
            AnnotationSchemaService aSchemaService)
    {
        entrezClient = aEntrezClient;
        pmcOaClient = aPmcOaClient;
        schemaService = aSchemaService;
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
        return "PubMed Central Open Access (experimental)";
    }

    @Override
    public ExternalSearchProvider<PubMedProviderTraits> getNewExternalSearchProvider()
    {
        return new PubMedCentralProvider(entrezClient, pmcOaClient, schemaService);
    }

    @Override
    public Panel createTraitsEditor(String aId, IModel<DocumentRepository> aDocumentRepository)
    {
        return new PubMedProviderTraitsEditor(aId, aDocumentRepository);
    }

    @Override
    public PubMedProviderTraits readTraits(DocumentRepository aDocumentRepository)
    {
        PubMedProviderTraits traits = null;
        try {
            traits = fromJsonString(PubMedProviderTraits.class,
                    aDocumentRepository.getProperties());
        }
        catch (IOException e) {
            LOG.error("Error while reading traits", e);
        }

        if (traits == null) {
            traits = new PubMedProviderTraits();
        }

        return traits;
    }

    @Override
    public void writeTraits(DocumentRepository aDocumentRepository,
            PubMedProviderTraits aProperties)
    {
        try {
            String json = toJsonString(aProperties);
            aDocumentRepository.setProperties(json);
        }
        catch (IOException e) {
            LOG.error("Error while writing traits", e);
        }
    }
}
