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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model;

import java.io.Serializable;
import java.util.Optional;

import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.inception.support.annotation.OffsetSpan;

public class VRange
    implements Serializable
{
    private static final long serialVersionUID = 7134433544759110764L;

    private final int begin;
    private final int end;
    private final boolean clippedAtBegin;
    private final boolean clippedAtEnd;

    public VRange(int aBegin, int aEnd)
    {
        begin = aBegin;
        end = aEnd;
        clippedAtBegin = false;
        clippedAtEnd = false;
    }

    private VRange(int aBegin, int aEnd, boolean aClippedAtBegin, boolean aClippedAtEnd)
    {
        begin = aBegin;
        end = aEnd;
        clippedAtBegin = aClippedAtBegin;
        clippedAtEnd = aClippedAtEnd;
    }

    public int getBegin()
    {
        return begin;
    }

    public int getEnd()
    {
        return end;
    }

    public boolean isClippedAtBegin()
    {
        return clippedAtBegin;
    }

    public boolean isClippedAtEnd()
    {
        return clippedAtEnd;
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
        if (aEnd < aViewportBegin || aBegin > aViewPortEnd) {
            return Optional.empty();
        }

        int begin = aBegin - aViewportBegin;
        boolean clippedAtBegin = false;
        if (begin < 0) {
            begin = 0;
            clippedAtBegin = true;
        }

        int end = aEnd;
        boolean clippedAtEnd = false;
        if (end > aViewPortEnd) {
            end = aViewPortEnd;
            clippedAtEnd = true;
        }
        end = end - aViewportBegin;

        return Optional.of(new VRange(begin, end, clippedAtBegin, clippedAtEnd));
    }

    @Override
    public String toString()
    {
        return "[" + begin + "-" + end + "]";
    }
}
