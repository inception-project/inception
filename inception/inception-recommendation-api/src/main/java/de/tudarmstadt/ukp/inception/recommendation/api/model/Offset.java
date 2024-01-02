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

import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationPredicates;

public class Offset
    implements Comparable<Offset>, Serializable, Position
{
    private static final long serialVersionUID = -3084534351646334021L;

    private final int begin;
    private final int end;

    public Offset(AnnotationFS aAnnotation)
    {
        begin = aAnnotation.getBegin();
        end = aAnnotation.getEnd();
    }

    public Offset(int aBeginCharacter, int aEndCharacter)
    {
        begin = aBeginCharacter;
        end = aEndCharacter;
    }

    public Offset(Offset aOffset)
    {
        begin = aOffset.begin;
        end = aOffset.end;
    }

    @Override
    public String toString()
    {
        return "[" + begin + "," + end + "]";
    }

    public int getBegin()
    {
        return begin;
    }

    public int getEnd()
    {
        return end;
    }

    public int length()
    {
        return end - begin;
    }

    public boolean overlaps(final Offset i)
    {
        return AnnotationPredicates.overlapping(getBegin(), getEnd(), i.getBegin(), i.getEnd());
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + begin;
        result = prime * result + end;
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
        Offset other = (Offset) obj;
        if (begin != other.begin) {
            return false;
        }
        if (end != other.end) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(Offset o)
    {
        if (o == null) {
            return 1;
        }

        if (this == o) {
            return 0;
        }

        if (begin == o.begin) {
            // Sort by end decreasing
            return o.end - end;
        }
        else {
            // Sort by begin increasing
            return begin - o.begin;
        }
    }
}
