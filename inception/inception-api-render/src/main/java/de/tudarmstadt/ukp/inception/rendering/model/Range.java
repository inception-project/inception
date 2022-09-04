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
package de.tudarmstadt.ukp.inception.rendering.model;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Objects;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;

public class Range
    implements Serializable
{
    private static final long serialVersionUID = -6261188569647696831L;

    public static final Range UNDEFINED = new Range(-1, -1);

    private final int begin;
    private final int end;

    public Range(CAS aCas)
    {
        begin = 0;
        end = aCas.getDocumentText().length();
    }

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

    public Range(Iterable<AnnotationFS> aAnnotations)
    {
        Iterator<AnnotationFS> i = aAnnotations.iterator();
        if (!i.hasNext()) {
            begin = -1;
            end = -1;
            return;
        }

        AnnotationFS current = i.next();
        int b = current.getBegin();
        int e = current.getEnd();

        while (i.hasNext()) {
            current = i.next();
            b = Math.min(current.getBegin(), b);
            e = Math.max(current.getEnd(), e);
        }

        begin = b;
        end = e;
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
    public String toString()
    {
        return "[" + begin + "-" + end + "]";
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
