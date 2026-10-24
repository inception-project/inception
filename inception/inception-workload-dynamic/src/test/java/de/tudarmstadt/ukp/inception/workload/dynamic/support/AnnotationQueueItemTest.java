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
package de.tudarmstadt.ukp.inception.workload.dynamic.support;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_FINISHED;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;

class AnnotationQueueItemTest
{
    @Test
    void thatDocumentReadyForCurationIsAnnotationFinished()
    {
        var sourceDoc = sourceDocumentInState(SourceDocumentState.ANNOTATION_IN_PROGRESS);

        var item = new AnnotationQueueItem(sourceDoc, List.of(finishedBy("user1", sourceDoc)), 1,
                Duration.ZERO);

        assertThat(item.getState()).isEqualTo(ANNOTATION_FINISHED);
        assertThat(item.isInCuration()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = SourceDocumentState.class, names = { "CURATION_IN_PROGRESS",
            "CURATION_FINISHED" })
    void thatCurationStateIsKeptWhenReadyForCuration(SourceDocumentState aState)
    {
        var sourceDoc = sourceDocumentInState(aState);

        var item = new AnnotationQueueItem(sourceDoc, List.of(finishedBy("user1", sourceDoc)), 1,
                Duration.ZERO);

        // Without the curation check, this would be ANNOTATION_FINISHED
        assertThat(item.getState()).isEqualTo(aState);
        assertThat(item.isInCuration()).isTrue();
    }

    private static SourceDocument sourceDocumentInState(SourceDocumentState aState)
    {
        return SourceDocument.builder() //
                .withName("test-doc.txt") //
                .withProject(new Project("test-project")) //
                .withFormat("text") //
                .withState(aState) //
                .build();
    }

    private static AnnotationDocument finishedBy(String aUser, SourceDocument aSourceDoc)
    {
        return AnnotationDocument.builder() //
                .withUser(aUser) //
                .forDocument(aSourceDoc) //
                .withState(FINISHED) //
                .build();
    }
}
