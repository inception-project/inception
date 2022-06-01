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

import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_FREQUENCY;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_ID_RANK;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_LABEL_NC;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_LEVENSHTEIN_MENTION;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_LEVENSHTEIN_MENTION_CONTEXT;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_LEVENSHTEIN_QUERY;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_LEVENSHTEIN_QUERY_NC;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_NUM_RELATIONS;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_QUERY;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_QUERY_IS_LOWER_CASE;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_QUERY_NC;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_SIGNATURE_OVERLAP_SCORE;

import java.util.Comparator;

import org.apache.commons.lang3.builder.CompareToBuilder;

import de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity;

public class BaselineRankingStrategy
{
    private static final Comparator<CandidateEntity> INSTANCE = (e1, e2) -> new CompareToBuilder()
            // Did the user enter an URI and does the candidate exactly match it?
            // Note that the comparator sorts ascending by value, so a match is represented using
            // a 0 while a mismatch is represented using 1. The typical case is that neither
            // candidate matches the query which causes the next ranking criteria to be evaluated
            .append(queryMatchesIri(e1), queryMatchesIri(e2))
            // Prefer matches where the query appears in the label
            .append(labelMatchesQueryNC(e1), labelMatchesQueryNC(e2))
            // Compare geometric mean of the Levenshtein distance to query and mention
            // since both are important and a very close similarity in say the mention outweighs
            // a not so close similarity in the query
            .append(weightedLevenshteinDistance(e1), weightedLevenshteinDistance(e2))
            // Prefer good matches on the query over good matches on the mention
            .append(queryOverMention(e1), queryOverMention(e2))
            // Cased over caseless
            .append(casedOverCaseless(e1), casedOverCaseless(e2))
            // A high signature overlap score is preferred.
            .append(e2.get(KEY_SIGNATURE_OVERLAP_SCORE).get(),
                    e1.get(KEY_SIGNATURE_OVERLAP_SCORE).get())
            // A low edit distance is preferred.
            .append(e1.get(KEY_LEVENSHTEIN_MENTION_CONTEXT).get(),
                    e2.get(KEY_LEVENSHTEIN_MENTION_CONTEXT).get())
            // A high entity frequency is preferred.
            .append(e2.get(KEY_FREQUENCY).get(), e1.get(KEY_FREQUENCY).get())
            // A high number of related relations is preferred.
            .append(e2.get(KEY_NUM_RELATIONS).get(), e1.get(KEY_NUM_RELATIONS).get())
            // A low wikidata ID rank is preferred.
            .append(e1.get(KEY_ID_RANK).get(), e2.get(KEY_ID_RANK).get())
            // Finally order alphabetically
            .append(e1.getLabel().toLowerCase(e1.getLocale()),
                    e2.getLabel().toLowerCase(e2.getLocale()))
            .toComparison();

    private static double queryMatchesIri(CandidateEntity aCandidate)
    {
        return aCandidate.get(KEY_QUERY).map(q -> q.equals(aCandidate.getIRI()) ? 0 : 1).orElse(1);
    }

    private static double labelMatchesQueryNC(CandidateEntity aCandidate)
    {
        return aCandidate.get(KEY_QUERY_NC)
                .map(q -> aCandidate.get(KEY_LABEL_NC).map(l -> l.contains(q) ? 0 : 1).orElse(1))
                .orElse(1);
    }

    private static double casedOverCaseless(CandidateEntity aCandidate)
    {
        int queryNC = aCandidate.get(KEY_LEVENSHTEIN_QUERY_NC).get();
        int query = aCandidate.get(KEY_LEVENSHTEIN_QUERY).get();

        return queryNC <= query ? 0 : 1;
    }

    private static double queryOverMention(CandidateEntity aCandidate)
    {
        boolean caseInsensitive = aCandidate.get(KEY_QUERY_IS_LOWER_CASE).orElse(true);
        int query = aCandidate
                .get(caseInsensitive ? KEY_LEVENSHTEIN_QUERY_NC : KEY_LEVENSHTEIN_QUERY).get();
        int mention = aCandidate.get(KEY_LEVENSHTEIN_MENTION).get();

        return query <= mention ? 0 : 1;
    }

    private static double weightedLevenshteinDistance(CandidateEntity aCandidate)
    {
        boolean caseInsensitive = aCandidate.get(KEY_QUERY_IS_LOWER_CASE).orElse(true);

        int query = aCandidate
                .get(caseInsensitive ? KEY_LEVENSHTEIN_QUERY_NC : KEY_LEVENSHTEIN_QUERY).get();
        int mention = aCandidate.get(KEY_LEVENSHTEIN_MENTION).get();

        if (query == Integer.MAX_VALUE && mention == Integer.MAX_VALUE) {
            return Double.MAX_VALUE;
        }

        if (query == Integer.MAX_VALUE) {
            return Math.sqrt(mention);
        }

        if (mention == Integer.MAX_VALUE) {
            return Math.sqrt(query);
        }

        return Math.sqrt(query * mention);
    }

    public static Comparator<CandidateEntity> getInstance()
    {
        return INSTANCE;
    }
}
