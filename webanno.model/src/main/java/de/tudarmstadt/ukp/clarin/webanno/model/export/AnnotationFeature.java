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
package de.tudarmstadt.ukp.clarin.webanno.model.export;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * All required contents of {@link de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature} to be
 * exported.
 *
 * @author Seid Muhie Yimam
 *
 */


@JsonIgnoreProperties(ignoreUnknown = true)
public class AnnotationFeature
{
    @JsonProperty("name")
    String name;

    @JsonProperty("tag_set")
    private TagSet tagSet;

    @JsonProperty("uiName")
    private String uiName;

    @JsonProperty("type")
    private String type;

    @JsonProperty("enabled")
    private boolean enabled = true;

    @JsonProperty("visible")
    private boolean visible = false;

    @JsonProperty("description")
    private String description;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getUiName()
    {
        return uiName;
    }

    public void setUiName(String uiName)
    {
        this.uiName = uiName;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public boolean isVisible()
    {
        return visible;
    }

    public void setVisible(boolean visible)
    {
        this.visible = visible;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public TagSet getTagSet()
    {
        return tagSet;
    }

    public void setTagSet(TagSet tagSet)
    {
        this.tagSet = tagSet;
    }

}
