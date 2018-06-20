/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.kb.exporter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(alphabetic = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExportedKnowledgeBase
{    
    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private String type;
    
    @JsonProperty("classIri")
    private String classIri;

    @JsonProperty("subclassIri")
    private String subclassIri;

    @JsonProperty("typeIri")
    private String typeIri;

    @JsonProperty("descriptionIri")
    private String descriptionIri;

    @JsonProperty("labelIri")
    private String labelIri;

    @JsonProperty("propertyTypeIri")
    private String propertyTypeIri;

    @JsonProperty("readOnly")
    private boolean readOnly;

    @JsonProperty("enabled")
    private boolean enabled;

    @JsonProperty("reification")
    private String reification;
    
    @JsonProperty("supportConceptLinking")
    private boolean supportConceptLinking;
    
    @JsonProperty("basePrefix")
    private String basePrefix;
    
    // set to null for local knowledge bases
    @JsonProperty("remoteURL")
    private String remoteURL;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getClassIri()
    {
        return classIri;
    }

    public void setClassIri(String aClassIri)
    {
        classIri = aClassIri;
    }

    public String getSubclassIri()
    {
        return subclassIri;
    }

    public void setSubclassIri(String aSubclassIri)
    {
        subclassIri = aSubclassIri;
    }

    public String getTypeIri()
    {
        return typeIri;
    }

    public void setTypeIri(String aTypeIri)
    {
        typeIri = aTypeIri;
    }

    public String getDescriptionIri()
    {
        return descriptionIri;
    }

    public void setDescriptionIri(String aDescriptionIri)
    {
        descriptionIri = aDescriptionIri;
    }

    public String getLabelIri()
    {
        return labelIri;
    }

    public void setLabelIri(String aLabelIri)
    {
        labelIri = aLabelIri;
    }

    public String getPropertyTypeIri()
    {
        return propertyTypeIri;
    }

    public void setPropertyTypeIri(String aPropertyTypeIri)
    {
        propertyTypeIri = aPropertyTypeIri;
    }

    public boolean isReadOnly()
    {
        return readOnly;
    }

    public void setReadOnly(boolean isReadOnly)
    {
        readOnly = isReadOnly;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean isEnabled)
    {
        enabled = isEnabled;
    }

    public String getReification()
    {
        return reification;
    }

    public void setReification(String aReification)
    {
        reification = aReification;
    }
    
    public void setSupportConceptLinking(boolean aSupportConceptLinking) {
        supportConceptLinking = aSupportConceptLinking;
    }
    
    public boolean isSupportConceptLinking() {
        return supportConceptLinking;
    }
    
    public String getBasePrefix()
    {
        return basePrefix;
    }

    public void setBasePrefix(String aBasePrefix)
    {
        basePrefix = aBasePrefix;
    }
    
    public String getRemoteURL()
    {
        return remoteURL;
    }

    public void setRemoteURL(String aRemoteURL)
    {
        remoteURL = aRemoteURL;
    }
}
