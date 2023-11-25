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
package de.tudarmstadt.ukp.inception.recommendation.imls.ollama.prompt;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectOverlapping;

import java.util.stream.Stream;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.util.CasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

public class PerAnnotationBindingsGenerator
    implements PromptBindingsGenerator
{

    @Override
    public Stream<PromptContext> generate(CAS aCas, int aBegin, int aEnd)
    {
        var candidateType = CasUtil.getAnnotationType(aCas, Sentence.class);
        return selectOverlapping(aCas, candidateType, aBegin, aEnd).stream().map(candidate -> {
            var sentence = aCas.select(Sentence.class).covering(candidate) //
                    .map(Sentence::getCoveredText) //
                    .findFirst().orElse("");
            var context = new PromptContext(candidate);
            context.set(VAR_TEXT, candidate.getCoveredText());
            context.set(VAR_SENTENCE, sentence);
            return context;

        });
    }
}
