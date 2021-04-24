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
package de.tudarmstadt.ukp.clarin.webanno.model;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Type;

/**
 * A persistence object for meta-data of source documents. The content of the source document is
 * stored in the file system.
 */
@Entity
@Table(name = "source_document", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "name", "project" }) })
public class SourceDocument
    implements Serializable
{
    private static final long serialVersionUID = 8496087166198616020L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "project")
    Project project;

    private String format;

    @Column(nullable = false)
    @Type(type = "de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateType")
    private SourceDocumentState state = SourceDocumentState.NEW;

    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    private int sentenceAccessed = 0;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = true)
    private Date created;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = true)
    private Date updated;

    /*
     * This field are only here because we still may have the non-nullable columns in the DB. Once
     * we can properly migrate the database schema, this can go away.
     */
    @Deprecated
    private boolean trainingDocument = false;

    /*
     * This field are only here because we still may have the non-nullable columns in the DB. Once
     * we can properly migrate the database schema, this can go away.
     */
    @Deprecated
    private boolean processed = false;

    public SourceDocument()
    {
        // Nothing to do
    }

    public SourceDocument(String aName, Project aProject, String aFormat)
    {
        super();
        name = aName;
        project = aProject;
        format = aFormat;
        state = SourceDocumentState.NEW;
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long aId)
    {
        id = aId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String aName)
    {
        name = aName;
    }

    public Project getProject()
    {
        return project;
    }

    public void setProject(Project aProject)
    {
        project = aProject;
    }

    public String getFormat()
    {
        return format;
    }

    public void setFormat(String aFormat)
    {
        format = aFormat;
    }

    public SourceDocumentState getState()
    {
        return state;
    }

    public void setState(SourceDocumentState aState)
    {
        state = aState;
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

    @PrePersist
    protected void onCreate()
    {
        // When we import data, we set the fields via setters and don't want these to be
        // overwritten by this event handler.
        if (created == null) {
            created = new Date();
            updated = created;
        }
    }

    @PreUpdate
    protected void onUpdate()
    {
        updated = new Date();
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

    @Override
    public int hashCode()
    {
        return Objects.hash(name, project);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SourceDocument other = (SourceDocument) obj;
        return Objects.equals(name, other.name) && Objects.equals(project, other.project);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        builder.append(name);
        builder.append("](");
        builder.append(id);
        builder.append(")");
        return builder.toString();
    }

    public static final Comparator<SourceDocument> NAME_COMPARATOR = Comparator
            .comparing(SourceDocument::getName);
}
