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
package de.tudarmstadt.ukp.clarin.webanno.brat.display.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Entity;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Offsets;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

public class EntityTest
{
    @Test
    public void toJsonTest() throws IOException
    {
        String json = JSONUtil.toPrettyJsonString(
                new Entity(new VID(1, 2), "type", new Offsets(1, 2), "label", "color", true));

        // @formatter:off
        assertEquals(
                String.join("\n",
                        "[ \"1.2\", \"type\", [ [ 1, 2 ] ], {",
                        "  \"l\" : \"label\",",
                        "  \"c\" : \"color\",",
                        "  \"a\" : 1",
                        "} ]"),
                json);
        // @formatter:on
    }
}
