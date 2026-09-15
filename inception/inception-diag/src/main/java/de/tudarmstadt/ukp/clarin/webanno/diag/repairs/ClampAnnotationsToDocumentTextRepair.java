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
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.findAllFeatureStructures;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.getRealCas;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.Repair.Safe;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

/**
 * Clamps annotations whose offsets lie outside the document text - beyond its end or before its
 * beginning - back into the document text.
 * <p>
 * Counterpart to {@code AllAnnotationsWithinDocumentTextCheck}. The document text is not modified -
 * only the offsets of the affected annotations are.
 * <p>
 * A CAS should never get into this state: the sofa string is write-once, so annotations cannot
 * normally end up pointing outside of it. It can happen where we bypass that using
 * {@code ICasUtil.forceOverwriteSofa} - e.g. {@code RemoveBomRepair}, which shifts all annotations
 * by one after dropping a BOM but only reaches the reachable feature structures, or
 * {@code Controller_ImplBase}, which chomps a trailing line break off an uploaded annotation CAS on
 * the assumption that nobody annotated across it.
 * <p>
 * Because the original sofa is gone by then, the original extent of the annotation cannot be
 * reconstructed from the CAS. Clamping shrinks the annotation to the part of its span that actually
 * exists in the document text, which makes the CAS consistent and usable again - it does not
 * restore the original annotation. See #6246.
 */
@Safe(false)
public class ClampAnnotationsToDocumentTextRepair
    implements Repair
{
    @Override
    public void repair(SourceDocument aDocument, String aDataOwner, CAS aCas,
            List<LogMessage> aMessages)
    {
        var documentText = aCas.getDocumentText();
        if (documentText == null) {
            return;
        }

        var documentTextLength = documentText.length();

        aCas.protectIndexes(() -> {
            // Use the same traversal as RemoveBomRepair: annotations that are reachable but not
            // indexed can be left behind out of bounds just as well, and they break rendering all
            // the same.
            for (var fs : findAllFeatureStructures(getRealCas(aCas))) {
                if (!(fs instanceof Annotation ann)) {
                    continue;
                }

                var oldBegin = ann.getBegin();
                var oldEnd = ann.getEnd();

                if (oldBegin >= 0 && oldEnd >= 0 && oldBegin <= documentTextLength
                        && oldEnd <= documentTextLength) {
                    continue;
                }

                // Clamp into [0, documentTextLength] - a negative offset breaks getCoveredText()
                // just as much as one beyond the end of the text does.
                var newBegin = max(0, min(oldBegin, documentTextLength));
                // Keep end >= begin. Negative-size annotations are SwitchBeginAndEndOnNegative-
                // SizedAnnotationsRepair's business, but clamping must not newly introduce or
                // worsen one.
                var newEnd = max(newBegin, min(oldEnd, documentTextLength));

                ann.setBegin(newBegin);
                ann.setEnd(newEnd);

                aMessages.add(info(this,
                        "Clamped [%s] at [%d-%d] to [%d-%d] to fit the document text [%d]",
                        ann.getType().getName(), oldBegin, oldEnd, newBegin, newEnd,
                        documentTextLength));
            }
        });
    }
}
