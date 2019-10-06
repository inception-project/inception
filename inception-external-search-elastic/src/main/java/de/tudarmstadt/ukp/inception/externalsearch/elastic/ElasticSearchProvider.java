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
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.RandomScoreFunctionBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.text.TextFormatSupport;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchHighlight;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchProvider;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.elastic.traits.ElasticSearchProviderTraits;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;

public class ElasticSearchProvider
    implements ExternalSearchProvider<ElasticSearchProviderTraits>
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private static final String ELASTIC_HIT_METADATA_KEY = "metadata";
    private static final String ELASTIC_HIT_DOC_KEY = "doc";
    private static final String METADATA_SOURCE_KEY = "source";
    private static final String METADATA_URI_KEY = "uri";
    private static final String METADATA_LANGUAGE_KEY = "language";
    private static final String METADATA_TIMESTAMP_KEY = "timestamp";
    private static final String DOC_TEXT_KEY = "text";
    
    @Override
    public List<ExternalSearchResult> executeQuery(DocumentRepository aRepository,
            ElasticSearchProviderTraits aTraits, String aQuery)
        throws IOException
    {
        List<ExternalSearchResult> results = new ArrayList<>();

        String indexName = aTraits.getIndexName();
        String hostUrl = aTraits.getRemoteUrl().replaceFirst("https?://", "")
                .replaceFirst("www.", "")
                .split(":")[0];
        
        try (RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(new HttpHost(hostUrl, 9200, "http")))) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            HighlightBuilder.Field highlightField =
                    new HighlightBuilder.Field(aTraits.getDefaultField());
            highlightField.highlighterType("unified");
            highlightBuilder.field(highlightField);
    
            SearchRequest searchRequest = new SearchRequest(indexName);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                    .fetchSource(null, ELASTIC_HIT_DOC_KEY);
            RandomScoreFunctionBuilder randomFunc = ScoreFunctionBuilders.randomFunction();
            randomFunc.seed(aTraits.getSeed());
            if (aTraits.isRandomOrder()) {
                searchSourceBuilder.query(QueryBuilders.functionScoreQuery(
                        QueryBuilders.constantScoreQuery(
                            QueryBuilders.termQuery(aTraits.getDefaultField(), aQuery)
                        ).boost(1.0f),
                        randomFunc));
            }
            else {
                searchSourceBuilder.query(QueryBuilders.termQuery(
                        aTraits.getDefaultField(), aQuery));
            }
            searchSourceBuilder.highlighter(highlightBuilder);
            searchSourceBuilder.size(aTraits.getResultSize());
            searchRequest.source(searchSourceBuilder);
            
            SearchResponse response = client.search(searchRequest);
    
            for (SearchHit hit: response.getHits().getHits()) {
                if (hit.getSourceAsMap() == null ||
                        hit.getSourceAsMap().get(ELASTIC_HIT_METADATA_KEY) == null) {
                    log.warn("Result has no document metadata: " + hit);
                    continue;
                }
                
                ExternalSearchResult result = new ExternalSearchResult(aRepository, indexName,
                        hit.getId());
    
                // The title will be filled with the hit id, since there is no title in the
                // ElasticSearch hit
                result.setDocumentTitle(hit.getId());
                
                // If the order is random, then the score doesn't reflect the quality, so we do not
                // forward it to the user
                if (!aTraits.isRandomOrder()) {
                    result.setScore((double) hit.getScore());
                }
    
                Map<String, Object> hitSource = hit.getSourceAsMap();
                Map<String, String> metadata = (Map) hitSource.get(ELASTIC_HIT_METADATA_KEY);
    
                // Set the metadata fields
                result.setOriginalSource(metadata.get(METADATA_SOURCE_KEY));
                result.setOriginalUri(metadata.get(METADATA_URI_KEY));
                result.setLanguage(metadata.get(METADATA_LANGUAGE_KEY));
                result.setTimestamp(metadata.get(METADATA_TIMESTAMP_KEY));
    
                if (hit.getHighlightFields().size() != 0) {
    
                    // There are highlights, set them in the result
                    List<ExternalSearchHighlight> highlights = new ArrayList<>();
                    if (hit.getHighlightFields().get(aTraits.getDefaultField()) != null) {
                        for (Text highlight : hit.getHighlightFields()
                                .get(aTraits.getDefaultField())
                                .getFragments()) {
                            highlights.add(new ExternalSearchHighlight(highlight.toString()));
                        }
                    }
                    result.setHighlights(highlights);
                }
                results.add(result);
            }
        }

        return results;
    }

    @Override
    public String getDocumentText(DocumentRepository aRepository,
            ElasticSearchProviderTraits aTraits, String aCollectionId, String aDocumentId)
            throws IOException
    {
        if (!aCollectionId.equals(aTraits.getIndexName())) {
            throw new IllegalArgumentException(
                    "Requested collection name does not match connection collection name");
        }
    
        GetRequest getRequest = new GetRequest(
                aTraits.getIndexName(), aTraits.getObjectType(), aDocumentId
        );
        
        String hostUrl = aTraits.getRemoteUrl().replaceFirst("https?://", "")
                .replaceFirst("www.", "")
                .split(":")[0];
        
        try (RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(new HttpHost(hostUrl, 9200, "http")))) {
            // Send get query
            Map<String, String> document =
                    (Map) client.get(getRequest).getSourceAsMap().get(ELASTIC_HIT_DOC_KEY);
            return (document.get(DOC_TEXT_KEY));
        }
    }

    @Override
    public InputStream getDocumentAsStream(DocumentRepository aRepository,
            ElasticSearchProviderTraits aTraits, String aCollectionId, String aDocumentId)
            throws IOException
    {
        return IOUtils.toInputStream(
                getDocumentText(aRepository, aTraits, aCollectionId, aDocumentId), UTF_8);
    }
    
    @Override
    public String getDocumentFormat(DocumentRepository aRepository, Object aTraits,
            String aCollectionId, String aDocumentId)
    {
        return TextFormatSupport.ID;
    }
}
