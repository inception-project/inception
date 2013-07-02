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
package de.tudarmstadt.ukp.clarin.webanno.export.model;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

/**
 * All required contents of a tagset to be exported. The tagsets to be exported are those
 * created for a project, hence project specific.
 * @author Seid Muhie Yimam
 *
 */
@JsonPropertyOrder(value = { "name", "description", "language", "type", "typeName",
        "typeDescription" ,"tags" })
public class TagSet
{
    @JsonProperty("name")
    String name;
    @JsonProperty("description")
    String description;
    @JsonProperty("language")
    String language;
    @JsonProperty("type")
    String type;
    @JsonProperty("type_name")
    String typeName;
    @JsonProperty("type_description")
    String typeDescription;
    @JsonProperty("tags")
    List<Tag> tags = new ArrayList<Tag>();
    public String getName()
    {
        return name;
    }
    public void setName(String aName)
    {
        name = aName;
    }
    public String getDescription()
    {
        return description;
    }
    public void setDescription(String aDescription)
    {
        description = aDescription;
    }
    public String getLanguage()
    {
        return language;
    }
    public void setLanguage(String aLanguage)
    {
        language = aLanguage;
    }
    public String getType()
    {
        return type;
    }
    public void setType(String aType)
    {
        type = aType;
    }
    public String getTypeName()
    {
        return typeName;
    }
    public void setTypeName(String aTypeName)
    {
        typeName = aTypeName;
    }
    public String getTypeDescription()
    {
        return typeDescription;
    }
    public void setTypeDescription(String aTypeDescription)
    {
        typeDescription = aTypeDescription;
    }
    public List<Tag> getTags()
    {
        return tags;
    }
    public void setTags(List<Tag> aTags)
    {
        tags = aTags;
    }

}
