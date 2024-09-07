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
package de.tudarmstadt.ukp.inception.rendering.vmodel;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

public abstract class VObject
    implements Serializable
{
    private static final long serialVersionUID = -2362598130503053908L;

    private final AnnotationLayer layer;
    private final Map<String, String> features;
    private final int equivalenceSet;

    private VID vid;
    private String color;
    private String label;
    private double score;
    private boolean hideScore;
    private boolean actionButtons;
    private boolean placeholder;

    public VObject(AnnotationLayer aLayer, VID aVid, Map<String, String> aFeatures)
    {
        this(aLayer, aVid, -1, aFeatures);
    }

    public VObject(AnnotationLayer aLayer, VID aVid, int aEquivalenceSet,
            Map<String, String> aFeatures)
    {
        layer = aLayer;
        vid = aVid;
        features = aFeatures != null ? aFeatures : new HashMap<>();
        equivalenceSet = aEquivalenceSet;
    }

    public VID getVid()
    {
        return vid;
    }

    public void setVid(VID aVid)
    {
        vid = aVid;
    }

    public AnnotationLayer getLayer()
    {
        return layer;
    }

    public int getEquivalenceSet()
    {
        return equivalenceSet;
    }

    public Map<String, String> getFeatures()
    {
        return features;
    }

    public void setColorHint(String aColor)
    {
        color = aColor;
    }

    public String getColorHint()
    {
        return color;
    }

    public void setLabelHint(String aLabelHint)
    {
        label = aLabelHint;
    }

    public String getLabelHint()
    {
        return label;
    }

    public boolean isActionButtons()
    {
        return actionButtons;
    }

    public void setActionButtons(boolean aActionButtons)
    {
        actionButtons = aActionButtons;
    }

    public double getScore()
    {
        return score;
    }

    public void setScore(double aScore)
    {
        score = aScore;
    }

    public boolean isHideScore()
    {
        return hideScore;
    }

    public void setHideScore(boolean aHideScore)
    {
        hideScore = aHideScore;
    }

    public void setPlaceholder(boolean aPlaceholder)
    {
        placeholder = aPlaceholder;
    }

    public boolean isPlaceholder()
    {
        return placeholder;
    }
}
