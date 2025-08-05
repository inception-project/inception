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

import java.util.Optional;

import org.apache.commons.lang3.Validate;
import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public abstract class SpanAnnotationRequest_ImplBase<T extends SpanAnnotationRequest_ImplBase<T>>
{
    private final SourceDocument document;
    private final String documentOwner;
    private final CAS cas;
    private final int begin;
    private final int end;
    private final T originalRequest;
    private final AnchoringMode anchoringMode;

    public SpanAnnotationRequest_ImplBase(SourceDocument aDocument, String aDocumentOwner, CAS aCas,
            int aBegin, int aEnd, AnchoringMode aAnchoringMode)
    {
        this(null, aDocument, aDocumentOwner, aCas, aBegin, aEnd, aAnchoringMode);
    }

    protected SpanAnnotationRequest_ImplBase(T aOriginal, SourceDocument aDocument,
            String aDocumentOwner, CAS aCas, int aBegin, int aEnd, AnchoringMode aAnchoringMode)
    {
        Validate.isTrue(aBegin <= aEnd, "Annotation begin [%d] must smaller or equal to end [%d]",
                aBegin, aEnd);

        originalRequest = aOriginal;
        document = aDocument;
        documentOwner = aDocumentOwner;
        cas = aCas;
        begin = aBegin;
        end = aEnd;
        anchoringMode = aAnchoringMode;
    }

    public SourceDocument getDocument()
    {
        return document;
    }

    public String getDocumentOwner()
    {
        return documentOwner;
    }

    public CAS getCas()
    {
        return cas;
    }

    public int getBegin()
    {
        return begin;
    }

    public int getEnd()
    {
        return end;
    }

    public AnchoringMode getAnchoringMode()
    {
        return anchoringMode;
    }

    public Optional<T> getOriginalRequest()
    {
        return Optional.ofNullable(originalRequest);
    }

    public abstract T changeSpan(int aBegin, int aEnd, AnchoringMode aAnchoringMode);
}
