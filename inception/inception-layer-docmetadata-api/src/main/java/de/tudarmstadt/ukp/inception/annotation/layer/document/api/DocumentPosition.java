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
package de.tudarmstadt.ukp.inception.annotation.layer.document.api;

import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.cas.AnnotationBase;

import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureMultiplicityMode;
import de.tudarmstadt.ukp.inception.curation.api.Position_ImplBase;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;

/**
 * Represents a document position.
 */
public class DocumentPosition
    extends Position_ImplBase
{
    private static final long serialVersionUID = -1020728944030217843L;

    private DocumentPosition(Builder builder)
    {
        super(builder.collectionId, builder.documentId, builder.type, builder.linkFeature,
                builder.linkRole, builder.linkTargetBegin, builder.linkTargetEnd,
                builder.linkTargetText, builder.linkFeatureMultiplicityMode);
    }

    public DocumentPosition(String aCollectionId, String aDocumentId, String aType, String aFeature,
            String aRole, int aLinkTargetBegin, int aLinkTargetEnd, String aLinkTargetText,
            LinkFeatureMultiplicityMode aLinkCompareBehavior)
    {
        super(aCollectionId, aDocumentId, aType, aFeature, aRole, aLinkTargetBegin, aLinkTargetEnd,
                aLinkTargetText, aLinkCompareBehavior);
    }

    @Override
    public String toString()
    {
        var builder = new StringBuilder();
        toStringFragment(builder);
        if (!builder.isEmpty()) {
            builder.append(", ");
        }
        builder.insert(0, "Document [");
        builder.append(']');
        return builder.toString();
    }

    @Override
    public String toMinimalString()
    {
        return "Document";
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

        private String linkFeature = null;
        private String linkRole = null;
        private int linkTargetBegin = -1;
        private int linkTargetEnd = -1;
        private String linkTargetText = null;
        private LinkFeatureMultiplicityMode linkFeatureMultiplicityMode = null;

        private Builder()
        {
        }

        public Builder forAnnotation(AnnotationBase aAnnotation)
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

        public DocumentPosition build()
        {
            return new DocumentPosition(this);
        }
    }
}
