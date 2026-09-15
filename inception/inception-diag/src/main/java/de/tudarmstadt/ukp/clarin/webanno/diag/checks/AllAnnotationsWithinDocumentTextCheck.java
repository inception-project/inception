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

import static de.tudarmstadt.ukp.inception.support.logging.LogMessage.error;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.findAllFeatureStructures;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.getRealCas;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

/**
 * Checks that no annotation extends beyond the end of the document text.
 * <p>
 * This should not be possible - the sofa string is write-once - but it can happen where we bypass
 * that using {@code ICasUtil.forceOverwriteSofa} and replace the document text with a shorter one
 * while annotations keep their original offsets.
 * <p>
 * Any code calling {@code AnnotationFS.getCoveredText()} on such an annotation fails with an
 * {@code IndexOutOfBoundsException}, which - depending on where it happens - can make a document
 * impossible to open. See #6246.
 */
public class AllAnnotationsWithinDocumentTextCheck
    implements Check
{
    @Override
    public boolean check(SourceDocument aDocument, String aDataOwner, CAS aCas,
            List<LogMessage> aMessages)
    {
        var documentText = aCas.getDocumentText();
        if (documentText == null) {
            return true;
        }

        var documentTextLength = documentText.length();

        var ok = true;
        // Use the same traversal as RemoveBomRepair: annotations that are reachable but not indexed
        // can be left behind out of bounds just as well, and they break rendering all the same.
        for (var fs : findAllFeatureStructures(getRealCas(aCas))) {
            if (!(fs instanceof Annotation ann)) {
                continue;
            }

            // Negative-size annotations are reported by NegativeSizeAnnotationsCheck - here we only
            // care about annotations reaching beyond the end of the document text.
            if (ann.getEnd() > documentTextLength || ann.getBegin() > documentTextLength) {
                aMessages.add(error(this,
                        "[%s] at [%d-%d] extends beyond the end of the document text [%d]",
                        ann.getType().getName(), ann.getBegin(), ann.getEnd(), documentTextLength));
                ok = false;
            }
        }

        return ok;
    }
}
