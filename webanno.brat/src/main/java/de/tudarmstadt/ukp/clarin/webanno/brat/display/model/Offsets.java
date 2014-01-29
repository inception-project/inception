/*******************************************************************************
 * Copyright 2012
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.display.model;

import java.io.IOException;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import de.tudarmstadt.ukp.clarin.webanno.brat.message.BeanAsArraySerializer;
/**
 * The start and End offset positions of a span annotation as required by the Brat protocol
 * @author Seid Muhie Yimam
 * @author Richard Eckart de Castilho
 * @see {@link Entity}
 */
@JsonSerialize(using = BeanAsArraySerializer.class)
@JsonDeserialize(using = Offsets.OffsetsDeserializer.class)
@JsonPropertyOrder(value = { "begin", "end" })
public class Offsets
{
    private int begin;
    private int end;

    public Offsets()
    {
        // Nothing to do
    }

    public Offsets(int aBegin, int aEnd)
    {
        super();
        begin = aBegin;
        end = aEnd;
    }

    public int getBegin()
    {
        return begin;
    }

    public void setBegin(int aBegin)
    {
        begin = aBegin;
    }

    public int getEnd()
    {
        return end;
    }

    public void setEnd(int aEnd)
    {
        end = aEnd;
    }

    /**
     * Deserialize {@link Offsets} from JSON to Java.
     *
     * @author Richard Eckart de Castilho
     */
    public static class OffsetsDeserializer
        extends JsonDeserializer<Offsets>
    {
        @Override
        public Offsets deserialize(JsonParser aJp, DeserializationContext aCtxt)
            throws IOException, JsonProcessingException
        {
            Offsets offsets = new Offsets();

            if (aJp.getCurrentToken() != JsonToken.START_ARRAY) {
                aCtxt.mappingException("Expecting array begin");
            }

            if (aJp.nextToken() == JsonToken.VALUE_NUMBER_INT) {
                offsets.begin = aJp.getIntValue();
            }
            else {
                aCtxt.mappingException("Expecting begin offset as integer");
            }

            if (aJp.nextToken() == JsonToken.VALUE_NUMBER_INT) {
                offsets.end = aJp.getIntValue();
            }
            else {
                aCtxt.mappingException("Expecting end offset as integer");
            }

            if (aJp.getCurrentToken() != JsonToken.END_ARRAY) {
                aCtxt.mappingException("Expecting array end");
            }

            return offsets;
        }
    }
}
