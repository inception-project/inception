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
package de.tudarmstadt.ukp.clarin.webanno.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasSessionException;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasStorageServiceAction;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasStorageServiceLoader;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.ConcurentCasModificationException;
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
     * @throws CasSessionException
     *             if no CAS storage session in available for the current thread or if the session
     *             does not permit writing.
     * @throws IOException
     *             if there was and error writing the CAS
     */
    void writeCas(SourceDocument aDocument, CAS aCas, String aUserName)
        throws IOException, CasSessionException;

    /**
     * Retrieve the annotation CAS of a given user for a given {@link SourceDocument}. By default
     * applies the CAS doctor. This uses {@link CasAccessMode#EXCLUSIVE_WRITE_ACCESS} and
     * {@link CasUpgradeMode#NO_CAS_UPGRADE}.
     *
     * @param aDocument
     *            the document.
     * @param aUsername
     *            the user.
     * @return the CAS.
     * @throws IOException
     *             if there was a problem loading or creating the CAS.
     * @throws CasSessionException
     *             if no CAS storage session in available for the current thread.
     */
    CAS readCas(SourceDocument aDocument, String aUsername) throws IOException, CasSessionException;

    /**
     * Retrieve the annotation CAS of a given user for a given {@link SourceDocument}. If
     * {@link CasAccessMode#SHARED_READ_ONLY_ACCESS} is used, then
     * {@link CasUpgradeMode#AUTO_CAS_UPGRADE} is used, otherwise
     * {@link CasUpgradeMode#NO_CAS_UPGRADE} is used.
     *
     * @param aDocument
     *            the document.
     * @param aUsername
     *            the user.
     * @param aAccessMode
     *            in which mode to read the CAS.
     * @return the CAS.
     * @throws IOException
     *             if there was a problem loading or creating the CAS.
     * @throws CasSessionException
     *             if no CAS storage session in available for the current thread.
     */
    CAS readCas(SourceDocument aDocument, String aUsername, CasAccessMode aAccessMode)
        throws IOException, CasSessionException;

    /**
     * Reads the CAS containing the annotation data for the given user on the given document. If
     * there is no CAS yet for that user/document combination, create one using the given
     * {@link CasProvider}.
     * <p>
     * <b>NOTE:</b> the CAS returned by the supplier <b>must not be managed</b>. If it is retrieved
     * via the storage services, then the {@link CasAccessMode#UNMANAGED_ACCESS} must be used.
     * 
     * @param aDocument
     *            the document to obtain the CAS for.
     * @param aUsername
     *            the user to obtain the CAS for.
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
     * @throws CasSessionException
     *             if no CAS storage session in available for the current thread.
     */
    CAS readOrCreateCas(SourceDocument aDocument, String aUsername, CasUpgradeMode aUpgradeMode,
            CasProvider aSupplier, CasAccessMode aAccessMode)
        throws IOException, CasSessionException;

    /**
     * Delete a CAS from the storage and also remove it from the active session.
     * 
     * @param aDocument
     *            the document to delete the CAS for.
     * @param aUsername
     *            the user to delete the CAS for.
     * @return if the CAS was successfully deleted.
     * @throws IOException
     *             if the CAS could not be deleted.
     * @throws CasSessionException
     *             if no CAS storage session in available for the current thread or if the session
     *             does not permit writing.
     */
    boolean deleteCas(SourceDocument aDocument, String aUsername)
        throws IOException, CasSessionException;

    void exportCas(SourceDocument aDocument, String aUser, OutputStream aStream) throws IOException;

    void importCas(SourceDocument aDocument, String aUser, InputStream aStream) throws IOException;

    boolean existsCas(SourceDocument aDocument, String aUser) throws IOException;

    /**
     * Runs {@code CasDoctor} in repair mode on the given CAS (if repairs are active), otherwise it
     * runs only in analysis mode.
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

    Optional<Long> getCasTimestamp(SourceDocument aDocument, String aUser) throws IOException;

    Optional<Long> verifyCasTimestamp(SourceDocument aDocument, String aUser,
            long aExpectedTimeStamp, String aContextAction)
        throws IOException, ConcurentCasModificationException;

    /**
     * Upgrades the given CAS in the storage.
     * 
     * @param aDocument
     *            the document to upgrade the CAS for.
     * @param aUser
     *            the user to upgrade the CAS for.
     * @throws IOException
     *             if the CAS could not be loaded, upgraded or saved.
     * @throws CasSessionException
     *             if no CAS storage session in available for the current thread or if the session
     *             does not permit writing.
     */
    void upgradeCas(SourceDocument aDocument, String aUser) throws IOException, CasSessionException;

    void forceActionOnCas(SourceDocument aDocument, String aUser, CasStorageServiceLoader aLoader,
            CasStorageServiceAction aAction, boolean aSave)
        throws IOException;
}
