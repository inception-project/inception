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
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Represents a single annotation session — one browser tab open on a document.
 * A new session is created each time the annotator opens the document (each tab = one session).
 */
@Entity
@Table(name = "annotation_session")
public class AnnotationSession
        implements Serializable
{
    private static final long serialVersionUID = -4181649938345470440L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "document", nullable = false)
    private SourceDocument document;

    @Column(name = "user", nullable = false)
    private String user;

    @ManyToOne
    @JoinColumn(name = "project", nullable = false)
    private Project project;

    /**
     * When the annotator opened the document for this session.
     */
    @Column(name = "openedAt", nullable = false)
    private Instant openedAt;

    /**
     * When the session ended. Null if the session is still open (or was not cleanly closed).
     * Set either when the browser tab is closed/navigated away, or when the same user opens
     * the same document again (implicitly closing the previous session).
     */
    @Column(name = "closedAt")
    private Instant closedAt;

    /**
     * Total time (ms) the user had the annotation page open and was actively interacting
     * (mouse movement, keyboard, scroll). Tracked client-side; paused on tab-switch or idle.
     */
    @Column(name = "activeTimeMs", nullable = false)
    private long activeTimeMs = 0;

    /**
     * Number of annotation changes (create/update/delete) made during this session.
     */
    @Column(name = "changesCount", nullable = false)
    private int changesCount = 0;

    public AnnotationSession()
    {
        // Required by JPA
    }

    public AnnotationSession(SourceDocument aDocument, String aUser, Project aProject,
            Instant aOpenedAt)
    {
        document = aDocument;
        user = aUser;
        project = aProject;
        openedAt = aOpenedAt;
    }

    public Long getId()
    {
        return id;
    }

    public SourceDocument getDocument()
    {
        return document;
    }

    public String getUser()
    {
        return user;
    }

    public Project getProject()
    {
        return project;
    }

    public Instant getOpenedAt()
    {
        return openedAt;
    }

    public Instant getClosedAt()
    {
        return closedAt;
    }

    public void setClosedAt(Instant aClosedAt)
    {
        closedAt = aClosedAt;
    }

    public long getActiveTimeMs()
    {
        return activeTimeMs;
    }

    public void addActiveTime(long aDeltaMs)
    {
        activeTimeMs += aDeltaMs;
    }

    public int getChangesCount()
    {
        return changesCount;
    }

    public void incrementChangesCount()
    {
        changesCount++;
    }
}
