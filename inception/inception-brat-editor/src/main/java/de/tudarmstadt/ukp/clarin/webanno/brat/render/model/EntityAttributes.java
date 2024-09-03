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

import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.tudarmstadt.ukp.inception.support.json.NumericBooleanSerializer;

/**
 * This is not part of the original brat data model.
 */
@JsonInclude(Include.NON_DEFAULT)
public class EntityAttributes
{
    public static final String VALUE_CLIPPED_AT_START = "s";
    public static final String VALUE_CLIPPED_AT_END = "e";

    public static final String ATTR_LABEL = "l";
    public static final String ATTR_COLOR = "c";
    public static final String ATTR_HOVER_TEXT = "h";
    public static final String ATTR_ACTION_BUTTONS = "a";
    public static final String ATTR_CLIPPED = "cl";
    public static final String ATTR_SCORE = "s";

    private @JsonProperty(ATTR_LABEL) String labelText;
    private @JsonProperty(ATTR_COLOR) String color;
    private @JsonProperty(ATTR_HOVER_TEXT) String hoverText;
    @JsonSerialize(using = NumericBooleanSerializer.class)
    private @JsonProperty(ATTR_ACTION_BUTTONS) boolean actionButtons;
    private @JsonProperty(ATTR_CLIPPED) String clipped;

    @JsonInclude(Include.NON_DEFAULT)
    @JsonSerialize(using = ScoreSerializer.class)
    private @JsonProperty(ATTR_SCORE) double score;

    public void setLabelText(String aLabelText)
    {
        labelText = aLabelText;
    }

    public String getLabelText()
    {
        return labelText;
    }

    public String getColor()
    {
        return color;
    }

    public void setColor(String aColor)
    {
        color = aColor;
    }

    public void setHoverText(String aHovertext)
    {
        hoverText = aHovertext;
    }

    public String getHoverText()
    {
        return hoverText;
    }

    public Boolean isActionButtons()
    {
        return actionButtons;
    }

    public void setActionButtons(Boolean aActionButtons)
    {
        actionButtons = aActionButtons;
    }

    public void setClippedAtStart(boolean aFlag)
    {
        if (clipped == null) {
            if (!aFlag) {
                return;
            }

            clipped = VALUE_CLIPPED_AT_START;
            return;
        }

        if (aFlag) {
            if (clipped.startsWith(VALUE_CLIPPED_AT_START)) {
                return;
            }

            clipped = VALUE_CLIPPED_AT_START + clipped;
        }
        else {
            if (!clipped.startsWith(VALUE_CLIPPED_AT_START)) {
                return;
            }

            clipped = removeStart(clipped, VALUE_CLIPPED_AT_START);
        }
    }

    public void setClippedAtEnd(boolean aFlag)
    {
        if (clipped == null) {
            if (!aFlag) {
                return;
            }

            clipped = VALUE_CLIPPED_AT_END;
            return;
        }

        if (aFlag) {
            if (clipped.endsWith(VALUE_CLIPPED_AT_END)) {
                return;
            }

            clipped = clipped + VALUE_CLIPPED_AT_END;
        }
        else {
            if (!clipped.endsWith(VALUE_CLIPPED_AT_END)) {
                return;
            }

            clipped = removeEnd(clipped, VALUE_CLIPPED_AT_END);
        }
    }

    public String getClipped()
    {
        return clipped;
    }

    public void setClipped(String aClipped)
    {
        clipped = aClipped;
    }

    public void setScore(double aScore)
    {
        score = aScore;
    }

    public double getScore()
    {
        return score;
    }
}
