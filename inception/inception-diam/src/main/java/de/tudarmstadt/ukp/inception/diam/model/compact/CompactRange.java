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
package de.tudarmstadt.ukp.inception.diam.model.compact;

import static com.fasterxml.jackson.core.JsonToken.END_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.START_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.VALUE_NUMBER_INT;

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.tudarmstadt.ukp.inception.support.json.BeanAsArraySerializer;

/**
 * The start and End offset positions of a span annotation as required by the Brat protocol
 */
@JsonSerialize(using = BeanAsArraySerializer.class)
@JsonDeserialize(using = CompactRange.OffsetsDeserializer.class)
@JsonPropertyOrder(value = { "begin", "end" })
public class CompactRange
{
    private int begin;
    private int end;

    public CompactRange()
    {
        // Nothing to do
    }

    public CompactRange(int aBegin, int aEnd)
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

    @Override
    public String toString()
    {
        return "[" + begin + "-" + end + "]";
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(begin, end);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CompactRange other = (CompactRange) obj;
        return begin == other.begin && end == other.end;
    }

    /**
     * Deserialize {@link CompactRange} from JSON to Java.
     *
     */
    public static class OffsetsDeserializer
        extends JsonDeserializer<CompactRange>
    {
        @Override
        public CompactRange deserialize(JsonParser aJp, DeserializationContext aCtxt)
            throws IOException
        {
            CompactRange offsets = new CompactRange();

            if (aJp.getCurrentToken() != START_ARRAY) {
                aCtxt.reportWrongTokenException(CompactRange.class, START_ARRAY,
                        "Expecting array begin");
            }

            if (aJp.nextToken() == VALUE_NUMBER_INT) {
                offsets.begin = aJp.getIntValue();
            }
            else {
                aCtxt.reportWrongTokenException(CompactRange.class, VALUE_NUMBER_INT,
                        "Expecting begin offset as integer");
            }

            if (aJp.nextToken() == VALUE_NUMBER_INT) {
                offsets.end = aJp.getIntValue();
            }
            else {
                aCtxt.reportWrongTokenException(CompactRange.class, VALUE_NUMBER_INT,
                        "Expecting end offset as integer");
            }

            if (aJp.nextToken() != END_ARRAY) {
                aCtxt.reportWrongTokenException(CompactRange.class, END_ARRAY,
                        "Expecting array end");
            }

            return offsets;
        }
    }
}
