/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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
 * All required contents of a project to be exported.
 * @author Seid Muhie Yimam
 *
 */
@JsonPropertyOrder(value = { "name", "description", "reverse", "type", "typeName",
        "typeDescription" ,"tags" })
public class Project
{
    @JsonProperty("name")
    String name;
    @JsonProperty("description")
    String description;
    @JsonProperty("reverse")
    boolean reverse;

    @JsonProperty("source_documents")
    private List<SourceDocument> sourceDocuments = new ArrayList<SourceDocument>();

    @JsonProperty("annotation_documents")
    private List<AnnotationDocument> annotationDocuments = new ArrayList<AnnotationDocument>();


    @JsonProperty("project_permissions")
    private List<ProjectPermission> projectPermissions = new ArrayList<ProjectPermission>();

    @JsonProperty("tag_sets")
    private List<TagSet> tagSets = new ArrayList<TagSet>();

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

    public boolean isReverse()
    {
        return reverse;
    }

    public void setReverse(boolean reverse)
    {
        this.reverse = reverse;
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

}
