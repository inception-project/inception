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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectSentenceCovering;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectTokensCovered;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_FREQUENCY;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_ID_RANK;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_LEVENSHTEIN_MENTION;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_LEVENSHTEIN_MENTION_CONTEXT;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_LEVENSHTEIN_QUERY;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_MENTION;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_MENTION_CONTEXT;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_NUM_RELATIONS;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_QUERY;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_SIGNATURE_OVERLAP_SCORE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.conceptlinking.feature.EntityRankingFeatureGenerator;
import de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;

public class BaselineRanker
    implements Ranker
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final Comparator<CandidateEntity> INSTANCE = (e1, e2) -> new CompareToBuilder()
            // Did the user enter an URI and does the candidate exactly match it?
            // Note that the comparator sorts ascending by value, so a match is represented using
            // a 0 while a mistmatch is represented using 1. The typical case is that neither
            // candidate matches the query which causes the next ranking criteria to be evaluated
            .append(e1.get(KEY_QUERY).map(q -> q.equals(e1.getIRI()) ? 0 : 1).orElse(1),
                    e2.get(KEY_QUERY).map(q -> q.equals(e2.getIRI()) ? 0 : 1).orElse(1))
            // Compare geometric mean of the Levenshtein distance to query and mention
            // since both are important and a very close similarity in say the mention outweighs
            // a not so close similarity in the query
            .append(weightedLevenshteinDistance(e1), weightedLevenshteinDistance(e2))
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

    private static double weightedLevenshteinDistance(CandidateEntity aCandidate)
    {
        int query = aCandidate.get(KEY_LEVENSHTEIN_QUERY).get();
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

        // If the distance of the query and mention is the same, then give the query a slight
        // benefit so the user see's items matching the query he/she entered first
        if (query > 0 && query == mention) {
            query = query - 1;
        }

        return Math.sqrt(query * mention);
    }

    public static Comparator<CandidateEntity> getInstance()
    {
        return INSTANCE;
    }

    private final List<EntityRankingFeatureGenerator> featureGenerators;
    private final Set<String> stopwords;
    private final int candidatesLimit;
    private final int contextSize;

    public BaselineRanker(List<EntityRankingFeatureGenerator> aFeatureGenerators,
                          Set<String> aStopwords, int aCandidatesLimit, int aContextSize) {
        featureGenerators = aFeatureGenerators;
        stopwords = aStopwords;
        candidatesLimit = aCandidatesLimit;
        contextSize = aContextSize;
    }

    @Override
    public List<KBHandle> rank(String aQuery, String aMention, Set<KBHandle> aCandidates,
                               CAS aCas, int aBeginOffset) {
        // Set the feature values
        List<CandidateEntity> candidates = aCandidates.parallelStream()
                .map(CandidateEntity::new)
                .map(candidate -> initCandidate(candidate, aQuery, aMention, aCas, aBeginOffset))
                .map(candidate -> {
                    for (EntityRankingFeatureGenerator generator : featureGenerators) {
                        generator.apply(candidate);
                    }
                    return candidate;
                })
                .collect(Collectors.toCollection(ArrayList::new));

        for (KBHandle c : aCandidates) {
            System.out.println(c);
        }

        // Do the main ranking
        // Sort candidates by multiple keys.
        candidates.sort(BaselineRanker.getInstance());

        List<KBHandle> results = candidates.stream()
                .map(candidate -> {
                    KBHandle handle = candidate.getHandle();
                    handle.setDebugInfo(String.valueOf(candidate.getFeatures()));
                    return handle;
                })
                .limit(candidatesLimit)
                .collect(Collectors.toList());

        return results;
    }

    private CandidateEntity initCandidate(CandidateEntity candidate, String aQuery, String aMention,
                                          CAS aCas, int aBegin)
    {
        candidate.put(KEY_MENTION, aMention);
        candidate.put(KEY_QUERY, aQuery);

        if (aCas != null) {
            AnnotationFS sentence = selectSentenceCovering(aCas, aBegin);
            if (sentence != null) {
                List<String> mentionContext = new ArrayList<>();
                Collection<AnnotationFS> tokens = selectTokensCovered(sentence);
                // Collect left context
                tokens.stream()
                        .filter(t -> t.getEnd() <= aBegin)
                        .sorted(Comparator.comparingInt(AnnotationFS::getBegin).reversed())
                        .limit(contextSize)
                        .map(t -> t.getCoveredText().toLowerCase(candidate.getLocale()))
                        .filter(s -> !stopwords.contains(s))
                        .forEach(mentionContext::add);
                // Collect right context
                tokens.stream()
                        .filter(t -> t.getBegin() >= (aBegin + aMention.length()))
                        .limit(contextSize)
                        .map(t -> t.getCoveredText().toLowerCase(candidate.getLocale()))
                        .filter(s -> !stopwords.contains(s))
                        .forEach(mentionContext::add);
                candidate.put(KEY_MENTION_CONTEXT, mentionContext);
            }
            else {
                log.warn("Mention sentence could not be determined. Skipping.");
            }
        }
        return candidate;
    }
}
