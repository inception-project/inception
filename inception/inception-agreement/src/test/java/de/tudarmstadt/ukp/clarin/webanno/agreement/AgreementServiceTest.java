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

import static de.tudarmstadt.ukp.clarin.webanno.agreement.AgreementService.isCurationToEvaluate;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;

class AgreementServiceTest
{
    @Test
    void thatCurationIsEvaluatedOnceStarted()
    {
        var traits = new DefaultAgreementTraits();

        assertThat(isCurationToEvaluate(document(ANNOTATION_IN_PROGRESS), traits)).isFalse();
        assertThat(isCurationToEvaluate(document(CURATION_IN_PROGRESS), traits)).isTrue();
        assertThat(isCurationToEvaluate(document(CURATION_FINISHED), traits)).isTrue();
    }

    @Test
    void thatCurationIsOnlyEvaluatedOnceFinishedWhenLimitedToFinished()
    {
        var traits = new DefaultAgreementTraits();
        traits.setLimitToFinishedDocuments(true);

        assertThat(isCurationToEvaluate(document(ANNOTATION_IN_PROGRESS), traits)).isFalse();
        assertThat(isCurationToEvaluate(document(CURATION_IN_PROGRESS), traits)).isFalse();
        assertThat(isCurationToEvaluate(document(CURATION_FINISHED), traits)).isTrue();
    }

    private static SourceDocument document(SourceDocumentState aState)
    {
        var document = new SourceDocument();
        document.setState(aState);
        return document;
    }
}
