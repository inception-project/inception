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
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class MoveSpanAnnotationRequest
    extends SpanAnnotationRequest_ImplBase<MoveSpanAnnotationRequest>
{
    private final AnnotationFS annotation;

    private MoveSpanAnnotationRequest(Builder builder)
    {
        super(builder.original, builder.document, builder.documentOwner, builder.cas, builder.begin,
                builder.end, builder.anchoringMode);
        annotation = builder.annotation;
    }

    public AnnotationFS getAnnotation()
    {
        return annotation;
    }

    @Override
    public MoveSpanAnnotationRequest changeSpan(int aBegin, int aEnd, AnchoringMode aAnchoringMode)
    {
        return MoveSpanAnnotationRequest.builder() //
                .withOriginal(this) //
                .withDocument(getDocument(), getDocumentOwner(), getCas()) //
                .withAnnotation(getAnnotation()) //
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
        private MoveSpanAnnotationRequest original;
        private SourceDocument document;
        private String documentOwner;
        private CAS cas;
        private int begin;
        private int end;
        private AnnotationFS annotation;
        private AnchoringMode anchoringMode;

        private Builder()
        {
        }

        public Builder withOriginal(MoveSpanAnnotationRequest aOriginal)
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

        public Builder withAnnotation(AnnotationFS aAnnotation)
        {
            annotation = aAnnotation;
            return this;
        }

        public Builder withAnchoringMode(AnchoringMode aAnchoringMode)
        {
            anchoringMode = aAnchoringMode;
            return this;
        }

        public MoveSpanAnnotationRequest build()
        {
            return new MoveSpanAnnotationRequest(this);
        }
    }
}
