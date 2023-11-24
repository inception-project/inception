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
package de.tudarmstadt.ukp.inception.annotation.events;

import org.apache.uima.cas.CAS;
import org.springframework.context.ApplicationEvent;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.support.wicket.event.HybridApplicationUIEvent;

public class DocumentOpenedEvent
    extends ApplicationEvent
    implements HybridApplicationUIEvent
{
    private static final long serialVersionUID = -2739175937794842083L;

    private final CAS cas;
    private final SourceDocument document;
    // user who owns/annotates the opened document
    private final String documentOwner;
    // user who opened the document
    private final String sessionOwner;
    private final AnnotationDocumentState stateBeforeOpening;

    public DocumentOpenedEvent(Object aSource, CAS aCas, SourceDocument aDocument,
            AnnotationDocumentState aStateBeforeOpening, String aDocumentOwner,
            String aSessionOwner)
    {
        super(aSource);
        cas = aCas;
        document = aDocument;
        documentOwner = aDocumentOwner;
        sessionOwner = aSessionOwner;
        stateBeforeOpening = aStateBeforeOpening;
    }

    public CAS getCas()
    {
        return cas;
    }

    public SourceDocument getDocument()
    {
        return document;
    }

    public String getSessionOwner()
    {
        return sessionOwner;
    }

    public String getDocumentOwner()
    {
        return documentOwner;
    }

    public AnnotationDocumentState getStateBeforeOpening()
    {
        return stateBeforeOpening;
    }
}
