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

import de.tudarmstadt.ukp.inception.recommendation.imls.elg.client.ElgServiceClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.client.ElgServiceClientImpl;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.model.ElgServiceResponse;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

public class ElgServiceClientImplTest
{
    private ElgServiceClient sut;

    @BeforeEach
    public void setup()
    {
        sut = new ElgServiceClientImpl();
    }

    @Disabled("Won't run without a token")
    @Test
    public void test() throws Exception
    {
        String syncUrl = "https://live.european-language-grid.eu/execution/process/gatenercen";
        var token = "hahaha!";
        String text = ("John Smith is working at IBM London.");
        ElgServiceResponse response = sut.invokeService(syncUrl, token, text);

        System.out.println(JSONUtil.toPrettyJsonString(response));
    }
}
