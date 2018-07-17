/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.log.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "logged_event")
public class LoggedEvent
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String event;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date created;

    /**
     * The user who triggered the event. Can be different from the annotator to whom the 
     * related annotation document does belong.
     */
    @Column(nullable = false)
    private String user;
    
    /**
     * If the event does not belong to a project, then the project ID should be -1.
     */
    @Column(nullable = false)
    private long project;
    
    /**
     * If the event does not belong to a source document, then the document ID should be -1.
     */
    @Column(nullable = false)
    private long document;
    
    /**
     * If the event does not belong to an annotation document, then the annotator ID should be null.
     */
    @Column(nullable = true)
    private String annotator;
    
    @Lob
    @Column(length = 64000, nullable = true)
    private String details;

    public Date getCreated()
    {
        return created;
    }

    public void setCreated(Date aCreated)
    {
        created = aCreated;
    }

    public long getProject()
    {
        return project;
    }

    public void setProject(long aProject)
    {
        project = aProject;
    }

    public String getAnnotator()
    {
        return annotator;
    }

    public void setAnnotator(String aAnnotator)
    {
        annotator = aAnnotator;
    }

    public String getUser()
    {
        return user;
    }

    public void setUser(String aUser)
    {
        user = aUser;
    }

    public String getEvent()
    {
        return event;
    }

    public void setEvent(String aEvent)
    {
        event = aEvent;
    }

    public String getDetails()
    {
        return details;
    }

    public void setDetails(String aDetails)
    {
        details = aDetails;
    }

    public long getDocument()
    {
        return document;
    }

    public void setDocument(long aDocument)
    {
        document = aDocument;
    }

    public long getId()
    {
        return id;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("LoggedEvent [created=");
        builder.append(created);
        builder.append(", event=");
        builder.append(event);
        if (user != null) {
            builder.append(", user=");
            builder.append(user);
        }
        if (project != -1) {
            builder.append(", project=");
            builder.append(project);
        }
        if (document != -1) {
            builder.append(", document=");
            builder.append(document);
        }
        if (annotator != null) {
            builder.append(", annotator=");
            builder.append(annotator);
        }
        if (details != null) {
            builder.append(", details=");
            builder.append(details);
        }
        builder.append("]");
        return builder.toString();
    }
}
