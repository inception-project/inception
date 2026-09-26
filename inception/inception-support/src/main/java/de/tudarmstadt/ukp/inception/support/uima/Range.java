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
package de.tudarmstadt.ukp.inception.support.uima;

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
    implements Serializable, Comparable<Range>, IRange
{
    private static final long serialVersionUID = -6261188569647696831L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String BEGIN = "begin";
    private static final String END = "end";

    private static final int UNDEFINED_OFFSET = -1;

    /**
     * A range that names no location. This is the <b>only</b> negative range that can be
     * constructed.
     */
    public static final Range UNDEFINED = new Range(UNDEFINED_OFFSET, UNDEFINED_OFFSET);

    private final @JsonProperty(BEGIN) int begin;
    private final @JsonProperty(END) int end;

    @JsonCreator
    public Range(@JsonProperty(BEGIN) int aBegin, @JsonProperty(END) int aEnd)
    {
        if ((aBegin < 0 || aEnd < 0) && !(aBegin == UNDEFINED_OFFSET && aEnd == UNDEFINED_OFFSET)) {
            throw new IllegalArgumentException(
                    format("Range [%d-%d] has negative offsets", aBegin, aEnd));
        }

        begin = aBegin;
        end = aEnd;
    }

    public Range(IRange aRange)
    {
        this(aRange.getBegin(), aRange.getEnd());
    }

    /**
     * @deprecated Use {@link #rangeCoveringDocument} instead.
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    public Range(CAS aCas)
    {
        this(0, aCas.getDocumentText().length());
    }

    public Range(AnnotationFS aAnnotation)
    {
        this(aAnnotation.getBegin(), aAnnotation.getEnd());
    }

    /**
     * @deprecated Use {@link #rangeCoveringAnnotations(Iterable)} instead.
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    public Range(Iterable<? extends AnnotationFS> aAnnotations)
    {
        this(spanOf(aAnnotations));
    }

    /**
     * An empty collection has no span, which is exactly what {@link #UNDEFINED} means.
     */
    private static Range spanOf(Iterable<? extends AnnotationFS> aAnnotations)
    {
        var i = aAnnotations.iterator();
        if (!i.hasNext()) {
            return UNDEFINED;
        }

        var current = i.next();
        var b = current.getBegin();
        var e = current.getEnd();

        while (i.hasNext()) {
            current = i.next();
            b = min(current.getBegin(), b);
            e = max(current.getEnd(), e);
        }

        return new Range(b, e);
    }

    @Override
    public int getBegin()
    {
        return begin;
    }

    @Override
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

        if (clippedBegin > length || clippedEnd > length || clippedEnd < 0) {
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

    /**
     * @param aRange
     *            the range to test, may be {@code null}.
     * @return whether the range expresses no location. Construction rejects any other negative
     *         range, so this is the only way a range can fail to name one.
     */
    public static boolean isUndefined(Range aRange)
    {
        return aRange == null || UNDEFINED.equals(aRange);
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
