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

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * Source document information to be exported/imported
 */
@JsonPropertyOrder(value = { "name", "format", "state" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExportedSourceDocument
{
    @JsonProperty("name")
    private String name;

    @JsonProperty("format")
    private String format;

    @JsonProperty("state")
    private SourceDocumentState state;

    @JsonProperty("timestamp")
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

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
