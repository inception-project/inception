/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.span;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.LinkCompareBehavior;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.Position;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.Position_ImplBase;

/**
 * Represents a span position in the text.
 */
public class SpanPosition extends Position_ImplBase
{
    private final int begin;
    private final int end;
    private final String text;

    public SpanPosition(String aCollectionId, String aDocumentId, int aCasId, String aType,
            int aBegin, int aEnd, String aText, String aFeature, String aRole,
            int aLinkTargetBegin, int aLinkTargetEnd, String aLinkTargetText,
            LinkCompareBehavior aLinkCompareBehavior)
    {
        super(aCollectionId, aDocumentId, aCasId, aType, aFeature, aRole, aLinkTargetBegin,
                aLinkTargetEnd, aLinkTargetText, aLinkCompareBehavior);
        begin = aBegin;
        end = aEnd;
        text = aText;
    }
    
    /**
     * @return the begin offset.
     */
    public int getBegin()
    {
        return begin;
    }

    /**
     * @return the end offset.
     */
    public int getEnd()
    {
        return end;
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
            SpanPosition otherSpan = (SpanPosition) aOther;
            if (begin == otherSpan.begin) {
                return otherSpan.end - end;
            }
            else {
                return begin - otherSpan.begin;
            }
        }
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Span [");
        toStringFragment(builder);
        builder.append(", span=(").append(begin).append('-').append(end).append(')');
        builder.append('[').append(text).append(']');
        builder.append(']');
        return builder.toString();
    }

    @Override
    public String toMinimalString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(begin).append('-').append(end).append(" [").append(text).append(']');
        LinkCompareBehavior linkCompareBehavior = getLinkCompareBehavior();
        if (linkCompareBehavior != null) {
            switch (linkCompareBehavior) {
            case LINK_TARGET_AS_LABEL:
                builder.append(" role: [").append(getRole()).append(']');
                break;
            case LINK_ROLE_AS_LABEL:
                builder.append(" -> [").append(getLinkTargetBegin()).append('-')
                        .append(getLinkTargetEnd()).append(" [").append(getLinkTargetText())
                        .append(']');
                break;
            default:
                throw new IllegalStateException("Unknown link target comparison mode ["
                        + linkCompareBehavior + "]");
            }
        }
        return builder.toString();
    }
}
