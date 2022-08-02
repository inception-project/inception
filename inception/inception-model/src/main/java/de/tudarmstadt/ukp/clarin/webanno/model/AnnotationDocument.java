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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Type;

/**
 * A persistence object for meta-data of annotation documents. The content of annotation document is
 * stored in a file system.
 *
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

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "project")
    private Project project;

    private String user;

    @ManyToOne
    @JoinColumn(name = "document")
    private SourceDocument document;

    /**
     * The effective state of the annotation document. This state may be set by the annotator user
     * or by a third person (e.g. curator/manager) or the system (e.g. workload manager).
     */
    @Column(nullable = false)
    @Type(type = "de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateType")
    private AnnotationDocumentState state = AnnotationDocumentState.NEW;

    /**
     * State manually set by the annotator user. If a third person (e.g. curator/manager) or the
     * system (e.g. workload manager) wants to change the state, they only change {@link #state} and
     * leave this field here as it is. The exception is if the document is reset in which case the
     * state should be cleared. This state is maintained mostly for informational purposes, e.g. to
     * allow managers to see whether an annotation document as marked as finished by the annotator
     * or if it was marked as finished by the manager or by the system.
     */
    @Column(nullable = true)
    @Type(type = "de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateType")
    private AnnotationDocumentState annotatorState;

    /**
     * Comment the anntoator can leave when marking a document as finished. Typically used to report
     * problems to the curator.
     */
    @Lob
    @Column(length = 64000)
    private String annotatorComment;

    /**
     * Last change made to the annotations or the last state transition triggered by the annotator
     * user. State changes triggered by a third person (e.g. curator/manager) or by the system (e.g.
     * workload manager) should not update the timestamp - except if the change is a reset of the
     * annotation document in which case the timestamp should be cleared.
     */
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    @Column(name = "sentenceAccessed")
    private int sentenceAccessed = 0;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = true)
    private Date created;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = true)
    private Date updated;

    public AnnotationDocument()
    {
        // Nothing to do
    }

    public AnnotationDocument(String aUser, SourceDocument aDocument)
    {
        user = aUser;
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

    public String getUser()
    {
        return user;
    }

    public void setUser(String aUser)
    {
        user = aUser;
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
}
