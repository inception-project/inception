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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.model;

import java.util.Objects;

import org.apache.uima.cas.text.AnnotationFS;

public class Range
{
    public static final Range UNDEFINED = new Range(-1, -1);

    private final int begin;
    private final int end;

    public Range(int aBegin, int aEnd)
    {
        begin = aBegin;
        end = aEnd;
    }

    public Range(AnnotationFS aAnnotation)
    {
        begin = aAnnotation.getBegin();
        end = aAnnotation.getEnd();
    }

    public int getBegin()
    {
        return begin;
    }

    public int getEnd()
    {
        return end;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(begin, end);
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
        Range other = (Range) obj;
        return begin == other.begin && end == other.end;
    }
}
