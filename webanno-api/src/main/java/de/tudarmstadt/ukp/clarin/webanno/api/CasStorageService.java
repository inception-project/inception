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

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode;
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
     * @param aCas
     *            The annotated CAS object
     * @param aUserName
     *            the user who annotates the document if it is user's annotation document OR the
     *            CURATION_USER
     */
    void writeCas(SourceDocument aDocument, CAS aCas, String aUserName) throws IOException;
    
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
     * @param aAccessMode
     *            in which mode to read the CAS.
     * @return the CAS.
     * @throws IOException
     *             if there was a problem loading or creating the CAS.
     */
    CAS readCas(SourceDocument aDocument, String aUsername, boolean aAnalyzeAndRepair,
            CasAccessMode aAccessMode)
        throws IOException;

    /**
     * Reads the CAS containing the annotation data for the given user on the given document. If
     * there is no CAS yet for that user/document combination, create one using the given
     * {@link CasProvider}.
     * 
     * @param aDocument
     *            the document to obtain the CAS for.
     * @param aUsername
     *            the user to obtain the CAS for.
     * @param aAnalyzeAndRepair
     *            whether the CAS should be analyzed and repaired after being retrieved.
     * @param aUpgradeMode
     *            whether the CAS should be upgraded to the latest project type system.
     * @param aSupplier
     *            used to create a new CAS if none for the given user/document combination exists in
     *            the storage.
     * @return the CAS
     * @throws IOException
     *             if the CAS could not be loaded or created.
     */
    CAS readOrCreateCas(SourceDocument aDocument, String aUsername, boolean aAnalyzeAndRepair,
            CasUpgradeMode aUpgradeMode, CasProvider aSupplier)
        throws IOException;

    /**
     * Reads the CAS containing the annotation data for the given user on the given document. If
     * there is no CAS yet for that user/document combination, create one using the given
     * {@link CasProvider}.
     * 
     * @param aDocument
     *            the document to obtain the CAS for.
     * @param aUsername
     *            the user to obtain the CAS for.
     * @param aAnalyzeAndRepair
     *            whether the CAS should be analyzed and repaired after being retrieved.
     * @param aUpgradeMode
     *            whether the CAS should be upgraded to the latest project type system.
     * @param aSupplier
     *            used to create a new CAS if none for the given user/document combination exists in
     *            the storage.
     * @param aAccessMode
     *            the CAS access mode.
     * @return the CAS
     * @throws IOException
     *             if the CAS could not be loaded or created.
     */
    CAS readOrCreateCas(SourceDocument aDocument, String aUsername, boolean aAnalyzeAndRepair,
            CasUpgradeMode aUpgradeMode, CasProvider aSupplier, CasAccessMode aAccessMode)
        throws IOException;

    boolean deleteCas(SourceDocument aDocument, String aUsername) throws IOException;
    
    File getAnnotationFolder(SourceDocument aDocument) throws IOException;
    
    File getCasFile(SourceDocument aDocument, String aUser) throws IOException;

    boolean existsCas(SourceDocument aDocument, String aUser) throws IOException;

    /**
     * Runs {@code CasDoctor} in repair mode on the given CAS (if repairs are active), otherwise
     * it runs only in analysis mode.
     * <p>
     * <b>Note:</b> {@code CasDoctor} is an optional service. If no {@code CasDoctor} implementation
     * is available, this method returns without doing anything.
     * 
     * @param aDocument
     *            the document
     * @param aUsername
     *            the user owning the CAS (used for logging)
     * @param aCas
     *            the CAS object
     */
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
