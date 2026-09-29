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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.editor;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet.CURATION_SET;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet.forUser;
import static de.tudarmstadt.ukp.clarin.webanno.ui.annotation.editor.ViewportSyncLink.Obstacle.DIFFERENT_DOCUMENTS;
import static de.tudarmstadt.ukp.clarin.webanno.ui.annotation.editor.ViewportSyncLink.Obstacle.DIFFERENT_PAGING;
import static de.tudarmstadt.ukp.clarin.webanno.ui.annotation.editor.ViewportSyncLink.Obstacle.MIXED_PAGING;
import static de.tudarmstadt.ukp.clarin.webanno.ui.annotation.editor.ViewportSyncLink.Obstacle.NO_DOCUMENT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.annotation.editor.ViewportSyncLink.Obstacle.NO_EDITOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DocumentEditor;
import de.tudarmstadt.ukp.inception.rendering.paging.NoPagingStrategy;
import de.tudarmstadt.ukp.inception.rendering.paging.PagingStrategy;

class ViewportSyncLinkTest
{
    private SourceDocument docA;
    private SourceDocument docB;

    @BeforeEach
    void setup()
    {
        docA = new SourceDocument("a.txt", null, "text");
        docA.setId(1l);
        docB = new SourceDocument("b.txt", null, "text");
        docB.setId(2l);
    }

    @Test
    void thatUnpagedEditorsCanSyncOnTheSameDocument()
    {
        var sut = link(editor(docA, unpaged()), editor(docA, unpaged()));

        assertThat(sut.getObstacle()).isEmpty();
    }

    @Test
    void thatUnpagedEditorsCanSyncOnDifferentDocuments()
    {
        var sut = link(editor(docA, unpaged()), editor(docB, unpaged()));

        assertThat(sut.getObstacle()).isEmpty();
    }

    @Test
    void thatPagedEditorsWithTheSameStrategyCanSyncOnTheSameDocument()
    {
        var sut = link(editor(docA, pagedA()), editor(docA, pagedA()));

        assertThat(sut.getObstacle()).isEmpty();
    }

    @Test
    void thatPagedEditorsCannotSyncOnDifferentDocuments()
    {
        var sut = link(editor(docA, pagedA()), editor(docB, pagedA()));

        assertThat(sut.getObstacle()).contains(DIFFERENT_DOCUMENTS);
    }

    @Test
    void thatPagedEditorsWithDifferentStrategiesCannotSync()
    {
        var sut = link(editor(docA, pagedA()), editor(docA, pagedB()));

        assertThat(sut.getObstacle()).contains(DIFFERENT_PAGING);
    }

    @Test
    void thatMixedPagingCannotSync()
    {
        assertThat(link(editor(docA, pagedA()), editor(docA, unpaged())).getObstacle())
                .contains(MIXED_PAGING);
        assertThat(link(editor(docA, unpaged()), editor(docA, pagedA())).getObstacle())
                .contains(MIXED_PAGING);
    }

    @Test
    void thatTheDataOwnerDoesNotMatter()
    {
        var sut = link(editor(docA, pagedA(), forUser("annotator")),
                editor(docA, pagedA(), CURATION_SET));

        assertThat(sut.getObstacle()).isEmpty();
    }

    @Test
    void thatAnEditorWithoutDocumentCannotSync()
    {
        var sut = link(editor(docA, unpaged()), editor(null, unpaged()));

        assertThat(sut.getObstacle()).contains(NO_DOCUMENT);
    }

    @Test
    void thatAMissingEditorCannotSync()
    {
        var sut = link(editor(docA, unpaged()), null);

        assertThat(sut.getObstacle()).contains(NO_EDITOR);
    }

    @Test
    void thatEnabledPossibleSyncLinksBothEditors()
    {
        var sut = link(editor(docA, unpaged(), "a"), editor(docB, unpaged(), "b"));

        assertThat(sut.linkScript(true)).hasValueSatisfying(script -> assertThat(script) //
                .contains("ExternalEditor.viewportSync.link('a', 'b');") //
                .doesNotContain("unlink"));
    }

    @Test
    void thatDisabledSyncUnlinksBothEditors()
    {
        var sut = link(editor(docA, unpaged(), "a"), editor(docB, unpaged(), "b"));

        assertThat(sut.linkScript(false)).hasValueSatisfying(script -> assertThat(script) //
                .contains("ExternalEditor.viewportSync.unlink('a');") //
                .contains("ExternalEditor.viewportSync.unlink('b');") //
                .doesNotContain(".link("));
    }

    @Test
    void thatImpossibleSyncUnlinksBothEditors()
    {
        var sut = link(editor(docA, pagedA(), "a"), editor(docA, unpaged(), "b"));

        assertThat(sut.linkScript(true)).hasValueSatisfying(script -> assertThat(script) //
                .contains("ExternalEditor.viewportSync.unlink('a');") //
                .contains("ExternalEditor.viewportSync.unlink('b');") //
                .doesNotContain(".link("));
    }

    @Test
    void thatAMissingEditorOnlyUnlinksTheOther()
    {
        var sut = link(editor(docA, unpaged(), "a"), null);

        assertThat(sut.linkScript(true)).hasValueSatisfying(script -> assertThat(script) //
                .contains("ExternalEditor.viewportSync.unlink('a');") //
                .doesNotContain(".link("));
    }

    @Test
    void thatNoScriptIsEmittedWithoutRegisteredEditors()
    {
        var sut = link(null, null);

        assertThat(sut.linkScript(true)).isEmpty();
    }

    private static ViewportSyncLink link(DocumentEditor aFirst, DocumentEditor aSecond)
    {
        return new ViewportSyncLink(() -> aFirst, () -> aSecond);
    }

    private static DocumentEditor editor(SourceDocument aDocument, PagingStrategy aPaging)
    {
        return editor(aDocument, aPaging, forUser("annotator"), null);
    }

    private static DocumentEditor editor(SourceDocument aDocument, PagingStrategy aPaging,
            String aClientId)
    {
        return editor(aDocument, aPaging, forUser("annotator"), aClientId);
    }

    private static DocumentEditor editor(SourceDocument aDocument, PagingStrategy aPaging,
            AnnotationSet aDataOwner)
    {
        return editor(aDocument, aPaging, aDataOwner, null);
    }

    private static DocumentEditor editor(SourceDocument aDocument, PagingStrategy aPaging,
            AnnotationSet aDataOwner, String aClientId)
    {
        var state = mock(AnnotatorState.class);
        when(state.getDocument()).thenReturn(aDocument);
        when(state.getDataOwner()).thenReturn(aDataOwner);
        when(state.getPagingStrategy()).thenReturn(aPaging);

        var editor = mock(DocumentEditor.class);
        when(editor.getAnnotatorState()).thenReturn(state);
        when(editor.getViewportSyncClientId()).thenReturn(Optional.ofNullable(aClientId));
        return editor;
    }

    private static PagingStrategy unpaged()
    {
        return new NoPagingStrategy();
    }

    private static PagingStrategy pagedA()
    {
        return mock(PagingA.class);
    }

    private static PagingStrategy pagedB()
    {
        return mock(PagingB.class);
    }

    interface PagingA
        extends PagingStrategy
    {
        // Stands in for one paging strategy class
    }

    interface PagingB
        extends PagingStrategy
    {
        // Stands in for another paging strategy class
    }
}
