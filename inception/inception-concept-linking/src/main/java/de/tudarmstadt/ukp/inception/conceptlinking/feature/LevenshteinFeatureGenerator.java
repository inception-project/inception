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
package de.tudarmstadt.ukp.inception.conceptlinking.feature;

import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_LEVENSHTEIN_MENTION;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_LEVENSHTEIN_MENTION_CONTEXT;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_LEVENSHTEIN_MENTION_NC;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_LEVENSHTEIN_QUERY;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_LEVENSHTEIN_QUERY_NC;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_MENTION;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_MENTION_CONTEXT;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_QUERY;
import static org.apache.commons.lang3.StringUtils.join;

import org.apache.commons.text.similarity.LevenshteinDistance;

import de.tudarmstadt.ukp.inception.conceptlinking.config.EntityLinkingServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link EntityLinkingServiceAutoConfiguration#levenshteinFeatureGenerator()}.
 * </p>
 */
public class LevenshteinFeatureGenerator
    implements EntityRankingFeatureGenerator
{
    private final LevenshteinDistance lev = LevenshteinDistance.getDefaultInstance();

    @Override
    public void apply(CandidateEntity aCandidate)
    {
        String label = aCandidate.getLabel();
        String labelNC = aCandidate.getLabel().toLowerCase(aCandidate.getLocale());

        aCandidate.get(KEY_MENTION) //
                .map(mention -> lev.apply(label, mention)) //
                .ifPresent(score -> aCandidate.put(KEY_LEVENSHTEIN_MENTION, score));

        aCandidate.get(KEY_MENTION) //
                .map(mention -> lev.apply(labelNC, mention)) //
                .ifPresent(score -> aCandidate.put(KEY_LEVENSHTEIN_MENTION_NC, score));

        aCandidate.get(KEY_QUERY) //
                .map(query -> lev.apply(label, query)) //
                .ifPresent(score -> aCandidate.put(KEY_LEVENSHTEIN_QUERY, score));

        aCandidate.get(KEY_QUERY) //
                .map(query -> lev.apply(labelNC, query)) //
                .ifPresent(score -> aCandidate.put(KEY_LEVENSHTEIN_QUERY_NC, score));

        aCandidate.get(KEY_MENTION_CONTEXT) //
                .map(context -> lev.apply(label, join(context, ' '))) //
                .ifPresent(score -> aCandidate.put(KEY_LEVENSHTEIN_MENTION_CONTEXT, score));
    }
}
