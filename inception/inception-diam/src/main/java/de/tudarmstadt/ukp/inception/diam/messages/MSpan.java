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

import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VSpan;

public class MSpan
    extends MAnnotation
{
    private final int begin;
    private final int end;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public MSpan(@JsonProperty("vid") VID aVid, @JsonProperty("color") String aColor,
            @JsonProperty("label") String aLabel, @JsonProperty("begin") int aBegin,
            @JsonProperty("end") int aEnd)
    {
        super(aVid, aColor, aLabel);
        begin = aBegin;
        end = aEnd;
    }

    public MSpan(VSpan aSpan)
    {
        super(aSpan);
        begin = aSpan.getRanges().get(0).getBegin();
        end = aSpan.getRanges().get(0).getEnd();
    }

    public int getBegin()
    {
        return begin;
    }

    public int getEnd()
    {
        return end;
    }
}
