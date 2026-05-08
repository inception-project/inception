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
package de.tudarmstadt.ukp.inception.workload.matrix.management.support;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IGNORE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.NEW;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_IN_PROGRESS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;

class DocumentMatrixRowTest
{
    @Test
    void thatStateIsNewWhenNoAnnotationDocumentsExist()
    {
        var project = new Project("test-project");
        var sourceDoc = new SourceDocument("test-doc.txt", project, "text");

        var annotator1 = AnnotationSet.forTest("user1");
        var annotator2 = AnnotationSet.forTest("user2");
        var annotators = Set.of(annotator1, annotator2);

        var row = new DocumentMatrixRow(sourceDoc, annotators);

        assertThat(row.getState()).isEqualTo(SourceDocumentState.NEW);
    }

    @Test
    void thatStateIsAnnotationFinishedWhenAllAnnotatorsFinished()
    {
        var project = new Project("test-project");
        var sourceDoc = new SourceDocument("test-doc.txt", project, "text");

        var annotator1 = AnnotationSet.forTest("user1");
        var annotator2 = AnnotationSet.forTest("user2");
        var annotators = Set.of(annotator1, annotator2);

        var row = new DocumentMatrixRow(sourceDoc, annotators);

        // Add finished annotation documents
        var annDoc1 = AnnotationDocument.builder() //
                .withUser("user1") //
                .forDocument(sourceDoc) //
                .withState(FINISHED) //
                .build();

        var annDoc2 = AnnotationDocument.builder() //
                .withUser("user2") //
                .forDocument(sourceDoc) //
                .withState(FINISHED) //
                .build();

        row.add(annDoc1);
        row.add(annDoc2);

        assertThat(row.getState()).isEqualTo(ANNOTATION_FINISHED);
    }

    @Test
    void thatStateIsInProgressWhenSomeAnnotatorsFinished()
    {
        var project = new Project("test-project");
        var sourceDoc = new SourceDocument("test-doc.txt", project, "text");

        var annotator1 = AnnotationSet.forTest("user1");
        var annotator2 = AnnotationSet.forTest("user2");
        var annotators = Set.of(annotator1, annotator2);

        var row = new DocumentMatrixRow(sourceDoc, annotators);

        var annDoc1 = AnnotationDocument.builder() //
                .withUser("user1") //
                .forDocument(sourceDoc) //
                .withState(FINISHED) //
                .build();

        var annDoc2 = AnnotationDocument.builder() //
                .withUser("user2") //
                .forDocument(sourceDoc) //
                .withState(NEW) //
                .build();

        row.add(annDoc1);
        row.add(annDoc2);

        assertThat(row.getState()).isEqualTo(ANNOTATION_IN_PROGRESS);
    }

    @Test
    void thatStateIsInProgressWhenAtLeastOneAnnotatorInProgress()
    {
        var project = new Project("test-project");
        var sourceDoc = new SourceDocument("test-doc.txt", project, "text");

        var annotator1 = AnnotationSet.forTest("user1");
        var annotator2 = AnnotationSet.forTest("user2");
        var annotators = Set.of(annotator1, annotator2);

        var row = new DocumentMatrixRow(sourceDoc, annotators);

        var annDoc1 = AnnotationDocument.builder() //
                .withUser("user1") //
                .forDocument(sourceDoc) //
                .withState(IN_PROGRESS) //
                .build();

        row.add(annDoc1);

        assertThat(row.getState()).isEqualTo(ANNOTATION_IN_PROGRESS);
    }

    @Test
    void thatIgnoredAnnotatorsAreExcludedFromRequiredCount()
    {
        var project = new Project("test-project");
        var sourceDoc = new SourceDocument("test-doc.txt", project, "text");

        var annotator1 = AnnotationSet.forTest("user1");
        var annotator2 = AnnotationSet.forTest("user2");
        var annotator3 = AnnotationSet.forTest("user3");
        var annotators = Set.of(annotator1, annotator2, annotator3);

        var row = new DocumentMatrixRow(sourceDoc, annotators);

        var annDoc1 = AnnotationDocument.builder() //
                .withUser("user1") //
                .forDocument(sourceDoc) //
                .withState(FINISHED) //
                .build();

        var annDoc2 = AnnotationDocument.builder() //
                .withUser("user2") //
                .forDocument(sourceDoc) //
                .withState(IGNORE) //
                .build();

        var annDoc3 = AnnotationDocument.builder() //
                .withUser("user3") //
                .forDocument(sourceDoc) //
                .withState(FINISHED) //
                .build();

        row.add(annDoc1);
        row.add(annDoc2);
        row.add(annDoc3);

        // Should be ANNOTATION_FINISHED because 2/2 non-ignored annotators are finished
        assertThat(row.getState()).isEqualTo(ANNOTATION_FINISHED);
    }

    @Test
    void thatStateIsCorrectWhenDisplayNameMatchesUsername()
    {
        var project = new Project("test-project");
        var sourceDoc = new SourceDocument("test-doc.txt", project, "text");

        // When display name matches username, everything works
        var annotator = AnnotationSet.forTest("alice"); // Both id and displayName are "alice"
        var annotators = Set.of(annotator);

        var row = new DocumentMatrixRow(sourceDoc, annotators);

        var annDoc = AnnotationDocument.builder() //
                .withUser("alice") //
                .forDocument(sourceDoc) //
                .withState(FINISHED) //
                .build();

        row.add(annDoc);

        // This works correctly because both AnnotationSets have the same displayName
        assertThat(row.getState()).isEqualTo(ANNOTATION_FINISHED);
    }

    /**
     * This test documents the EXPECTED behavior after the bug is fixed. When a user's display name
     * differs from their username, getAnnotationDocument() should still be able to retrieve the
     * annotation document.
     */
    @Test
    void thatGetAnnotationDocumentReturnsDocumentWhenDisplayNameDiffers()
    {
        var project = new Project("test-project");
        var sourceDoc = new SourceDocument("test-doc.txt", project, "text");

        var annotatorWithDisplayName = new AnnotationSet("bob", "Bob Jones");
        var annotators = Set.of(annotatorWithDisplayName);

        var row = new DocumentMatrixRow(sourceDoc, annotators);

        var annDoc = AnnotationDocument.builder() //
                .withUser("bob") //
                .forDocument(sourceDoc) //
                .withState(FINISHED) //
                .build();

        row.add(annDoc);

        // This currently fails but should work after the fix
        assertThat(row.getAnnotationDocument(annotatorWithDisplayName)).isNotNull();
        assertThat(row.getAnnotationDocument(annotatorWithDisplayName).getState())
                .isEqualTo(FINISHED);
    }
}
