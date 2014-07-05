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

import java.util.Arrays;
import java.util.List;

/**
 * Type of an arc. Defines properties such as color, possible targets, etc. Example:
 * "arcs":[{"type":"anaphoric","color":"green","arrowHead":"triangle,5","labels":["anaphoric"],
 * "targets":["nam"],"dashArray":""}]....
 */
public class RelationType
{
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
    private List<String> targets;
    private String dashArray;

    public RelationType()
    {
        // Nothing to do
    }

    // allow similar colors per layer
    // FIXME we must not have such global states (public static)
    // public static HashMap<String, Color> typeToColor = new HashMap<String, Color>();

    
    public RelationType(String aName, String aType, String aTarget, String aColor)
    {
        this(aName, aType, aTarget, aColor, "triangle,5");
    }
    
    public RelationType(String aName, String aType, String aTarget, String aColor,
            String aArrowHead)
    {
        this(aColor, aArrowHead, Arrays.asList(aName), aType, Arrays.asList(aTarget), "");
    }
    
    public RelationType(String aColor, String aArrowHead, List<String> aLabels, String aType,
            List<String> aTargets, String aDashArray/*, boolean aStaticColor*/)
    {
        super();

      /*  if(!aStaticColor){
        Color goodBgColor;
        if (typeToColor.containsKey(aType)) {
            goodBgColor = typeToColor.get(aType);
        }
        else {
            goodBgColor = TagColor.generateDifferingPastelColor(typeToColor.values());
            typeToColor.put(aType, goodBgColor);
        }

        color =  TagColor.encodeRGB(goodBgColor);
        }
        else{
            color = aColor;
        }*/
        color = aColor;
        arrowHead = aArrowHead;
        labels = aLabels;
        type = aType;
        targets = aTargets;
        dashArray = aDashArray;
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

    public List<String> getTargets()
    {
        return targets;
    }

    public void setTargets(List<String> aTargets)
    {
        targets = aTargets;
    }

    public String getDashArray()
    {
        return dashArray;
    }

    public void setDashArray(String aDashArray)
    {
        dashArray = aDashArray;
    }
}
