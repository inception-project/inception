/*
 * Copyright 2020
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
package de.tudarmstadt.ukp.clarin.webanno.api;

import java.io.IOException;
import java.util.Optional;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.springframework.security.access.prepost.PreAuthorize;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public interface MergeDocumentService
{
    /**
     * Create a curation annotation document under a special user named as 
     * "CURATION_USER" or "CORRECTION_USER"
     *
     * @param aCas
     *            the CAS.
     * @param document
     *            the source document.
     * @throws IOException
     *             if an I/O error occurs.
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void writeResultCas(CAS aCas, SourceDocument aDocument, boolean aUpdateTimestamp)
        throws IOException;

    CAS readResultCas(SourceDocument document)
        throws IOException;

    void upgradeResultCas(CAS aCurCas, SourceDocument document)
            throws UIMAException, IOException;

    String getResultCasUser();
    
    /**
     * A method to check if there exist a result document already. Base result document
     * should be the same for all users
     *
     * @param sourceDocument
     *            the source document.
     * @return if a correction document exists.
     */
    boolean existsResultCas(SourceDocument sourceDocument)
        throws IOException;

    Optional<Long> getResultCasTimestamp(SourceDocument aDocument) throws IOException;
}
