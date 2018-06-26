/*
 * Copyright 2012
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

import java.io.File;
import java.io.IOException;

import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public interface CasStorageService
{
    String SERVICE_NAME = "casStorageService";
    
    boolean existsCas(SourceDocument aSourceDocument, String aUsername) throws IOException;

    /**
     * Creates an annotation document (either user's annotation document or CURATION_USER's
     * annotation document)
     *
     * @param aDocument
     *            the {@link SourceDocument}
     * @param aJcas
     *            The annotated CAS object
     * @param aUserName
     *            the user who annotates the document if it is user's annotation document OR the
     *            CURATION_USER
     */
    void writeCas(SourceDocument aDocument, JCas aJcas, String aUserName)
        throws IOException;
    
    /**
     * For a given {@link SourceDocument}, return the {@link AnnotationDocument} for the user or for
     * the CURATION_USER
     *
     * @param aDocument
     *            the {@link SourceDocument}
     * @param aUsername
     *            the {@link User} who annotates the {@link SourceDocument} or the CURATION_USER
     */
    JCas readCas(SourceDocument aDocument, String aUsername)
        throws IOException;
        

    JCas readCas(SourceDocument aDocument, String aUsername, boolean aAnalyzeAndRepair)
        throws IOException;
    
    boolean deleteCas(SourceDocument aDocument, String aUsername)
        throws IOException;
    
    File getAnnotationFolder(SourceDocument aDocument)
            throws IOException;
    
    void analyzeAndRepair(SourceDocument aDocument, String aUsername, CAS aCas);
    
    boolean isCacheEnabled();

    /**
     * Disables the CAS cache for the current request cycle. This is useful to avoid quickly filling
     * up the memory during bulk operations e.g. repairing all CASes in a project. It is also useful
     * when a CAS needs to be copied and modified, e.g. during a curation re-merge.
     */
    void disableCache();

    /**
     * Enables the CAS cache for the current request cycle.
     */
    void enableCache();
}
