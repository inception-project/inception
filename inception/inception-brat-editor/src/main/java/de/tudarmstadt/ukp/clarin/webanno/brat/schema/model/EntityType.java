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
package de.tudarmstadt.ukp.clarin.webanno.brat.schema.model;

import static de.tudarmstadt.ukp.clarin.webanno.brat.render.BratSerializerImpl.abbreviate;
import static java.util.Arrays.asList;

import java.util.List;

/**
 * Different attributes of an Entity used for its visualisation formats. It looks like
 *
 * {"name":"Named Entity","type":"Named Entity","unused":true,"fgColor":"black","bgColor":"cyan",
 * "borderColor":"green","labels":[], "children":[{"name":"LOC","type":"LOC","unused":false,
 * "fgColor":"black","bgColor":"cyan","borderColor":"green","labels":["LOC"],"children":[],
 * "attributes":[],"arcs":[]},....
 */
public class EntityType
{
    private String name;
    private String type;

    private String fgColor;
    private String bgColor;
    private String borderColor;

    private List<String> labels;

    private List<RelationType> arcs;

    // @Deprecated
    // private List<SpanType> children;
    //
    // @Deprecated
    // private List<String> attributes;
    //
    // @Deprecated
    // private boolean unused;
    //
    // @Deprecated
    // private String hotkey;

    public EntityType()
    {
        // Nothing to do
    }

    /**
     * @param aName
     *            the "name" (the UIMA type name).
     * @param aLabel
     *            the "label" which is displayed to the user e.g. in the comment pop-up (the display
     *            name)
     * @param aType
     *            the "type" (brat type name, which is basically an identifier used in the UI visual
     *            model)
     */
    public EntityType(String aName, String aLabel, String aType)
    {
        name = aName;
        type = aType;
        labels = asList(aLabel, abbreviate(aLabel));
    }

    public String getName()
    {
        return name;
    }

    public void setName(String aName)
    {
        name = aName;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String aType)
    {
        type = aType;
    }

    // @Deprecated
    // public boolean isUnused()
    // {
    // return unused;
    // }
    //
    // @Deprecated
    // public void setUnused(boolean aUnused)
    // {
    // unused = aUnused;
    // }
    //
    // @Deprecated
    // public String getHotkey()
    // {
    // return hotkey;
    // }
    //
    // @Deprecated
    // public void setHotkey(String aHotkey)
    // {
    // hotkey = aHotkey;
    // }

    public String getFgColor()
    {
        return fgColor;
    }

    public void setFgColor(String aFgColor)
    {
        fgColor = aFgColor;
    }

    public String getBgColor()
    {
        return bgColor;
    }

    public void setBgColor(String aBgColor)
    {
        bgColor = aBgColor;
    }

    public String getBorderColor()
    {
        return borderColor;
    }

    public void setBorderColor(String aBorderColor)
    {
        borderColor = aBorderColor;
    }

    public List<String> getLabels()
    {
        return labels;
    }

    public void setLabels(List<String> aLabels)
    {
        labels = aLabels;
    }

    // @Deprecated
    // public List<SpanType> getChildren()
    // {
    // return children;
    // }
    //
    // @Deprecated
    // public void setChildren(List<SpanType> aChildren)
    // {
    // children = aChildren;
    // }
    //
    // @Deprecated
    // public List<String> getAttributes()
    // {
    // return attributes;
    // }
    //
    // @Deprecated
    // public void setAttributes(List<String> aAttributes)
    // {
    // attributes = aAttributes;
    // }

    public List<RelationType> getArcs()
    {
        return arcs;
    }

    public void setArcs(List<RelationType> aArcs)
    {
        arcs = aArcs;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
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
        EntityType other = (EntityType) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        }
        else if (!name.equals(other.name)) {
            return false;
        }
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        }
        else if (!type.equals(other.type)) {
            return false;
        }
        return true;
    }

}
