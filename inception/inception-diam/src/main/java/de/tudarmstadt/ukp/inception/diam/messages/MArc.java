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

import de.tudarmstadt.ukp.inception.rendering.vmodel.VArc;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;

public class MArc
    extends MAnnotation
{
    private final VID source;
    private final VID target;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public MArc(@JsonProperty("vid") VID aVid, @JsonProperty("color") String aColor,
            @JsonProperty("label") String aLabel, @JsonProperty("source") VID aSource,
            @JsonProperty("target") VID aTarget)
    {
        super(aVid, aColor, aLabel);
        source = aSource;
        target = aTarget;
    }

    public MArc(VArc aVArc)
    {
        super(aVArc);
        source = aVArc.getSource();
        target = aVArc.getTarget();
    }

    public VID getSource()
    {
        return source;
    }

    public VID getTarget()
    {
        return target;
    }
}
