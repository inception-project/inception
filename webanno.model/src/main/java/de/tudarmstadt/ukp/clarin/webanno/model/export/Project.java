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

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

import de.tudarmstadt.ukp.clarin.webanno.model.Mode;

/**
 * All required contents of a project to be exported.
 * @author Seid Muhie Yimam
 *
 */

@JsonPropertyOrder(value = { "name", "description","mode","version"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Project
{
    @JsonProperty("name")
    String name;
    @JsonProperty("description")
    String description;

    @JsonProperty("mode")
    private Mode mode = Mode.ANNOTATION;

    @JsonProperty("source_documents")
    private List<SourceDocument> sourceDocuments;

    @JsonProperty("annotation_documents")
    private List<AnnotationDocument> annotationDocuments;


    @JsonProperty("project_permissions")
    private List<ProjectPermission> projectPermissions ;

    @JsonProperty("tag_sets")
    private List<TagSet> tagSets = new ArrayList<TagSet>();

    @JsonProperty("layers")
    private List<AnnotationLayer> layers;

    @JsonProperty("mira_templates")
    private List<MiraTemplate> miraTemplates;
    
    @JsonProperty("crowd_jobs")
    private List<CrowdJob> crowdJobs;


    @JsonProperty("version")
    private int version;

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

    public List<SourceDocument> getSourceDocuments()
    {
        return sourceDocuments;
    }

    public void setSourceDocuments(List<SourceDocument> sourceDocuments)
    {
        this.sourceDocuments = sourceDocuments;
    }

    public List<AnnotationDocument> getAnnotationDocuments()
    {
        return annotationDocuments;
    }

    public void setAnnotationDocuments(List<AnnotationDocument> annotationDocuments)
    {
        this.annotationDocuments = annotationDocuments;
    }

    public List<ProjectPermission> getProjectPermissions()
    {
        return projectPermissions;
    }

    public void setProjectPermissions(List<ProjectPermission> projectPermissions)
    {
        this.projectPermissions = projectPermissions;
    }

    public List<TagSet> getTagSets()
    {
        return tagSets;
    }

    public void setTagSets(List<TagSet> tagSets)
    {
        this.tagSets = tagSets;
    }

    public Mode getMode()
    {
        return mode;
    }

    public void setMode(Mode mode)
    {
        this.mode = mode;
    }

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int version)
    {
        this.version = version;
    }

    public List<AnnotationLayer> getLayers()
    {
        return layers;
    }

    public void setLayers(List<AnnotationLayer> layers)
    {
        this.layers = layers;
    }

    public List<MiraTemplate> getMiraTemplates()
    {
        return miraTemplates;
    }

    public void setMiraTemplates(List<MiraTemplate> miraTemplates)
    {
        this.miraTemplates = miraTemplates;
    }

	public List<CrowdJob> getCrowdJobs() {
		return crowdJobs;
	}

	public void setCrowdJobs(List<CrowdJob> crowdJobs) {
		this.crowdJobs = crowdJobs;
	}

}
