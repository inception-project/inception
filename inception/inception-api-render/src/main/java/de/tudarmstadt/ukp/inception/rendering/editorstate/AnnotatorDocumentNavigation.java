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
package de.tudarmstadt.ukp.inception.rendering.editorstate;

import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;

public interface AnnotatorDocumentNavigation
{
    // ---------------------------------------------------------------------------------------------
    // Document
    // ---------------------------------------------------------------------------------------------
    void clearDocument();

    SourceDocument getDocument();

    void setDocument(SourceDocument aDocument, List<SourceDocument> aDocuments);

    void refreshDocument(DocumentService aDocumentService);

    int getDocumentIndex();

    int getNumberOfDocuments();

    // ---------------------------------------------------------------------------------------------
    // Navigation within or across a document
    // ---------------------------------------------------------------------------------------------
    default boolean hasPreviousDocument(List<SourceDocument> aDocuments)
    {
        return aDocuments.indexOf(getDocument()) > 0;
    }

    default boolean hasNextDocument(List<SourceDocument> aDocuments)
    {
        int currentDocumentIndex = aDocuments.indexOf(getDocument());
        return currentDocumentIndex >= 0 && currentDocumentIndex < aDocuments.size() - 1;
    }

    /**
     * Moves the selection to the document preceding the current document in the given list. Has no
     * effect if the current document is already the first document or if the current document is
     * not actually part of the given list.
     * 
     * @param aDocuments
     *            a list of documents.
     * @return whether the current document has changed.
     */
    default boolean moveToPreviousDocument(List<SourceDocument> aDocuments)
    {
        // Index of the current source document in the list
        int currentDocumentIndex = aDocuments.indexOf(getDocument());

        // If the first the document
        if (currentDocumentIndex <= 0) {
            return false;
        }

        setDocument(aDocuments.get(currentDocumentIndex - 1), aDocuments);

        return true;
    }

    /**
     * Moves the selection to the document following the current document in the given list. Has no
     * effect if the current document is already the last document or if the current document is not
     * actually part of the given list.
     * 
     * @param aDocuments
     *            a list of documents.
     * @return whether the current document has changed.
     */
    default boolean moveToNextDocument(List<SourceDocument> aDocuments)
    {
        // Index of the current source document in the list
        int currentDocumentIndex = aDocuments.indexOf(getDocument());

        // If the last document
        if (currentDocumentIndex < 0 || currentDocumentIndex >= aDocuments.size() - 1) {
            return false;
        }

        setDocument(aDocuments.get(currentDocumentIndex + 1), aDocuments);

        return true;
    }
}
