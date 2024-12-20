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

import static org.apache.commons.lang3.StringUtils.abbreviate;
import static org.apache.commons.lang3.StringUtils.abbreviateMiddle;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class TokensAndSententencedDoNotOverlapCheck
    implements Check
{
    @Override
    public boolean check(SourceDocument aDocument, String aDataOwner, CAS aCas,
            List<LogMessage> aMessages)
    {
        return checkTokens(aCas, aMessages) && checkSentences(aCas, aMessages);
    }

    private boolean checkTokens(CAS aCas, List<LogMessage> aMessages)
    {
        boolean ok = true;
        var tokenIter = aCas.<Annotation> select(Token._TypeName).iterator();
        Annotation prevToken = null;
        while (tokenIter.hasNext()) {
            var token = tokenIter.next();
            if (prevToken != null && token.getBegin() < prevToken.getEnd()) {
                aMessages.add(LogMessage.error(this,
                        "Token [%s]@[%d-%d] overlaps with previous token [%s]@[%d-%d]",
                        abbreviate(token.getCoveredText(), "…", 10), token.getBegin(),
                        token.getEnd(), abbreviate(prevToken.getCoveredText(), "…", 10),
                        prevToken.getBegin(), prevToken.getEnd()));
                ok = false;
            }
            prevToken = token;
        }
        return ok;
    }

    private boolean checkSentences(CAS aCas, List<LogMessage> aMessages)
    {
        boolean ok = true;
        var sentenceIter = aCas.<Annotation> select(Sentence._TypeName).iterator();
        Annotation prevSentence = null;
        while (sentenceIter.hasNext()) {
            var sentence = sentenceIter.next();
            if (prevSentence != null && sentence.getBegin() < prevSentence.getEnd()) {
                aMessages.add(LogMessage.error(this,
                        "Sentence [%s]@[%d-%d] overlaps with previous sentence at [%s]@[%d-%d]",
                        abbreviateMiddle(sentence.getCoveredText(), "…", 10), sentence.getBegin(),
                        sentence.getEnd(), abbreviateMiddle(prevSentence.getCoveredText(), "…", 10),
                        prevSentence.getBegin(), prevSentence.getEnd()));
                ok = false;
            }
            prevSentence = sentence;
        }
        return ok;
    }
}
