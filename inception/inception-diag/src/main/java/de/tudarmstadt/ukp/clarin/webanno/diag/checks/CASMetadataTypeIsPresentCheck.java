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

import java.util.List;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

/**
 * Checks if the type {@link CASMetadata} is defined in the type system of this CAS. If this is not
 * the case, then the application may not be able to detect concurrent modifications.
 */
public class CASMetadataTypeIsPresentCheck
    implements Check
{
    @Override
    public boolean check(SourceDocument aDocument, String aDataOwner, CAS aCas,
            List<LogMessage> aMessages)
    {
        if (aCas.getTypeSystem().getType(CASMetadata._TypeName) == null) {
            aMessages.add(LogMessage.info(this, "CAS needs upgrade to support CASMetadata which is "
                    + "required to detect concurrent modifications to CAS files."));
            return true;
        }

        if (aCas.select(CASMetadata.class).isEmpty()) {
            aMessages.add(LogMessage.warn(this,
                    "CAS contains no CASMetadata. Cannot check concurrent access."));
        }

        // This is an informative check - not critical, so we always pass it.
        return true;
    }
}
