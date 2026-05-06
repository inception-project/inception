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
package de.tudarmstadt.ukp.inception.conceptlinking.ranking;

import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_QUERY;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_QUERY_IS_LOWER_CASE;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.SCORE_LEVENSHTEIN_MENTION;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.SCORE_LEVENSHTEIN_MENTION_CONTEXT;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.SCORE_LEVENSHTEIN_QUERY;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.SCORE_LEVENSHTEIN_QUERY_NC;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.SCORE_TOKEN_OVERLAP_QUERY_NC;
import static java.lang.Integer.MAX_VALUE;

import java.util.Comparator;

import de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity;

public class BaselineRankingStrategy
{
    private static final Comparator<CandidateEntity> INSTANCE = Comparator
            // 1. Did the user enter an URI and does the candidate exactly match it?
            // Note that the comparator sorts ascending by value, so a match is represented using
            // a 0 while a mismatch is represented using 1. The typical case is that neither
            .comparing(BaselineRankingStrategy::queryMatchesIri)
            // 2. Require token overlap
            .thenComparing(BaselineRankingStrategy::queryTokenOverlapNC)
            // 3. Compare geometric mean of the Levenshtein distance to query and mention
            // since both are important and a very close similarity in say the mention outweighs
            // a not so close similarity in the query (lower distance is better)
            .thenComparing(BaselineRankingStrategy::queryMentionWeightedLevenshteinDistance)
            // 4. Prefer good matches on the query over good matches on the mention
            .thenComparing(BaselineRankingStrategy::queryOverMention)
            // 5. Cased over case-less
            .thenComparing(BaselineRankingStrategy::queryCasedOverCaseless)
            // 6. A low edit distance is preferred.
            .thenComparing(BaselineRankingStrategy::mentionContextLevenshteinDistance)
            // 7. Finally order alphabetically
            .thenComparing(e -> e.getLabel().toLowerCase(e.getLocale()));

    private static int queryMatchesIri(CandidateEntity aCandidate)
    {
        return aCandidate.get(KEY_QUERY).map(q -> q.equals(aCandidate.getIRI()) ? 0 : 1).orElse(1);
    }

    private static int queryTokenOverlapNC(CandidateEntity aCandidate)
    {
        return aCandidate.get(SCORE_TOKEN_OVERLAP_QUERY_NC).orElse(MAX_VALUE);
    }

    private static int mentionContextLevenshteinDistance(CandidateEntity aCandidate)
    {
        return aCandidate.get(SCORE_LEVENSHTEIN_MENTION_CONTEXT).orElse(MAX_VALUE);
    }

    private static int queryCasedOverCaseless(CandidateEntity aCandidate)
    {
        var queryNC = aCandidate.get(SCORE_LEVENSHTEIN_QUERY_NC).orElse(MAX_VALUE);
        var query = aCandidate.get(SCORE_LEVENSHTEIN_QUERY).orElse(MAX_VALUE);

        return queryNC <= query ? 0 : 1;
    }

    private static int queryOverMention(CandidateEntity aCandidate)
    {
        var caseInsensitive = aCandidate.get(KEY_QUERY_IS_LOWER_CASE).orElse(true);
        var query = aCandidate
                .get(caseInsensitive ? SCORE_LEVENSHTEIN_QUERY_NC : SCORE_LEVENSHTEIN_QUERY).get();
        var mention = aCandidate.get(SCORE_LEVENSHTEIN_MENTION).get();

        return query <= mention ? 0 : 1;
    }

    private static double queryMentionWeightedLevenshteinDistance(CandidateEntity aCandidate)
    {
        var caseInsensitive = aCandidate.get(KEY_QUERY_IS_LOWER_CASE).orElse(true);

        var queryDistance = aCandidate
                .get(caseInsensitive ? SCORE_LEVENSHTEIN_QUERY_NC : SCORE_LEVENSHTEIN_QUERY).get();
        var mentionDistance = aCandidate.get(SCORE_LEVENSHTEIN_MENTION).get();

        if (queryDistance == MAX_VALUE && mentionDistance == MAX_VALUE) {
            return Double.MAX_VALUE;
        }

        if (queryDistance == MAX_VALUE) {
            return Math.sqrt(mentionDistance);
        }

        if (mentionDistance == MAX_VALUE) {
            return Math.sqrt(queryDistance);
        }

        return Math.sqrt(queryDistance * mentionDistance);
    }

    public static Comparator<CandidateEntity> getInstance()
    {
        return INSTANCE;
    }
}
