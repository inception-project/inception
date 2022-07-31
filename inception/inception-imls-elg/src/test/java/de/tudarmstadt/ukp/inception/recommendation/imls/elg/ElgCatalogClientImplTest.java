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
package de.tudarmstadt.ukp.inception.recommendation.imls.elg;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.recommendation.imls.elg.client.ElgCatalogClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.client.ElgCatalogClientImpl;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.model.ElgCatalogEntity;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.model.ElgCatalogEntityDetails;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.model.ElgCatalogSearchResponse;

public class ElgCatalogClientImplTest
{
    private ElgCatalogClient sut;

    @BeforeEach
    public void setup()
    {
        sut = new ElgCatalogClientImpl();
    }

    @Disabled("Not really a test")
    @Test
    public void testCatalogQuery() throws Exception
    {
        // ElgCatalogSearchResponse response =
        sut.search("GATE");

        // System.out.println(JSONUtil.toPrettyJsonString(response));
    }

    @Disabled("Not really a test")
    @Test
    public void testRetrievingDetails() throws Exception
    {
        // ElgCatalogEntityDetails response =
        sut.details(
                "https://live.european-language-grid.eu/catalogue_backend/api/registry/metadatarecord/627/");

        // System.out.println(JSONUtil.toPrettyJsonString(response));
    }

    @Disabled("Not really a test")
    @Test
    public void findNonExecutableServices() throws Exception
    {
        ElgCatalogSearchResponse response = sut.search("");
        for (ElgCatalogEntity e : response.getResults()) {
            ElgCatalogEntityDetails details = sut.details(e.getDetailUrl());
            System.out.printf("%s %b %s%n", e.getResourceName(), details.getServiceInfo() != null,
                    e.getDetailUrl());
        }
    }
}
