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
package de.tudarmstadt.ukp.clarin.webanno.automation.service.export.model;

import java.util.Date;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationFeatureReference;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.TrainDocumentState;

/**
 * Source document information to be exported/imported.
 */
@JsonPropertyOrder(value = { "name", "format", "state" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExportedTrainingDocument
{
    @JsonProperty("name")
    private String name;

    @JsonProperty("format")
    private String format;
    
    @ManyToOne
    @JoinColumn(name = "project")
    private Project project;

    @JsonProperty("state")
    private TrainDocumentState state;

    @JsonProperty("timestamp")
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    @JsonProperty("sentence_accessed")
    private int sentenceAccessed = 0;

    @JsonProperty("processed")
    private boolean processed = false;

    @JsonProperty("feature")
    private ExportedAnnotationFeatureReference feature;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getFormat()
    {
        return format;
    }

    public void setFormat(String format)
    {
        this.format = format;
    }

    public TrainDocumentState getState()
    {
        return state;
    }

    public void setState(TrainDocumentState state)
    {
        this.state = state;
    }

    public Date getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(Date timestamp)
    {
        this.timestamp = timestamp;
    }

    public int getSentenceAccessed()
    {
        return sentenceAccessed;
    }

    public void setSentenceAccessed(int sentenceAccessed)
    {
        this.sentenceAccessed = sentenceAccessed;
    }

    public boolean isProcessed()
    {
        return processed;
    }

    public void setProcessed(boolean processed)
    {
        this.processed = processed;
    }

    public ExportedAnnotationFeatureReference getFeature()
    {
        return feature;
    }

    public void setFeature(ExportedAnnotationFeatureReference feature)
    {
        this.feature = feature;
    }

    public Project getProject()
    {
        return project;
    }

    public void setProject(Project project)
    {
        this.project = project;
    }
}
