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
package de.tudarmstadt.ukp.inception.log.exporter;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @see <a href=
 *      "https://cassiomolin.com/2017/01/24/deserializing-json-property-as-string-with-jackson/">
 *      Deserializing JSON property as String with Jackson</a>
 */
public class RawJsonDeserializer
    extends JsonDeserializer<String>
{
    @Override
    public String deserialize(JsonParser aParser, DeserializationContext aContext)
        throws IOException, JsonProcessingException
    {
        var mapper = (ObjectMapper) aParser.getCodec();
        var node = mapper.readTree(aParser);
        return mapper.writeValueAsString(node);
    }
}
