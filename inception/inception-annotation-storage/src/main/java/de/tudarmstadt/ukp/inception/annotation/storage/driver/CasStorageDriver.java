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
package de.tudarmstadt.ukp.inception.annotation.storage.driver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.ConcurentCasModificationException;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageMetadata;

public interface CasStorageDriver
{
    CAS readCas(SourceDocument aDocument, String aUser) throws IOException;

    void writeCas(SourceDocument aDocument, String aUser, CAS aCas) throws IOException;

    void exportCas(SourceDocument aDocument, String aUser, OutputStream aStream) throws IOException;

    void importCas(SourceDocument aDocument, String aUser, InputStream aStream) throws IOException;

    boolean deleteCas(SourceDocument aDocument, String aUser) throws IOException;

    boolean existsCas(SourceDocument aDocument, String aUser) throws IOException;

    Optional<CasStorageMetadata> getCasMetadata(SourceDocument aDocument, String aUser)
        throws IOException;

    Optional<Long> verifyCasTimestamp(SourceDocument aDocument, String aUser,
            long aExpectedTimeStamp, String aContextAction)
        throws IOException, ConcurentCasModificationException;
}
