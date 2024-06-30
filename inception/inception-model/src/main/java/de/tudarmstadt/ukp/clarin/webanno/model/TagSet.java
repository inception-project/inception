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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A persistence object for a TagSet
 */
@Entity
@Table(name = "tag_set", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "name", "project" }) })
public class TagSet
    implements Serializable
{
    private static final long serialVersionUID = 5644063605817006810L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project")
    Project project;

    @Column(nullable = false)
    private String name;

    private String language;

    @Column(length = 64000)
    private String description;

    private boolean createTag = false;

    public TagSet()
    {
        // Nothing to do
    }

    public TagSet(Project aProject, String aName)
    {
        project = aProject;
        name = aName;
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

    public String getLanguage()
    {
        return language;
    }

    public void setLanguage(String aLanguage)
    {
        language = aLanguage;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String aDescription)
    {
        description = aDescription;
    }

    public Project getProject()
    {
        return project;
    }

    public void setProject(Project aProject)
    {
        project = aProject;
    }

    public boolean isCreateTag()
    {
        return createTag;
    }

    public void setCreateTag(boolean createTag)
    {
        this.createTag = createTag;
    }

    @Override
    public String toString()
    {
        return "[" + name + "](" + id + ")";
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        TagSet other = (TagSet) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        }
        else if (!name.equals(other.name)) {
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
