/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter;

import java.util.Optional;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class CreateSpanAnnotationRequest
{
    private final SourceDocument document;
    private final String username;
    private final CAS jcas;
    private final int begin;
    private final int end;
    private final CreateSpanAnnotationRequest originalRequest;

    public CreateSpanAnnotationRequest(SourceDocument aDocument, String aUsername, CAS aJCas,
            int aBegin, int aEnd)
    {
        this(null, aDocument, aUsername, aJCas, aBegin, aEnd);
    }

    private CreateSpanAnnotationRequest(CreateSpanAnnotationRequest aOriginal,
            SourceDocument aDocument, String aUsername, CAS aJCas, int aBegin, int aEnd)
    {
        originalRequest = aOriginal;
        document = aDocument;
        username = aUsername;
        jcas = aJCas;
        begin = aBegin;
        end = aEnd;
    }

    public SourceDocument getDocument()
    {
        return document;
    }

    public String getUsername()
    {
        return username;
    }

    public CAS getCas()
    {
        return jcas;
    }

    public int getBegin()
    {
        return begin;
    }

    public int getEnd()
    {
        return end;
    }
    
    public Optional<CreateSpanAnnotationRequest> getOriginalRequest()
    {
        return Optional.ofNullable(originalRequest);
    }
    
    public CreateSpanAnnotationRequest changeSpan(int aBegin, int aEnd)
    {
        return new CreateSpanAnnotationRequest(this, document, username, jcas, aBegin, aEnd);
    }
}
