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

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.support.json.BeanAsArraySerializer;

/**
 * An Entity is a span annotation with id, type and offsets of the span annotation of the format
 * ["p_22406","KON",[[1125,1128]]] - id of POS 22406, of type KON and start offset=1125, end offset
 * = 1128
 *
 */
@JsonSerialize(using = BeanAsArraySerializer.class)
@JsonPropertyOrder(value = { "vid", "type", "offsets", "attributes" })
public class Entity
{
    private VID vid;
    private String type;
    private List<Offsets> offsets = new ArrayList<>();
    // WEBANNO EXTENSION BEGIN
    private EntityAttributes attributes = new EntityAttributes();
    // WEBANNO EXTENSION END

    public Entity()
    {
        // Nothing to do
    }

    public Entity(int aId, String aType, Offsets aOffsets, String aLabelText, String aColor)
    {
        this(aId, aType, asList(aOffsets), aLabelText, aColor, false);
    }

    public Entity(int aId, String aType, List<Offsets> aOffsets, String aLabelText, String aColor)
    {
        this(new VID(aId), aType, aOffsets, aLabelText, aColor, false);
    }

    public Entity(int aId, String aType, List<Offsets> aOffsets, String aLabelText, String aColor,
            boolean aActionButtons)
    {
        this(new VID(aId), aType, aOffsets, aLabelText, aColor, aActionButtons);
    }

    public Entity(VID aVid, String aType, Offsets aOffsets, String aLabelText, String aColor,
            boolean aActionButtons)
    {
        this(aVid, aType, asList(aOffsets), aLabelText, aColor, aActionButtons);
    }

    public Entity(VID aVid, String aType, Offsets aOffsets, String aLabelText, String aColor)
    {
        this(aVid, aType, asList(aOffsets), aLabelText, aColor, false);
    }

    public Entity(VID aVid, String aType, List<Offsets> aOffsets, String aLabelText, String aColor)
    {
        this(aVid, aType, aOffsets, aLabelText, aColor, false);
    }

    public Entity(VID aVid, String aType, List<Offsets> aOffsets, String aLabelText, String aColor,
            boolean aActionButtons)
    {
        vid = aVid;
        type = aType;
        offsets = aOffsets;
        attributes.setLabelText(aLabelText);
        attributes.setColor(aColor);
        attributes.setActionButtons(aActionButtons);
    }

    @Deprecated
    public int getId()
    {
        return vid.getId();
    }

    @Deprecated
    public void setId(int aId)
    {
        vid = new VID(aId);
    }

    public VID getVid()
    {
        return vid;
    }

    public void setVid(VID aVid)
    {
        vid = aVid;
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

    public EntityAttributes getAttributes()
    {
        return attributes;
    }

    public void setAttributes(EntityAttributes aAttributes)
    {
        attributes = aAttributes;
    }

    /**
     * @deprecated Use {@code getAttributes().setLabelText(...)}.
     */
    @SuppressWarnings("javadoc")
    @JsonIgnore
    @Deprecated
    public void setLabelText(String aLabelText)
    {
        attributes.setLabelText(aLabelText);
    }

    /**
     * @deprecated Use {@code getAttributes().getLabelText()}.
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    public String getLabelText()
    {
        return attributes.getLabelText();
    }

    /**
     * @deprecated Use {@code getAttributes().getColor()}.
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    public String getColor()
    {
        return attributes.getColor();
    }

    /**
     * @deprecated Use {@code getAttributes().setColor(...)}.
     */
    @SuppressWarnings("javadoc")
    @JsonIgnore
    @Deprecated
    public void setColor(String aColor)
    {
        attributes.setColor(aColor);
    }
}
