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
package de.tudarmstadt.ukp.inception.externalsearch.pubmed.entrez;

import static java.util.stream.Collectors.joining;

import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.http.HttpMethod;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import de.tudarmstadt.ukp.inception.externalsearch.pubmed.entrez.model.ESearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.pubmed.entrez.model.ESummaryResult;

public class EntrezClient
{
    private static final String PARAM_ID = "id";
    private static final String PARAM_TERM = "term";
    private static final String PARAM_RETSTART = "retstart";
    private static final String PARAM_DB = "db";
    private static final String PARAM_RETMAX = "retmax";
    private static final String PARAM_SORT = "sort";

    private static final String EUTILS_BASE_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils";
    private static final String ESEARCH_URL = EUTILS_BASE_URL
            + "/esearch.fcgi?db={db}&retstart={retstart}&retmax={retmax}&term={term}&sort={sort}";
    private static final String ESUMMARY_URL = EUTILS_BASE_URL + "/esummary.fcgi?db={db}&id={id}";
    private static final String EFETCH_URL = EUTILS_BASE_URL + "/efetch.fcgi?db={db}&id={id}";

    private final RestTemplate restTemplate;

    public EntrezClient()
    {
        restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJackson2XmlHttpMessageConverter());
    }

    // https://www.ncbi.nlm.nih.gov/books/NBK25499/
    public ESearchResult esearch(String aDb, String aQuery, int aOffset, int aPageSize)
    {
        var variables = Map.of( //
                PARAM_DB, aDb, //
                PARAM_RETSTART, Integer.toString(aOffset), //
                PARAM_RETMAX, Integer.toString(aPageSize), //
                PARAM_TERM, aQuery, //
                PARAM_SORT, "relevance");

        var response = restTemplate.exchange(ESEARCH_URL, HttpMethod.GET, null, ESearchResult.class,
                variables);

        return response.getBody();
    }

    public ESummaryResult esummary(String aDb, int... aIDs)
    {
        var variables = Map.of( //
                PARAM_DB, aDb, //
                PARAM_RETMAX, Integer.toString(aIDs.length), //
                PARAM_ID, IntStream.of(aIDs).mapToObj(Integer::toString).collect(joining(",")));

        var response = restTemplate.exchange(ESUMMARY_URL, HttpMethod.GET, null,
                ESummaryResult.class, variables);

        return response.getBody();
    }

    public String efetch(String aDb, String aID)
    {
        var variables = Map.of( //
                PARAM_DB, aDb, //
                PARAM_ID, aID);

        var response = restTemplate.exchange(EFETCH_URL, HttpMethod.GET, null, String.class,
                variables);

        return response.getBody();
    }
}
