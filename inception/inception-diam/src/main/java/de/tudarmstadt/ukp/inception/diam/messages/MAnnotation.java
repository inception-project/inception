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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VObject;
import de.tudarmstadt.ukp.inception.schema.api.feature.TypeUtil;

public abstract class MAnnotation
{
    private final VID vid;
    @JsonInclude(Include.NON_NULL)
    private final String color;
    private final String label;

    public MAnnotation(VObject aObject)
    {
        vid = aObject.getVid();
        color = aObject.getColorHint();
        label = TypeUtil.getUiLabelText(aObject);
    }

    public MAnnotation(VID aVid, String aColor, String aLabel)
    {
        vid = aVid;
        color = aColor;
        label = aLabel;
    }

    public VID getVid()
    {
        return vid;
    }

    public String getColor()
    {
        return color;
    }

    public String getLabel()
    {
        return label;
    }
}
