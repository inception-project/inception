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

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.recommendation.imls.hf.client.HfInferenceClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.hf.client.HfInferenceClientImpl;
import de.tudarmstadt.ukp.inception.recommendation.imls.hf.model.HfEntityGroup;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

public class HfInferenceClientTest
{
    private HfInferenceClient sut;

    @BeforeEach
    public void setup()
    {
        sut = new HfInferenceClientImpl();
    }

    @Disabled("Won't run without a token")
    @Test
    public void test() throws Exception
    {
        String modelId = "dbmdz/bert-large-cased-finetuned-conll03-english";
        var token = "Muahahhaa!";
        String text = "John works for ACME Company in Alaska. He studied at the University of "
                + "California in Los Angeles.";
        List<HfEntityGroup> response = sut.invokeService(modelId, token, text);

        System.out.println(JSONUtil.toPrettyJsonString(response));
    }
}
