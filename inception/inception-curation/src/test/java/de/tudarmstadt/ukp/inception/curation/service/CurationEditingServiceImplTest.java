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
package de.tudarmstadt.ukp.inception.curation.service;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.UNMANAGED_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.FORCE_CAS_UPGRADE;
import static de.tudarmstadt.ukp.inception.curation.service.CurationMergeMode.FILL_ONLY;
import static de.tudarmstadt.ukp.inception.curation.service.CurationMergeMode.LOAD_ONLY;
import static de.tudarmstadt.ukp.inception.curation.service.CurationMergeMode.RECREATE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeStrategy;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;

class CurationEditingServiceImplTest
{
    private static final String CURATION_USER = "CURATION_USER";

    private DocumentService documentService;
    private CurationDocumentService curationDocumentService;
    private CurationMergeService curationMergeService;

    private CurationEditingServiceImpl sut;

    private Project project;
    private SourceDocument document;
    private List<AnnotationLayer> layers;
    private MergeStrategy mergeStrategy;

    @BeforeEach
    void setup()
    {
        documentService = mock(DocumentService.class);
        curationDocumentService = mock(CurationDocumentService.class);
        curationMergeService = mock(CurationMergeService.class);

        sut = new CurationEditingServiceImpl(documentService, curationDocumentService,
                curationMergeService);

        project = new Project("project");
        document = new SourceDocument("doc.txt", project, "text");
        layers = emptyList();
        mergeStrategy = mock(MergeStrategy.class);
    }

    private User user(String aUsername)
    {
        return new User(aUsername);
    }

    private CAS readOrCreate(CurationMergeMode aMergeMode) throws Exception
    {
        return sut.readOrCreateCurationCas(document, CURATION_USER, layers, mergeStrategy,
                aMergeMode);
    }

    private CAS readCuration(CurationMergeMode aMergeMode, String aTemplateUser, boolean aUpgrade)
        throws Exception
    {
        return sut.readCurationCas(document, CURATION_USER, emptyMap(), aTemplateUser, aUpgrade,
                layers, mergeStrategy, aMergeMode);
    }

    @Nested
    @DisplayName("readOrCreateCurationCas refuses when curation is not possible")
    class Refusal
    {
        @Test
        void thatDocumentInNonCuratableStateIsRefused() throws Exception
        {
            when(curationDocumentService.isDocumentCuratable(document)).thenReturn(false);

            assertThatExceptionOfType(DocumentNotCuratableException.class) //
                    .isThrownBy(() -> readOrCreate(LOAD_ONLY)) //
                    .satisfies(e -> assertThat(e.getDocument()).isSameAs(document));

            verify(curationDocumentService, never()).markCurationInProgress(any());
            verifyNoInteractions(curationMergeService);
        }

        @Test
        void thatDocumentWithNeitherAnnotatorsNorCurationCasIsRefused() throws Exception
        {
            when(curationDocumentService.isDocumentCuratable(document)).thenReturn(true);
            when(curationDocumentService.listCuratableUsers(document)).thenReturn(emptyList());
            when(curationDocumentService.existsCurationCas(document)).thenReturn(false);

            assertThatExceptionOfType(DocumentNotCuratableException.class) //
                    .isThrownBy(() -> readOrCreate(LOAD_ONLY));

            verify(curationDocumentService, never()).markCurationInProgress(any());
            verifyNoInteractions(curationMergeService);
        }

        @Test
        @DisplayName("RECREATE without curatable users must not discard the curator's work")
        void thatRecreateWithoutCuratableUsersIsRefused() throws Exception
        {
            when(curationDocumentService.isDocumentCuratable(document)).thenReturn(true);
            when(curationDocumentService.listCuratableUsers(document)).thenReturn(emptyList());
            // A curation CAS exists - so this is not the "nothing to curate" case above, but the
            // more dangerous one where re-merging would destroy existing curation results.
            when(curationDocumentService.existsCurationCas(document)).thenReturn(true);

            assertThatExceptionOfType(CurationNotPossibleException.class) //
                    .isThrownBy(() -> readOrCreate(RECREATE)) //
                    .satisfies(e -> assertThat(e)
                            .isExactlyInstanceOf(CurationNotPossibleException.class));

            verify(curationDocumentService, never()).deleteCurationCas(any());
            verify(curationDocumentService, never()).markCurationInProgress(any());
            verifyNoInteractions(curationMergeService);
        }

        @Test
        void thatLoadOnlyWithoutCuratableUsersButWithCurationCasIsAllowed() throws Exception
        {
            when(curationDocumentService.isDocumentCuratable(document)).thenReturn(true);
            when(curationDocumentService.listCuratableUsers(document)).thenReturn(emptyList());
            when(curationDocumentService.existsCurationCas(document)).thenReturn(true);
            var curationCas = mock(CAS.class);
            when(curationDocumentService.readCurationCas(document)).thenReturn(curationCas);

            assertThat(readOrCreate(LOAD_ONLY)).isSameAs(curationCas);

            verify(curationDocumentService).markCurationInProgress(document);
            verifyNoInteractions(curationMergeService);
        }
    }

    @Nested
    @DisplayName("readCurationCas honours the merge mode")
    class MergeModes
    {
        @Test
        void thatRecreateMergesIntoATemplateCasAndReplacesTheCurationCas() throws Exception
        {
            when(curationDocumentService.existsCurationCas(document)).thenReturn(true);
            var templateCas = mock(CAS.class);
            when(documentService.readAnnotationCas(any(SourceDocument.class),
                    any(AnnotationSet.class), any(), any())).thenReturn(templateCas);

            assertThat(readCuration(RECREATE, "ann1", true)).isSameAs(templateCas);

            verify(documentService).readAnnotationCas(document, AnnotationSet.forUser("ann1"),
                    FORCE_CAS_UPGRADE, UNMANAGED_ACCESS);

            // The delete must happen only after the merge succeeded, so that a failing merge
            // leaves the existing curation CAS intact.
            var order = inOrder(curationMergeService, curationDocumentService);
            order.verify(curationMergeService).mergeCasses(document, CURATION_USER, templateCas,
                    emptyMap(), mergeStrategy, layers, true);
            order.verify(curationDocumentService).deleteCurationCas(document);
            order.verify(curationDocumentService).writeCurationCas(templateCas, document, false);

            // RECREATE builds a fresh CAS from the template - the old one is never read.
            verify(curationDocumentService, never()).readCurationCas(any());
        }

        @Test
        void thatFillOnlyMergesIntoTheExistingCurationCasWithoutDeletingIt() throws Exception
        {
            when(curationDocumentService.existsCurationCas(document)).thenReturn(true);
            var curationCas = mock(CAS.class);
            when(curationDocumentService.readCurationCas(document)).thenReturn(curationCas);

            assertThat(readCuration(FILL_ONLY, "ann1", true)).isSameAs(curationCas);

            var order = inOrder(curationDocumentService, curationMergeService);
            order.verify(curationDocumentService).upgradeCurationCas(curationCas, document);
            // aClearTargetCas is false - curator decisions already in the CAS are preserved.
            order.verify(curationMergeService).mergeCasses(document, CURATION_USER, curationCas,
                    emptyMap(), mergeStrategy, layers, false);
            order.verify(curationDocumentService).writeCurationCas(curationCas, document, true);

            verify(curationDocumentService, never()).deleteCurationCas(any());
            verify(documentService, never()).readAnnotationCas(any(SourceDocument.class),
                    any(AnnotationSet.class), any(), any());
        }

        @Test
        void thatFillOnlySkipsTheUpgradeWhenNotRequested() throws Exception
        {
            when(curationDocumentService.existsCurationCas(document)).thenReturn(true);
            var curationCas = mock(CAS.class);
            when(curationDocumentService.readCurationCas(document)).thenReturn(curationCas);

            readCuration(FILL_ONLY, "ann1", false);

            verify(curationDocumentService, never()).upgradeCurationCas(any(), any());
            // The merge still happens and is still persisted.
            verify(curationMergeService).mergeCasses(document, CURATION_USER, curationCas,
                    emptyMap(), mergeStrategy, layers, false);
            verify(curationDocumentService).writeCurationCas(curationCas, document, true);
        }

        @Test
        void thatLoadOnlyReadsTheCurationCasWithoutMerging() throws Exception
        {
            when(curationDocumentService.existsCurationCas(document)).thenReturn(true);
            var curationCas = mock(CAS.class);
            when(curationDocumentService.readCurationCas(document)).thenReturn(curationCas);

            assertThat(readCuration(LOAD_ONLY, "ann1", true)).isSameAs(curationCas);

            verify(curationDocumentService).upgradeCurationCas(curationCas, document);
            // The upgrade has to be persisted, otherwise it would be redone on every load.
            verify(curationDocumentService).writeCurationCas(curationCas, document, true);
            verifyNoInteractions(curationMergeService);
        }

        @Test
        void thatLoadOnlyWithoutUpgradeDoesNotWriteTheCurationCas() throws Exception
        {
            when(curationDocumentService.existsCurationCas(document)).thenReturn(true);
            var curationCas = mock(CAS.class);
            when(curationDocumentService.readCurationCas(document)).thenReturn(curationCas);

            assertThat(readCuration(LOAD_ONLY, "ann1", false)).isSameAs(curationCas);

            verify(curationDocumentService, never()).upgradeCurationCas(any(), any());
            verify(curationDocumentService, never()).writeCurationCas(any(), any(), anyBoolean());
            verifyNoInteractions(curationMergeService);
        }
    }

    @Nested
    @DisplayName("A missing curation CAS forces a full merge")
    class MissingCurationCas
    {
        @Test
        void thatLoadOnlyIsPromotedToRecreateWhenNoCurationCasExists() throws Exception
        {
            when(curationDocumentService.existsCurationCas(document)).thenReturn(false);
            var templateCas = mock(CAS.class);
            when(documentService.readAnnotationCas(any(SourceDocument.class),
                    any(AnnotationSet.class), any(), any())).thenReturn(templateCas);

            assertThat(readCuration(LOAD_ONLY, "ann1", true)).isSameAs(templateCas);

            // Despite LOAD_ONLY having been requested, a full merge is performed.
            verify(curationMergeService).mergeCasses(document, CURATION_USER, templateCas,
                    emptyMap(), mergeStrategy, layers, true);
            verify(curationDocumentService).writeCurationCas(templateCas, document, false);
            verify(curationDocumentService, never()).readCurationCas(any());
        }

        @Test
        void thatFillOnlyIsPromotedToRecreateWhenNoCurationCasExists() throws Exception
        {
            when(curationDocumentService.existsCurationCas(document)).thenReturn(false);
            var templateCas = mock(CAS.class);
            when(documentService.readAnnotationCas(any(SourceDocument.class),
                    any(AnnotationSet.class), any(), any())).thenReturn(templateCas);

            assertThat(readCuration(FILL_ONLY, "ann1", true)).isSameAs(templateCas);

            verify(curationMergeService).mergeCasses(document, CURATION_USER, templateCas,
                    emptyMap(), mergeStrategy, layers, true);
            verify(curationDocumentService, never()).readCurationCas(any());
        }

        @Test
        void thatRecreateWithoutATemplateUserFails() throws Exception
        {
            when(curationDocumentService.existsCurationCas(document)).thenReturn(false);

            assertThatExceptionOfType(IllegalStateException.class) //
                    .isThrownBy(() -> readCuration(LOAD_ONLY, null, true)) //
                    .withMessageContaining("without a template annotation document");

            verifyNoInteractions(curationMergeService);
        }
    }

    @Nested
    @DisplayName("readOrCreateCurationCas wiring")
    class Wiring
    {
        @Test
        void thatTheFirstCuratableUserIsUsedAsTemplateAndCurationIsMarkedInProgress()
            throws Exception
        {
            var users = asList(user("ann1"), user("ann2"));
            when(curationDocumentService.isDocumentCuratable(document)).thenReturn(true);
            when(curationDocumentService.listCuratableUsers(document)).thenReturn(users);
            when(curationDocumentService.existsCurationCas(document)).thenReturn(false);
            when(documentService.readAllCasesSharedNoUpgrade(document, users))
                    .thenReturn(emptyMap());
            var templateCas = mock(CAS.class);
            when(documentService.readAnnotationCas(any(SourceDocument.class),
                    any(AnnotationSet.class), any(), any())).thenReturn(templateCas);

            assertThat(readOrCreate(RECREATE)).isSameAs(templateCas);

            verify(documentService).readAnnotationCas(document, AnnotationSet.forUser("ann1"),
                    FORCE_CAS_UPGRADE, UNMANAGED_ACCESS);
            verify(documentService).readAllCasesSharedNoUpgrade(document, users);

            // Marking curation as in progress must happen only once the CAS is actually there.
            var order = inOrder(curationDocumentService);
            order.verify(curationDocumentService).writeCurationCas(eq(templateCas), eq(document),
                    anyBoolean());
            order.verify(curationDocumentService).markCurationInProgress(document);
        }
    }

    @Nested
    class IsOpenableForCuration
    {
        @Test
        void thatNonCuratableDocumentIsNotOpenable()
        {
            when(curationDocumentService.isDocumentCuratable(document)).thenReturn(false);

            assertThat(sut.isOpenableForCuration(document)).isFalse();
        }

        @Test
        void thatDocumentWithCuratableUsersIsOpenable()
        {
            when(curationDocumentService.isDocumentCuratable(document)).thenReturn(true);
            when(curationDocumentService.listCuratableUsers(document))
                    .thenReturn(asList(user("ann1")));

            assertThat(sut.isOpenableForCuration(document)).isTrue();
        }

        @Test
        void thatDocumentWithOnlyACurationCasIsOpenable() throws Exception
        {
            when(curationDocumentService.isDocumentCuratable(document)).thenReturn(true);
            when(curationDocumentService.listCuratableUsers(document)).thenReturn(emptyList());
            when(curationDocumentService.existsCurationCas(document)).thenReturn(true);

            assertThat(sut.isOpenableForCuration(document)).isTrue();
        }

        @Test
        void thatDocumentWithNeitherIsNotOpenable() throws Exception
        {
            when(curationDocumentService.isDocumentCuratable(document)).thenReturn(true);
            when(curationDocumentService.listCuratableUsers(document)).thenReturn(emptyList());
            when(curationDocumentService.existsCurationCas(document)).thenReturn(false);

            assertThat(sut.isOpenableForCuration(document)).isFalse();
        }

        @Test
        @DisplayName("An I/O error fails open - we assume the curation CAS exists")
        void thatIoErrorIsTreatedAsOpenable() throws Exception
        {
            when(curationDocumentService.isDocumentCuratable(document)).thenReturn(true);
            when(curationDocumentService.listCuratableUsers(document)).thenReturn(emptyList());
            when(curationDocumentService.existsCurationCas(document))
                    .thenThrow(new IOException("cannot read"));

            assertThat(sut.isOpenableForCuration(document)).isTrue();
        }
    }

    @Nested
    class FindNextCuratableDocument
    {
        private SourceDocument doc1;
        private SourceDocument doc2;
        private SourceDocument doc3;
        private List<SourceDocument> documents;

        @BeforeEach
        void setup()
        {
            doc1 = new SourceDocument("doc1.txt", project, "text");
            doc2 = new SourceDocument("doc2.txt", project, "text");
            doc3 = new SourceDocument("doc3.txt", project, "text");
            documents = asList(doc1, doc2, doc3);
        }

        private void openable(SourceDocument aDocument, boolean aOpenable)
        {
            when(curationDocumentService.isDocumentCuratable(aDocument)).thenReturn(aOpenable);
            if (aOpenable) {
                when(curationDocumentService.listCuratableUsers(aDocument))
                        .thenReturn(asList(user("ann1")));
            }
        }

        @Test
        void thatTheNextOpenableDocumentIsReturned()
        {
            openable(doc2, true);

            assertThat(sut.findNextCuratableDocument(documents, doc1)).contains(doc2);
        }

        @Test
        void thatNonOpenableDocumentsAreSkipped()
        {
            openable(doc2, false);
            openable(doc3, true);

            assertThat(sut.findNextCuratableDocument(documents, doc1)).contains(doc3);
        }

        @Test
        void thatSearchStartsStrictlyAfterTheGivenDocument()
        {
            openable(doc1, true);
            openable(doc2, false);
            openable(doc3, false);

            // doc1 itself is openable, but must not be returned as its own successor.
            assertThat(sut.findNextCuratableDocument(documents, doc1)).isEmpty();
        }

        @Test
        void thatTheLastDocumentHasNoSuccessor()
        {
            assertThat(sut.findNextCuratableDocument(documents, doc3)).isEmpty();
        }

        @Test
        void thatAnUnknownDocumentYieldsNoSuccessor()
        {
            var unknown = new SourceDocument("other.txt", project, "text");

            assertThat(sut.findNextCuratableDocument(documents, unknown)).isEmpty();
            verifyNoInteractions(curationDocumentService);
        }

        @Test
        void thatNoOpenableDocumentYieldsEmpty()
        {
            openable(doc2, false);
            openable(doc3, false);

            assertThat(sut.findNextCuratableDocument(documents, doc1)).isEmpty();
        }
    }
}
