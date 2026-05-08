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

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class CreateSpanAnnotationRequest
    extends SpanAnnotationRequest_ImplBase<CreateSpanAnnotationRequest>
{
    private CreateSpanAnnotationRequest(Builder builder)
    {
        super(builder.original, builder.document, builder.documentOwner, builder.cas, builder.begin,
                builder.end, builder.anchoringMode);
    }

    @Override
    public CreateSpanAnnotationRequest changeSpan(int aBegin, int aEnd,
            AnchoringMode aAnchoringMode)
    {
        return CreateSpanAnnotationRequest.builder() //
                .withOriginal(this) //
                .withDocument(getDocument(), getDocumentOwner(), getCas()) //
                .withRange(aBegin, aEnd) //
                .withAnchoringMode(aAnchoringMode) //
                .build();
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private CreateSpanAnnotationRequest original;
        private SourceDocument document;
        private String documentOwner;
        private CAS cas;
        private int begin;
        private int end;
        private AnchoringMode anchoringMode;

        private Builder()
        {
        }

        public Builder withOriginal(CreateSpanAnnotationRequest aOriginal)
        {
            original = aOriginal;
            return this;
        }

        public Builder withDocument(SourceDocument aDocument)
        {
            document = aDocument;
            return this;
        }

        public Builder withDataOwner(String aDocumentOwner)
        {
            documentOwner = aDocumentOwner;
            return this;
        }

        public Builder withCas(CAS aCas)
        {
            cas = aCas;
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

        public Builder withDocument(SourceDocument aDocument, String aDocumentOwner, CAS aCas)
        {
            document = aDocument;
            documentOwner = aDocumentOwner;
            cas = aCas;
            return this;
        }

        public Builder withRange(int aBegin, int aEnd)
        {
            begin = aBegin;
            end = aEnd;
            return this;
        }

        public Builder withAnchoringMode(AnchoringMode aAnchoringMode)
        {
            anchoringMode = aAnchoringMode;
            return this;
        }

        public CreateSpanAnnotationRequest build()
        {
            return new CreateSpanAnnotationRequest(this);
        }
    }
}
