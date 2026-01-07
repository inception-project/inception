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
package de.tudarmstadt.ukp.clarin.webanno.brat.render.model;

import static tools.jackson.core.JsonToken.END_ARRAY;
import static tools.jackson.core.JsonToken.START_ARRAY;
import static tools.jackson.core.JsonToken.VALUE_NUMBER_INT;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.tudarmstadt.ukp.inception.support.json.BeanAsArraySerializer;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

/**
 * The start and End offset positions of a span annotation as required by the Brat protocol
 * 
 * @see Entity
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

    public boolean isEmpty()
    {
        return end == begin;
    }

    @Override
    public String toString()
    {
        return "[" + begin + "-" + end + "]";
    }

    /**
     * Deserialize {@link Offsets} from JSON to Java.
     *
     */
    public static class OffsetsDeserializer
        extends ValueDeserializer<Offsets>
    {
        @Override
        public Offsets deserialize(JsonParser aJp, DeserializationContext aCtxt)
        {
            Offsets offsets = new Offsets();

            if (aJp.currentToken() != START_ARRAY) {
                aCtxt.reportWrongTokenException(this, START_ARRAY, "Expecting array begin");
            }

            if (aJp.nextToken() == VALUE_NUMBER_INT) {
                offsets.begin = aJp.getIntValue();
            }
            else {
                aCtxt.reportWrongTokenException(this, VALUE_NUMBER_INT,
                        "Expecting begin offset as integer");
            }

            if (aJp.nextToken() == VALUE_NUMBER_INT) {
                offsets.end = aJp.getIntValue();
            }
            else {
                aCtxt.reportWrongTokenException(this, VALUE_NUMBER_INT,
                        "Expecting end offset as integer");
            }

            if (aJp.currentToken() != END_ARRAY) {
                aCtxt.reportWrongTokenException(this, END_ARRAY, "Expecting array end");
            }

            return offsets;
        }
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof Offsets)) {
            return false;
        }

        var castOther = (Offsets) other;
        return Objects.equals(begin, castOther.begin) && Objects.equals(end, castOther.end);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(begin, end);
    }
}
