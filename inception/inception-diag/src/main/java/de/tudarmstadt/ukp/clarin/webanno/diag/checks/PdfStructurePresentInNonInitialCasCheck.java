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
package de.tudarmstadt.ukp.clarin.webanno.diag.checks;

import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.INITIAL_CAS_PSEUDO_USER;

import java.util.List;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class PdfStructurePresentInNonInitialCasCheck
    implements Check
{
    public static final String PDF_CHUNK_TYPE = "org.dkpro.core.api.pdf.type.PdfChunk";
    public static final String PDF_PAGE_TYPE = "org.dkpro.core.api.pdf.type.PdfPage";

    @Override
    public boolean check(SourceDocument aDocument, String aDataOwner, CAS aCas,
            List<LogMessage> aMessages)
    {
        if (INITIAL_CAS_PSEUDO_USER.equals(aDataOwner)) {
            // We do not want to check the initial CAS
            return true;
        }

        if (aCas.select(PDF_PAGE_TYPE).isEmpty() && aCas.select(PDF_CHUNK_TYPE).isEmpty()) {
            // No PDF structure present - good
            return true;
        }

        aMessages.add(
                LogMessage.warn(this, "PDF document structure is present in a non-initial CAS."));
        return false;
    }
}
