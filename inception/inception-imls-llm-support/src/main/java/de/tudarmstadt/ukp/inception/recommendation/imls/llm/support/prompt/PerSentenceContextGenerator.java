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

import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.selectOverlapping;

import java.util.Map;
import java.util.stream.Stream;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.util.CasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;

public class PerSentenceContextGenerator
    implements PromptContextGenerator
{
    @Override
    public Stream<PromptContext> generate(RecommendationEngine aEngine, CAS aCas, int aBegin,
            int aEnd, Map<String, ? extends Object> aBindings)
    {
        var sentenceType = CasUtil.getAnnotationType(aCas, Sentence.class);

        var candidates = selectOverlapping(aCas, sentenceType, aBegin, aEnd);

        return candidates.stream().map(candidate -> {
            var context = new PromptContext(candidate);
            context.setAll(aBindings);
            context.set(VAR_CAS, new CasWrapper(aCas));
            context.set(VAR_TEXT, candidate.getCoveredText());
            return context;
        });
    }
}
