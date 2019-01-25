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

import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchHighlight;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchProvider;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.elastic.model.ElasticSearchHit;
import de.tudarmstadt.ukp.inception.externalsearch.elastic.model.ElasticSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.elastic.traits.ElasticSearchProviderTraits;

public class ElasticSearchProvider
    implements ExternalSearchProvider
{

    private static final String highlight_start_tag = "<em>";

    private static final String highlight_end_tag = "</em>";

    private String remoteUrl = "http://xxxx";

    private String indexName = "common-crawl-en";
    
    private String searchPath = "_search";

    private String objectType = "texts";
    
    // Number of results retrieved from the server
    private int resultSize = 1000;

    @Override
    public boolean connect(String aUrl, String aUser, String aPassword)
    {
        // Always return true, no connection needed
        return true;
    }

    @Override
    public void disconnect()
    {
        // Nothing to do, no connection needed in this provider
    }

    @Override
    public boolean isConnected()
    {
        // Always return true, no connection needed
        return true;
    }

    @Override
    public List<ExternalSearchResult> executeQuery(Object aProperties,
            User aUser, String aQuery, String aSortOrder, String... sResultField)
    {
        List<ExternalSearchResult> results = new ArrayList<ExternalSearchResult>();

        RestTemplate restTemplate = new RestTemplate();

        ElasticSearchResult queryResult;

        ElasticSearchProviderTraits properties = (ElasticSearchProviderTraits) aProperties; 

        remoteUrl = properties.getRemoteUrl();
        indexName = properties.getIndexName();
        searchPath = properties.getSearchPath();
        
        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        
        // Set query
        String query = "{\"size\":%d,\"query\":{\"match\":"
                + "{\"doc.text\":\"%s\"}},\"highlight\":{\"fields\":{\"doc.text\":{}}}}";

        // Set body
        String body = String.format(query, resultSize, aQuery);

        // Set http entity
        HttpEntity<String> entity = new HttpEntity<String>(body, headers);

        // Prepare search URL
        String searchUrl = remoteUrl + "/" + indexName + "/" + searchPath;
        
        // Send post query
        queryResult = restTemplate.postForObject(searchUrl, entity,
                ElasticSearchResult.class);

        for (ElasticSearchHit hit : queryResult.getHits().getHits()) {
            ExternalSearchResult result = new ExternalSearchResult();

            // The title will be filled with the hit id, since there is no title in the
            // ElasticSearch hit
            result.setDocumentTitle(hit.get_id());
            result.setScore(hit.get_score());

            if (hit.get_source() != null) {
                if (hit.get_source().getDoc() != null) {
                    result.setText(hit.get_source().getDoc().getText());
                }
                if (hit.get_source().getMetadata() != null) {
                    // Set the metadata fields
                    result.setDocumentId(hit.get_source().getMetadata().getId());
                    result.setLanguage(hit.get_source().getMetadata().getLanguage());
                    result.setSource(hit.get_source().getMetadata().getSource());
                    result.setTimestamp(hit.get_source().getMetadata().getTimestamp());
                    result.setUri(hit.get_source().getMetadata().getUri());
                }
            }
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

                    ExternalSearchHighlight externalSearchHighlight = new ExternalSearchHighlight(
                        highlight);

                    // remove markers from the highlight
                    String highlight_clean = highlight.replace(highlight_start_tag, "")
                        .replace(highlight_end_tag, "");

                    // find the matching highlight offset in the original text
                    int highlight_start_index = originalText.indexOf(highlight_clean);

                    // find offset to all keywords in the highlight
                    // they are enclosed in <em> </em> tags in the highlight
                    while (highlight.contains(highlight_start_tag)) {
                        int start = highlight_start_index + highlight.indexOf(highlight_start_tag);
                        highlight = highlight.replaceFirst(highlight_start_tag, "");
                        int end = highlight_start_index + highlight.indexOf(highlight_end_tag);
                        highlight = highlight.replaceFirst(highlight_end_tag, "");
                        externalSearchHighlight.addOffset(start, end);
                    }

                    highlights.add(externalSearchHighlight);
                }
                result.setHighlights(highlights);
            }
            results.add(result);
        }

        return results;
    }

    @Override
    public ExternalSearchResult getDocumentById(Object aProperties, String aId)
    {
        ElasticSearchProviderTraits properties = (ElasticSearchProviderTraits) aProperties; 

        remoteUrl = properties.getRemoteUrl();
        indexName = properties.getIndexName();
        objectType = properties.getObjectType();

        RestTemplate restTemplate = new RestTemplate();

        String getUrl = remoteUrl + "/" + indexName + "/" + objectType + "/" + aId;

        // Send get query
        ElasticSearchHit document = restTemplate.getForObject(getUrl, ElasticSearchHit.class);

        ExternalSearchResult result = new ExternalSearchResult();
        
        result.setDocumentId(aId);
        result.setText(document.get_source().getDoc().getText());
        
        return result;
    }

}
