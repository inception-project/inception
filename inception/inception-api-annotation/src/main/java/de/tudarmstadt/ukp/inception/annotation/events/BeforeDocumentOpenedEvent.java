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

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.support.wicket.event.HybridApplicationUIEvent;

/**
 * Fire BeforeDocumentOpenedEvent to give listeners a chance to look at or even modify the CAS
 * before the editor gets it. Modification should not be performed if editable is false. If editable
 * is true, then the modification is persisted.
 */
public class BeforeDocumentOpenedEvent
    extends ApplicationEvent
    implements HybridApplicationUIEvent
{
    private static final long serialVersionUID = -4644605041626140906L;

    private final CAS cas;
    private final SourceDocument document;
    // user who owns/annotates the opened document
    private final String documentOwner;
    // user who opened the document
    private final String sessionOwner;
    private final boolean editable;

    public BeforeDocumentOpenedEvent(Object aSource, CAS aCas, SourceDocument aDocument,
            String aDocumentOwner, String aSessionOwner, boolean aEditable)
    {
        super(aSource);
        cas = aCas;
        document = aDocument;
        documentOwner = aDocumentOwner;
        sessionOwner = aSessionOwner;
        editable = aEditable;
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

    public boolean isEditable()
    {
        return editable;
    }
}
