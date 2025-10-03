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
 * A persistence object for meta-data of annotation documents. The content of annotation document is
 * stored in a file system.
 */
@Entity
@Table(name = "annotation_document", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "name", "project", "user" }) })
public class AnnotationDocument
    implements Serializable
{
    private static final long serialVersionUID = 8496087166198616020L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Deprecated
    @Column(name = "name", nullable = false)
    private String name;

    @Deprecated
    @ManyToOne
    @JoinColumn(name = "project")
    private Project project;

    @Column(name = "user")
    private String user;

    @ManyToOne
    @JoinColumn(name = "document")
    private SourceDocument document;

    /**
     * The effective state of the annotation document. This state may be set by the annotator user
     * or by a third person (e.g. curator/manager) or the system (e.g. workload manager).
     */
    @Column(name = "state", nullable = false)
    @Type(AnnotationDocumentStateType.class)
    private AnnotationDocumentState state = AnnotationDocumentState.NEW;

    /**
     * State manually set by the annotator user. If a third person (e.g. curator/manager) or the
     * system (e.g. workload manager) wants to change the state, they only change {@link #state} and
     * leave this field here as it is. The exception is if the document is reset in which case the
     * state should be cleared. This state is maintained mostly for informational purposes, e.g. to
     * allow managers to see whether an annotation document as marked as finished by the annotator
     * or if it was marked as finished by the manager or by the system.
     */
    @Column(name = "annotatorState", nullable = true)
    @Type(AnnotationDocumentStateType.class)
    private AnnotationDocumentState annotatorState;

    /**
     * Comment the anntoator can leave when marking a document as finished. Typically used to report
     * problems to the curator.
     */
    @Column(name = "annotatorComment", length = 64000)
    private String annotatorComment;

    /**
     * Last change made to the annotations or the last state transition triggered by the annotator
     * user. State changes triggered by a third person (e.g. curator/manager) or by the system (e.g.
     * workload manager) should not update the timestamp - except if the change is a reset of the
     * annotation document in which case the timestamp should be cleared.
     */
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    /**
     * @deprecated no longer used.
     */
    @Deprecated
    @Column(name = "sentenceAccessed")
    private int sentenceAccessed = 0;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created", nullable = true)
    private Date created;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated", nullable = true)
    private Date updated;

    private AnnotationDocument(Builder builder)
    {
        id = builder.id;
        name = builder.name;
        project = builder.project;
        user = builder.user;
        document = builder.document;
        state = builder.state;
        annotatorState = builder.annotatorState;
        annotatorComment = builder.annotatorComment;
        timestamp = builder.timestamp;
        sentenceAccessed = builder.sentenceAccessed;
        created = builder.created;
        updated = builder.updated;
    }

    public AnnotationDocument()
    {
        // Nothing to do
    }

    public AnnotationDocument(String aUser, SourceDocument aDocument)
    {
        setUser(aUser);
        document = aDocument;
        name = aDocument.getName();
        project = aDocument.getProject();
    }

    public SourceDocument getDocument()
    {
        return document;
    }

    public void setDocument(SourceDocument aDocument)
    {
        document = aDocument;
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long aId)
    {
        id = aId;
    }

    /**
     * @deprecated Use {@link #getDocument() getDocument().getName()} instead.
     * @return the name of the source document this annotation document is for.
     */
    @Deprecated
    public String getName()
    {
        return name;
    }

    @Deprecated
    public void setName(String aName)
    {
        name = aName;
    }

    /**
     * @deprecated Use {@link #getDocument() getDocument().getProject()} instead.
     * @return the project of the source document this annotation document is for.
     */
    @Deprecated
    public Project getProject()
    {
        return project;
    }

    @Deprecated
    public void setProject(Project aProject)
    {
        project = aProject;
    }

    /**
     * @deprecated Use {@link #getAnnotationSet}
     */
    @Deprecated
    public String getUser()
    {
        return user;
    }

    /**
     * @deprecated Use {@link #setAnnotationSet}
     */
    @Deprecated
    public void setUser(String aUser)
    {
        user = aUser;
    }

    public AnnotationSet getAnnotationSet()
    {
        return AnnotationSet.forUser(user);
    }

    public void setAnnotationSet(AnnotationSet aSet)
    {
        user = aSet.id();
    }

    public AnnotationDocumentState getState()
    {
        return state;
    }

    public void setState(AnnotationDocumentState aState)
    {
        state = aState;
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

    /**
     * @param aTimestamp
     *            last change to the actual annotations in the CAS. The change to the annotation
     *            document record is tracked in {@link #getUpdated()}
     */
    public void setTimestamp(Date aTimestamp)
    {
        timestamp = aTimestamp;
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

    /**
     * @return last change to the annotation document record.
     */
    public Date getUpdated()
    {
        return updated;
    }

    public void setUpdated(Date aUpdated)
    {
        updated = aUpdated;
    }

    @Override
    public String toString()
    {
        return "[" + user + "@" + name + "](" + id + ")";
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof AnnotationDocument)) {
            return false;
        }
        AnnotationDocument castOther = (AnnotationDocument) other;
        return Objects.equals(name, castOther.name) && Objects.equals(project, castOther.project)
                && Objects.equals(user, castOther.user)
                && Objects.equals(document, castOther.document);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name, project, user, document);
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private Long id;
        private String name;
        private Project project;
        private String user;
        private SourceDocument document;
        private AnnotationDocumentState state = AnnotationDocumentState.NEW;
        private AnnotationDocumentState annotatorState;
        private String annotatorComment;
        private Date timestamp;
        /**
         * @deprecated no longer used.
         */
        @Deprecated
        private int sentenceAccessed = 0;
        private Date created;
        private Date updated;

        private Builder()
        {
            // Nothing
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

        public Builder withUser(String aUser)
        {
            user = aUser;
            return this;
        }

        public Builder withDocument(SourceDocument aDocument)
        {
            document = aDocument;
            return this;
        }

        public Builder forDocument(SourceDocument aDocument)
        {
            document = aDocument;
            name = aDocument.getName();
            project = aDocument.getProject();
            return this;
        }

        public Builder withState(AnnotationDocumentState aState)
        {
            state = aState;
            return this;
        }

        public Builder withAnnotatorState(AnnotationDocumentState aAnnotatorState)
        {
            annotatorState = aAnnotatorState;
            return this;
        }

        public Builder withAnnotatorComment(String aAnnotatorComment)
        {
            annotatorComment = aAnnotatorComment;
            return this;
        }

        public Builder withTimestamp(Date aTimestamp)
        {
            timestamp = aTimestamp;
            return this;
        }

        /**
         * @deprecated no longer used.
         */
        @Deprecated
        public Builder withSentenceAccessed(int aSentenceAccessed)
        {
            sentenceAccessed = aSentenceAccessed;
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

        public AnnotationDocument build()
        {
            return new AnnotationDocument(this);
        }
    }
}
