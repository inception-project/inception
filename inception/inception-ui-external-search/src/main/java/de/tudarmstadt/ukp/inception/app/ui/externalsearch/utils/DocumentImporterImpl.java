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
package de.tudarmstadt.ukp.inception.app.ui.externalsearch.utils;

import java.io.IOException;
import java.io.InputStream;

import org.apache.uima.UIMAException;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.app.ui.externalsearch.config.ExternalSearchUIAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchService;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;

/**
 * Imports documents from the external document repository into the project.
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ExternalSearchUIAutoConfiguration#documentImporter}.
 * </p>
 */
public class DocumentImporterImpl
    implements DocumentImporter
{
    private final DocumentService documentService;
    private final ExternalSearchService externalSearchService;

    @Autowired
    public DocumentImporterImpl(DocumentService aDocumentService,
            ExternalSearchService aExternalSearchService)
    {
        documentService = aDocumentService;
        externalSearchService = aExternalSearchService;
    }

    @Override
    public boolean importDocumentFromDocumentRepository(User aUser, Project aProject,
            String aCollectionId, String aDocumentId, DocumentRepository aRepository)
        throws IOException
    {
        if (documentService.existsSourceDocument(aProject, aDocumentId)) {
            return false;
        }

        SourceDocument document = new SourceDocument();
        document.setName(aDocumentId);
        document.setProject(aProject);
        document.setFormat(
                externalSearchService.getDocumentFormat(aRepository, aCollectionId, aDocumentId));

        try (InputStream is = externalSearchService.getDocumentAsStream(aRepository, aCollectionId,
                aDocumentId)) {
            documentService.uploadSourceDocument(is, document);
        }
        catch (IOException | UIMAException e) {
            throw new IOException("Unable to retrieve document [" + aDocumentId + "]", e);
        }

        return true;
    }
}
