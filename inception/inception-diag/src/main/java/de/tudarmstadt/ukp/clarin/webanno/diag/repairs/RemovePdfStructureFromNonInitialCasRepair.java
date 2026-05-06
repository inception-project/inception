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
package de.tudarmstadt.ukp.clarin.webanno.diag.repairs;

import static de.tudarmstadt.ukp.clarin.webanno.diag.checks.PdfStructurePresentInNonInitialCasCheck.PDF_CHUNK_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.diag.checks.PdfStructurePresentInNonInitialCasCheck.PDF_PAGE_TYPE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.INITIAL_CAS_PSEUDO_USER;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;

import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.Repair.Safe;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

@Safe(false)
public class RemovePdfStructureFromNonInitialCasRepair
    implements Repair
{
    @Override
    public void repair(SourceDocument aDocument, String aDataOwner, CAS aCas,
            List<LogMessage> aMessages)
    {
        if (INITIAL_CAS_PSEUDO_USER.equals(aDataOwner)) {
            // We do not want to touch the initial CAS
            return;
        }

        var deleted = removePdfLayout(aCas);

        aMessages.add(LogMessage.info(this,
                "Removed PDF structure from non-initial CAS (%d nodes removed)", deleted));
    }

    private static int removePdfLayout(CAS aCas)
    {
        var toDelete = new ArrayList<FeatureStructure>();
        aCas.select(PDF_CHUNK_TYPE).forEach(toDelete::add);
        aCas.select(PDF_PAGE_TYPE).forEach(toDelete::add);
        toDelete.forEach(aCas::removeFsFromIndexes);
        return toDelete.size();
    }
}
