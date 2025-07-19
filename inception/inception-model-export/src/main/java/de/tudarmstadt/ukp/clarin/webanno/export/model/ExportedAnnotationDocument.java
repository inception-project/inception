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

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * Annotation document information to be exported/imported
 */
@JsonPropertyOrder(value = { "name", "user", "state", "timestamp" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExportedAnnotationDocument
{
    @JsonProperty("name")
    private String name;

    @JsonProperty("user")
    private String user;

    @JsonProperty("state")
    private AnnotationDocumentState state;

    @JsonProperty("annotatorState")
    private AnnotationDocumentState annotatorState;

    @JsonProperty("annotatorComment")
    private String annotatorComment;

    @JsonProperty("timestamp")
    private Date timestamp;

    /**
     * @deprecated no longer used.
     */
    @Deprecated
    @JsonProperty("sentence_accessed")
    private int sentenceAccessed = 0;

    @JsonProperty("created")
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

    @JsonProperty("updated")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updated;

    /**
     * @return the name of the source document this annotation document belongs to.
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name
     *            the name of the source document this annotation document belongs to.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    public String getUser()
    {
        return user;
    }

    public void setUser(String user)
    {
        this.user = user;
    }

    public AnnotationDocumentState getState()
    {
        return state;
    }

    public void setState(AnnotationDocumentState state)
    {
        this.state = state;
    }

    public AnnotationDocumentState getAnnotatorState()
    {
        return annotatorState;
    }

    public void setAnnotatorState(AnnotationDocumentState aAnnotatorState)
    {
        annotatorState = aAnnotatorState;
    }

    public String getAnnotatorComment()
    {
        return annotatorComment;
    }

    public void setAnnotatorComment(String aAnnotatorComment)
    {
        annotatorComment = aAnnotatorComment;
    }

    public Date getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(Date timestamp)
    {
        this.timestamp = timestamp;
    }

    /**
     * @deprecated no longer used.
     */
    @Deprecated
    public int getSentenceAccessed()
    {
        return sentenceAccessed;
    }

    /**
     * @deprecated no longer used.
     */
    @Deprecated
    public void setSentenceAccessed(int sentenceAccessed)
    {
        this.sentenceAccessed = sentenceAccessed;
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
