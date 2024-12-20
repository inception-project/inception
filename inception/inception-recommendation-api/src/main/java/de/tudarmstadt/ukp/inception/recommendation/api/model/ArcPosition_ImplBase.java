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
import org.apache.uima.cas.text.AnnotationFS;

public abstract class ArcPosition_ImplBase<T extends ArcPosition_ImplBase<?>>
    implements Serializable, Position, Comparable<T>
{
    private static final long serialVersionUID = 6630083774056904670L;

    protected final int sourceBegin;
    protected final int sourceEnd;
    protected final int targetBegin;
    protected final int targetEnd;

    public ArcPosition_ImplBase(AnnotationFS aSource, AnnotationFS aTarget)
    {
        sourceBegin = aSource.getBegin();
        sourceEnd = aSource.getEnd();
        targetBegin = aTarget.getBegin();
        targetEnd = aTarget.getEnd();
    }

    public ArcPosition_ImplBase(int aSourceBegin, int aSourceEnd, int aTargetBegin, int aTargetEnd)
    {
        sourceBegin = aSourceBegin;
        sourceEnd = aSourceEnd;
        targetBegin = aTargetBegin;
        targetEnd = aTargetEnd;
    }

    public ArcPosition_ImplBase(T aOther)
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

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ArcPosition_ImplBase that = (ArcPosition_ImplBase) o;
        return sourceBegin == that.sourceBegin && sourceEnd == that.sourceEnd
                && targetBegin == that.targetBegin && targetEnd == that.targetEnd;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(sourceBegin, sourceEnd, targetBegin, targetEnd);
    }

    @Override
    public int compareTo(T o)
    {
        return new CompareToBuilder() //
                .append(getSourceBegin(), o.getSourceBegin()) //
                .append(getSourceEnd(), o.getSourceEnd()) //
                .append(getTargetBegin(), o.getTargetBegin()) //
                .append(getTargetEnd(), o.getTargetEnd()).toComparison();
    }
}
