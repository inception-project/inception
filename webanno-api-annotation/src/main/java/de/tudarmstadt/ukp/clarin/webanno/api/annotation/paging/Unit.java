/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging;

import java.io.Serializable;

import javax.annotation.Nullable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Unit
    implements Serializable
{
    private static final long serialVersionUID = -8969756502471237659L;
    
    private final int index;
    private final int begin;
    private final int end;
    private final String id;    
    
    /**
     * @param aIndex index (1-based)
     * @param aBegin begin character offset
     * @param aEnd end character offset
     */
    public Unit(int aIndex, int aBegin, int aEnd)
    {
        this(null, aIndex, aBegin, aEnd);
    }

    /**
     * 
     * @param aId unit ID (if any) or {@code null}
     * @param aIndex index (1-based)
     * @param aBegin begin character offset
     * @param aEnd end character offset
     */
    public Unit(@Nullable String aId, int aIndex, int aBegin, int aEnd)
    {
        id = aId;
        index = aIndex;
        begin = aBegin;
        end = aEnd;
    }
    
    public int getIndex()
    {
        return index;
    }

    public int getBegin()
    {
        return begin;
    }

    public int getEnd()
    {
        return end;
    }

    @Nullable
    public String getId()
    {
        return id;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof Unit)) {
            return false;
        }
        Unit castOther = (Unit) other;
        return new EqualsBuilder().append(begin, castOther.begin).append(end, castOther.end)
                .append(id, castOther.id).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(begin).append(end).append(id).toHashCode();
    }
}
