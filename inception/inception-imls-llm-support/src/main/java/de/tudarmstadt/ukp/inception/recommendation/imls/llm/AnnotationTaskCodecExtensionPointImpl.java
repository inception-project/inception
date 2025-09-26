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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response.AnnotationTaskCodec;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.traits.LlmRecommenderTraits;
import de.tudarmstadt.ukp.inception.support.extensionpoint.ExtensionPoint_ImplBase;

public class AnnotationTaskCodecExtensionPointImpl
    extends ExtensionPoint_ImplBase<AnnotationTaskCodecQuery, AnnotationTaskCodec>
    implements AnnotationTaskCodecExtensionPoint
{
    public AnnotationTaskCodecExtensionPointImpl(
            @Lazy @Autowired(required = false) List<AnnotationTaskCodec> aExtensions)
    {
        super(aExtensions);
    }

    @Override
    public Optional<AnnotationTaskCodec> getExtension(Recommender aRecommender,
            LlmRecommenderTraits aTraits)
    {
        return getExtension(new AnnotationTaskCodecQuery(aRecommender, aTraits));
    }

    @Override
    public Optional<AnnotationTaskCodec> getExtension(AnnotationTaskCodecQuery aTraits)
    {
        var candidates = getExtensions(aTraits);

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        if (candidates.size() > 1) {
            throw new IllegalArgumentException("Multiple matching candidates: " + candidates);
        }

        return Optional.of(candidates.get(0));
    }
}
