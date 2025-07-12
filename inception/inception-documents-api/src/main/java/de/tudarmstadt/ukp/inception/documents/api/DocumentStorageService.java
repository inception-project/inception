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
package de.tudarmstadt.ukp.inception.documents.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.wicket.util.resource.IResourceStream;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public interface DocumentStorageService
{
    /**
     * @return a stream to the source document file.
     * 
     * @param document
     *            the source document.
     * @throws IOException
     *             if there was an IO-level problem.
     */
    InputStream openSourceDocumentFile(SourceDocument document) throws IOException;

    /**
     * Write the data from the given input stream to the source document file.
     * 
     * @param inputStream
     *            the source document data.
     * @param document
     *            the source document.
     * @throws IOException
     *             if there was an IO-level problem.
     */
    void writeSourceDocumentFile(SourceDocument document, InputStream inputStream)
        throws IOException;

    /**
     * Copy the source document file to the target file
     * 
     * @param document
     *            the source document.
     * @param targetFile
     *            the target directory.
     * @throws IOException
     *             if there was an IO-level problem.
     */
    void copySourceDocumentFile(SourceDocument document, File targetFile) throws IOException;

    /**
     * @param document
     *            the source document.
     * @return the size of the source document file.
     */
    long getSourceDocumentFileSize(SourceDocument document);

    File getSourceDocumentFile(SourceDocument aDocument);

    IResourceStream getSourceDocumentResourceStream(SourceDocument aDocument);

    IResourceStream getSourceDocumentResourceStream(SourceDocument aDocument, String aContentType);

    void removeSourceDocumentFile(SourceDocument aDocument) throws IOException;

    boolean existsSourceDocumentFile(SourceDocument aDocument) throws IOException;

    void renameSourceDocumentFile(SourceDocument aDocument, String aNewName) throws IOException;
}
