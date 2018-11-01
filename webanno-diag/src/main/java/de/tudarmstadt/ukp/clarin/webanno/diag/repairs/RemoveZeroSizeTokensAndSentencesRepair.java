/*
 * Copyright 2016
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
package de.tudarmstadt.ukp.clarin.webanno.diag.repairs;

import static org.apache.uima.fit.util.JCasUtil.select;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.Repair.Safe;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogLevel;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Stem;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

@Safe(false)
public class RemoveZeroSizeTokensAndSentencesRepair
    implements Repair
{
    private Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void repair(Project aProject, CAS aCas, List<LogMessage> aMessages)
    {
        try {
            for (Sentence s : select(aCas.getJCas(), Sentence.class)) {
                if (s.getBegin() >= s.getEnd()) {
                    s.removeFromIndexes();
                    aMessages.add(new LogMessage(this, LogLevel.INFO,
                            "Removed sentence with illegal span: %s", s));
                }
            }

            for (Token t : select(aCas.getJCas(), Token.class)) {
                if (t.getBegin() >= t.getEnd()) {
                    Lemma lemma = t.getLemma();
                    if (lemma != null) {
                        lemma.removeFromIndexes();
                        aMessages.add(new LogMessage(this, LogLevel.INFO,
                                "Removed lemma attached to token with illegal span: %s", t));
                    }

                    POS pos = t.getPos();
                    if (pos != null) {
                        pos.removeFromIndexes();
                        aMessages.add(new LogMessage(this, LogLevel.INFO,
                                "Removed POS attached to token with illegal span: %s", t));
                    }

                    Stem stem = t.getStem();
                    if (stem != null) {
                        stem.removeFromIndexes();
                        aMessages.add(new LogMessage(this, LogLevel.INFO,
                                "Removed stem attached to token with illegal span: %s", t));
                    }

                    t.removeFromIndexes();
                    aMessages.add(new LogMessage(this, LogLevel.INFO,
                            "Removed token with illegal span: %s", t));
                }
            }

        }
        catch (CASException e) {
            log.error("Unabled to access JCas", e);
            aMessages.add(
                    new LogMessage(this, LogLevel.ERROR, "Unabled to access JCas", e.getMessage()));
        }
    }
}
