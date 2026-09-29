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
package de.tudarmstadt.ukp.clarin.webanno.agreement.results;

import static de.tudarmstadt.ukp.clarin.webanno.agreement.SkipReason.FAILED;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.SkipReason.NOT_ANNOTATED;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.SkipReason.NOT_IN_CURATION_STATE;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.results.SkipDescriptions.describeSkips;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.agreement.AgreementSummary;
import de.tudarmstadt.ukp.clarin.webanno.agreement.SkipReason;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

class SkipDescriptionsTest
{
    private final User alice = user("alice", "Alice");
    private final User bob = user("bob", "Bob");

    @Test
    void thatNothingIsDescribedWithoutSkips()
    {
        var summary = new AgreementSummary("custom.Span", "value", 0.8);

        assertThat(describeSkips(summary, new DefaultAgreementTraits(), alice, bob)).isEmpty();
    }

    @Test
    void thatSkipsAreCountedPerReasonAndNameTheRater()
    {
        var summary = skipped(NOT_IN_CURATION_STATE, null);
        summary.merge(skipped(NOT_IN_CURATION_STATE, null));
        summary.merge(skipped(NOT_ANNOTATED, "bob"));
        summary.merge(skipped(FAILED, null));

        assertThat(describeSkips(summary, new DefaultAgreementTraits(), alice, bob)) //
                .startsWith("Documents skipped:") //
                .contains("- not in a curation state: 2") //
                .contains("- not worked on by Bob: 1") //
                .contains("- calculation failed (see log): 1");
    }

    @Test
    void thatFailureToLoadNamesTheRater()
    {
        var summary = skipped(FAILED, "alice");
        summary.merge(skipped(FAILED, null));

        assertThat(describeSkips(summary, new DefaultAgreementTraits(), alice, bob)) //
                .contains("- data of Alice could not be loaded (see log): 1") //
                .contains("- calculation failed (see log): 1");
    }

    @Test
    void thatMissingDocumentsAreDescribedAsNotFinishedWhenLimitedToFinished()
    {
        // Only finished documents are loaded then, so a rater may well have started the document.
        var traits = new DefaultAgreementTraits();
        traits.setLimitToFinishedDocuments(true);

        assertThat(describeSkips(skipped(NOT_ANNOTATED, "alice"), traits, alice, bob)) //
                .contains("- not finished by Alice: 1");
    }

    @Test
    void thatCurationIsDescribedAsNotFinishedWhenLimitedToFinished()
    {
        // Documents still being curated are skipped then as well, so "not in a curation state"
        // would be wrong for them.
        var traits = new DefaultAgreementTraits();
        traits.setLimitToFinishedDocuments(true);

        assertThat(describeSkips(skipped(NOT_IN_CURATION_STATE, null), traits, alice, bob)) //
                .contains("- curation not finished: 1");
    }

    private static AgreementSummary skipped(SkipReason aReason, String aRater)
    {
        var layer = new AnnotationLayer();
        layer.setName("custom.Span");

        var feature = new AnnotationFeature();
        feature.setName("value");
        feature.setLayer(layer);

        return AgreementSummary.skipped(layer, feature, aReason, aRater);
    }

    private static User user(String aUsername, String aUiName)
    {
        var user = new User(aUsername);
        user.setUiName(aUiName);
        return user;
    }
}
