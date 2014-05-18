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

import java.util.Date;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;

/**
 * Source document information to be exported/imported
 *
 * @author Seid Muhie Yimam
 *
 */
@JsonPropertyOrder(value = { "name", "format", "state" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class SourceDocument
{
    @JsonProperty("name")
    String name;

    @JsonProperty("format")
    String format;

    @JsonProperty("state")
    SourceDocumentState state;

    @JsonProperty("timestamp")
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    @JsonProperty("sentence_accessed")
    private int sentenceAccessed = 0;

    @JsonProperty("training_document")
    private boolean trainingDocument = false;

    @JsonProperty("processed")
    private boolean processed = false;

    @JsonProperty("feature")
    AnnotationFeature feature;

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

    public SourceDocumentState getState()
    {
        return state;
    }

    public void setState(SourceDocumentState state)
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

    public boolean isTrainingDocument()
    {
        return trainingDocument;
    }

    public void setTrainingDocument(boolean trainingDocument)
    {
        this.trainingDocument = trainingDocument;
    }

    public boolean isProcessed()
    {
        return processed;
    }

    public void setProcessed(boolean processed)
    {
        this.processed = processed;
    }

    public AnnotationFeature getFeature()
    {
        return feature;
    }

    public void setFeature(AnnotationFeature feature)
    {
        this.feature = feature;
    }

}
