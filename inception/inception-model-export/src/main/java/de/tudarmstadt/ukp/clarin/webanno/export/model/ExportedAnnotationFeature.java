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
package de.tudarmstadt.ukp.clarin.webanno.export.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;

/**
 * All required contents of {@link de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature} to be
 * exported.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExportedAnnotationFeature
{
    @JsonProperty("name")
    private String name;

    @JsonProperty("tag_set")
    private ExportedTagSet tagSet;

    @JsonProperty("uiName")
    private String uiName;

    @JsonProperty("type")
    private String type;

    @JsonProperty("enabled")
    private boolean enabled = true;

    @JsonProperty("visible")
    private boolean visible = false;

    @JsonProperty("include_in_hover")
    private boolean includeInHover = false;

    @JsonProperty("required")
    private boolean required = false;

    @JsonProperty("remember")
    private boolean remember = false;

    @JsonProperty("hideUnconstraintFeature")
    private boolean hideUnconstraintFeature = false;

    @JsonProperty("description")
    private String description;

    @JsonIgnore
    private Long projectId;

    @JsonProperty("multi_value_mode")
    private MultiValueMode multiValueMode;

    @JsonProperty("link_mode")
    private LinkMode linkMode;

    @JsonProperty("link_type_name")
    private String linkTypeName;

    @JsonProperty("link_type_role_feature_name")
    private String linkTypeRoleFeatureName;

    @JsonProperty("link_type_target_feature_name")
    private String linkTypeTargetFeatureName;

    @JsonProperty("traits")
    private String traits;

    @JsonProperty("curatable")
    private boolean curatable = true;

    @JsonProperty("rank")
    private int rank = 0;

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

    public boolean isIncludeInHover()
    {
        return includeInHover;
    }

    public void setIncludeInHover(boolean includeInHover)
    {
        this.includeInHover = includeInHover;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public ExportedTagSet getTagSet()
    {
        return tagSet;
    }

    public void setTagSet(ExportedTagSet tagSet)
    {
        this.tagSet = tagSet;
    }

    public Long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(Long aProjectId)
    {
        projectId = aProjectId;
    }

    public MultiValueMode getMultiValueMode()
    {
        return multiValueMode;
    }

    public void setMultiValueMode(MultiValueMode aMultiValueMode)
    {
        multiValueMode = aMultiValueMode;
    }

    public LinkMode getLinkMode()
    {
        return linkMode;
    }

    public void setLinkMode(LinkMode aLinkMode)
    {
        linkMode = aLinkMode;
    }

    public String getLinkTypeName()
    {
        return linkTypeName;
    }

    public void setLinkTypeName(String aLinkTypeName)
    {
        linkTypeName = aLinkTypeName;
    }

    public String getLinkTypeRoleFeatureName()
    {
        return linkTypeRoleFeatureName;
    }

    public void setLinkTypeRoleFeatureName(String aLinkTypeRoleFeatureName)
    {
        linkTypeRoleFeatureName = aLinkTypeRoleFeatureName;
    }

    public String getLinkTypeTargetFeatureName()
    {
        return linkTypeTargetFeatureName;
    }

    public void setLinkTypeTargetFeatureName(String aLinkTypeTargetFeatureName)
    {
        linkTypeTargetFeatureName = aLinkTypeTargetFeatureName;
    }

    public boolean isRequired()
    {
        return required;
    }

    public void setRequired(boolean aRequired)
    {
        required = aRequired;
    }

    public boolean isRemember()
    {
        return remember;
    }

    public void setRemember(boolean aRemember)
    {
        remember = aRemember;
    }

    public boolean isHideUnconstraintFeature()
    {
        return hideUnconstraintFeature;
    }

    public void setHideUnconstraintFeature(boolean hideUnconstraintFeature)
    {
        this.hideUnconstraintFeature = hideUnconstraintFeature;
    }

    public String getTraits()
    {
        return traits;
    }

    public void setTraits(String aTraits)
    {
        traits = aTraits;
    }

    public boolean isCuratable()
    {
        return curatable;
    }

    public void setCuratable(boolean aCuratable)
    {
        curatable = aCuratable;
    }

    public int getRank()
    {
        return rank;
    }

    public void setRank(int aRank)
    {
        rank = aRank;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof ExportedAnnotationFeature)) {
            return false;
        }
        var castOther = (ExportedAnnotationFeature) other;
        return new EqualsBuilder() //
                .append(name, castOther.name) //
                .append(type, castOther.type) //
                .append(projectId, castOther.projectId) //
                .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder() //
                .append(name) //
                .append(type) //
                .append(projectId) //
                .toHashCode();
    }
}
