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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.prompt;

import java.util.Map;
import java.util.stream.Stream;

import org.apache.uima.cas.CAS;
import org.dkpro.core.api.xml.type.XmlElement;

import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.support.text.TrimUtils;

public class PerParagraphContextGenerator
    implements PromptContextGenerator
{
    @Override
    public Stream<PromptContext> generate(RecommendationEngine aEngine, CAS aCas, int aBegin,
            int aEnd, Map<String, ? extends Object> aBindings)
    {
        if (aCas == null || aCas.getDocumentText().isEmpty()) {
            return Stream.empty();
        }

        var paragraphElements = aCas.select(XmlElement.class) //
                .filter(e -> "p".equals(e.getQName())) //
                .toList();

        // Use XML/HTML structure if available
        if (!paragraphElements.isEmpty()) {
            return paragraphElements.stream().map(paragraph -> {
                var context = new PromptContext(paragraph);
                context.setAll(aBindings);
                context.set(VAR_CAS, new CasWrapper(aCas));
                context.set(VAR_TEXT, paragraph.getCoveredText());
                return context;
            });
        }

        return Stream
                .iterate(new PromptContext(0, 0, ""), state -> state != null,
                        state -> nextContext(aCas, state)) //
                .filter(pc -> !pc.getText().isBlank());
    }

    private PromptContext nextContext(CAS aCas, PromptContext state)
    {
        var text = aCas.getDocumentText();

        if (text.substring(state.getRange().getEnd(), aCas.getDocumentText().length()).isBlank()) {
            return null;
        }

        var lineBreakSequenceLength = 0;
        var i = state.getRange().getEnd();
        var seenContent = false;
        while (i < text.length()) {
            if (text.charAt(i) == '\n') {
                lineBreakSequenceLength++;
            }
            else if (text.charAt(i) != '\r') {
                if (lineBreakSequenceLength > 1 && seenContent) {
                    break;
                }

                lineBreakSequenceLength = 0;
                seenContent = true;
            }

            i++;
        }

        var offset = new int[] { state.getRange().getEnd(), Math.min(i, text.length()) };
        var contextEnd = offset[1];

        TrimUtils.trim(text, offset);

        return new PromptContext(offset[0], contextEnd, text.substring(offset[0], offset[1]));
    }
}
