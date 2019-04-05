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
package de.tudarmstadt.ukp.inception.conceptlinking.ranking;

import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_FREQUENCY;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_ID_RANK;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_LEVENSHTEIN_MENTION;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_LEVENSHTEIN_MENTION_CONTEXT;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_LEVENSHTEIN_QUERY;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_NUM_RELATIONS;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_QUERY;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_SIGNATURE_OVERLAP_SCORE;

import java.util.Comparator;

import org.apache.commons.lang3.builder.CompareToBuilder;

import de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity;

public class BaselineRankingStrategy
{
    private static Comparator<CandidateEntity> INSTANCE = (e1, e2) -> new CompareToBuilder()
            // Did the user enter an URI and does the candidate exactly match it?
            // Note that the comparator sorts ascending by value, so a match is represented using
            // a 0 while a mistmatch is represented using 1. The typical case is that neither
            // candidate matches the query which causes the next ranking criteria to be evaluated
            .append(e1.get(KEY_QUERY).map(q -> q.equals(e1.getIRI()) ? 0 : 1).orElse(1), 
                    e2.get(KEY_QUERY).map(q -> q.equals(e2.getIRI()) ? 0 : 1).orElse(1))
            // The edit distance between query and label is given high importance
            // Comparing simultaneously against the edit distance to the query and to the 
            // mention causes items similar to either to be ranked up
            .append(Math.min(e1.get(KEY_LEVENSHTEIN_QUERY).get(),
                            e1.get(KEY_LEVENSHTEIN_MENTION).get()),
                    Math.min(e2.get(KEY_LEVENSHTEIN_QUERY).get(),
                            e2.get(KEY_LEVENSHTEIN_MENTION).get()))
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
    
    public static Comparator<CandidateEntity> getInstance()
    {
        return INSTANCE;
    }
}
