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
package de.tudarmstadt.ukp.inception.recommendation.imls.hf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.recommendation.imls.hf.client.HfHubClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.hf.client.HfHubClientImpl;
import de.tudarmstadt.ukp.inception.recommendation.imls.hf.model.HfModelCard;
import de.tudarmstadt.ukp.inception.recommendation.imls.hf.model.HfModelDetails;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

public class HfHubClientImplTest
{
    private HfHubClient sut;

    @BeforeEach
    public void setup()
    {
        sut = new HfHubClientImpl();
    }

    @Disabled("Not really a test")
    @Test
    public void testCatalogQuery() throws Exception
    {
        HfModelCard[] response = sut.listModels("bert");

        System.out.println(JSONUtil.toPrettyJsonString(response));
    }

    @Disabled("Not really a test")
    @Test
    public void testRetrievingDetails() throws Exception
    {
        HfModelDetails details = sut.details("alvaroalon2/biobert_chemical_ner");

        System.out.println(JSONUtil.toPrettyJsonString(details));
    }

    @Disabled("Not really a test")
    @Test
    public void findNonExecutableServices() throws Exception
    {
        for (HfModelCard e : sut.listModels("")) {
            HfModelDetails details = sut.details(e.getModelId());
            System.out.printf("%s %s %n", e.getModelId(), details.getTags());
        }
    }
}
