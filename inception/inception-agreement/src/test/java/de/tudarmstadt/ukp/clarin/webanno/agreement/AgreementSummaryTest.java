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
package de.tudarmstadt.ukp.clarin.webanno.agreement;

import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.Tag.INCOMPLETE_POSITION;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.dkpro.statistics.agreement.coding.CodingAnnotationStudy;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.FullCodingAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.ConfigurationSet;

class AgreementSummaryTest
{
    private static final String TYPE = "custom.Span";
    private static final String FEATURE = "value";

    @Test
    void thatRaterWithoutAnnotationsIsToldApartFromDisjunctPositions()
    {
        // rater1 annotated nothing, so every position is incomplete and - with incomplete positions
        // excluded - the study is empty. The study alone then cannot tell this apart from both
        // raters annotating at different positions.
        var onlyRater2 = summaryOf(incompleteSet("rater2"), incompleteSet("rater2"));

        assertThat(onlyRater2.getAnnotatedPositionCount("rater1")).isZero();
        assertThat(onlyRater2.getAnnotatedPositionCount("rater2")).isEqualTo(2);

        var disjunct = summaryOf(incompleteSet("rater1"), incompleteSet("rater2"));

        assertThat(disjunct.getAnnotatedPositionCount("rater1")).isEqualTo(1);
        assertThat(disjunct.getAnnotatedPositionCount("rater2")).isEqualTo(1);
    }

    @Test
    void thatAnnotatedPositionCountsAddUpWhenMerging()
    {
        // A rater who annotated only in the second document still annotated in the pair, and a
        // skipped document contributes nothing.
        var summary = summaryOf(incompleteSet("rater2"));
        summary.merge(summaryOf(incompleteSet("rater1")));
        summary.merge(new AgreementSummary(TYPE, FEATURE));

        assertThat(summary.getAnnotatedPositionCount("rater1")).isEqualTo(1);
        assertThat(summary.getAnnotatedPositionCount("rater2")).isEqualTo(1);
    }

    private static ConfigurationSet incompleteSet(String... aCasGroupIds)
    {
        var set = new ConfigurationSet(null).addTags(INCOMPLETE_POSITION);
        for (var casGroupId : aCasGroupIds) {
            set.addCasGroupId(casGroupId);
        }
        return set;
    }

    private static AgreementSummary summaryOf(ConfigurationSet... aSets)
    {
        var result = new FullCodingAgreementResult(TYPE, FEATURE, null,
                new CodingAnnotationStudy(2), List.of("rater1", "rater2"), List.of(aSets), true);
        return new AgreementSummary(result);
    }
}
