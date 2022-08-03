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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class ImmutableTag
    implements Serializable
{
    private static final long serialVersionUID = -8402497864975003660L;

    private final Long tagsetId;
    private final Long id;
    private final String name;
    private final String description;

    public ImmutableTag(Tag aTag)
    {
        if (aTag.getTagSet() != null) {
            tagsetId = aTag.getTagSet().getId();
        }
        else {
            tagsetId = null;
        }
        id = aTag.getId();
        name = aTag.getName();
        description = aTag.getDescription();
    }

    public ImmutableTag(String aName, String aDescription)
    {
        tagsetId = null;
        id = null;
        name = aName;
        description = aDescription;
    }

    public Long getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public Long getTagsetId()
    {
        return tagsetId;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof ImmutableTag)) {
            return false;
        }
        ImmutableTag castOther = (ImmutableTag) other;
        return new EqualsBuilder().append(name, castOther.name).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(name).toHashCode();
    }
}
