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
package de.tudarmstadt.ukp.clarin.webanno.diag.checks;

import static org.apache.uima.fit.util.JCasUtil.select;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogLevel;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class NoZeroSizeTokensAndSentencesCheck
    implements Check
{
    private Logger log = LoggerFactory.getLogger(getClass());
   
    @Override
    public boolean check(Project aProject, CAS aCas, List<LogMessage> aMessages)
    {
        try {
            boolean ok = true;
            
            for (Token t : select(aCas.getJCas(), Token.class)) {
                if (t.getBegin() >= t.getEnd()) {
                    aMessages.add(new LogMessage(this, LogLevel.ERROR,
                            "Token with illegal span: %s", t));
                    ok = false;
                }
            }

            for (Sentence s : select(aCas.getJCas(), Sentence.class)) {
                if (s.getBegin() >= s.getEnd()) {
                    aMessages.add(new LogMessage(this, LogLevel.ERROR,
                            "Sentence with illegal span: %s", s));
                    ok = false;
                }
            }
            
            return ok;
        }
        catch (CASException e) {
            log.error("Unabled to access JCas", e);
            aMessages.add(new LogMessage(this, LogLevel.ERROR,
                    "Unabled to access JCas", e.getMessage()));
            return false;
        }
    }
}
