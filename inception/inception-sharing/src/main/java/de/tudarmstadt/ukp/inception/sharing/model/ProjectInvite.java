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
package de.tudarmstadt.ukp.inception.sharing.model;

import static de.tudarmstadt.ukp.inception.sharing.model.Mandatoriness.NOT_ALLOWED;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import org.hibernate.annotations.Type;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity
@Table(name = "project_invite")
public class ProjectInvite
    implements Serializable
{
    private static final long serialVersionUID = -2795919324253421263L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "project")
    private Project project;

    @Column(nullable = false)
    private String inviteId;

    @Column(nullable = true)
    private String userIdPlaceholder;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date expirationDate;

    @Column(length = 64000)
    private String invitationText;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = true)
    private Date created;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = true)
    private Date updated;

    @Column(nullable = false)
    private boolean guestAccessible = false;

    @Type(MandatorinessType.class)
    @Column(nullable = false)
    private Mandatoriness askForEMail = NOT_ALLOWED;

    @Column(nullable = false)
    private boolean disableOnAnnotationComplete = true;

    @Column(nullable = false)
    private int maxAnnotatorCount;

    public ProjectInvite()
    {
        // constructor for JPA
    }

    public ProjectInvite(Project aProject, String aInviteId, Date aExpirationDate)
    {
        super();
        project = aProject;
        inviteId = aInviteId;
        expirationDate = aExpirationDate;
    }

    public Project getProject()
    {
        return project;
    }

    public String getInviteId()
    {
        return inviteId;
    }

    public void setInviteId(String aInviteId)
    {
        inviteId = aInviteId;
    }

    public Date getExpirationDate()
    {
        return expirationDate;
    }

    public void setExpirationDate(Date aExpirationDate)
    {
        expirationDate = aExpirationDate;
    }

    public void setProject(Project aProject)
    {
        project = aProject;
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long aId)
    {
        id = aId;
    }

    public String getInvitationText()
    {
        return invitationText;
    }

    public void setInvitationText(String aInvitationText)
    {
        invitationText = aInvitationText;
    }

    public String getUserIdPlaceholder()
    {
        return userIdPlaceholder;
    }

    public void setUserIdPlaceholder(String aUserIdPlaceholder)
    {
        userIdPlaceholder = aUserIdPlaceholder;
    }

    public boolean isGuestAccessible()
    {
        return guestAccessible;
    }

    public void setGuestAccessible(boolean aGuestAccessible)
    {
        guestAccessible = aGuestAccessible;
    }

    public Mandatoriness getAskForEMail()
    {
        return askForEMail;
    }

    public void setAskForEMail(Mandatoriness aAskForEMail)
    {
        askForEMail = aAskForEMail;
    }

    public boolean isDisableOnAnnotationComplete()
    {
        return disableOnAnnotationComplete;
    }

    public void setDisableOnAnnotationComplete(boolean aDisableOnAnnotationComplete)
    {
        disableOnAnnotationComplete = aDisableOnAnnotationComplete;
    }

    public int getMaxAnnotatorCount()
    {
        return maxAnnotatorCount;
    }

    public void setMaxAnnotatorCount(int aMaxAnnotatorCount)
    {
        maxAnnotatorCount = aMaxAnnotatorCount;
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
    public boolean equals(final Object other)
    {
        if (!(other instanceof ProjectInvite)) {
            return false;
        }
        ProjectInvite castOther = (ProjectInvite) other;
        return Objects.equals(project, castOther.project)
                && Objects.equals(inviteId, castOther.inviteId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(project, inviteId);
    }
}
