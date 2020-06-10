/*
 * Copyright 2020
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
package de.tudarmstadt.ukp.inception.externalsearch.solr;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BaseHttpSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;

import de.tudarmstadt.ukp.clarin.webanno.text.TextFormatSupport;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchHighlight;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchProvider;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;
import de.tudarmstadt.ukp.inception.externalsearch.solr.traits.SolrSearchProviderTraits;


public class SolrSearchProvider
        implements ExternalSearchProvider<SolrSearchProviderTraits>
{

    private static final String DOC_ID_KEY = "id";
    private static final String DOC_NAME_KEY = "name";
    private static final String DOC_TITLE_KEY = "title";

    private static final String DOC_URI_KEY = "uri";
    private static final String DOC_LANGUAGE_KEY = "language";
    private static final String DOC_TIMESTAMP_KEY = "timestamp";

    /**
     * Search documents in a Solr repository
     * @param aRepository Solr repository
     * @param aTraits Param for client connection
     * @param aQuery The query that we retrieve from the search bar
     * @return A list of results that Inception can read and display
     * @throws SolrException Connection exception and request exception
     */

    public List<ExternalSearchResult> executeQuery(DocumentRepository aRepository,
                                                   SolrSearchProviderTraits aTraits, String aQuery)
            throws IOException
    {
        List<ExternalSearchResult> results = new ArrayList<>();
        //build client

        HttpSolrClient solrClient = makeClient(aTraits);
        try {

            SolrQuery query = new SolrQuery();
            solrClient.query(aTraits.getIndexName(), query);
            try {
                query = new SolrQuery();
                query.setParam("qt", aTraits.getSearchPath());
                solrClient.query(aTraits.getIndexName(), query);

                if (aQuery.isEmpty() | aQuery.equals("*:*"))
                    aQuery = "*:*";
                else {
                    aQuery = aTraits.getDefaultField() + ":" + aQuery;
                }

                query.setQuery(aQuery);

                //RANDOM ORDER
                if (aTraits.isRandomOrder()) {
                    query.setSort("random_" + aTraits.getSeed(), SolrQuery.ORDER.asc);
                }

                query.addField("*");
                query.addField("score");

                query.setHighlight(true);
                query.addHighlightField(aTraits.getDefaultField());

                query.setRows(aTraits.getResultSize());
                query.setParam("collection", aTraits.getIndexName());


                //RESPONSE
                QueryResponse response = new QueryResponse();
                try {
                    response = solrClient.query(query);
                    SolrDocumentList documents = response.getResults();

                    for (SolrDocument document : documents) {
                        ExternalSearchResult result = new ExternalSearchResult(aRepository,
                                aTraits.getIndexName(), (String) document.getFirstValue("id"));

                        if (!aTraits.isRandomOrder()) {
                            double d = (float) document.getFirstValue("score");
                            result.setScore(d);
                        }

                        fillResultWithMetadata(result, document, aTraits);

                        if (response.getHighlighting().size() != 0) {
                            List<ExternalSearchHighlight> highlights = new ArrayList<>();
                            Map<String, Map<String, List<String>>> idHighlight =
                                    response.getHighlighting();

                            if (idHighlight.get(document.getFirstValue("id"))
                                    .get(aTraits.getDefaultField()) != null) {
                                for (String chaine : idHighlight.get(document.getFirstValue("id"))
                                        .get(aTraits.getDefaultField())) {
                                    highlights.add(new ExternalSearchHighlight(chaine));
                                }
                            }

                            result.setHighlights(highlights);
                        }
                        results.add(result);
                    }
                    System.out.println("Found " + documents.getNumFound() + " documents");
                }
                catch (BaseHttpSolrClient.RemoteSolrException e) {
                    throw new IOException(e.getMessage());
                }
            } catch (BaseHttpSolrClient.RemoteSolrException e) {
                throw new IOException("Error : The search path does not exist");
            }

        } catch (BaseHttpSolrClient.RemoteSolrException e) {
            throw new IOException("Error : The name of the collection does not exist");
        }
        catch (SolrServerException | HttpHostConnectException e)
        {
            throw new IOException(e.getMessage());
        }

        return results;
    }
    /**
     * Convert data from SolrDocument to ExternalSearchResult
     * @param result result has just the id complete before the methode
     * @param document contain all the information about the document
     * @param aTraits request parameters
     */
    private void fillResultWithMetadata(ExternalSearchResult result, SolrDocument document,
                                        SolrSearchProviderTraits aTraits)
            throws IOException, SolrException
    {
        if (isNotBlank((String) document.getFirstValue(DOC_NAME_KEY)))
        {
            result.setDocumentTitle((String) document.getFirstValue(DOC_NAME_KEY));
        }
        else if (isNotBlank((String) document.getFirstValue(DOC_TITLE_KEY)))
        {
            result.setDocumentTitle((String) document.getFirstValue(DOC_TITLE_KEY));
        }
        else
            result.setDocumentTitle((String) document.getFirstValue(DOC_ID_KEY));

        if (isNotBlank((String) document.getFirstValue(DOC_LANGUAGE_KEY)))
            result.setLanguage((String) document.getFirstValue(DOC_LANGUAGE_KEY));


        if (isNotBlank((String) document.getFirstValue(DOC_URI_KEY)))
            result.setOriginalUri((String) document.getFirstValue(DOC_URI_KEY));
        else
            result.setOriginalUri(aTraits.getIndexName());

        // If there is no timestamp then we use the computer timestamp
        if (isNotBlank((String) document.getFirstValue(DOC_TIMESTAMP_KEY)))
            result.setTimestamp((String) document.getFirstValue(DOC_TIMESTAMP_KEY));
        else
        {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();
            result.setTimestamp(dtf.format(now));
        }

        // Field with the entire text
        result.setOriginalSource((String) document.getFirstValue(aTraits.getTextField()));

    }


    /**
     * Search a Solrdocument by id and return an ExternalSearchResult.
     * @param aRepository Solr repository
     * @param aTraits request parameters
     * @param aCollectionId the name of the collection
     * @param aDocumentId the id of the document
     * @return is used by the module external search core in order to get a preview of the document
     * @throws SolrException Connection exception and request exception
     */
    @Override
    public ExternalSearchResult getDocumentResult(DocumentRepository aRepository,
                                                  SolrSearchProviderTraits aTraits,
                                                  String aCollectionId,
                                                  String aDocumentId)
            throws IOException, SolrException {
        if (!aCollectionId.equals(aTraits.getIndexName())) {
            throw new IllegalArgumentException(
                    "Requested collection name does not match connection collection name");
        }

        SolrQuery getQuery = new SolrQuery(DOC_ID_KEY + ":" + aDocumentId);

        try (HttpSolrClient client = makeClient(aTraits)) {
            ExternalSearchResult result = new ExternalSearchResult(aRepository, aCollectionId,
                    aDocumentId);



            // Send get query
            try {
                QueryResponse response = client.query(aTraits.getIndexName(), getQuery);
                SolrDocumentList documents = response.getResults();
                SolrDocument document = documents.get(0);
                fillResultWithMetadata(result, document, aTraits);
            } catch (SolrServerException e) {
                e.printStackTrace();
            }

            return result;
        }
    }

    /**
     * Search a Solrdocument by id and return the text
     * @param aRepository Solr repository
     * @param aTraits Request parameters
     * @param aCollectionId Name of the collection / index
     * @param aDocumentId unique id of the document
     * @return the text of the document
     * @throws SolrException wrong id or connection issues
     */
    @Override
    public String getDocumentText(DocumentRepository aRepository,
                                  SolrSearchProviderTraits aTraits,
                                  String aCollectionId, String aDocumentId)
            throws IOException
    {
        if (!aCollectionId.equals(aTraits.getIndexName()))
        {
            throw new IllegalArgumentException(
                    "Requested collection name does not match connection collection name");
        }

        SolrQuery getQuery = new SolrQuery(DOC_ID_KEY + ":" + aDocumentId);
        getQuery.setRows(1);

        HttpSolrClient client = makeClient(aTraits);

        QueryResponse response = new QueryResponse();
        SolrDocumentList documents = new SolrDocumentList();
        SolrDocument document = new SolrDocument();

        getQuery.setRows(1);
        getQuery.setParam("collection", aTraits.getIndexName());
        getQuery.setParam("qt", aTraits.getSearchPath());

        try {
            response = client.query(getQuery);
            documents = response.getResults();
            document = documents.get(0);
        } catch (SolrException | SolrServerException e) {
            e.printStackTrace();
        }
        return (String) document.getFirstValue(aTraits.getTextField());

    }


    @Override
    public InputStream getDocumentAsStream(DocumentRepository aRepository,
                                           SolrSearchProviderTraits aTraits,
                                           String aCollectionId, String aDocumentId)
            throws IOException {
        return IOUtils.toInputStream(
                getDocumentText(aRepository, aTraits, aCollectionId, aDocumentId), UTF_8);
    }

    @Override
    public String getDocumentFormat(DocumentRepository aRepository,
                                    SolrSearchProviderTraits aTraits,
                                    String aCollectionId, String aDocumentId)
    {
        return TextFormatSupport.ID;
    }

    /**
     * Create à client and connect it to the server
     * @param aTraits parameters of the connection
     * @return HttpSolrClient object use synchrone methode
     */
    private HttpSolrClient makeClient(SolrSearchProviderTraits aTraits)
            throws HttpHostConnectException
    {

        return new HttpSolrClient.Builder(aTraits.getRemoteUrl() + "/solr")
                .withConnectionTimeout(10000)
                .withSocketTimeout(60000)
                .build();

    }
}
