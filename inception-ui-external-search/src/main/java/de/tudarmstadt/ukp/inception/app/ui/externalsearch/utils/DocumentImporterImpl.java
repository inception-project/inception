/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.app.ui.externalsearch.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.uima.UIMAException;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.app.ui.externalsearch.config.ExternalSearchUIAutoConfiguration;
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
    private static final String PLAIN_TEXT = "text";
    
    private final DocumentService documentService;
    private final ExternalSearchService externalSearchService;

    @Autowired
    public DocumentImporterImpl(DocumentService aDocumentService,
            ExternalSearchService aExternalSearchService)
    {
        documentService = aDocumentService;
        externalSearchService = aExternalSearchService;
    }

    /**
     * @return a boolean value. True if import was successful. False if import was aborted because
     *         the document already exists.
     */
    @Override
    public boolean importDocumentFromDocumentRepository(User aUser, Project aProject,
            String aDocumentTitle, DocumentRepository aRepository)
        throws IOException
    {
        String text = externalSearchService.getDocumentById(aUser, aRepository, aDocumentTitle)
            .getText();

        if (documentService.existsSourceDocument(aProject, aDocumentTitle)) {
            return false;
        }

        SourceDocument document = new SourceDocument();
        document.setName(aDocumentTitle);
        document.setProject(aProject);
        document.setFormat(PLAIN_TEXT);

        try (InputStream is = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))) {
            documentService.uploadSourceDocument(is, document);
        }
        catch (IOException | UIMAException e) {
            throw new IOException("Unable to retrieve document [" + aDocumentTitle + "]", e);
        }
        
        return true;
    }
}
