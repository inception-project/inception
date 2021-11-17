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
package de.tudarmstadt.ukp.inception.diam.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public class MViewportUpdate
{
    private final int begin;
    private final int end;
    private final JsonNode diff;

    /**
     * @param aBegin
     *            begin offset of the updates contained in the diff. Like the end offset, this
     *            information is mainly informative and currently used only during testing. It might
     *            also not be accurate.
     * @param aEnd
     *            end offset of the updates contained in the diff.
     * @param aDiff
     *            the diff.
     */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public MViewportUpdate(@JsonProperty("begin") int aBegin, @JsonProperty("end") int aEnd,
            @JsonProperty("diff") JsonNode aDiff)
    {
        begin = aBegin;
        end = aEnd;
        diff = aDiff;
    }

    public int getBegin()
    {
        return begin;
    }

    public int getEnd()
    {
        return end;
    }

    public JsonNode getDiff()
    {
        return diff;
    }
}
