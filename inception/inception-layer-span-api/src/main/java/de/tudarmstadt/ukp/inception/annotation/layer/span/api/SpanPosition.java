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
package de.tudarmstadt.ukp.inception.annotation.layer.span.api;

import java.util.Objects;

import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureMultiplicityMode;
import de.tudarmstadt.ukp.inception.curation.api.Position;
import de.tudarmstadt.ukp.inception.curation.api.Position_ImplBase;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;

/**
 * Represents a span position in the text.
 */
public class SpanPosition
    extends Position_ImplBase
{
    private static final long serialVersionUID = 7672904919600263605L;

    private final int begin;
    private final int end;
    private final String text;

    private SpanPosition(Builder builder)
    {
        super(builder.collectionId, builder.documentId, builder.type, builder.linkFeature,
                builder.linkRole, builder.linkTargetBegin, builder.linkTargetEnd,
                builder.linkTargetText, builder.linkFeatureMultiplicityMode);
        begin = builder.begin;
        end = builder.end;
        text = builder.text;
    }

    public SpanPosition(String aCollectionId, String aDocumentId, String aType, int aBegin,
            int aEnd, String aText)
    {
        super(aCollectionId, aDocumentId, aType);
        begin = aBegin;
        end = aEnd;
        text = aText;
    }

    /**
     * @return the span position that owns the link feature (if this is a link feature position).
     */
    public SpanPosition getBasePosition()
    {
        return new SpanPosition(getCollectionId(), getDocumentId(), getType(), begin, end, text);
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
    public boolean equals(final Object other)
    {
        if (!(other instanceof SpanPosition)) {
            return false;
        }
        if (!super.equals(other)) {
            return false;
        }
        SpanPosition castOther = (SpanPosition) other;
        return Objects.equals(begin, castOther.begin) && Objects.equals(end, castOther.end);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), begin, end);
    }

    @Override
    public String toString()
    {
        var builder = new StringBuilder();
        toStringFragment(builder);
        if (!builder.isEmpty()) {
            builder.append(", ");
        }

        builder.insert(0, "Span [");
        builder.append("span=(").append(begin).append('-').append(end).append(')');
        builder.append('[').append(text).append(']');
        builder.append(']');
        return builder.toString();
    }

    @Override
    public String toMinimalString()
    {
        var builder = new StringBuilder();
        builder.append(begin).append('-').append(end).append(" [").append(text).append(']');

        var linkCompareBehavior = getLinkFeatureMultiplicityMode();
        if (linkCompareBehavior != null) {
            switch (linkCompareBehavior) {
            case ONE_TARGET_MULTIPLE_ROLES:
                builder.append(" role: [").append(getLinkRole()).append(']');
                break;
            case MULTIPLE_TARGETS_ONE_ROLE:
                builder.append(" -> [").append(getLinkTargetBegin()).append('-')
                        .append(getLinkTargetEnd()).append(" [").append(getLinkTargetText())
                        .append(']');
                break;
            case MULTIPLE_TARGETS_MULTIPLE_ROLES:
                builder.append(" -> ").append(getLinkRole()).append("@[")
                        .append(getLinkTargetBegin()).append('-').append(getLinkTargetEnd())
                        .append(" [").append(getLinkTargetText()).append(']');
                break;
            default:
                throw new IllegalStateException(
                        "Unknown link target comparison mode [" + linkCompareBehavior + "]");
            }
        }

        return builder.toString();
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private String collectionId;
        private String documentId;
        private String type;

        private int begin;
        private int end;
        private String text;

        private String linkFeature = null;
        private String linkRole = null;
        private int linkTargetBegin = -1;
        private int linkTargetEnd = -1;
        private String linkTargetText = null;
        private LinkFeatureMultiplicityMode linkFeatureMultiplicityMode = null;

        private Builder()
        {
        }

        public Builder forAnnotation(Annotation aAnnotation)
        {
            var cas = aAnnotation.getCAS();
            try {
                var dmd = WebAnnoCasUtil.getDocumentMetadata(cas);
                collectionId = FSUtil.getFeature(dmd, "collectionId", String.class);
                documentId = FSUtil.getFeature(dmd, "documentId", String.class);
            }
            catch (IllegalArgumentException e) {
                // We use this information only for debugging - so we can ignore if the information
                // is missing.
                collectionId = null;
                documentId = null;
            }

            type = aAnnotation.getType().getName();
            begin = aAnnotation.getBegin();
            end = aAnnotation.getEnd();
            text = aAnnotation.getCoveredText();

            return this;
        }

        public Builder withCollectionId(String aCollectionId)
        {
            collectionId = aCollectionId;
            return this;
        }

        public Builder withDocumentId(String aDocumentId)
        {
            documentId = aDocumentId;
            return this;
        }

        public Builder withType(String aType)
        {
            type = aType;
            return this;
        }

        public Builder withLinkFeature(String aFeature)
        {
            linkFeature = aFeature;
            return this;
        }

        public Builder withLinkRole(String aRole)
        {
            linkRole = aRole;
            return this;
        }

        public Builder withLinkTarget(AnnotationFS aLinkTarget)
        {
            linkTargetBegin = aLinkTarget.getBegin();
            linkTargetEnd = aLinkTarget.getEnd();
            linkTargetText = aLinkTarget.getCoveredText();
            return this;
        }

        public Builder withLinkTargetBegin(int aLinkTargetBegin)
        {
            linkTargetBegin = aLinkTargetBegin;
            return this;
        }

        public Builder withLinkTargetEnd(int aLinkTargetEnd)
        {
            linkTargetEnd = aLinkTargetEnd;
            return this;
        }

        public Builder withLinkTargetText(String aLinkTargetText)
        {
            linkTargetText = aLinkTargetText;
            return this;
        }

        public Builder withLinkFeatureMultiplicityMode(LinkFeatureMultiplicityMode aBehavior)
        {
            linkFeatureMultiplicityMode = aBehavior;
            return this;
        }

        public Builder withBegin(int aBegin)
        {
            begin = aBegin;
            return this;
        }

        public Builder withEnd(int aEnd)
        {
            end = aEnd;
            return this;
        }

        public Builder withText(String aText)
        {
            text = aText;
            return this;
        }

        public SpanPosition build()
        {
            return new SpanPosition(this);
        }
    }
}
