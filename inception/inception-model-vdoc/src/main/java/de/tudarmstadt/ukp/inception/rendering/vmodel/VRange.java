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
package de.tudarmstadt.ukp.inception.rendering.vmodel;

import java.io.Serializable;
import java.util.Optional;

import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationPredicates;

import de.tudarmstadt.ukp.inception.support.annotation.OffsetSpan;

public class VRange
    implements Serializable
{
    private static final long serialVersionUID = 7134433544759110764L;

    private final int begin;
    private final int end;
    private final int originalBegin;
    private final int originalEnd;

    /**
     * @param aAdjustedBegin
     *            begin adjusted to the viewport (can be negative).
     * @param aAdjustedEnd
     *            end adjusted to the viewport (can be negative).
     */
    public VRange(int aAdjustedBegin, int aAdjustedEnd)
    {
        begin = aAdjustedBegin;
        end = aAdjustedEnd;
        originalBegin = aAdjustedBegin;
        originalEnd = aAdjustedEnd;
    }

    /**
     * @param aAdjustedBegin
     *            begin adjusted to the viewport (can be negative).
     * @param aAdjustedEnd
     *            end adjusted to the viewport (can be negative).
     * @param aClippedBegin
     *            begin clipped to the viewport.
     * @param aClippedEnd
     *            end clipped to the viewport.
     */
    private VRange(int aAdjustedBegin, int aAdjustedEnd, int aClippedBegin, int aClippedEnd)
    {
        originalBegin = aAdjustedBegin;
        originalEnd = aAdjustedEnd;
        begin = aClippedBegin;
        end = aClippedEnd;
    }

    public int getBegin()
    {
        return begin;
    }

    public int getEnd()
    {
        return end;
    }

    public int getOriginalBegin()
    {
        return originalBegin;
    }

    public int getOriginalEnd()
    {
        return originalEnd;
    }

    public boolean isClippedAtBegin()
    {
        return originalBegin != begin;
    }

    public boolean isClippedAtEnd()
    {
        return originalEnd != end;
    }

    public static Optional<VRange> clippedRange(int aViewportBegin, int aViewPortEnd,
            AnnotationFS aAnnotation)
    {
        return clippedRange(aViewportBegin, aViewPortEnd, aAnnotation.getBegin(),
                aAnnotation.getEnd());
    }

    public static Optional<VRange> clippedRange(int aViewportBegin, int aViewPortEnd,
            OffsetSpan aAnnotation)
    {
        return clippedRange(aViewportBegin, aViewPortEnd, aAnnotation.getBegin(),
                aAnnotation.getEnd());
    }

    public static Optional<VRange> clippedRange(VDocument aDoc, int aBegin, int aEnd)
    {
        return clippedRange(aDoc.getWindowBegin(), aDoc.getWindowEnd(), aBegin, aEnd);
    }

    public static Optional<VRange> clippedRange(VDocument aDoc, OffsetSpan aFS)
    {
        return clippedRange(aDoc.getWindowBegin(), aDoc.getWindowEnd(), aFS.getBegin(),
                aFS.getEnd());
    }

    public static Optional<VRange> clippedRange(VDocument aDoc, AnnotationFS aFS)
    {
        return clippedRange(aDoc.getWindowBegin(), aDoc.getWindowEnd(), aFS.getBegin(),
                aFS.getEnd());
    }

    public static Optional<VRange> clippedRange(int aViewportBegin, int aViewPortEnd, int aBegin,
            int aEnd)
    {
        // Range is fully outside the viewport.
        if (!AnnotationPredicates.overlapping(aViewportBegin, aViewPortEnd, aBegin, aEnd)) {
            return Optional.empty();
        }

        int begin = aBegin - aViewportBegin;
        if (begin < 0) {
            begin = 0;
        }

        int end = aEnd;
        if (end > aViewPortEnd) {
            end = aViewPortEnd;
        }
        end = end - aViewportBegin;

        return Optional.of(new VRange(aBegin - aViewportBegin, aEnd - aViewportBegin, begin, end));
    }

    @Override
    public String toString()
    {
        return "[" + begin + "-" + end + "]";
    }
}
