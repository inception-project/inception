/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.model;

import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public interface AnnotatorDocumentNavigation
{
    // ---------------------------------------------------------------------------------------------
    // Document
    // ---------------------------------------------------------------------------------------------
    SourceDocument getDocument();

    void setDocument(SourceDocument aDocument, List<SourceDocument> aDocuments);

    int getDocumentIndex();

    int getNumberOfDocuments();
    
    // ---------------------------------------------------------------------------------------------
    // Navigation within or across a document
    // ---------------------------------------------------------------------------------------------
    default void moveToPreviousDocument(List<SourceDocument> aDocuments)
    {
        // Index of the current source document in the list
        int currentDocumentIndex = aDocuments.indexOf(getDocument());

        // If the first the document
        if (currentDocumentIndex <= 0) {
            throw new IllegalStateException("This is the first document!");
        }

        setDocument(aDocuments.get(currentDocumentIndex - 1), aDocuments);
    }

    default void moveToNextDocument(List<SourceDocument> aDocuments)
    {
        // Index of the current source document in the list
        int currentDocumentIndex = aDocuments.indexOf(getDocument());

        // If the last document
        if (currentDocumentIndex >= aDocuments.size() - 1) {
            throw new IllegalStateException("This is the last document!");
        }

        setDocument(aDocuments.get(currentDocumentIndex + 1), aDocuments);
    }
}
