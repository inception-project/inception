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

import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.recommendation.imls.elg.model.ElgResponseContainer;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

public class ElgServiceModelDeserializationTest
{
    @Test
    public void testTextsCannedResponse() throws Exception
    {
        var response = "{\"response\":{\"type\":\"texts\",\"texts\":[{\"annotations\":{\"Person\":[{\"start\":5,\"end\":10,\"features\":{\"nif:anchorOf\":\"Smith\",\"itsrdf:taClassRef\":\"http://dbpedia.org/ontology/Person\"}}],\"Location\":[],\"Organisation\":[{\"start\":25,\"end\":35,\"features\":{\"nif:anchorOf\":\"IBM London\",\"itsrdf:taClassRef\":\"http://dbpedia.org/ontology/Organisation\"}}],\"Miscellaneous\":[]},\"content\":\"John Smith is working at IBM London.\"}]}}\n";

        // ElgResponseContainer resp =
        JSONUtil.fromJsonString(ElgResponseContainer.class, response);

        // System.out.println(JSONUtil.toPrettyJsonString(resp));
    }

    @Test
    public void testCannedAnnotationsResponse() throws Exception
    {
        var response = "{\"response\":{\"type\":\"annotations\",\"annotations\":{\"Person\":[{\"start\":0,\"end\":10,\"features\":{\"firstName\":\"John\",\"gender\":\"male\",\"surname\":\"Smith\",\"kind\":\"fullName\",\"rule\":\"PersonFull\",\"ruleFinal\":\"PersonFinal\"}}],\"Location\":[],\"Organization\":[{\"start\":25,\"end\":35,\"features\":{\"orgType\":\"company\",\"rule\":\"GazOrganization\",\"ruleFinal\":\"OrgCountryFinal\"}}],\"Date\":[]}}}\n";

        // ElgResponseContainer resp =
        JSONUtil.fromJsonString(ElgResponseContainer.class, response);

        // System.out.println(JSONUtil.toPrettyJsonString(resp));
    }
}
