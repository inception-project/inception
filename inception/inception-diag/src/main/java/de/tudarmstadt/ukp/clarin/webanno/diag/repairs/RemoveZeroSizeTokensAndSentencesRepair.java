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

import static de.tudarmstadt.ukp.inception.support.logging.LogLevel.INFO;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.selectSentences;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;

import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.Repair.Safe;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;

@Safe(false)
public class RemoveZeroSizeTokensAndSentencesRepair
    implements Repair
{
    @Override
    public void repair(SourceDocument aDocument, String aDataOwner, CAS aCas,
            List<LogMessage> aMessages)
    {
        for (AnnotationFS s : selectSentences(aCas)) {
            if (s.getBegin() >= s.getEnd()) {
                aCas.removeFsFromIndexes(s);
                aMessages.add(
                        new LogMessage(this, INFO, "Removed sentence with illegal span: %s", s));
            }
        }

        for (AnnotationFS t : WebAnnoCasUtil.selectTokens(aCas)) {
            if (t.getBegin() >= t.getEnd()) {
                AnnotationFS lemma = FSUtil.getFeature(t, "lemma", AnnotationFS.class);
                if (lemma != null) {
                    aCas.removeFsFromIndexes(lemma);
                    aMessages.add(new LogMessage(this, INFO,
                            "Removed lemma attached to token with illegal span: %s", t));
                }

                AnnotationFS pos = FSUtil.getFeature(t, "pos", AnnotationFS.class);
                if (pos != null) {
                    aCas.removeFsFromIndexes(pos);
                    aMessages.add(new LogMessage(this, INFO,
                            "Removed POS attached to token with illegal span: %s", t));
                }

                AnnotationFS stem = FSUtil.getFeature(t, "stem", AnnotationFS.class);
                if (stem != null) {
                    aCas.removeFsFromIndexes(stem);
                    aMessages.add(new LogMessage(this, INFO,
                            "Removed stem attached to token with illegal span: %s", t));
                }

                aCas.removeFsFromIndexes(t);
                aMessages.add(new LogMessage(this, INFO, "Removed token with illegal span: %s", t));
            }
        }
    }
}
