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

import java.util.Optional;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public abstract class DocumentAnnotationRequest_ImplBase<T extends DocumentAnnotationRequest_ImplBase<T>>
{
    private final SourceDocument document;
    private final String documentOwner;
    private final CAS cas;
    private final T originalRequest;

    public DocumentAnnotationRequest_ImplBase(SourceDocument aDocument, String aDocumentOwner,
            CAS aCas)
    {
        this(null, aDocument, aDocumentOwner, aCas);
    }

    protected DocumentAnnotationRequest_ImplBase(T aOriginal, SourceDocument aDocument,
            String aDocumentOwner, CAS aCas)
    {
        originalRequest = aOriginal;
        document = aDocument;
        documentOwner = aDocumentOwner;
        cas = aCas;
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

    public Optional<T> getOriginalRequest()
    {
        return Optional.ofNullable(originalRequest);
    }

    public abstract T changeSpan(int aBegin, int aEnd, AnchoringMode aAnchoringMode);
}
