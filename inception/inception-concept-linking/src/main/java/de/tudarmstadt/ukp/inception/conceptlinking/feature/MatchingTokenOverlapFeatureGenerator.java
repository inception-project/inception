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

import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_MENTION_BOW;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_MENTION_BOW_NC;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_MENTION_CONTEXT;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_QUERY_BEST_MATCH_TERM_NC;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_QUERY_BOW;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_QUERY_BOW_NC;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.SCORE_TOKEN_OVERLAP_MENTION;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.SCORE_TOKEN_OVERLAP_MENTION_CONTEXT;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.SCORE_TOKEN_OVERLAP_MENTION_NC;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.SCORE_TOKEN_OVERLAP_QUERY;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.SCORE_TOKEN_OVERLAP_QUERY_NC;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.sortedBagOfWords;
import static java.util.Arrays.copyOf;

import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.inception.conceptlinking.config.EntityLinkingServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link EntityLinkingServiceAutoConfiguration#matchingTokenOverlapFeatureGenerator}.
 * </p>
 */
@Order(200) // Make sure QUERY_BEST_MATCH_TERM from Levenshtein is overwritten
public class MatchingTokenOverlapFeatureGenerator
    implements EntityRankingFeatureGenerator
{

    @Override
    public void apply(CandidateEntity aCandidate)
    {
        update(aCandidate, aCandidate.getLabel());
        aCandidate.getHandle().getMatchTerms().forEach(p -> update(aCandidate, p.getKey()));
    }

    private void update(CandidateEntity aCandidate, String aTerm)
    {
        var tokens = sortedBagOfWords(aTerm);
        var tokensNC = sortedBagOfWords(aTerm.toLowerCase(aCandidate.getLocale()));

        aCandidate.get(KEY_MENTION_BOW_NC) //
                .map(mention -> bagOfWordsDistance(tokensNC, mention)) //
                .filter(score -> score >= 0) //
                .ifPresent(score -> aCandidate.mergeMin(SCORE_TOKEN_OVERLAP_MENTION_NC, score));

        aCandidate.get(KEY_QUERY_BOW_NC) //
                .map(query -> bagOfWordsDistance(tokensNC, query)) //
                .filter(score -> score >= 0) //
                .ifPresent(score -> {
                    if (aCandidate.mergeMin(SCORE_TOKEN_OVERLAP_QUERY_NC, score)) {
                        aCandidate.put(KEY_QUERY_BEST_MATCH_TERM_NC, aTerm);
                    }
                });

        aCandidate.get(KEY_MENTION_BOW) //
                .map(mention -> bagOfWordsDistance(tokens, mention)) //
                .filter(score -> score >= 0) //
                .ifPresent(score -> aCandidate.mergeMin(SCORE_TOKEN_OVERLAP_MENTION, score));

        aCandidate.get(KEY_QUERY_BOW) //
                .map(query -> bagOfWordsDistance(tokens, query)) //
                .filter(score -> score >= 0) //
                .ifPresent(score -> aCandidate.mergeMin(SCORE_TOKEN_OVERLAP_QUERY, score));

        aCandidate.get(KEY_MENTION_CONTEXT) //
                .map(context -> bagOfWordsDistance(tokens, context.toArray(String[]::new))) //
                .filter(score -> score >= 0) //
                .ifPresent(
                        score -> aCandidate.mergeMin(SCORE_TOKEN_OVERLAP_MENTION_CONTEXT, score));
    }

    static int bagOfWordsDistance(String[] aSortedBowCandidate, String[] aSortedBowUser)
    {
        if (aSortedBowCandidate == null || aSortedBowUser == null) {
            return -1;
        }

        var sortedBowCandidate = copyOf(aSortedBowCandidate, aSortedBowCandidate.length);

        var userTermsMatched = 0;
        var distance = 0;

        // First go over the terms provided by the user
        for (var userIndex = 0; userIndex < aSortedBowUser.length; userIndex++) {
            var userTerm = aSortedBowUser[userIndex];

            if (userTerm.length() == 0) {
                continue;
            }

            var userTermMatched = false;

            for (var candIndex = 0; candIndex < sortedBowCandidate.length; candIndex++) {
                var candTerm = sortedBowCandidate[candIndex];

                if (candTerm == null || candTerm.length() == 0) {
                    continue;
                }

                if (candTerm.startsWith(userTerm)) {
                    distance += candTerm.length() - userTerm.length();
                    // Since the terms are sorted by length and are unique, there won't be another
                    // term that is a longer prefix of the current candidate. Thus, we can remove
                    // the candidate.
                    sortedBowCandidate[candIndex] = null;
                    userTermMatched = true;
                }
            }

            if (userTermMatched) {
                userTermsMatched++;
            }
        }

        // Any candidate terms that were not matched by a user term contribute to the distance
        for (var candIndex = 0; candIndex < sortedBowCandidate.length; candIndex++) {
            var candTerm = sortedBowCandidate[candIndex];
            if (candTerm != null) {
                distance += candTerm.length();
            }
        }

        if (userTermsMatched == aSortedBowUser.length) {
            return distance;
        }

        return -1;
    }
}
