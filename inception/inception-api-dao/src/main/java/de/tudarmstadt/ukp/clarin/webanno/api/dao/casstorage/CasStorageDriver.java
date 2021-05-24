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
package de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public interface CasStorageDriver
{
    CAS readCas(SourceDocument aDocument, String aUser) throws IOException;

    void writeCas(SourceDocument aDocument, String aUserName, CAS aCas) throws IOException;

    boolean deleteCas(SourceDocument aDocument, String aUser) throws IOException;

    boolean existsCas(SourceDocument aDocument, String aUser) throws IOException;

    Optional<Long> getCasTimestamp(SourceDocument aDocument, String aUser) throws IOException;

    @Deprecated
    public File getCasFile(SourceDocument aDocument, String aUser) throws IOException;
}
