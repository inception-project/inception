/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.inception.externalsearch.pubannotation;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchHighlight;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchProvider;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.format.PubAnnotationSectionsFormatSupport;
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model.PubAnnotationDocumentHandle;
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model.PubAnnotationDocumentSection;
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.traits.PubAnnotationProviderTraits;

public class PubAnnotationProvider
    implements ExternalSearchProvider<PubAnnotationProviderTraits>
{
    private static final Logger LOG = LoggerFactory.getLogger(PubAnnotationProvider.class);

    public List<PubAnnotationDocumentHandle> query(PubAnnotationProviderTraits aTraits,
            String aQuery)
    {
        Map<String, String> variables = new HashMap<>();
        variables.put("keywords", aQuery);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<List<PubAnnotationDocumentHandle>> response = restTemplate.exchange(
                aTraits.getUrl() + "/docs.json?keywords={keywords}", HttpMethod.GET, null,
                new DocumentHandleList(), variables);

        return response.getBody();
    }
    
    @Override
    public List<ExternalSearchResult> executeQuery(DocumentRepository aDocumentRepository,
            PubAnnotationProviderTraits aTraits, String aQuery)
    {
        List<PubAnnotationDocumentHandle> response = query(aTraits, aQuery);
        
        List<ExternalSearchResult> results = new ArrayList<>();
        for (PubAnnotationDocumentHandle handle : response) {
            ExternalSearchResult result = new ExternalSearchResult(aDocumentRepository,
                    handle.getSourceDb(), handle.getSourceId() + ".json");
            result.setOriginalSource(handle.getSourceDb());
            result.setDocumentTitle(handle.getUrl());
            result.setHighlights(handle.getHighlights().stream()
                    .map(ExternalSearchHighlight::new)
                    .collect(Collectors.toList()));
            results.add(result);
        }
        
        return results;
    }
    
    @Override
    public ExternalSearchResult getDocumentResult(DocumentRepository aRepository,
            PubAnnotationProviderTraits aTraits, String aCollectionId, String aDocumentId)
        throws IOException
    {
        ExternalSearchResult result = new ExternalSearchResult(aRepository, aCollectionId,
                aDocumentId);
        return result;
    }

    @Override
    public String getDocumentText(DocumentRepository aDocumentRepository,
            PubAnnotationProviderTraits aTraits, String aCollectionId, String aDocumentId)
    {
        return getSections(aDocumentRepository, aTraits, aCollectionId, aDocumentId).stream()
                .map(PubAnnotationDocumentSection::getText)
                .collect(Collectors.joining("\n\n"));
    }

    @Override
    public InputStream getDocumentAsStream(DocumentRepository aDocumentRepository,
            PubAnnotationProviderTraits aTraits, String aCollectionId, String aDocumentId)
        throws IOException
    {
        String json = JSONUtil.toJsonString(
                getSections(aDocumentRepository, aTraits, aCollectionId, aDocumentId));

        return IOUtils.toInputStream(json, UTF_8);
    }

    private List<PubAnnotationDocumentSection> getSections(DocumentRepository aDocumentRepository,
            PubAnnotationProviderTraits aTraits, String aCollectionId, String aDocumentId)
    {
        Map<String, String> variables = new HashMap<>();
        variables.put("collectionId", aCollectionId);
        variables.put("documentId", aDocumentId);
 
        RestTemplate restTemplate = new RestTemplate();

        try {
            // If the document has multiple sections, a list is returned...
            ResponseEntity<List<PubAnnotationDocumentSection>> response = restTemplate.exchange(
                    aTraits.getUrl() + "/docs/sourcedb/{collectionId}/sourceid/{documentId}",
                    HttpMethod.GET, null, PubAnnotationDocumentSection.SPRING_LIST_TYPE_REF,
                    variables);
           
            return response.getBody();
        }
        catch (RestClientException e) {
            // If the document has as single section, an object is returned...
            PubAnnotationDocumentSection section = restTemplate.getForObject(
                    aTraits.getUrl() + "/docs/sourcedb/{collectionId}/sourceid/{documentId}",
                    PubAnnotationDocumentSection.class, variables);
            
            return asList(section);
        }
    }
    
    @Override
    public String getDocumentFormat(DocumentRepository aRepository,
            PubAnnotationProviderTraits aTraits, String aCollectionId, String aDocumentId)
        throws IOException
    {
        return PubAnnotationSectionsFormatSupport.ID;
    }

    private static class DocumentHandleList
        extends ParameterizedTypeReference<List<PubAnnotationDocumentHandle>>
    {
        // Just a type reference
    }
}
