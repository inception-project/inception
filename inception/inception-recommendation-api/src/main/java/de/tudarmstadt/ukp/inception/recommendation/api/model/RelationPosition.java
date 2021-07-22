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
package de.tudarmstadt.ukp.inception.recommendation.api.model;

import java.io.Serializable;
import java.util.Objects;

import org.apache.commons.lang3.builder.CompareToBuilder;

public class RelationPosition
    implements Serializable, Position, Comparable<RelationPosition>
{
    private static final long serialVersionUID = -3084534351646334021L;

    private final int sourceBegin;
    private final int sourceEnd;
    private final int targetBegin;
    private final int targetEnd;

    public RelationPosition(int aSourceBegin, int aSourceEnd, int aTargetBegin, int aTargetEnd)
    {
        sourceBegin = aSourceBegin;
        sourceEnd = aSourceEnd;
        targetBegin = aTargetBegin;
        targetEnd = aTargetEnd;
    }

    public RelationPosition(RelationPosition aOther)
    {
        sourceBegin = aOther.sourceBegin;
        sourceEnd = aOther.sourceEnd;
        targetBegin = aOther.targetBegin;
        targetEnd = aOther.targetEnd;
    }

    @Override
    public String toString()
    {

        return String.format("RelationPosition{(%d, %d) -> (%d, %d)}", sourceBegin, sourceEnd,
                targetBegin, targetEnd);
    }

    public int getSourceBegin()
    {
        return sourceBegin;
    }

    public int getSourceEnd()
    {
        return sourceEnd;
    }

    public int getTargetBegin()
    {
        return targetBegin;
    }

    public int getTargetEnd()
    {
        return targetEnd;
    }

    public boolean overlaps(final RelationPosition i)
    {
        throw new UnsupportedOperationException("Not implemented yet");
        // // Cases:
        // //
        // // start end
        // // | |
        // // 1 ####### |
        // // 2 | #######
        // // 3 ####################################
        // // 4 | ####### |
        // // | |
        //
        // return (((i.getStart() <= getStart()) && (getStart() < i.getEnd())) || // Case 1-3
        // ((i.getStart() < getEnd()) && (getEnd() <= i.getEnd())) || // Case 1-3
        // ((getStart() <= i.getStart()) && (i.getEnd() <= getEnd()))); // Case 4
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
        RelationPosition that = (RelationPosition) o;
        return sourceBegin == that.sourceBegin && sourceEnd == that.sourceEnd
                && targetBegin == that.targetBegin && targetEnd == that.targetEnd;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(sourceBegin, sourceEnd, targetBegin, targetEnd);
    }

    @Override
    public int compareTo(RelationPosition o)
    {
        return new CompareToBuilder() //
                .append(getSourceBegin(), o.getSourceBegin()) //
                .append(getSourceEnd(), o.getSourceEnd()) //
                .append(getTargetBegin(), o.getTargetBegin()) //
                .append(getTargetEnd(), o.getTargetEnd()).toComparison();
    }
}
