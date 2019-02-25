/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.conceptlinking.service.feature;

import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_LEVENSHTEIN_MENTION;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_LEVENSHTEIN_MENTION_CONTEXT;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_LEVENSHTEIN_QUERY;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_MENTION;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_MENTION_CONTEXT;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_QUERY;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity;

@Component
public class LevenshteinFeatureGenerator
    implements EntityRankingFeatureGenerator
{
    private final LevenshteinDistance lev = LevenshteinDistance.getDefaultInstance();

    @Override
    public void apply(CandidateEntity aCandidate)
    {
        String label = aCandidate.getLabel();

        aCandidate.get(KEY_MENTION).map(mention -> lev.apply(label, mention))
                .ifPresent(score -> aCandidate.put(KEY_LEVENSHTEIN_MENTION, score));

        aCandidate.get(KEY_QUERY).map(query -> lev.apply(label, query))
                .ifPresent(score -> aCandidate.put(KEY_LEVENSHTEIN_QUERY, score));

        aCandidate.get(KEY_MENTION_CONTEXT)
                .map(context -> lev.apply(label, StringUtils.join(context, ' ')))
                .ifPresent(score -> aCandidate.put(KEY_LEVENSHTEIN_MENTION_CONTEXT, score));
    }
}
