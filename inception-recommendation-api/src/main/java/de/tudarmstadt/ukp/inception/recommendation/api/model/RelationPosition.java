/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.api.model;

import java.io.Serializable;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class RelationPosition
    implements Comparable<RelationPosition>, Serializable, Position
{
    private static final long serialVersionUID = -3084534351646334021L;

    private final int source;
    private final int target;
    
    public RelationPosition(int aSource, int aTarget)
    {
        source = aSource;
        target = aTarget;
    }

    public RelationPosition(RelationPosition aPosition)
    {
        source = aPosition.source;
        target = aPosition.target;
    }

    @Override
    public String toString()
    {
        return "[" + source + "->" + target + "]";
    }

    public int getSource()
    {
        return source;
    }

    public int getTarget()
    {
        return target;
    }

    public boolean overlaps(final RelationPosition i)
    {
        throw new UnsupportedOperationException("Not implemented yet");
//        // Cases:
//        //
//        //         start                     end
//        //           |                        |
//        //  1     #######                     |
//        //  2        |                     #######
//        //  3   ####################################
//        //  4        |        #######         |
//        //           |                        |
//
//        return (((i.getStart() <= getStart()) && (getStart() < i.getEnd())) || // Case 1-3
//                ((i.getStart() < getEnd()) && (getEnd() <= i.getEnd())) || // Case 1-3
//                ((getStart() <= i.getStart()) && (i.getEnd() <= getEnd()))); // Case 4
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof RelationPosition)) {
            return false;
        }
        RelationPosition castOther = (RelationPosition) other;
        return new EqualsBuilder().append(source, castOther.source).append(target, castOther.target)
                .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(source).append(target).toHashCode();
    }

    @Override
    public int compareTo(final RelationPosition other)
    {
        return new CompareToBuilder().append(source, other.source).append(target, other.target)
                .toComparison();
    }
}
