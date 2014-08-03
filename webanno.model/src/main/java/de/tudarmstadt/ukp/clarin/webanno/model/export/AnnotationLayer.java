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

import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * All required contents of a project to be exported.
 *
 * @author Seid Muhie Yimam
 *
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class AnnotationLayer
{
    @JsonProperty("name")
    String name;

    @JsonProperty("features")
    private List<AnnotationFeature> features;

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

    @JsonProperty("attach_type")
    private AnnotationLayer attachType;

    @JsonProperty("attach_feature")
    private AnnotationFeature attachFeature;

    @JsonProperty("lock_to_token_offset")
    private boolean lockToTokenOffset = true;

    @JsonProperty("allow_stacking")
    private boolean allowStacking;

    @JsonProperty("cross_sentence")
    private boolean crossSentence;

    @JsonProperty("multiple_tokens")
    private boolean multipleTokens;
    
    @JsonProperty("project_name")
    private String projectName;

    @JsonProperty("linked_list_behavior")
    private boolean linkedListBehavior;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public List<AnnotationFeature> getFeatures()
    {
        return features;
    }

    public void setFeatures(List<AnnotationFeature> features)
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

    public AnnotationLayer getAttachType()
    {
        return attachType;
    }

    public void setAttachType(AnnotationLayer attachType)
    {
        this.attachType = attachType;
    }

    public boolean isLockToTokenOffset()
    {
        return lockToTokenOffset;
    }

    public void setLockToTokenOffset(boolean lockToTokenOffset)
    {
        this.lockToTokenOffset = lockToTokenOffset;
    }

    public boolean isAllowStacking()
    {
        return allowStacking;
    }

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

    public boolean isMultipleTokens()
    {
        return multipleTokens;
    }

    public void setMultipleTokens(boolean multipleTokens)
    {
        this.multipleTokens = multipleTokens;
    }

    public AnnotationFeature getAttachFeature()
    {
        return attachFeature;
    }

    public void setAttachFeature(AnnotationFeature attachFeature)
    {
        this.attachFeature = attachFeature;
    }

	public String isProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public boolean isLinkedListBehavior()
    {
        return linkedListBehavior;
    }

    public void setLinkedListBehavior(boolean aLinkedListBehavior)
    {
        linkedListBehavior = aLinkedListBehavior;
    }

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((projectName == null)?0:projectName.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AnnotationLayer other = (AnnotationLayer) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (projectName != other.projectName)
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}
}
