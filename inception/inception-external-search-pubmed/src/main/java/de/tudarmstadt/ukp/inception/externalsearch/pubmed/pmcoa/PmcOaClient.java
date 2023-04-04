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
package de.tudarmstadt.ukp.inception.externalsearch.pubmed.pmcoa;

import java.util.Map;

import org.springframework.http.HttpMethod;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import de.tudarmstadt.ukp.inception.externalsearch.pubmed.traits.PubMedProviderTraits;

public class PmcOaClient
{
    private static final String PMCOA_BASE_URL = "https://www.ncbi.nlm.nih.gov/research/bionlp/RESTful/pmcoa.cgi";
    private static final String BIOC_URL = PMCOA_BASE_URL + "/BioC_xml/{id}/unicode";

    private static final String PARAM_ID = "id";

    private final RestTemplate restTemplate;

    public PmcOaClient()
    {
        restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJackson2XmlHttpMessageConverter());
    }

    public byte[] bioc(PubMedProviderTraits aTraits, String aID)
    {
        var variables = Map.of( //
                PARAM_ID, aID);

        var response = restTemplate.exchange(BIOC_URL, HttpMethod.GET, null, byte[].class,
                variables);

        return response.getBody();
    }
}
