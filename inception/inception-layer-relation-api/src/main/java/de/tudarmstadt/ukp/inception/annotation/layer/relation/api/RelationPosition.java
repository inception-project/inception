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
package de.tudarmstadt.ukp.inception.annotation.layer.relation.api;

import java.util.Objects;

import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureMultiplicityMode;
import de.tudarmstadt.ukp.inception.curation.api.Position;
import de.tudarmstadt.ukp.inception.curation.api.Position_ImplBase;

/**
 * Represents a span position in the text.
 */
public class RelationPosition
    extends Position_ImplBase
{
    private static final long serialVersionUID = 2389265017101957950L;

    private final int sourceBegin;
    private final int sourceEnd;
    private final String sourceText;
    private final int targetBegin;
    private final int targetEnd;
    private final String targetText;

    public RelationPosition(String aCollectionId, String aDocumentId, String aType,
            int aSourceBegin, int aSourceEnd, String aSourceText, int aTargetBegin, int aTargetEnd,
            String aTargetText, String aFeature, String aRole, int aLinkTargetBegin,
            int aLinkTargetEnd, String aLinkTargetText,
            LinkFeatureMultiplicityMode aLinkCompareBehavior)
    {
        super(aCollectionId, aDocumentId, aType, aFeature, aRole, aLinkTargetBegin, aLinkTargetEnd,
                aLinkTargetText, aLinkCompareBehavior);
        sourceBegin = aSourceBegin;
        sourceEnd = aSourceEnd;
        sourceText = aSourceText;
        targetBegin = aTargetBegin;
        targetEnd = aTargetEnd;
        targetText = aTargetText;
    }

    /**
     * @return the source begin offset.
     */
    public int getSourceBegin()
    {
        return sourceBegin;
    }

    /**
     * @return the source end offset.
     */
    public int getSourceEnd()
    {
        return sourceEnd;
    }

    /**
     * @return the target begin offset.
     */
    public int getTargetBegin()
    {
        return targetBegin;
    }

    /**
     * @return the target end offset.
     */
    public int getTargetEnd()
    {
        return targetEnd;
    }

    @Override
    public int compareTo(Position aOther)
    {
        int superCompare = super.compareTo(aOther);
        if (superCompare != 0) {
            return superCompare;
        }
        // Order doesn't really matter, but this should sort in the same way as UIMA does:
        // begin ascending
        // end descending
        else {
            RelationPosition otherSpan = (RelationPosition) aOther;
            if (sourceBegin != otherSpan.sourceBegin) {
                return sourceBegin - otherSpan.sourceBegin;
            }
            else if (sourceEnd != otherSpan.sourceEnd) {
                return otherSpan.sourceEnd - sourceEnd;
            }
            else if (targetBegin != otherSpan.targetBegin) {
                return targetBegin - otherSpan.targetBegin;
            }
            else {
                return otherSpan.targetEnd - targetEnd;
            }
        }
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof RelationPosition)) {
            return false;
        }
        if (!super.equals(other)) {
            return false;
        }
        RelationPosition castOther = (RelationPosition) other;
        return Objects.equals(sourceBegin, castOther.sourceBegin)
                && Objects.equals(sourceEnd, castOther.sourceEnd)
                && Objects.equals(targetBegin, castOther.targetBegin)
                && Objects.equals(targetEnd, castOther.targetEnd);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), sourceBegin, sourceEnd, targetBegin, targetEnd);
    }

    @Override
    public String toString()
    {
        var builder = new StringBuilder();
        toStringFragment(builder);
        if (!builder.isEmpty()) {
            builder.append(", ");
        }
        builder.insert(0, "Relation [");
        builder.append("source=(").append(sourceBegin).append('-').append(sourceEnd).append(')');
        builder.append('[').append(sourceText).append(']');
        builder.append(", target=(").append(targetBegin).append('-').append(targetEnd).append(')');
        builder.append('[').append(targetText).append(']');
        builder.append("]");
        return builder.toString();
    }

    @Override
    public String toMinimalString()
    {
        return "(" + sourceBegin + '-' + sourceEnd + ')' + '[' + sourceText + ']' + " -> ("
                + targetBegin + '-' + targetEnd + ')' + " [" + targetText + ']';
    }
}
