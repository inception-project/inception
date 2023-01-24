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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_EMPTY)
public class CompactAnnotationAttributes
{
    public static final String ATTR_LABEL = "l";
    public static final String ATTR_COLOR = "c";
    public static final String ATTR_COMMENTS = "cm";

    private @JsonProperty(ATTR_LABEL) String labelText;
    private @JsonProperty(ATTR_COLOR) String color;
    private @JsonProperty(ATTR_COMMENTS) List<CompactComment> comments;

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
