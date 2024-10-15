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

import static de.tudarmstadt.ukp.inception.support.logging.LogMessage.info;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.Repair.Safe;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

@Safe(false)
public class SwitchBeginAndEndOnNegativeSizedAnnotationsRepair
    implements Repair
{
    @Override
    public void repair(SourceDocument aDocument, String aDataOwner, CAS aCas,
            List<LogMessage> aMessages)
    {
        for (Annotation ann : aCas.select(Annotation.class)) {
            if (ann.getBegin() > ann.getEnd()) {
                aCas.removeFsFromIndexes(ann);
                int oldBegin = ann.getBegin();
                ann.setBegin(ann.getEnd());
                ann.setEnd(oldBegin);
                aCas.addFsToIndexes(ann);
                aMessages.add(info(this, "Switch begin/end on [%s] at [%d-%d]",
                        ann.getType().getName(), ann.getBegin(), ann.getEnd()));
            }
        }
    }
}
