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

import static de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctorUtils.getNonIndexedFSesWithOwner;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class AllFeatureStructuresIndexedCheck
    implements Check
{
    @Override
    public boolean check(SourceDocument aDocument, String aDataOwner, CAS aCas,
            List<LogMessage> aMessages)
    {
        Map<FeatureStructure, FeatureStructure> nonIndexed = getNonIndexedFSesWithOwner(aCas);

        if (nonIndexed.isEmpty()) {
            return true;
        }

        aMessages
                .add(LogMessage.error(this, "Unindexed feature structures: %d", nonIndexed.size()));

        int count = 0;
        for (Entry<FeatureStructure, FeatureStructure> e : nonIndexed.entrySet()) {
            if (count >= 100) {
                break;
            }

            aMessages.add(LogMessage.error(this,
                    "Non-indexed feature structure [%s] reachable through [%s]", e.getKey(),
                    e.getValue()));
            count++;
        }

        if (count >= 100) {
            aMessages.add(LogMessage.error(this,
                    "In total [%d] annotations were reachable but not indexed", count));
        }

        return nonIndexed.isEmpty();
    }
}
