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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.documents;

import static java.util.Arrays.asList;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.wicketstuff.event.annotation.AbstractAjaxAwareEvent;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

/**
 * Fired when a user clicks to delete a document.
 */
public class SourceDocumentImportedEvent
    extends AbstractAjaxAwareEvent
{
    private final List<SourceDocument> documents;

    public SourceDocumentImportedEvent(AjaxRequestTarget aTarget, SourceDocument... aDocuments)
    {
        this(aTarget, asList(aDocuments));
    }

    public SourceDocumentImportedEvent(AjaxRequestTarget aTarget, List<SourceDocument> aDocuments)
    {
        super(aTarget);

        documents = aDocuments;
    }

    public List<SourceDocument> getDocuments()
    {
        return documents;
    }
}
