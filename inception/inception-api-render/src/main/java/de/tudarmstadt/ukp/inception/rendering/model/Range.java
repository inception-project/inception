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

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.String.format;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.Objects;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Range
    implements Serializable, Comparable<Range>
{
    private static final long serialVersionUID = -6261188569647696831L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String BEGIN = "begin";
    private static final String END = "end";

    public static final Range UNDEFINED = new Range(-1, -1);

    private final @JsonProperty(BEGIN) int begin;
    private final @JsonProperty(END) int end;

    @JsonCreator
    public Range(@JsonProperty(BEGIN) int aBegin, @JsonProperty(END) int aEnd)
    {
        begin = aBegin;
        end = aEnd;
    }

    /**
     * @deprecated Use {@link #rangeCoveringDocument} instead.
     */
    @Deprecated
    public Range(CAS aCas)
    {
        begin = 0;
        end = aCas.getDocumentText().length();
    }

    public Range(AnnotationFS aAnnotation)
    {
        begin = aAnnotation.getBegin();
        end = aAnnotation.getEnd();
    }

    /**
     * @deprecated Use {@link #rangeCoveringAnnotations(Iterable)} instead.
     */
    @Deprecated
    public Range(Iterable<? extends AnnotationFS> aAnnotations)
    {
        var i = aAnnotations.iterator();
        if (!i.hasNext()) {
            begin = -1;
            end = -1;
            return;
        }

        var current = i.next();
        int b = current.getBegin();
        int e = current.getEnd();

        while (i.hasNext()) {
            current = i.next();
            b = min(current.getBegin(), b);
            e = max(current.getEnd(), e);
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

    public static Range rangeClippedToDocument(CAS aCas, int aBegin, int aEnd)
    {
        var length = aCas.getDocumentText().length();

        var begin = min(aBegin, aEnd);
        var end = max(aBegin, aEnd);

        var clippedBegin = max(0, begin);
        var clippedEnd = min(length, end);

        if (clippedBegin > length || clippedEnd > length) {
            throw new IllegalArgumentException(format(
                    "Range [%d-%d] is fully outside the document [%d-%d]", begin, end, 0, length));
        }

        if (clippedBegin != begin || clippedEnd != end) {
            LOG.warn("Range [{}-{}] clipped to [{}-{}]", begin, end, clippedBegin, clippedEnd);
        }

        return new Range(clippedBegin, clippedEnd);
    }

    public static Range rangeCoveringDocument(CAS aCas)
    {
        return new Range(aCas);
    }

    public static Range rangeCoveringAnnotations(Iterable<? extends AnnotationFS> aAnnotations)
    {
        return new Range(aAnnotations);
    }

    @Override
    public String toString()
    {
        return "[" + begin + "-" + end + "]";
    }

    @Override
    public int compareTo(Range aOther)
    {
        if (this == aOther) {
            return 0;
        }

        // Sort by begin ascending
        int cmp = Integer.compare(begin, aOther.begin);
        if (cmp != 0) {
            return cmp;
        }

        // Sort by end descending
        return Integer.compare(aOther.end, end);
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
