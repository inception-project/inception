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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.tudarmstadt.ukp.clarin.webanno.model.ScriptDirection;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * All required contents of a project to be exported.
 */
@JsonPropertyOrder(value = { "name", "slug", "description", "mode", "version" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExportedProject
{
    @JsonProperty(value = "name", required = true)
    private String name;

    @JsonProperty("slug")
    private String slug;

    @JsonProperty("description")
    private String description;

    @JsonProperty(value = "mode", required = true)
    private String mode;

    @JsonProperty("source_documents")
    private List<ExportedSourceDocument> sourceDocuments = new ArrayList<>();

    @JsonProperty("annotation_documents")
    private List<ExportedAnnotationDocument> annotationDocuments = new ArrayList<>();

    @JsonProperty("project_permissions")
    private List<ExportedProjectPermission> projectPermissions = new ArrayList<>();

    @JsonProperty("tag_sets")
    private List<ExportedTagSet> tagSets = new ArrayList<>();

    @JsonProperty("layers")
    private List<ExportedAnnotationLayer> layers = new ArrayList<>();

    @JsonProperty("version")
    private int version;

    @JsonProperty("disableExport")
    private boolean disableExport;

    @JsonProperty("script_direction")
    private ScriptDirection scriptDirection;

    @JsonProperty("created")
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

    @JsonProperty("updated")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updated;

    @JsonProperty("anonymous_curation")
    private boolean anonymousCuration;

    private Map<String, Object> properties = new HashMap<>();

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getSlug()
    {
        return slug;
    }

    public void setSlug(String aSlug)
    {
        slug = aSlug;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public List<ExportedSourceDocument> getSourceDocuments()
    {
        return sourceDocuments;
    }

    public void setSourceDocuments(List<ExportedSourceDocument> sourceDocuments)
    {
        this.sourceDocuments = sourceDocuments;
    }

    public List<ExportedAnnotationDocument> getAnnotationDocuments()
    {
        return annotationDocuments;
    }

    public void setAnnotationDocuments(List<ExportedAnnotationDocument> annotationDocuments)
    {
        this.annotationDocuments = annotationDocuments;
    }

    public List<ExportedProjectPermission> getProjectPermissions()
    {
        return projectPermissions;
    }

    public void setProjectPermissions(List<ExportedProjectPermission> projectPermissions)
    {
        this.projectPermissions = projectPermissions;
    }

    public List<ExportedTagSet> getTagSets()
    {
        return tagSets;
    }

    public void setTagSets(List<ExportedTagSet> tagSets)
    {
        this.tagSets = tagSets;
    }

    public String getMode()
    {
        return mode;
    }

    public void setMode(String aMode)
    {
        mode = aMode;
    }

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int aVersion)
    {
        version = aVersion;
    }

    public boolean isDisableExport()
    {
        return disableExport;
    }

    public void setDisableExport(boolean aDisableExport)
    {
        disableExport = aDisableExport;
    }

    public List<ExportedAnnotationLayer> getLayers()
    {
        return layers;
    }

    public void setLayers(List<ExportedAnnotationLayer> aLayers)
    {
        layers = aLayers;
    }

    public ScriptDirection getScriptDirection()
    {
        return scriptDirection;
    }

    public void setScriptDirection(ScriptDirection aScriptDirection)
    {
        scriptDirection = aScriptDirection;
    }

    public Date getCreated()
    {
        return created;
    }

    public void setCreated(Date aCreated)
    {
        created = aCreated;
    }

    public Date getUpdated()
    {
        return updated;
    }

    public void setUpdated(Date aUpdated)
    {
        updated = aUpdated;
    }

    public boolean isAnonymousCuration()
    {
        return anonymousCuration;
    }

    public void setAnonymousCuration(boolean aAnonymousCuration)
    {
        anonymousCuration = aAnonymousCuration;
    }

    @JsonAnySetter
    public void setProperty(String name, Object value)
    {
        properties.put(name, value);
    }

    /**
     * Get the value of the given property.
     * 
     * @param aName
     *            the property name.
     * @param aToValueType
     *            the value type.
     * @return the value.
     */
    public <T> Optional<T> getProperty(String aName, Class<T> aToValueType)
    {
        Object value = properties.get(aName);
        if (value != null) {
            return Optional.of(JSONUtil.getObjectMapper().convertValue(value, aToValueType));
        }
        else {
            return Optional.empty();
        }
    }

    /**
     * Get the value of the given property as an array. This method never returns {@code null}. If
     * the property is not set, an empty array is returned.
     * 
     * @param aName
     *            the property name.
     * @param aToValueType
     *            the array component type.
     * @return the array.
     */
    @SuppressWarnings("unchecked")
    public <T> T[] getArrayProperty(String aName, Class<T> aToValueType)
    {
        Object value = properties.get(aName);
        if (value != null) {
            ObjectMapper mapper = JSONUtil.getObjectMapper();
            return JSONUtil.getObjectMapper().convertValue(value,
                    mapper.getTypeFactory().constructArrayType(aToValueType));
        }
        else {
            return (T[]) Array.newInstance(aToValueType, 0);
        }
    }

    @JsonAnyGetter
    public Map<String, Object> getProperties()
    {
        return properties;
    }
}
