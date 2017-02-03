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

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.JCas;
import org.springframework.security.access.prepost.PreAuthorize;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

public interface CorrectionDocumentService
{
    /**
     * Create an annotation document under a special user named "CORRECTION_USER"
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
    void writeCorrectionCas(JCas jCas, SourceDocument document, User user)
        throws IOException;

    JCas readCorrectionCas(SourceDocument document)
        throws UIMAException, IOException, ClassNotFoundException;

    void upgradeCorrectionCas(CAS aCurCas, SourceDocument document)
            throws UIMAException, IOException;

    /**
     * A method to check if there exist a correction document already. Base correction document
     * should be the same for all users
     *
     * @param document
     *            the source document.
     * @return if a correction document exists.
     */
    boolean existsCorrectionDocument(SourceDocument document);

    /**
     * check if there is an already automated document. This is important as automated document
     * should appear the same among users
     *
     * @param sourceDocument
     *            the source document.
     * @return if an automation document exists.
     */
    boolean existsCorrectionCas(SourceDocument sourceDocument);
}
