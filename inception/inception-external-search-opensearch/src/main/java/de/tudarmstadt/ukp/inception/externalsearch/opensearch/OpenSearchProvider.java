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
package de.tudarmstadt.ukp.inception.externalsearch.opensearch;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.functionscore.RandomScoreFunctionBuilder;
import org.opensearch.index.query.functionscore.ScoreFunctionBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.text.TextFormatSupport;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchHighlight;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchProvider;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;
import de.tudarmstadt.ukp.inception.externalsearch.opensearch.traits.OpenSearchProviderTraits;
import de.tudarmstadt.ukp.inception.security.client.auth.basic.BasicAuthenticationTraits;

public class OpenSearchProvider
    implements ExternalSearchProvider<OpenSearchProviderTraits>
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String OS_HIT_METADATA_KEY = "metadata";
    private static final String OS_HIT_DOC_KEY = "doc";
    private static final String METADATA_SOURCE_KEY = "source";
    private static final String METADATA_TITLE_KEY = "title";
    private static final String METADATA_URI_KEY = "uri";
    private static final String METADATA_LANGUAGE_KEY = "language";
    private static final String METADATA_TIMESTAMP_KEY = "timestamp";
    private static final String DOC_TEXT_KEY = "text";

    @Override
    public List<ExternalSearchResult> executeQuery(DocumentRepository aRepository,
            OpenSearchProviderTraits aTraits, String aQuery)
        throws IOException
    {
        List<ExternalSearchResult> results = new ArrayList<>();

        try (RestHighLevelClient client = makeClient(aTraits)) {
            HighlightBuilder highlightBuilder = new HighlightBuilder()
                    .field(new HighlightBuilder.Field(aTraits.getDefaultField())
                            .highlighterType("unified"));

            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                    .fetchSource(null, OS_HIT_DOC_KEY).highlighter(highlightBuilder)
                    .size(aTraits.getResultSize());

            QueryBuilder qb = QueryBuilders.simpleQueryStringQuery(aQuery)
                    .field(aTraits.getDefaultField());

            if (aTraits.isRandomOrder()) {
                RandomScoreFunctionBuilder randomFunc = ScoreFunctionBuilders.randomFunction();
                randomFunc.seed(aTraits.getSeed());
                searchSourceBuilder.query(QueryBuilders.functionScoreQuery(
                        QueryBuilders.constantScoreQuery(qb).boost(1.0f), randomFunc));
            }
            else {
                searchSourceBuilder.query(qb);
            }

            SearchRequest searchRequest = new SearchRequest(aTraits.getIndexName())
                    .source(searchSourceBuilder);
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

            for (SearchHit hit : response.getHits().getHits()) {
                if (hit.getSourceAsMap() == null
                        || hit.getSourceAsMap().get(OS_HIT_METADATA_KEY) == null) {
                    log.warn("Result has no document metadata: " + hit);
                    continue;
                }

                ExternalSearchResult result = new ExternalSearchResult(aRepository,
                        aTraits.getIndexName(), hit.getId());

                // If the order is random, then the score doesn't reflect the quality, so we do not
                // forward it to the user
                if (!aTraits.isRandomOrder()) {
                    result.setScore((double) hit.getScore());
                }

                fillResultWithMetadata(result, hit.getSourceAsMap());

                if (hit.getHighlightFields().size() != 0) {

                    // There are highlights, set them in the result
                    List<ExternalSearchHighlight> highlights = new ArrayList<>();
                    if (hit.getHighlightFields().get(aTraits.getDefaultField()) != null) {
                        for (var highlight : hit.getHighlightFields().get(aTraits.getDefaultField())
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

    private void fillResultWithMetadata(ExternalSearchResult result, Map<String, Object> aHitMap)
    {
        @SuppressWarnings("unchecked")
        var metadata = (Map<String, String>) aHitMap.get(OS_HIT_METADATA_KEY);

        // The title will be filled with the hit id, since there is no title in the OpenSearch hit
        if (isNotBlank(metadata.get(METADATA_TITLE_KEY))) {
            result.setDocumentTitle(metadata.get(METADATA_TITLE_KEY));
        }
        else {
            result.setDocumentTitle((String) aHitMap.get("id"));
        }

        // Set the metadata fields
        result.setOriginalSource(metadata.get(METADATA_SOURCE_KEY));
        result.setOriginalUri(metadata.get(METADATA_URI_KEY));
        result.setLanguage(metadata.get(METADATA_LANGUAGE_KEY));
        result.setTimestamp(metadata.get(METADATA_TIMESTAMP_KEY));
    }

    @Override
    public ExternalSearchResult getDocumentResult(DocumentRepository aRepository,
            OpenSearchProviderTraits aTraits, String aCollectionId, String aDocumentId)
        throws IOException
    {
        if (!aCollectionId.equals(aTraits.getIndexName())) {
            throw new IllegalArgumentException(
                    "Requested collection name does not match connection collection name");
        }

        GetRequest getRequest = new GetRequest(aTraits.getIndexName(), aDocumentId);

        try (RestHighLevelClient client = makeClient(aTraits)) {
            ExternalSearchResult result = new ExternalSearchResult(aRepository, aCollectionId,
                    aDocumentId);

            // Send get query
            fillResultWithMetadata(result,
                    client.get(getRequest, RequestOptions.DEFAULT).getSourceAsMap());

            return result;
        }
    }

    @Override
    public String getDocumentText(DocumentRepository aRepository, OpenSearchProviderTraits aTraits,
            String aCollectionId, String aDocumentId)
        throws IOException
    {
        if (!aCollectionId.equals(aTraits.getIndexName())) {
            throw new IllegalArgumentException(
                    "Requested collection name does not match connection collection name");
        }

        GetRequest getRequest = new GetRequest(aTraits.getIndexName(), aDocumentId);

        try (RestHighLevelClient client = makeClient(aTraits)) {
            // Send get query
            Map<String, Object> result = client.get(getRequest, RequestOptions.DEFAULT)
                    .getSourceAsMap();
            @SuppressWarnings("unchecked")
            var document = (Map<String, String>) result.get(OS_HIT_DOC_KEY);
            return (document.get(DOC_TEXT_KEY));
        }
    }

    @Override
    public InputStream getDocumentAsStream(DocumentRepository aRepository,
            OpenSearchProviderTraits aTraits, String aCollectionId, String aDocumentId)
        throws IOException
    {
        return IOUtils.toInputStream(
                getDocumentText(aRepository, aTraits, aCollectionId, aDocumentId), UTF_8);
    }

    @Override
    public String getDocumentFormat(DocumentRepository aRepository,
            OpenSearchProviderTraits aTraits, String aCollectionId, String aDocumentId)
    {
        return TextFormatSupport.ID;
    }

    private RestHighLevelClient makeClient(OpenSearchProviderTraits aTraits)
        throws MalformedURLException
    {
        URL hostUrl = new URL(aTraits.getRemoteUrl());
        RestClientBuilder builder = RestClient
                .builder(new HttpHost(hostUrl.getHost(), hostUrl.getPort(), hostUrl.getProtocol()));

        builder.setHttpClientConfigCallback(httpAsyncClientBuilder -> {
            maybeAuthenticate(aTraits, httpAsyncClientBuilder);
            maybeDisableSslVerification(aTraits, httpAsyncClientBuilder);
            return httpAsyncClientBuilder;
        });

        return new RestHighLevelClient(builder);
    }

    private void maybeAuthenticate(OpenSearchProviderTraits aTraits,
            HttpAsyncClientBuilder aClientBuilder)
    {
        if (aTraits.getAuthentication() == null) {
            return;
        }

        BasicAuthenticationTraits authTraits = (BasicAuthenticationTraits) aTraits
                .getAuthentication();

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(
                authTraits.getUsername(), authTraits.getPassword()));

        aClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
    }

    private void maybeDisableSslVerification(OpenSearchProviderTraits aTraits,
            HttpAsyncClientBuilder aClientBuilder)
    {
        if (aTraits.isSslVerification()) {
            return;
        }

        SSLContext sslContext = null;
        try {
            SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
            sslContextBuilder.loadTrustMaterial(null,
                    (X509Certificate[] chain, String authType) -> true);
            sslContext = sslContextBuilder.build();
        }
        catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            log.error("Cannot disable SSL verification", e);
        }

        if (sslContext == null) {
            return;
        }

        aClientBuilder.setSSLContext(sslContext);
        aClientBuilder.setSSLHostnameVerifier((s, sslSession) -> true);
    }
}
