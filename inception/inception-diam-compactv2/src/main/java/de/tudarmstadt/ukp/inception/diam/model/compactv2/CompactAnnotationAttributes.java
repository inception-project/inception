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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonInclude(Include.NON_EMPTY)
public class CompactAnnotationAttributes
{
    public static final String ATTR_LABEL = "l";
    public static final String ATTR_COLOR = "c";
    public static final String ATTR_COMMENTS = "cm";
    public static final String ATTR_SCORE = "s";
    public static final String ATTR_HIDE_SCORE = "hs";

    private @JsonProperty(ATTR_LABEL) String labelText;
    private @JsonProperty(ATTR_COLOR) String color;
    private @JsonProperty(ATTR_COMMENTS) List<CompactComment> comments;
    private @JsonProperty(ATTR_SCORE) double score;
    private @JsonProperty(ATTR_HIDE_SCORE) boolean hideScore;

    @JsonInclude(Include.NON_DEFAULT)
    @JsonSerialize(using = ScoreSerializer.class)
    public double getScore()
    {
        return score;
    }

    public void setScore(double aScore)
    {
        score = aScore;
    }

    @JsonFormat(shape = Shape.NUMBER)
    @JsonInclude(Include.NON_DEFAULT)
    public boolean isHideScore()
    {
        return hideScore;
    }

    public void setHideScore(boolean aHideScore)
    {
        hideScore = aHideScore;
    }

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

    public List<CompactComment> getComments()
    {
        if (comments == null) {
            comments = new ArrayList<>();
        }

        return comments;
    }

    public void setComments(List<CompactComment> aComments)
    {
        comments = aComments;
    }
}
