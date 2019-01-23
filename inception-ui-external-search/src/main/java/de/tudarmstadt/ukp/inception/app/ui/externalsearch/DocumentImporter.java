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
package de.tudarmstadt.ukp.inception.app.ui.externalsearch;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import org.apache.uima.UIMAException;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchService;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;

public class DocumentImporter implements Serializable
{

    private static final String PLAIN_TEXT = "text";

    private ExternalSearchService externalSearchService;

    private DocumentService documentService;

    private User user;

    private Project project;


    public DocumentImporter(ExternalSearchService aExternalSearchService,
        DocumentService aDocumentService, User aUser,Project aProject)
    {
        externalSearchService = aExternalSearchService;
        documentService = aDocumentService;
        user = aUser;

        project = aProject;
    }

    /**
     * @return a boolean value. True if import was successful. False if import was aborted because
     *         the document already exists.
     */
    public boolean importDocumentFromDocumentRepository(String aDocumentTitle,
        DocumentRepository aRepository) throws IOException
    {
        String text = externalSearchService.getDocumentById(user, aRepository, aDocumentTitle)
            .getText();

        if (documentService.existsSourceDocument(project, aDocumentTitle)) {
            return false;
        }

        SourceDocument document = new SourceDocument();
        document.setName(aDocumentTitle);
        document.setProject(project);
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
