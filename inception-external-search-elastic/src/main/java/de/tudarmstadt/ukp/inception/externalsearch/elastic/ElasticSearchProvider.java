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

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchProvider;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.elastic.configuration.ElasticSearchProviderProperties;

public class ElasticSearchProvider
    implements ExternalSearchProvider
{

    // private String serverUrl = "http://xxx";
    private String remoteUrl = "http://bart:9200";

    //    private String indexName = "index";
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

        ElasticSearchProviderProperties properties = (ElasticSearchProviderProperties) aProperties; 

        remoteUrl = properties.getRemoteUrl();
        indexName = properties.getIndexName();
        searchPath = properties.getSearchPath();
        
        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Set body
        String body = String.format("{\"size\":\"%d\",\"_source\":\"false\",\"query\":{\"match\" : "
                + "{\"doc.text\":\"%s\"}}}", resultSize, aQuery);

        // Set http entity
        HttpEntity<String> entity = new HttpEntity<String>(body, headers);

        // Prepare search URL
        String searchUrl = remoteUrl + "/" + indexName + "/" + searchPath;
        
        // Send post query
        queryResult = restTemplate.postForObject(searchUrl, entity,
                ElasticSearchResult.class);

        for (ElasticSearchHit hit : queryResult.getHits().getHits()) {
            ExternalSearchResult result = new ExternalSearchResult();

            result.setDocumentId(hit.get_id());
            if (hit.get_source() != null) {
                if (hit.get_source().getDoc() != null) {
                    result.setText(hit.get_source().getDoc().getText());
                }
            }
            results.add(result);
        }

        return results;
    }

    public ExternalSearchResult getDocumentById(Object aProperties, String aId)
    {
        ElasticSearchProviderProperties properties = (ElasticSearchProviderProperties) aProperties; 

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
