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

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpMethod;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException.NotFound;
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

    public byte[] bioc(PubMedProviderTraits aTraits, String aID) throws IOException
    {
        try {
            var variables = Map.of( //
                    PARAM_ID, aID);

            var response = restTemplate.exchange(BIOC_URL, HttpMethod.GET, null, byte[].class,
                    variables);

            return response.getBody();
        }
        catch (NotFound e) {
            throw new IOException("BioC version of document [" + aID + "] not found at ["
                    + BIOC_URL.replace("{id}", aID)
                    + "]. The Open Access files and BioC versions are not updated  as "
                    + "quickly as the PMC website itself is updated. It may take a couple of days until "
                    + "a particular file is available as BioC. Another reason could be that the document you "
                    + "are looking for is not included in the Open Access set. Try adding "
                    + "`\"open access\"[filter]` without \"`\" to your search to filter by Open Access files.");
        }
    }
}
