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
package de.tudarmstadt.ukp.clarin.webanno.brat.message;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import com.flipkart.zjsonpatch.JsonDiff;

public class JsonDiffTest
{
    @Test
    public void testJsonDiff() throws Exception
    {
        var f_base = "src/test/resources/brat_normal.json";
        var f_addedMiddle = "src/test/resources/brat_added_entity_near_middle.json";
        var f_removedMiddle = "src/test/resources/brat_removed_entity_in_middle.json";
        var f_removedEnd = "src/test/resources/brat_removed_entity_near_end.json";

        var jsonConverter = new MappingJackson2HttpMessageConverter();

        var mapper = jsonConverter.getObjectMapper();

        var base = mapper.readTree(new File(f_base));
        var addedMiddle = mapper.readTree(new File(f_addedMiddle));
        var removedMiddle = mapper.readTree(new File(f_removedMiddle));
        var removedEnd = mapper.readTree(new File(f_removedEnd));

        var d_addedMiddle = JsonDiff.asJson(base, addedMiddle);
        var d_removedMiddle = JsonDiff.asJson(base, removedMiddle);
        var d_removedEnd = JsonDiff.asJson(base, removedEnd);

        assertThat(d_addedMiddle.toString()) //
                .isEqualTo("[{\"op\":\"add\",\"path\":\"/entities/7\",\"value\":" //
                        + "[\"198\",\"0_de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS\"," //
                        + "[[29,32]],\"LALA\",\"#8dd3c7\",null]}]");
        assertThat(d_removedMiddle.toString()) //
                .isEqualTo("[{\"op\":\"remove\",\"path\":\"/entities/5\"}]");
        assertThat(d_removedEnd.toString()) //
                .isEqualTo("[{\"op\":\"remove\",\"path\":\"/entities/10\"}]");
    }
}
