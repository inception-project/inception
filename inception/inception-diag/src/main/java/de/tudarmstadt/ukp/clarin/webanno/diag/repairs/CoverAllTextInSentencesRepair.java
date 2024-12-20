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

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;

import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.Repair.Safe;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

@Safe(false)
public class CoverAllTextInSentencesRepair
    implements Repair
{
    @Override
    public void repair(SourceDocument aDocument, String aDataOwner, CAS aCas,
            List<LogMessage> aMessages)
    {
        int prevSentenceEnd = 0;
        for (Sentence sentence : aCas.select(Sentence.class)) {
            maybeAddSentence(aCas, prevSentenceEnd, sentence.getBegin(), aMessages);
            prevSentenceEnd = sentence.getEnd();
        }

        if (prevSentenceEnd < aCas.getDocumentText().length()) {
            maybeAddSentence(aCas, prevSentenceEnd, aCas.getDocumentText().length(), aMessages);
        }
    }

    private void maybeAddSentence(CAS aCas, int aInterSentenceBegin, int aInterSentenceEnd,
            List<LogMessage> aMessages)
    {
        if (!(aInterSentenceBegin < aInterSentenceEnd)) {
            return;
        }

        var interSentenceText = aCas.getDocumentText().substring(aInterSentenceBegin,
                aInterSentenceEnd);

        if (isBlank(interSentenceText)) {
            return;
        }

        Type sentenceType = aCas.getTypeSystem().getType(Sentence._TypeName);
        var newSentence = aCas.createAnnotation(sentenceType, aInterSentenceBegin,
                aInterSentenceEnd);
        newSentence.trim();
        aCas.addFsToIndexes(newSentence);

        aMessages.add(LogMessage.info(this, "Added new sentence at [%d-%d]", newSentence.getBegin(),
                newSentence.getEnd()));
    }
}
