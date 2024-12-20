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

import static de.tudarmstadt.ukp.clarin.webanno.diag.checks.UnreachableAnnotationsCheck.findAllFeatureStructures;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.forceOverwriteSofa;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.getRealCas;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.Repair.Safe;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

@Safe(false)
public class RemoveBomRepair
    implements Repair
{
    @Override
    public void repair(SourceDocument aDocument, String aDataOwner, CAS aCas,
            List<LogMessage> aMessages)
    {
        var cas = getRealCas(aCas);
        var text = cas.getDocumentText();
        if (text.length() < 1) {
            return;
        }

        if (text.charAt(0) != '\uFEFF' && text.charAt(0) != '\uFFFE') {
            return;
        }

        forceOverwriteSofa(cas, text.substring(1));

        cas.protectIndexes(() -> {
            findAllFeatureStructures(getRealCas(cas)).stream() //
                    .filter(fs -> fs instanceof Annotation) //
                    .map(fs -> (Annotation) fs) //
                    .forEach(ann -> {
                        var begin = ann.getBegin();
                        var end = ann.getEnd();

                        if (begin > 0) {
                            ann.setBegin(begin - 1);
                        }

                        if (end > 0) {
                            ann.setEnd(end - 1);
                        }
                    });
        });

        aMessages.add(LogMessage.info(this, "BOM removed and annotation offsets adjusted."));
    }
}
