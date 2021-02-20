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
package de.tudarmstadt.ukp.inception.log.exporter;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import de.tudarmstadt.ukp.inception.log.model.LoggedEvent;

@JsonPropertyOrder(alphabetic = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class ExportedLoggedEvent
{
    @JsonProperty("id")
    private long id;

    @JsonProperty("event")
    private String event;

    @JsonProperty("created")
    private Date created;

    @JsonProperty("user")
    private String user;

    @JsonProperty("document_name")
    private String documentName;

    @JsonProperty("annotator")
    private String annotator;

    @JsonProperty("details")
    @JsonRawValue
    @JsonDeserialize(using = RawJsonDeserializer.class)
    private String details;

    public long getId()
    {
        return id;
    }

    public void setId(long aId)
    {
        id = aId;
    }

    public String getEvent()
    {
        return event;
    }

    public void setEvent(String aEvent)
    {
        event = aEvent;
    }

    public Date getCreated()
    {
        return created;
    }

    public void setCreated(Date aCreated)
    {
        created = aCreated;
    }

    public String getUser()
    {
        return user;
    }

    public void setUser(String aUser)
    {
        user = aUser;
    }

    public String getDocumentName()
    {
        return documentName;
    }

    public void setDocumentName(String aDocumentName)
    {
        documentName = aDocumentName;
    }

    public String getAnnotator()
    {
        return annotator;
    }

    public void setAnnotator(String aAnnotator)
    {
        annotator = aAnnotator;
    }

    public String getDetails()
    {
        return details;
    }

    public void setDetails(String aDetails)
    {
        details = aDetails;
    }

    public static ExportedLoggedEvent fromLoggedEvent(String documentName, LoggedEvent event)
    {
        ExportedLoggedEvent exportedEvent = new ExportedLoggedEvent();
        exportedEvent.setId(event.getId());
        exportedEvent.setCreated(event.getCreated());
        exportedEvent.setDocumentName(documentName);
        exportedEvent.setEvent(event.getEvent());
        exportedEvent.setAnnotator(event.getAnnotator());
        exportedEvent.setUser(event.getUser());
        exportedEvent.setDetails(event.getDetails());
        return exportedEvent;
    }
}
