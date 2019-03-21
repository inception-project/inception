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
package de.tudarmstadt.ukp.clarin.webanno.api;

import java.io.IOException;
import java.util.Optional;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.springframework.security.access.prepost.PreAuthorize;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public interface CorrectionDocumentService
{
    String SERVICE_NAME = "correctionDocumentService";
    
    /**
     * Create an annotation document under a special user named "CORRECTION_USER"
     *
     * @param jCas
     *            the JCas.
     * @param document
     *            the source document.
     * @throws IOException
     *             if an I/O error occurs.
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void writeCorrectionCas(CAS jCas, SourceDocument document)
        throws IOException;

    CAS readCorrectionCas(SourceDocument document)
        throws IOException;

    void upgradeCorrectionCas(CAS aCurCas, SourceDocument document)
            throws UIMAException, IOException;

    /**
     * A method to check if there exist a correction document already. Base correction document
     * should be the same for all users
     *
     * @param sourceDocument
     *            the source document.
     * @return if a correction document exists.
     */
    boolean existsCorrectionCas(SourceDocument sourceDocument)
        throws IOException;

    Optional<Long> getCorrectionCasTimestamp(SourceDocument aDocument) throws IOException;
}
