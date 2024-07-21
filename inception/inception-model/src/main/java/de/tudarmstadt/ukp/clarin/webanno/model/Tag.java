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

/**
 * A persistence object for a Tag
 */
@Entity
@Table(name = "tag")
public class Tag
    implements Serializable
{
    private static final long serialVersionUID = -1490540239189868920L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 64000)
    private String description;

    @ManyToOne
    @JoinColumn(name = "tagset")
    private TagSet tagSet;

    @Column(nullable = false)
    private int rank;

    public Tag()
    {
        // Nothing to do
    }

    public Tag(TagSet aTagSet, String aName)
    {
        tagSet = aTagSet;
        name = aName;
    }

    public Tag(String aName, String aDescription)
    {
        name = aName;
        description = aDescription;
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

    public void setDescription(String aDescription)
    {
        description = aDescription;
    }

    public TagSet getTagSet()
    {
        return tagSet;
    }

    public void setTagSet(TagSet aTagSet)
    {
        tagSet = aTagSet;
    }

    public int getRank()
    {
        return rank;
    }

    public void setRank(int aRank)
    {
        rank = aRank;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((tagSet == null) ? 0 : tagSet.hashCode());
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
        Tag other = (Tag) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        }
        else if (!name.equals(other.name)) {
            return false;
        }
        if (tagSet == null) {
            if (other.tagSet != null) {
                return false;
            }
        }
        else if (!tagSet.equals(other.tagSet)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
