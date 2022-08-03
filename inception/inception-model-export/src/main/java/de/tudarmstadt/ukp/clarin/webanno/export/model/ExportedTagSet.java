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

import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * All required contents of a tagset to be exported. The tagsets to be exported are those created
 * for a project, hence project specific.
 */
@JsonPropertyOrder(value = { "name", "description", "language", "typeName", "tags", "createTag" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExportedTagSet
{
    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    /**
     * @deprecated Still kept for backwards compatibility with WebAnno prior to 2.0! during import.
     *             The property is only used for deserialization/import but not for serialization
     *             /export!
     */
    @Deprecated
    @JsonProperty(value = "type_name", access = WRITE_ONLY)
    private String typeName;

    @JsonProperty("language")
    private String language;

    @JsonProperty("tags")
    private List<ExportedTag> tags = new ArrayList<>();

    @JsonProperty("create_tag")
    private boolean createTag;

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

    /**
     * @deprecated Still kept for backwards compatibility with WebAnno prior to 2.0! during import.
     *             The property is only used for deserialization/import but not for serialization
     *             /export!
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    public String getTypeName()
    {
        return typeName;
    }

    /**
     * @deprecated Still kept for backwards compatibility with WebAnno prior to 2.0! during import.
     *             The property is only used for deserialization/import but not for serialization
     *             /export!
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    public void setTypeName(String aTypeName)
    {
        typeName = aTypeName;
    }

    public List<ExportedTag> getTags()
    {
        return tags;
    }

    public void setTags(List<ExportedTag> aTags)
    {
        tags = aTags;
    }

    public boolean isCreateTag()
    {
        return createTag;
    }

    public void setCreateTag(boolean createTag)
    {
        this.createTag = createTag;
    }
}
