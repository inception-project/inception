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
package de.tudarmstadt.ukp.clarin.webanno.diag;

import java.util.List;
import java.util.Set;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public interface CasDoctor
{
    Set<String> getActiveChecks();

    void setFatalChecks(boolean aFatalChecks);

    boolean isFatalChecks();

    void repair(SourceDocument aDocument, String aDataOwner, CAS aCas);

    boolean isRepairsActive();

    void repair(SourceDocument aDocument, String aDataOwner, CAS aCas, List<LogMessage> aMessages);

    boolean analyze(SourceDocument aDocument, String aDataOwner, CAS aCas)
        throws CasDoctorException;

    boolean analyze(SourceDocument aDocument, String aDataOwner, CAS aCas,
            List<LogMessage> aMessages)
        throws CasDoctorException;

    boolean analyze(SourceDocument aDocument, String aDataOwner, CAS aCas,
            List<LogMessage> aMessages, boolean aFatalChecks)
        throws CasDoctorException;

    void setActiveChecks(String... aActiveChecks);

    void setActiveRepairs(String... aActiveRepairs);
}
