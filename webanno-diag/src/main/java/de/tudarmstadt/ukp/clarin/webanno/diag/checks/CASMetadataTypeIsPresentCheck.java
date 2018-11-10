/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.clarin.webanno.diag.checks;

import java.util.List;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;

/**
 * Checks if the type {@link CASMetadata} is defined in the type system of this CAS. If this is
 * not the case, then the application may not be able to detect concurrent modifications.
 */
public class CASMetadataTypeIsPresentCheck
    implements Check
{
    @Override
    public boolean check(Project aProject, CAS aCas, List<LogMessage> aMessages)
    {
        if (aCas.getTypeSystem().getType(CASMetadata.class.getName()) == null) {
            aMessages.add(LogMessage.warn(this, "CAS needs upgrade to support CASMetadata which is "
                    + "required to detect concurrent modifications to CAS files."));
        }
        
        // This is an informative check - not critical, so we always pass it.
        return true;
    }
}
