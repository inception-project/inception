/*
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.clarin.webanno.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Type;

/**
 * A persistence object for a Project.
 */
@Entity
@Table(name = "project", uniqueConstraints = { @UniqueConstraint(columnNames = { "name" }) })
public class Project
    implements Serializable
{
    private static final long serialVersionUID = -5426914078691460011L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Lob
    @Column(length = 64000)
    private String description;

    @Column(nullable = false)
    private String mode;

    // version of the project
    private int version = 1;
    
    // Disable users from exporting annotation documents
    private boolean disableExport = false;
    
    @Type(type = "de.tudarmstadt.ukp.clarin.webanno.model.ScriptDirectionType")
    private ScriptDirection scriptDirection;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = true)
    private Date created;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = true)
    private Date updated;
    
    @Column(nullable = true)
    @Type(type = "de.tudarmstadt.ukp.clarin.webanno.model.ProjectStateType")
    private ProjectState state;
    
    @Column(nullable = false)
    private boolean anonymousCuration;

    public Project()
    {
        // Nothing to do
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


    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int version)
    {
        this.version = version;
    }

    public boolean isDisableExport()
    {
        return disableExport;
    }

    public void setDisableExport(boolean disableExport)
    {
        this.disableExport = disableExport;
    }

    public ScriptDirection getScriptDirection()
    {
        // If unset, default to LTR - property was not present in older WebAnno versions
        if (scriptDirection == null) {
            return ScriptDirection.LTR;
        }
        else {
            return scriptDirection;
        }
    }

    public void setScriptDirection(ScriptDirection scriptDirection)
    {
        this.scriptDirection = scriptDirection;
    }

    public String getMode()
    {
        return mode;
    }

    public void setMode(String aMode)
    {
        this.mode = aMode;
    }

    @PrePersist
    protected void onCreate()
    {
        // When we import data, we set the fields via setters and don't want these to be 
        // overwritten by this event handler.
        if (created != null) {
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
    
    public ProjectState getState()
    {
        return state;
    }

    public void setState(ProjectState aState)
    {
        state = aState;
    }
    
    public boolean isAnonymousCuration()
    {
        return anonymousCuration;
    }

    public void setAnonymousCuration(boolean aAnonymousCuration)
    {
        anonymousCuration = aAnonymousCuration;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        Project other = (Project) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        }
        else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Project [id=");
        builder.append(id);
        builder.append(", name=");
        builder.append(name);
        builder.append("]");
        return builder.toString();
    }
}
