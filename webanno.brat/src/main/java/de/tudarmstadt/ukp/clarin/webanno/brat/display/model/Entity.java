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

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import de.tudarmstadt.ukp.clarin.webanno.brat.message.BeanAsArraySerializer;

/**
 * An Entity is a span annotation with id, type and offsets of the span annotation of the format
 * ["p_22406","KON",[[1125,1128]]] - id of POS 22406, of type KON and start offset=1125, end offset
 * = 1128
 *
 * @author Seid Muhie Yimam
 *
 */
@JsonSerialize(using = BeanAsArraySerializer.class)
@JsonPropertyOrder(value = { "id", "type", "offsets", "labelText" })
public class Entity
{
    private int id;
    private String type;
    private List<Offsets> offsets = new ArrayList<Offsets>();
    // WEBANNO EXTENSION BEGIN
    private String labelText;
    // WEBANNO EXTENSION END

    public Entity()
    {
        // Nothing to do
    }

    public Entity(int aId, String aType, List<Offsets> aOffsets)
    {
        this(aId, aType, aOffsets, null);
    }

    public Entity(int aId, String aType, List<Offsets> aOffsets, String aLabelText)
    {
        super();
        id = aId;
        type = aType;
        offsets = aOffsets;
        labelText = aLabelText;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int aId)
    {
        id = aId;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String aType)
    {
        type = aType;
    }

    public List<Offsets> getOffsets()
    {
        return offsets;
    }

    public void setOffsets(List<Offsets> aOffsets)
    {
        offsets = aOffsets;
    }

    public void setLabelText(String aLabelText)
    {
        labelText = aLabelText;
    }
    
    public String getLabelText()
    {
        return labelText;
    }
}
