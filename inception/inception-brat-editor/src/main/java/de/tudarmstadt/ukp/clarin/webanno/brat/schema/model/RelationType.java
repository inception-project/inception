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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Entity;

/**
 * Type of an arc. Defines properties such as color, possible targets, etc. Example:
 * "arcs":[{"type":"anaphoric","color":"green","arrowHead":"triangle,5","labels":["anaphoric"],
 * "targets":["nam"],"dashArray":""}]....
 */
public class RelationType
    implements Serializable
{
    private static final long serialVersionUID = 4523870233669399336L;

    /**
     * The type of the arc annotation
     */
    private String type;

    private String color;
    private String arrowHead;

    /**
     * Possible List of labels to be used on the arc
     */
    private List<String> labels;

    /**
     * List of Target Labels, which are a span annotation ({@link Entity}
     */
    private Set<String> targets = new HashSet<>();
    private String dashArray;

    public RelationType()
    {
        // For serialization
    }

    // allow similar colors per layer
    // FIXME we must not have such global states (public static)
    // public static HashMap<String, Color> typeToColor = new HashMap<String, Color>();

    // public RelationType(String aName, String aType, String aTarget)
    // {
    // this(aName, aType, aTarget, null, "triangle,5");
    // }

    // public RelationType(String aName, String aType, String aTarget, String aColor)
    // {
    // this(aName, aType, aTarget, aColor, "triangle,5");
    // }

    public RelationType(String aLabel, String aType, String aTarget, String aColor,
            String aArrowHead, String aDashArray)
    {
        this(aColor, aArrowHead, asList(aLabel, abbreviate(aLabel)), aType, asList(aTarget),
                aDashArray);
    }

    private RelationType(String aColor, String aArrowHead, List<String> aLabels, String aType,
            List<String> aTargets, String aDashArray)
    {
        color = aColor;
        arrowHead = aArrowHead;
        labels = aLabels;
        type = aType;
        dashArray = aDashArray;

        if (aTargets != null) {
            targets.addAll(aTargets);
        }
    }

    public String getColor()
    {
        return color;
    }

    public void setColor(String aColor)
    {
        color = aColor;
    }

    public String getArrowHead()
    {
        return arrowHead;
    }

    public void setArrowHead(String aArrowHead)
    {
        arrowHead = aArrowHead;
    }

    public List<String> getLabels()
    {
        return labels;
    }

    public void setLabels(List<String> aLabels)
    {
        labels = aLabels;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String aType)
    {
        type = aType;
    }

    public Set<String> getTargets()
    {
        return targets;
    }

    public void addTarget(String aTarget)
    {
        targets.add(aTarget);
    }

    public void setTargets(Collection<String> aTargets)
    {
        targets.clear();
        targets.addAll(aTargets);
    }

    public String getDashArray()
    {
        return dashArray;
    }

    public void setDashArray(String aDashArray)
    {
        dashArray = aDashArray;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof RelationType)) {
            return false;
        }
        RelationType castOther = (RelationType) other;
        return new EqualsBuilder().append(type, castOther.type).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(type).toHashCode();
    }
}
