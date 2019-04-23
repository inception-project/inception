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

import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_LEVENSHTEIN_MENTION;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_LEVENSHTEIN_QUERY;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_QUERY;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;

public class BaselineRankingStrategyTest
{
    @Test
    public void thatiriExactlyMatchingQueryIsRankedFirst()
    {
        String query = "cand2";
        
        CandidateEntity cand1 = new CandidateEntity(new KBHandle("cand1"))
                .with(KEY_QUERY, query)
                .with(KEY_LEVENSHTEIN_QUERY, 0)
                .with(KEY_LEVENSHTEIN_MENTION, 0);
        CandidateEntity cand2 = new CandidateEntity(new KBHandle("cand2"))
                .with(KEY_QUERY, query)
                .with(KEY_LEVENSHTEIN_QUERY, 5)
                .with(KEY_LEVENSHTEIN_MENTION, 8);

        List<CandidateEntity> candidates = new ArrayList<>();
        candidates.add(cand1);
        candidates.add(cand2);
        
        candidates.sort(BaselineRankingStrategy.getInstance());
        
        assertThat(candidates)
                .as("Candidate where IRI exactly matches query comes before exact label match")
                .containsExactly(cand2, cand1);
    }
}
