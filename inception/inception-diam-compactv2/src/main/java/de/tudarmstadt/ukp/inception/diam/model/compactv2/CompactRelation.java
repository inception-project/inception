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
package de.tudarmstadt.ukp.inception.diam.model.compactv2;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.support.json.BeanAsArraySerializer;

@JsonSerialize(using = BeanAsArraySerializer.class)
@JsonPropertyOrder(value = { "layerId", "vid", "arguments", "attributes" })
public class CompactRelation
    implements CompactAnnotation
{
    private long layerId;

    private VID vid;

    /**
     * The initial/destination span annotations as shown in the example above
     */
    private List<CompactArgument> arguments = new ArrayList<>();

    private CompactSpanAttributes attributes = new CompactSpanAttributes();

    public CompactRelation()
    {
        // Nothing to do
    }

    public CompactRelation(AnnotationLayer aLayer, VID aVid, List<CompactArgument> aArguments,
            String aLabelText, String aColor)
    {
        layerId = aLayer.getId();
        vid = aVid;
        arguments = aArguments;
        attributes.setLabelText(aLabelText);
        attributes.setColor(aColor);
    }

    @Override
    public long getLayerId()
    {
        return layerId;
    }

    @Override
    public VID getVid()
    {
        return vid;
    }

    public void setVid(VID aVid)
    {
        vid = aVid;
    }

    public List<CompactArgument> getArguments()
    {
        return arguments;
    }

    public void setArguments(List<CompactArgument> aArguments)
    {
        arguments = aArguments;
    }

    public CompactSpanAttributes getAttributes()
    {
        return attributes;
    }

    public void setAttributes(CompactSpanAttributes aAttributes)
    {
        attributes = aAttributes;
    }
}
