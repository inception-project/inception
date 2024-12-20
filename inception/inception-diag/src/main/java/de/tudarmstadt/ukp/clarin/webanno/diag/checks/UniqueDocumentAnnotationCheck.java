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
import org.apache.uima.jcas.tcas.DocumentAnnotation;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

/**
 * Checks that there is only a single {@link DocumentAnnotation} (or subclass) in the CAS.
 */
public class UniqueDocumentAnnotationCheck
    implements Check
{
    @Override
    public boolean check(SourceDocument aDocument, String aDataOwner, CAS aCas,
            List<LogMessage> aMessages)
    {
        if (aCas.select(DocumentAnnotation.class).count() > 1) {
            aMessages.add(LogMessage.error(this, "There is more than one document annotation!"));
            return false;
        }

        return true;
    }
}
