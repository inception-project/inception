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
package de.tudarmstadt.ukp.inception.externalsearch.elastic;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import de.tudarmstadt.ukp.clarin.webanno.text.TextFormatSupport;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchHighlight;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchProvider;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.HighlightUtils;
import de.tudarmstadt.ukp.inception.externalsearch.elastic.model.ElasticSearchHit;
import de.tudarmstadt.ukp.inception.externalsearch.elastic.model.ElasticSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.elastic.traits.ElasticSearchProviderTraits;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;

public class ElasticSearchProvider
    implements ExternalSearchProvider<ElasticSearchProviderTraits>
{
    private static final String HIGHLIGHT_START_TAG = "<em>";

    private static final String HIGHLIGHT_END_TAG = "</em>";

    private static final Logger LOG = LoggerFactory.getLogger(ElasticSearchProvider.class);

    @Override
    public List<ExternalSearchResult> executeQuery(DocumentRepository aRepository,
            ElasticSearchProviderTraits aTraits, String aQuery)
    {
        List<ExternalSearchResult> results = new ArrayList<ExternalSearchResult>();

        RestTemplate restTemplate = new RestTemplate();

        ElasticSearchResult queryResult;

        String remoteUrl = aTraits.getRemoteUrl();
        String indexName = aTraits.getIndexName();
        String searchPath = aTraits.getSearchPath();
        
        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        
        // Set query
        String query = "{\"size\":%d,\"query\":{\"match\":"
                + "{\"doc.text\":\"%s\"}},\"highlight\":{\"fields\":{\"doc.text\":{}}}}";

        // Set body
        String body = String.format(query, aTraits.getResultSize(), aQuery);

        // Set http entity
        HttpEntity<String> entity = new HttpEntity<String>(body, headers);

        // Prepare search URL
        String searchUrl = remoteUrl + "/" + indexName + "/" + searchPath;
        
        // Send post query
        queryResult = restTemplate.postForObject(searchUrl, entity,
                ElasticSearchResult.class);

        for (ElasticSearchHit hit : queryResult.getHits().getHits()) {
            if (hit.get_source() == null || hit.get_source().getMetadata() == null) {
                LOG.warn("Result has no document metadata: " + hit);
                continue;
            }
            
            ExternalSearchResult result = new ExternalSearchResult(aRepository, indexName,
                    hit.get_id());

            // The title will be filled with the hit id, since there is no title in the
            // ElasticSearch hit
            result.setDocumentTitle(hit.get_id());
            result.setScore(hit.get_score());

            // Set the metadata fields
            result.setOriginalSource(hit.get_source().getMetadata().getSource());
            result.setOriginalUri(hit.get_source().getMetadata().getUri());
            result.setLanguage(hit.get_source().getMetadata().getLanguage());
            result.setTimestamp(hit.get_source().getMetadata().getTimestamp());

            if (hit.getHighlight() != null) {

                // Highlights from elastic search are small sections of the document text
                // with the keywords surrounded by the <em> tags. There are no offset information
                // for the highlights or the keywords in the document text. There is a feature
                // request for it (https://github.com/elastic/elasticsearch/issues/5736).
                // Until this feature is implemented, we currently try to find the keywords offsets
                // by finding the matching highlight in the document text, then the keywords offset
                // within highlight using <em> tags.
                String originalText = hit.get_source().getDoc().getText();

                // There are highlights, set them in the result
                List<ExternalSearchHighlight> highlights = new ArrayList<>();
                for (String highlight : hit.getHighlight().getDoctext()) {
                    Optional<ExternalSearchHighlight> exHighlight = HighlightUtils
                            .parseHighlight(highlight, originalText);
                    
                    exHighlight.ifPresent(highlights::add);
                }
                result.setHighlights(highlights);
            }
            results.add(result);
        }

        return results;
    }

    @Override
    public String getDocumentText(DocumentRepository aRepository,
            ElasticSearchProviderTraits aTraits, String aCollectionId, String aDocumentId)
    {
        if (!aCollectionId.equals(aTraits.getIndexName())) {
            throw new IllegalArgumentException(
                    "Requested collection name does not match connection collection name");
        }
        
        Map<String, String> variables = new HashMap<>();
        variables.put("index", aTraits.getIndexName());
        variables.put("object", aTraits.getObjectType());
        variables.put("documentId", aDocumentId);
        
        // Send get query
        RestTemplate restTemplate = new RestTemplate();
        ElasticSearchHit document = restTemplate.getForObject(
                aTraits.getRemoteUrl() + "/{index}/{object}/{documentId}", ElasticSearchHit.class,
                variables);

        return document.get_source().getDoc().getText();
    }

    @Override
    public InputStream getDocumentAsStream(DocumentRepository aRepository,
            ElasticSearchProviderTraits aTraits, String aCollectionId, String aDocumentId)
    {
        if (!aCollectionId.equals(aTraits.getIndexName())) {
            throw new IllegalArgumentException(
                    "Requested collection name does not match connection collection name");
        }
        
        Map<String, String> variables = new HashMap<>();
        variables.put("index", aTraits.getIndexName());
        variables.put("object", aTraits.getObjectType());
        variables.put("documentId", aDocumentId);
        
        // Send get query
        RestTemplate restTemplate = new RestTemplate();
        ElasticSearchHit document = restTemplate.getForObject(
                aTraits.getRemoteUrl() + "/{index}/{object}/{documentId}", ElasticSearchHit.class,
                variables);

        return IOUtils.toInputStream(document.get_source().getDoc().getText(), UTF_8);
    }
    
    @Override
    public String getDocumentFormat(DocumentRepository aRepository, Object aTraits,
            String aCollectionId, String aDocumentId)
        throws IOException
    {
        return TextFormatSupport.ID;
    }
}
