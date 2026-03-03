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

import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode;
import de.tudarmstadt.ukp.clarin.webanno.model.ValidationMode;

/**
 * All required contents of a project to be exported.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExportedAnnotationLayer
{
    @JsonProperty("name")
    private String name;

    @JsonProperty("features")
    private List<ExportedAnnotationFeature> features;

    @JsonProperty("uiName")
    private String uiName;

    @JsonProperty("type")
    private String type;

    @JsonProperty("description")
    private String description;

    @JsonProperty("enabled")
    private boolean enabled = true;

    @JsonProperty("built_in")
    private boolean builtIn = false;

    @JsonProperty("readonly")
    private boolean readonly = false;

    @JsonProperty("attach_type")
    private ExportedAnnotationLayerReference attachType;

    @JsonProperty("attach_feature")
    private ExportedAnnotationFeatureReference attachFeature;

    @JsonProperty("allow_stacking")
    private boolean allowStacking;

    @JsonProperty("cross_sentence")
    private boolean crossSentence;

    @JsonProperty("show_hover")
    private boolean showTextInHover = true;

    @JsonProperty("anchoring_mode")
    private AnchoringMode anchoringMode;

    @JsonProperty("overlap_mode")
    private OverlapMode overlapMode;

    @JsonProperty("validation_mode")
    private ValidationMode validationMode;

    @Deprecated
    @JsonProperty("lock_to_token_offset")
    private boolean lockToTokenOffset = true;

    @Deprecated
    @JsonProperty("multiple_tokens")
    private boolean multipleTokens;

    @JsonIgnore
    private Long projectId;

    @JsonProperty("linked_list_behavior")
    private boolean linkedListBehavior;

    @JsonProperty("on_click_javascript_action")
    private String onClickJavascriptAction;

    @JsonProperty("traits")
    private String traits;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public List<ExportedAnnotationFeature> getFeatures()
    {
        return features;
    }

    public void setFeatures(List<ExportedAnnotationFeature> features)
    {
        this.features = features;
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

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public boolean isBuiltIn()
    {
        return builtIn;
    }

    public void setBuiltIn(boolean builtIn)
    {
        this.builtIn = builtIn;
    }

    public ExportedAnnotationLayerReference getAttachType()
    {
        return attachType;
    }

    public void setAttachType(ExportedAnnotationLayerReference attachType)
    {
        this.attachType = attachType;
    }

    public ValidationMode getValidationMode()
    {
        return validationMode;
    }

    public void setValidationMode(ValidationMode aValidationMode)
    {
        validationMode = aValidationMode;
    }

    public void setAnchoringMode(AnchoringMode aAnchoringMode)
    {
        anchoringMode = aAnchoringMode;
    }

    public AnchoringMode getAnchoringMode()
    {
        return anchoringMode;
    }

    public OverlapMode getOverlapMode()
    {
        return overlapMode;
    }

    public void setOverlapMode(OverlapMode aOverlapMode)
    {
        overlapMode = aOverlapMode;
    }

    /**
     * @deprecated Superseded by {@link ExportedAnnotationLayer#getAnchoringMode()} but kept around
     *             for the time being to enable backwards compatibility of exported projects with
     *             older versions of WebAnno.
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    public boolean isLockToTokenOffset()
    {
        return lockToTokenOffset;
    }

    /**
     * @deprecated Superseded by {@link ExportedAnnotationLayer#setAnchoringMode(AnchoringMode)} but
     *             kept around for the time being to enable backwards compatibility of exported
     *             projects with older versions of WebAnno.
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    public void setLockToTokenOffset(boolean lockToTokenOffset)
    {
        this.lockToTokenOffset = lockToTokenOffset;
    }

    /**
     * @deprecated Superseded by {@link ExportedAnnotationLayer#getOverlapMode} but kept around for
     *             the time being to enable backwards compatibility of exported projects with older
     *             versions of WebAnno.
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    public boolean isAllowStacking()
    {
        return allowStacking;
    }

    /**
     * @deprecated Superseded by {@link ExportedAnnotationLayer#setOverlapMode} but kept around for
     *             the time being to enable backwards compatibility of exported projects with older
     *             versions of WebAnno.
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    public void setAllowStacking(boolean allowStacking)
    {
        this.allowStacking = allowStacking;
    }

    public boolean isCrossSentence()
    {
        return crossSentence;
    }

    public void setCrossSentence(boolean crossSentence)
    {
        this.crossSentence = crossSentence;
    }

    public boolean isShowTextInHover()
    {
        return showTextInHover;
    }

    public void setShowTextInHover(boolean showTextInHover)
    {
        this.showTextInHover = showTextInHover;
    }

    /**
     * @deprecated Superseded by {@link ExportedAnnotationLayer#getAnchoringMode()} but kept around
     *             for the time being to enable backwards compatibility of exported projects with
     *             older versions of WebAnno.
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    public boolean isMultipleTokens()
    {
        return multipleTokens;
    }

    /**
     * @deprecated Superseded by {@link ExportedAnnotationLayer#setAnchoringMode(AnchoringMode)} but
     *             kept around for the time being to enable backwards compatibility of exported
     *             projects with older versions of WebAnno.
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    public void setMultipleTokens(boolean multipleTokens)
    {
        this.multipleTokens = multipleTokens;
    }

    public ExportedAnnotationFeatureReference getAttachFeature()
    {
        return attachFeature;
    }

    public void setAttachFeature(ExportedAnnotationFeatureReference attachFeature)
    {
        this.attachFeature = attachFeature;
    }

    public Long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(Long aProjectId)
    {
        this.projectId = aProjectId;
    }

    public boolean isLinkedListBehavior()
    {
        return linkedListBehavior;
    }

    public void setLinkedListBehavior(boolean aLinkedListBehavior)
    {
        linkedListBehavior = aLinkedListBehavior;
    }

    public boolean isReadonly()
    {
        return readonly;
    }

    public void setReadonly(boolean aReadonly)
    {
        readonly = aReadonly;
    }

    public String getOnClickJavascriptAction()
    {
        return onClickJavascriptAction;
    }

    public void setOnClickJavascriptAction(String onClickAction)
    {
        this.onClickJavascriptAction = onClickAction;
    }

    public String getTraits()
    {
        return traits;
    }

    public void setTraits(String aTraits)
    {
        traits = aTraits;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof ExportedAnnotationLayer)) {
            return false;
        }
        ExportedAnnotationLayer castOther = (ExportedAnnotationLayer) other;
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
