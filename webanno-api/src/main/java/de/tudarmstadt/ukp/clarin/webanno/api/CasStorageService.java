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
import java.util.Optional;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public interface CasStorageService
{
    String SERVICE_NAME = "casStorageService";
    
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
    void writeCas(SourceDocument aDocument, CAS aJcas, String aUserName) throws IOException;
    
    /**
     * Retrieve the annotation CAS of a given user for a given {@link SourceDocument}. By default
     * applies the CAS doctor.
     *
     * @param aDocument
     *            the document.
     * @param aUsername
     *            the user.
     * @return the CAS.
     * @throws IOException
     *             if there was a problem loading or creating the CAS.
     */
    CAS readCas(SourceDocument aDocument, String aUsername) throws IOException;        

    /**
     * Retrieve the annotation CAS of a given user for a given {@link SourceDocument}.
     *
     * @param aDocument
     *            the document.
     * @param aUsername
     *            the user.
     * @param aAnalyzeAndRepair
     *            whether to apply the CAS doctor.
     * @return the CAS.
     * @throws IOException
     *             if there was a problem loading or creating the CAS.
     */
    CAS readCas(SourceDocument aDocument, String aUsername, boolean aAnalyzeAndRepair)
        throws IOException;

    /**
     * Retrieve the annotation CAS of a given user for a given {@link SourceDocument}. If it does
     * not exist, create it using the given supplier. The result is immediately persisted to the
     * storage.
     * 
     * @param aDocument
     *            the document.
     * @param aUsername
     *            the user.
     * @param aSupplier
     *            a function to create a new CAS if there is none yet.
     * @return the CAS.
     * @throws IOException
     *             if there was a problem loading or creating the CAS.
     */
    CAS readOrCreateCas(SourceDocument aDocument, String aUsername, JCasProvider aSupplier)
        throws IOException;

    boolean deleteCas(SourceDocument aDocument, String aUsername) throws IOException;
    
    File getAnnotationFolder(SourceDocument aDocument) throws IOException;
    
    File getCasFile(SourceDocument aDocument, String aUser) throws IOException;

    boolean existsCas(SourceDocument aDocument, String aUser) throws IOException;

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

    void performExclusiveBulkOperation(CasStorageOperation aOperation)
        throws UIMAException, IOException;
    
    @FunctionalInterface
    public static interface CasStorageOperation
    {
        void execute() throws IOException, UIMAException;
    }

    Optional<Long> getCasTimestamp(SourceDocument aDocument, String aUser) throws IOException;
}
