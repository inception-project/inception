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
package de.tudarmstadt.ukp.inception.annotation.layer.span;

import java.util.Optional;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class CreateSpanAnnotationRequest
{
    private final SourceDocument document;
    private final String username;
    private final CAS cas;
    private final int begin;
    private final int end;
    private final CreateSpanAnnotationRequest originalRequest;

    public CreateSpanAnnotationRequest(SourceDocument aDocument, String aUsername, CAS aCas,
            int aBegin, int aEnd)
    {
        this(null, aDocument, aUsername, aCas, aBegin, aEnd);
    }

    private CreateSpanAnnotationRequest(CreateSpanAnnotationRequest aOriginal,
            SourceDocument aDocument, String aUsername, CAS aCas, int aBegin, int aEnd)
    {
        originalRequest = aOriginal;
        document = aDocument;
        username = aUsername;
        cas = aCas;
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

    public Optional<CreateSpanAnnotationRequest> getOriginalRequest()
    {
        return Optional.ofNullable(originalRequest);
    }

    public CreateSpanAnnotationRequest changeSpan(int aBegin, int aEnd)
    {
        return new CreateSpanAnnotationRequest(this, document, username, cas, aBegin, aEnd);
    }
}
