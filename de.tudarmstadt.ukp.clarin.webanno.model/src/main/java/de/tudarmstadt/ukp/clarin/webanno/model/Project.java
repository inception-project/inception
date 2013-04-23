/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * A persistence object for a Project.
 *
 * @author Seid Muhie Yimam
 *
 */
@Entity
@Table(name = "project", uniqueConstraints = { @UniqueConstraint(columnNames = { "name" }) })
public class Project
    implements Serializable
{
    private static final long serialVersionUID = -5426914078691460011L;

    @Id
    @GeneratedValue
    private long id;

    @Column(nullable = false)
    private String name;

    @Lob
    private String description;

    /**
     * The model {@link ProjectPermission} contains the map between users and projects
     */
    @Deprecated
    @ManyToMany
    private Set<User> users = new HashSet<User>();

    private boolean reverseDependencyDirection;

    public Project()
    {
        // Nothing to do
    }

    public long getId()
    {
        return id;
    }

    public void setId(long aId)
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

    public Set<User> getUsers()
    {
        return users;
    }

    public void setUsers(Set<User> aUsers)
    {
        users = aUsers;
    }

    public boolean isReverseDependencyDirection()
    {
        return reverseDependencyDirection;
    }

    public void setReverseDependencyDirection(boolean aReverseDependencyDirection)
    {
        reverseDependencyDirection = aReverseDependencyDirection;
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
}
