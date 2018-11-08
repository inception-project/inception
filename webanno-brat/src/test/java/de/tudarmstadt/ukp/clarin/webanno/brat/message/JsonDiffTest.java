/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.clarin.webanno.brat.message;

import java.io.File;

import org.junit.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;

public class JsonDiffTest
{
    @Test
    public void testJsonDiff() throws Exception
    {
        String f_base = "src/test/resources/brat_normal.json";
        String f_addedMiddle = "src/test/resources/brat_added_entity_near_middle.json";
        String f_removedMiddle = "src/test/resources/brat_removed_entity_in_middle.json";
        String f_removedEnd = "src/test/resources/brat_removed_entity_near_end.json";

        MappingJackson2HttpMessageConverter jsonConverter = 
                new MappingJackson2HttpMessageConverter();
        
        ObjectMapper mapper = jsonConverter.getObjectMapper();
        
        JsonNode base = mapper.readTree(new File(f_base));
        JsonNode addedMiddle = mapper.readTree(new File(f_addedMiddle));
        JsonNode removedMiddle = mapper.readTree(new File(f_removedMiddle));
        JsonNode removedEnd = mapper.readTree(new File(f_removedEnd));
        
        JsonNode d_addedMiddle = JsonDiff.asJson(base, addedMiddle);
        JsonNode d_removedMiddle = JsonDiff.asJson(base, removedMiddle);
        JsonNode d_removedEnd = JsonDiff.asJson(base, removedEnd);
        
        System.out.println(d_addedMiddle);
        System.out.println(d_removedMiddle);
        System.out.println(d_removedEnd);
    }
}
