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

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;

public class PerAnnotationContextGenerator
    implements PromptContextGenerator
{
    @Override
    public Stream<PromptContext> generate(RecommendationEngine aEngine, CAS aCas, int aBegin,
            int aEnd, Map<String, ? extends Object> aBindings)
    {
        var predictedType = aEngine.getPredictedType(aCas);
        var candidates = selectOverlapping(aCas, predictedType, aBegin, aEnd);
        return candidates.stream().map(candidate -> {
            var sentence = aCas.select(Sentence.class).covering(candidate) //
                    .map(Sentence::getCoveredText) //
                    .findFirst().orElse("");
            var context = new PromptContext(candidate);
            context.setAll(aBindings);
            context.set(VAR_CAS, new CasWrapper(aCas));
            context.set(VAR_TEXT, candidate.getCoveredText());
            context.set(VAR_SENTENCE, sentence);
            return context;
        });
    }
}
