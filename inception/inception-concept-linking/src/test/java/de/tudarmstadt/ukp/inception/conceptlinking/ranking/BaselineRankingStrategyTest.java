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

import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_LABEL_NC;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_MENTION;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_MENTION_NC;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_QUERY;
import static de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity.KEY_QUERY_NC;
import static java.util.Arrays.asList;
import static java.util.Locale.ROOT;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.conceptlinking.feature.CasingFeatureGenerator;
import de.tudarmstadt.ukp.inception.conceptlinking.feature.EntityRankingFeatureGenerator;
import de.tudarmstadt.ukp.inception.conceptlinking.feature.LevenshteinFeatureGenerator;
import de.tudarmstadt.ukp.inception.conceptlinking.model.CandidateEntity;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;

public class BaselineRankingStrategyTest
{
    private Comparator<CandidateEntity> sut = BaselineRankingStrategy.getInstance();

    private List<EntityRankingFeatureGenerator> generators = asList( //
            new CasingFeatureGenerator(), //
            new LevenshteinFeatureGenerator());

    @Test
    public void thatIriExactlyMatchingQueryIsRankedFirst()
    {
        var query = "123";
        var mention = "456";

        var exactIriMatch = new KBHandle(query, "1");
        var labelMatchLev0 = new KBHandle("1", "123");
        var labelMatchLev1 = new KBHandle("2", "23");
        var labelMatchLev2 = new KBHandle("3", "3");
        var mentionMatchLev0 = new KBHandle("4", "456");
        var mentionMatchLev1 = new KBHandle("5", "56");
        var mentionMatchLev2 = new KBHandle("6", "6");

        var candidates = build(query, mention, //
                labelMatchLev2, labelMatchLev1, labelMatchLev0, //
                mentionMatchLev2, mentionMatchLev1, mentionMatchLev0, //
                exactIriMatch);

        assertThat(candidates.stream().sorted(sut))
                .as("Candidate where IRI exactly matches query comes before exact label match")
                .extracting(CandidateEntity::getHandle) //
                .containsSubsequence(exactIriMatch, labelMatchLev0);

        assertThat(candidates.stream().sorted(sut)) //
                .extracting(CandidateEntity::getHandle) //
                .as("With same Levenshtein distance, label match takes precedence") //
                .containsExactly( //
                        exactIriMatch, //
                        labelMatchLev0, //
                        mentionMatchLev0, //
                        labelMatchLev1, //
                        mentionMatchLev1, //
                        labelMatchLev2, //
                        mentionMatchLev2);
    }

    @Test
    public void thatCasedEditDistanceWinsIfUncasedEditDistanceIsEqual()
    {
        var query = "this";
        var mention = "Chicago";

        var labelMatch0 = new KBHandle("1", "This");
        var labelMatch2 = new KBHandle("3", "tHIS");

        var candidates = build(query, mention, labelMatch0, labelMatch2);
        candidates.sort(sut);

        assertThat(candidates) //
                .extracting(CandidateEntity::getHandle) //
                .as("Lower cased edit distance wins") //
                .containsExactly( //
                        labelMatch0, //
                        labelMatch2);
    }

    @Test
    public void thatQueryQuiteDifferentFromMention()
    {
        String query = "this";
        String mention = "Chicago";

        var labelMatch0 = new KBHandle("1", "This");
        var labelMatch1 = new KBHandle("2", "this");
        var labelMatch2 = new KBHandle("3", "tHIS");
        var labelMatch3 = new KBHandle("4", "these");
        var mentionMatch0 = new KBHandle("5", "Chicago");
        var mentionMatch1 = new KBHandle("6", "Chico");
        var mentionMatch2 = new KBHandle("7", "Chicago Police");

        var candidates = build(query, mention, //
                labelMatch0, labelMatch1, labelMatch2, labelMatch3, //
                mentionMatch0, mentionMatch1, mentionMatch2);
        candidates.sort(sut);

        assertThat(candidates) //
                .extracting(CandidateEntity::getHandle) //
                .as("With same Levenshtein distance, label match takes precedence") //
                .containsExactly( //
                        labelMatch0, //
                        labelMatch1, //
                        labelMatch2, //
                        mentionMatch0, //
                        mentionMatch1, //
                        labelMatch3, //
                        mentionMatch2);
    }

    private List<CandidateEntity> build(String aQuery, String aMention, KBHandle... aCandidates)
    {
        var results = new ArrayList<CandidateEntity>();
        for (var candidate : aCandidates) {
            var cand = new CandidateEntity(candidate) //
                    .with(KEY_LABEL_NC, candidate.getUiLabel().toLowerCase(ROOT))
                    .with(KEY_QUERY, aQuery) //
                    .with(KEY_QUERY_NC, aQuery.toLowerCase(ROOT)) //
                    .with(KEY_MENTION, aMention) //
                    .with(KEY_MENTION_NC, aMention.toLowerCase(ROOT));

            generators.forEach(gen -> gen.apply(cand));
            results.add(cand);
        }
        return results;
    }
}
