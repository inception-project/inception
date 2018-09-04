/* 
 * Copyright 2017
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
package de.tudarmstadt.ukp.inception.search.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.search.index.PhysicalIndex;

/**
 * A persistence object for meta-data of an index. The index is
 * stored in the file system.
 */

@Entity
@Table(name = "inception_index")
public class Index
    implements Serializable
{
    private static final long serialVersionUID = -8487663728083806672L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project")
    private Project project;

    private Boolean invalid;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = true)
    private Date creationDate;
    
    private String physicalProvider;
    
    @Transient
    private PhysicalIndex physicalIndex;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Project getProject()
    {
        return project;
    }

    public void setProject(Project project)
    {
        this.project = project;
    }

    public Boolean getInvalid()
    {
        return invalid;
    }

    public void setInvalid(Boolean state)
    {
        this.invalid = state;
    }

    public Date getCreationDate()
    {
        return creationDate;
    }

    public void setCreationDate(Date creationDate)
    {
        this.creationDate = creationDate;
    }

    public String getPhysicalProvider()
    {
        return physicalProvider;
    }

    public void setPhysicalProvider(String physicalProvider)
    {
        this.physicalProvider = physicalProvider;
    }

    public PhysicalIndex getPhysicalIndex()
    {
        return physicalIndex;
    }

    public void setPhysicalIndex(PhysicalIndex physicalIndex)
    {
        this.physicalIndex = physicalIndex;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Index that = (Index) o;

        if (invalid != that.invalid) {
            return false;
        }
        return project != null ? project.equals(that.project) : that.project == null;

    }

    @Override
    public int hashCode()
    {
        int result = project != null ? project.hashCode() : 0;
        return result;
    }
}
