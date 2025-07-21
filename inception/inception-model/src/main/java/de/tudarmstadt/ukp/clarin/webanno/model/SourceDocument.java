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

import org.hibernate.annotations.Type;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;

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
    @Type(SourceDocumentStateType.class)
    private SourceDocumentState state = SourceDocumentState.NEW;

    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = true)
    private Date created;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = true)
    private Date updated;

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

    private SourceDocument(Builder builder)
    {
        this.id = builder.id;
        this.name = builder.name;
        this.project = builder.project;
        this.format = builder.format;
        this.state = builder.state;
        this.timestamp = builder.timestamp;
        this.created = builder.created;
        this.updated = builder.updated;
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
        if (id != null) {
            return Objects.hash(id);
        }

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

        var other = (SourceDocument) obj;

        if (id != null && other.id != null) {
            return Objects.equals(id, other.id);
        }

        return Objects.equals(name, other.name) && Objects.equals(project, other.project);
    }

    @Override
    public String toString()
    {
        return "[" + name + "](" + id + ")";
    }

    public static final Comparator<SourceDocument> NAME_COMPARATOR = Comparator
            .comparing(SourceDocument::getName);

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private Long id;
        private String name;
        private Project project;
        private String format;
        private SourceDocumentState state = SourceDocumentState.NEW;
        private Date timestamp;
        private Date created;
        private Date updated;

        private Builder()
        {
            // No instanccs
        }

        public Builder withId(Long aId)
        {
            id = aId;
            return this;
        }

        public Builder withName(String aName)
        {
            name = aName;
            return this;
        }

        public Builder withProject(Project aProject)
        {
            project = aProject;
            return this;
        }

        public Builder withFormat(String aFormat)
        {
            format = aFormat;
            return this;
        }

        public Builder withState(SourceDocumentState aState)
        {
            state = aState;
            return this;
        }

        public Builder withTimestamp(Date aTimestamp)
        {
            timestamp = aTimestamp;
            return this;
        }

        public Builder withCreated(Date aCreated)
        {
            created = aCreated;
            return this;
        }

        public Builder withUpdated(Date aUpdated)
        {
            updated = aUpdated;
            return this;
        }

        public SourceDocument build()
        {
            return new SourceDocument(this);
        }
    }
}
