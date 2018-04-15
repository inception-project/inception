/*
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
 */
package de.tudarmstadt.ukp.clarin.webanno.export.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.tudarmstadt.ukp.clarin.webanno.model.ScriptDirection;

/**
 * All required contents of a project to be exported.
 */
@JsonPropertyOrder(value = { "name", "description", "mode", "version" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExportedProject
{
    @JsonProperty(value = "name", required = true)
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty(value = "mode", required = true)
    private String mode;

    @JsonProperty("source_documents")
    private List<ExportedSourceDocument> sourceDocuments;

    @JsonProperty("training_documents")
    private List<ExportedTrainingDocument> trainingDocuments;

    @JsonProperty("annotation_documents")
    private List<ExportedAnnotationDocument> annotationDocuments;

    @JsonProperty("project_permissions")
    private List<ExportedProjectPermission> projectPermissions;

    @JsonProperty("tag_sets")
    private List<ExportedTagSet> tagSets = new ArrayList<>();

    @JsonProperty("layers")
    private List<ExportedAnnotationLayer> layers;

    @JsonProperty("mira_templates")
    private List<ExportedMiraTemplate> miraTemplates = new ArrayList<>();

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
    
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
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

    public List<ExportedTrainingDocument> getTrainingDocuments()
    {
        return trainingDocuments;
    }

    public void setTrainingDocuments(List<ExportedTrainingDocument> trainingDocuments)
    {
        this.trainingDocuments = trainingDocuments;
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

    public void setVersion(int version)
    {
        this.version = version;
    }

    public List<ExportedAnnotationLayer> getLayers()
    {
        return layers;
    }

    public boolean isDisableExport()
    {
        return disableExport;
    }

    public void setDisableExport(boolean disableExport)
    {
        this.disableExport = disableExport;
    }

    public void setLayers(List<ExportedAnnotationLayer> layers)
    {
        this.layers = layers;
    }

    public List<ExportedMiraTemplate> getMiraTemplates()
    {
        return miraTemplates;
    }

    public void setMiraTemplates(List<ExportedMiraTemplate> miraTemplates)
    {
        this.miraTemplates = miraTemplates;
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
}
