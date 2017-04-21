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
package de.tudarmstadt.ukp.clarin.webanno.curation.storage;

import java.io.IOException;
import java.util.List;

import org.apache.uima.jcas.JCas;
import org.springframework.security.access.prepost.PreAuthorize;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public interface CurationDocumentService
{
    static final String SERVICE_NAME = "curationDocumentService";
    
    // --------------------------------------------------------------------------------------------
    // Methods related to curation
    // --------------------------------------------------------------------------------------------

    /**
     * Create a curation annotation document under a special user named as "CURATION_USER"
     *
     * @param jCas
     *            the JCas.
     * @param document
     *            the source document.
     * @param user
     *            the user.
     * @throws IOException
     *             if an I/O error occurs.
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void writeCurationCas(JCas jCas, SourceDocument document, User user, boolean aUpdateTimestamp)
        throws IOException;

    /**
     * Get a curation document for the given {@link SourceDocument}
     *
     * @param document
     *            the source document.
     * @return the curation JCas.
     * @throws IOException
     *             if an I/O error occurs.
     */
    JCas readCurationCas(SourceDocument document)
        throws IOException;

    /**
     * Remove a curation annotation document from the file system, for this {@link SourceDocument}
     *
     * @param sourceDocument
     *            the source document.
     * @param username
     *            the username.
     * @throws IOException
     *             if an I/O error occurs.
     */
    void removeCurationDocumentContent(SourceDocument sourceDocument, String username)
        throws IOException;
    
    List<SourceDocument> listCuratableSourceDocuments(Project aProject);
}
