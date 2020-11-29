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
import org.apache.uima.jcas.tcas.DocumentAnnotation;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;

/**
 * Checks that there is only a single {@link DocumentAnnotation} (or subclass) in the CAS.
 */
public class UniqueDocumentAnnotationCheck
    implements Check
{
    @Override
    public boolean check(Project aProject, CAS aCas, List<LogMessage> aMessages)
    {
        if (aCas.select(DocumentAnnotation.class).count() > 1) {
            aMessages.add(LogMessage.error(this, "There is more than one document annotation!"));
            return false;
        }

        return true;
    }
}
