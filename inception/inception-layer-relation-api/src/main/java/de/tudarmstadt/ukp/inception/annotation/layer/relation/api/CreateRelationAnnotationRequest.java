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
package de.tudarmstadt.ukp.inception.annotation.layer.relation.api;

import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class CreateRelationAnnotationRequest
{
    private final SourceDocument document;
    private final String username;
    private final CAS cas;
    private final AnnotationFS originFs;
    private final AnnotationFS targetFs;
    private final CreateRelationAnnotationRequest originalRequest;

    public CreateRelationAnnotationRequest(SourceDocument aDocument, String aUsername, CAS aCas,
            AnnotationFS aOriginFs, AnnotationFS aTargetFs)
    {
        this(null, aDocument, aUsername, aCas, aOriginFs, aTargetFs);
    }

    public CreateRelationAnnotationRequest(CreateRelationAnnotationRequest aOriginal,
            SourceDocument aDocument, String aUsername, CAS aCas, AnnotationFS aOriginFs,
            AnnotationFS aTargetFs)
    {
        originalRequest = aOriginal;
        document = aDocument;
        username = aUsername;
        cas = aCas;
        originFs = aOriginFs;
        targetFs = aTargetFs;
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

    public AnnotationFS getOriginFs()
    {
        return originFs;
    }

    public AnnotationFS getTargetFs()
    {
        return targetFs;
    }

    public Optional<CreateRelationAnnotationRequest> getOriginalRequest()
    {
        return Optional.ofNullable(originalRequest);
    }

    public CreateRelationAnnotationRequest changeRelation(AnnotationFS aOrigin,
            AnnotationFS aTarget)
    {
        return new CreateRelationAnnotationRequest(this, document, username, cas, aOrigin, aTarget);
    }
}
