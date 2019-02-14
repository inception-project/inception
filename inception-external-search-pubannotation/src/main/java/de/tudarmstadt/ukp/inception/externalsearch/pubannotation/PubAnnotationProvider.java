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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchProvider;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model.PubAnnotationDocumentHandle;
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model.PubAnnotationDocumentSection;
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.traits.PubAnnotationProviderTraits;

public class PubAnnotationProvider
    implements ExternalSearchProvider<PubAnnotationProviderTraits>
{
    private static final Logger LOG = LoggerFactory.getLogger(PubAnnotationProvider.class);

    @Override
    public List<ExternalSearchResult> executeQuery(DocumentRepository aDocumentRepository,
            PubAnnotationProviderTraits aTraits, String aQuery)
    {
        Map<String, String> params = Collections.singletonMap("keywords", aQuery);
        
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<List<PubAnnotationDocumentHandle>> response = restTemplate.exchange(
                aTraits.getUrl() + "/docs.json?keywords={keywords}", HttpMethod.GET, null,
                new DocumentHandleList(), params);
        
        List<ExternalSearchResult> results = new ArrayList<>();
        for (PubAnnotationDocumentHandle handle : response.getBody()) {
            ExternalSearchResult result = new ExternalSearchResult(aDocumentRepository,
                    handle.getSourceDb(), handle.getSourceId());
            result.setOriginalSource(handle.getSourceDb());
            result.setDocumentTitle(handle.getUrl());
            results.add(result);
        }
        
        return results;
    }

    @Override
    public String getDocumentById(DocumentRepository aDocumentRepository,
            PubAnnotationProviderTraits aTraits, String aSource, String aId)
    {
        RestTemplate restTemplate = new RestTemplate();

        Map<String, String> variables = new HashMap<>();
        variables.put("source", aSource);
        variables.put("documentId", aId);
        
        // HACK: https://github.com/pubannotation/pubannotation/issues/4
        String text;
        try {
            // If the document has multiple sections, a list is returned...
            ResponseEntity<List<PubAnnotationDocumentSection>> response = restTemplate.exchange(
                    aTraits.getUrl() + "/docs/sourcedb/{source}/sourceid/{documentId}.json",
                    HttpMethod.GET, null, new DocumentSectionList(), variables);
            
            text = response.getBody().stream()
                    .map(PubAnnotationDocumentSection::getText)
                    .collect(Collectors.joining("\n\n"));
        }
        catch (RestClientException e) {
            // If the document has as single section, an object is returned...
            PubAnnotationDocumentSection section = restTemplate.getForObject(
                    aTraits.getUrl() + "/docs/sourcedb/{source}/sourceid/{documentId}.json",
                    PubAnnotationDocumentSection.class, variables);
            
            text = section.getText();
        }

        return text;
    }

    private static class DocumentSectionList
        extends ParameterizedTypeReference<List<PubAnnotationDocumentSection>>
    {
        // Just a type reference
    }

    private static class DocumentHandleList
        extends ParameterizedTypeReference<List<PubAnnotationDocumentHandle>>
    {
        // Just a type reference
    }
}
