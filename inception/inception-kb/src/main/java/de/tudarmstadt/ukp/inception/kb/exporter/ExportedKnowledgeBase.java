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
package de.tudarmstadt.ukp.inception.kb.exporter;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(alphabetic = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExportedKnowledgeBase
{
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private String type;

    @JsonProperty("class_iri")
    private String classIri;

    @JsonProperty("subclass_iri")
    private String subclassIri;

    @JsonProperty("type_iri")
    private String typeIri;

    @JsonProperty("description_iri")
    private String descriptionIri;

    @JsonProperty("label_iri")
    private String labelIri;

    @JsonProperty("property_type_iri")
    private String propertyTypeIri;

    @JsonProperty("property_label_iri")
    private String propertyLabelIri;

    @JsonProperty("property_description_iri")
    private String propertyDescriptionIri;

    @JsonProperty("deprecation_property_iri")
    private String deprecationPropertyIri;

    @JsonProperty("full_text_search_iri")
    private String fullTextSearchIri;

    @JsonProperty("read_only")
    private boolean readOnly;

    @JsonProperty("use_fuzzy")
    private boolean useFuzzy;

    @JsonProperty("enabled")
    private boolean enabled;

    @JsonProperty("reification")
    private String reification;

    @JsonProperty("support_concept_linking")
    private boolean supportConceptLinking;

    @JsonProperty("base_prefix")
    private String basePrefix;

    @JsonProperty("root_concepts")
    private List<String> rootConcepts;

    @JsonProperty("additional_matching_properties")
    private List<String> additionalMatchingProperties;

    @JsonProperty("additional_languages")
    private List<String> additionalLanguages;

    @JsonProperty("default_language")
    private String defaultLanguage;

    @JsonProperty("default_dataset_iri")
    private String defaultDatasetIri;

    @JsonProperty("max_results")
    private int maxResults;

    /**
     * The IRI for a property describing B being a subproperty of A
     */
    @JsonProperty("sub_Property_IRI")
    private String subPropertyIri;

    // set to null for local knowledge bases
    @JsonProperty("remote_url")
    private String remoteURL;

    @JsonProperty("traits")
    private String traits;

    public String getId()
    {
        return id;
    }

    public void setId(String aId)
    {
        id = aId;
    }

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

    public String getPropertyLabelIri()
    {
        return propertyLabelIri;
    }

    public void setPropertyLabelIri(String aPropertyLabelIri)
    {
        propertyLabelIri = aPropertyLabelIri;
    }

    public String getPropertyDescriptionIri()
    {
        return propertyDescriptionIri;
    }

    public void setPropertyDescriptionIri(String aPropertyDescriptionIri)
    {
        propertyDescriptionIri = aPropertyDescriptionIri;
    }

    public String getFullTextSearchIri()
    {
        return fullTextSearchIri;
    }

    public void setFullTextSearchIri(String aFullTextSearchIri)
    {
        fullTextSearchIri = aFullTextSearchIri;
    }

    public String getDeprecationPropertyIri()
    {
        return deprecationPropertyIri;
    }

    public void setDeprecationPropertyIri(String aDeprecationPropertyIri)
    {
        deprecationPropertyIri = aDeprecationPropertyIri;
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

    @Deprecated
    public void setSupportConceptLinking(boolean aSupportConceptLinking)
    {
        supportConceptLinking = aSupportConceptLinking;
    }

    @Deprecated
    public boolean isSupportConceptLinking()
    {
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

    public List<String> getRootConcepts()
    {
        return rootConcepts;
    }

    public void setRootConcepts(List<String> aRootConcepts)
    {
        rootConcepts = aRootConcepts;
    }

    public void setAdditionalMatchingProperties(List<String> aAdditionalMatchingProperties)
    {
        additionalMatchingProperties = aAdditionalMatchingProperties;
    }

    public List<String> getAdditionalMatchingProperties()
    {
        return additionalMatchingProperties;
    }

    public void setAdditionalLanguages(List<String> aAdditionalLanguages)
    {
        additionalLanguages = aAdditionalLanguages;
    }

    public List<String> getAdditionalLanguages()
    {
        return additionalLanguages;
    }

    public String getDefaultLanguage()
    {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String aDefaultLanguage)
    {
        defaultLanguage = aDefaultLanguage;
    }

    public int getMaxResults()
    {
        return maxResults;
    }

    public void setMaxResults(int aMaxResults)
    {
        maxResults = aMaxResults;
    }

    public String getSubPropertyIri()
    {
        return subPropertyIri;
    }

    public void setSubPropertyIri(String subPropertyIri)
    {
        this.subPropertyIri = subPropertyIri;
    }

    public String getRemoteURL()
    {
        return remoteURL;
    }

    public void setRemoteURL(String aRemoteURL)
    {
        remoteURL = aRemoteURL;
    }

    public String getDefaultDatasetIri()
    {
        return defaultDatasetIri;
    }

    public void setDefaultDatasetIri(String aDefaultDatasetIri)
    {
        defaultDatasetIri = aDefaultDatasetIri;
    }

    public void setUseFuzzy(boolean aUseFuzzy)
    {
        useFuzzy = aUseFuzzy;
    }

    public boolean isUseFuzzy()
    {
        return useFuzzy;
    }

    public String getTraits()
    {
        return traits;
    }

    public void setTraits(String aTraits)
    {
        traits = aTraits;
    }
}
