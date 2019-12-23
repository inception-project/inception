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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectSentences;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectTokens;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogLevel;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;

public class NoZeroSizeTokensAndSentencesCheck
    implements Check
{
    @Override
    public boolean check(Project aProject, CAS aCas, List<LogMessage> aMessages)
    {
        boolean ok = true;
        
        for (AnnotationFS t : selectTokens(aCas)) {
            if (t.getBegin() >= t.getEnd()) {
                aMessages.add(new LogMessage(this, LogLevel.ERROR,
                        "Token with illegal span: %s", t));
                ok = false;
            }
        }

        for (AnnotationFS s : selectSentences(aCas)) {
            if (s.getBegin() >= s.getEnd()) {
                aMessages.add(new LogMessage(this, LogLevel.ERROR,
                        "Sentence with illegal span: %s", s));
                ok = false;
            }
        }
        
        return ok;
    }
}
