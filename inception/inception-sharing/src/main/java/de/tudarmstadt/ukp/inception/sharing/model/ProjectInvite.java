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

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;

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

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date expirationDate;

    public ProjectInvite(Project aProject, String aInviteId, Date aExpirationDate)
    {
        super();
        project = aProject;
        inviteId = aInviteId;
        expirationDate = aExpirationDate;
    }

    protected ProjectInvite()
    {
        // constructor for JPA
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

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((inviteId == null) ? 0 : inviteId.hashCode());
        result = prime * result + ((project == null) ? 0 : project.hashCode());
        return result;
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
        ProjectInvite other = (ProjectInvite) obj;
        if (inviteId == null) {
            if (other.inviteId != null) {
                return false;
            }
        }
        else if (!inviteId.equals(other.inviteId)) {
            return false;
        }
        if (project == null) {
            if (other.project != null) {
                return false;
            }
        }
        else if (!project.equals(other.project)) {
            return false;
        }
        return true;
    }

}
