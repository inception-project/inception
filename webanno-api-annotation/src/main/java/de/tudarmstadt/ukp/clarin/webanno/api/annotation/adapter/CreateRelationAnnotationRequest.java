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
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class CreateRelationAnnotationRequest
{
    private final SourceDocument document;
    private final String username;
    private final CAS jcas;
    private final AnnotationFS originFs;
    private final AnnotationFS targetFs;
    private final CreateRelationAnnotationRequest originalRequest;

    private final int windowBegin;
    private final int windowEnd;

    public CreateRelationAnnotationRequest(SourceDocument aDocument, String aUsername, CAS aJCas,
            AnnotationFS aOriginFs, AnnotationFS aTargetF, int aWindowBegin, int aWindowEnd)
    {
        this(null, aDocument, aUsername, aJCas, aOriginFs, aTargetF, aWindowBegin, aWindowEnd);
    }

    public CreateRelationAnnotationRequest(CreateRelationAnnotationRequest aOriginal,
            SourceDocument aDocument, String aUsername, CAS aJCas, AnnotationFS aOriginFs,
            AnnotationFS aTargetFs, int aWindowBegin, int aWindowEnd)
    {
        originalRequest = aOriginal;
        document = aDocument;
        username = aUsername;
        jcas = aJCas;
        originFs = aOriginFs;
        targetFs = aTargetFs;

        windowBegin = aWindowBegin;
        windowEnd = aWindowEnd;
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

    public AnnotationFS getOriginFs()
    {
        return originFs;
    }

    public AnnotationFS getTargetFs()
    {
        return targetFs;
    }

    public int getWindowBegin()
    {
        return windowBegin;
    }

    public int getWindowEnd()
    {
        return windowEnd;
    }

    public Optional<CreateRelationAnnotationRequest> getOriginalRequest()
    {
        return Optional.ofNullable(originalRequest);
    }

    public CreateRelationAnnotationRequest changeRelation(AnnotationFS aOrigin,
            AnnotationFS aTarget)
    {
        return new CreateRelationAnnotationRequest(this, document, username, jcas, aOrigin, aTarget,
                windowBegin, windowEnd);
    }
}
