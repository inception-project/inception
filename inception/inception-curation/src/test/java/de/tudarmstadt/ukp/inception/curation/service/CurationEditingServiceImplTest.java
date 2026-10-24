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

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.EXCLUSIVE_WRITE_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.NO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet.CURATION_SET;
import static de.tudarmstadt.ukp.inception.curation.service.CurationMergeMode.FILL_ONLY;
import static de.tudarmstadt.ukp.inception.curation.service.CurationMergeMode.LOAD_ONLY;
import static de.tudarmstadt.ukp.inception.curation.service.CurationMergeMode.RECREATE;
import static de.tudarmstadt.ukp.inception.curation.sidebar.CurationSidebarManagerPrefs.KEY_CURATION_SIDEBAR_MANAGER_PREFS;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.apache.uima.fit.factory.CasFactory.createCas;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeStrategy;
import de.tudarmstadt.ukp.inception.curation.sidebar.CurationSidebarManagerPrefs;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;

class CurationEditingServiceImplTest
{
    private static final String CURATION_USER = "CURATION_USER";

    private DocumentService documentService;
    private CurationDocumentService curationDocumentService;
    private CurationMergeService curationMergeService;
    private PreferencesService preferencesService;
    private CurationSidebarManagerPrefs curationPrefs;

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
        preferencesService = mock(PreferencesService.class);

        sut = new CurationEditingServiceImpl(documentService, curationDocumentService,
                curationMergeService, preferencesService);

        project = new Project("project");
        document = new SourceDocument("doc.txt", project, "text");

        curationPrefs = new CurationSidebarManagerPrefs();
        when(preferencesService.loadDefaultTraitsForProject(KEY_CURATION_SIDEBAR_MANAGER_PREFS,
                project)).thenReturn(curationPrefs);
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

    private CAS readCuration(CurationMergeMode aMergeMode, boolean aUpgrade) throws Exception
    {
        return sut.readCurationCas(document, CURATION_USER, emptyMap(), aUpgrade, layers,
                mergeStrategy, aMergeMode);
    }

    private CAS curationCasWithToken() throws Exception
    {
        var cas = createCas();
        cas.setDocumentText("text");
        cas.addFsToIndexes(new Token(cas.getJCas(), 0, 4));
        when(curationDocumentService.readCurationCas(document)).thenReturn(cas);
        return cas;
    }

    private void givenTheCurationCasIsCreated() throws Exception
    {
        doAnswer(call -> {
            call.getArgument(4, Runnable.class).run();
            return null;
        }).when(documentService).readOrCreateAnnotationCas(eq(document), eq(CURATION_SET),
                eq(NO_CAS_UPGRADE), eq(EXCLUSIVE_WRITE_ACCESS), any());
    }

    private void givenCuratableUsers(User... aUsers) throws IOException
    {
        var users = asList(aUsers);
        when(curationDocumentService.isDocumentCuratable(document)).thenReturn(true);
        when(curationDocumentService.listCuratableUsers(document)).thenReturn(users);
        when(documentService.readAllCasesSharedNoUpgrade(document, users)).thenReturn(emptyMap());
    }

    @Nested
    @DisplayName("readOrCreateCurationCas refuses when curation is not possible")
    class Refusal
    {
        @Test
        void thatDocumentInNonCuratableStateIsRefused() throws Exception
        {
            // Everything else would allow curation, so only the state check can refuse
            givenCuratableUsers(user("ann"));
            when(curationDocumentService.existsCurationCas(document)).thenReturn(true);
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
        void thatRecreateReplacesTheContentsOfTheCurationCas() throws Exception
        {
            var curationCas = curationCasWithToken();
            doAnswer(call -> {
                // The merge clears the target and fills it from the annotators
                var target = call.getArgument(2, CAS.class);
                target.reset();
                target.setDocumentText("text");
                target.addFsToIndexes(new NamedEntity(target.getJCas(), 0, 4));
                return null;
            }).when(curationMergeService).mergeCasses(eq(document), eq(CURATION_USER), any(),
                    eq(emptyMap()), eq(mergeStrategy), eq(layers), eq(true));

            // The locked curation CAS is kept - deleting it would release the lock
            assertThat(readCuration(RECREATE, true)).isSameAs(curationCas);

            assertThat(curationCas.select(Token.class).asList()).isEmpty();
            assertThat(curationCas.select(NamedEntity.class).asList()).hasSize(1);
            var order = inOrder(curationDocumentService);
            order.verify(curationDocumentService).upgradeCurationCas(curationCas, document);
            order.verify(curationDocumentService).writeCurationCas(curationCas, document, false);
            verify(curationDocumentService, never()).deleteCurationCas(any());
        }

        @Test
        void thatAFailedRecreateLeavesTheCurationCasAsItWas() throws Exception
        {
            var curationCas = curationCasWithToken();
            doAnswer(call -> {
                call.getArgument(2, CAS.class).reset();
                throw new IllegalStateException("boom");
            }).when(curationMergeService).mergeCasses(any(), any(), any(), any(), any(), any(),
                    anyBoolean());

            assertThatExceptionOfType(IllegalStateException.class) //
                    .isThrownBy(() -> readCuration(RECREATE, false));

            // The curation CAS stays in memory after the request - a half-done merge in it would
            // be saved by the next edit
            assertThat(curationCas.getDocumentText()).isEqualTo("text");
            assertThat(curationCas.select(Token.class).asList()).hasSize(1);
            verify(curationDocumentService, never()).writeCurationCas(any(), any(), anyBoolean());
            verify(curationDocumentService, never()).deleteCurationCas(any());
        }

        @Test
        void thatFillOnlyMergesIntoTheExistingCurationCasWithoutDeletingIt() throws Exception
        {
            var curationCas = mock(CAS.class);
            when(curationDocumentService.readCurationCas(document)).thenReturn(curationCas);

            assertThat(readCuration(FILL_ONLY, true)).isSameAs(curationCas);

            var order = inOrder(curationDocumentService, curationMergeService);
            order.verify(curationDocumentService).upgradeCurationCas(curationCas, document);
            // aClearTargetCas is false - curator decisions already in the CAS are preserved.
            order.verify(curationMergeService).mergeCasses(document, CURATION_USER, curationCas,
                    emptyMap(), mergeStrategy, layers, false);
            order.verify(curationDocumentService).writeCurationCas(curationCas, document, true);

            verify(curationDocumentService, never()).deleteCurationCas(any());
        }

        @Test
        void thatFillOnlySkipsTheUpgradeWhenNotRequested() throws Exception
        {
            var curationCas = mock(CAS.class);
            when(curationDocumentService.readCurationCas(document)).thenReturn(curationCas);

            readCuration(FILL_ONLY, false);

            verify(curationDocumentService, never()).upgradeCurationCas(any(), any());
            // The merge still happens and is still persisted.
            verify(curationMergeService).mergeCasses(document, CURATION_USER, curationCas,
                    emptyMap(), mergeStrategy, layers, false);
            verify(curationDocumentService).writeCurationCas(curationCas, document, true);
        }

        @Test
        void thatLoadOnlyReadsTheCurationCasWithoutMerging() throws Exception
        {
            var curationCas = mock(CAS.class);
            when(curationDocumentService.readCurationCas(document)).thenReturn(curationCas);

            assertThat(readCuration(LOAD_ONLY, true)).isSameAs(curationCas);

            verify(curationDocumentService).upgradeCurationCas(curationCas, document);
            // The upgrade has to be persisted, otherwise it would be redone on every load. But as
            // it does not change the annotations, the write must not announce itself.
            verify(curationDocumentService).writeCurationCasSilently(curationCas, document, true);
            verify(curationDocumentService, never()).writeCurationCas(any(), any(), anyBoolean());
            verifyNoInteractions(curationMergeService);
        }

        @Test
        void thatLoadOnlyWithoutUpgradeDoesNotWriteTheCurationCas() throws Exception
        {
            var curationCas = mock(CAS.class);
            when(curationDocumentService.readCurationCas(document)).thenReturn(curationCas);

            assertThat(readCuration(LOAD_ONLY, false)).isSameAs(curationCas);

            verify(curationDocumentService, never()).upgradeCurationCas(any(), any());
            verify(curationDocumentService, never()).writeCurationCas(any(), any(), anyBoolean());
            verify(curationDocumentService, never()).writeCurationCasSilently(any(), any(),
                    anyBoolean());
            verifyNoInteractions(curationMergeService);
        }

        @ParameterizedTest
        @EnumSource(CurationMergeMode.class)
        void thatAMissingCurationCasIsNotCreated(CurationMergeMode aMergeMode) throws Exception
        {
            when(curationDocumentService.readCurationCas(document))
                    .thenThrow(new FileNotFoundException("missing"));

            assertThatExceptionOfType(FileNotFoundException.class) //
                    .isThrownBy(() -> readCuration(aMergeMode, true));

            verifyNoInteractions(curationMergeService, documentService);
            verify(curationDocumentService, never()).writeCurationCas(any(), any(), anyBoolean());
        }
    }

    @Nested
    @DisplayName("readOrCreateCurationCas merges only into a curation CAS it created")
    class InitialMerge
    {
        @Test
        void thatACreatedCurationCasIsMergedIntoAndCurationIsMarkedInProgress() throws Exception
        {
            givenCuratableUsers(user("ann1"), user("ann2"));
            givenTheCurationCasIsCreated();
            var curationCas = curationCasWithToken();

            // Despite LOAD_ONLY having been requested, a full merge is performed
            assertThat(readOrCreate(LOAD_ONLY)).isSameAs(curationCas);

            verify(curationMergeService).mergeCasses(eq(document), eq(CURATION_USER), any(),
                    eq(emptyMap()), eq(mergeStrategy), eq(layers), eq(true));
            // Writing the merged CAS notifies listeners only if its annotation document exists.
            // Marking curation as in progress must happen only once the CAS is actually there.
            var order = inOrder(documentService, curationDocumentService);
            order.verify(documentService).createOrGetAnnotationDocument(document, CURATION_SET);
            order.verify(curationDocumentService).writeCurationCas(curationCas, document, false);
            order.verify(curationDocumentService).markCurationInProgress(document);
        }

        @Test
        void thatACreatedCurationCasIsNotMergedIntoWhenAutoMergeIsDisabled() throws Exception
        {
            curationPrefs.setAutoMergeCurationSidebar(false);
            givenCuratableUsers(user("ann1"), user("ann2"));
            givenTheCurationCasIsCreated();
            var curationCas = curationCasWithToken();

            assertThat(readOrCreate(LOAD_ONLY)).isSameAs(curationCas);

            // Curation starts from the document without annotations, but it does start
            verifyNoInteractions(curationMergeService);
            assertThat(curationCas.select(Token.class).asList()).hasSize(1);
            verify(curationDocumentService).upgradeCurationCas(curationCas, document);
            verify(curationDocumentService).markCurationInProgress(document);
            verify(curationDocumentService, never()).deleteCurationCas(any());
        }

        @Test
        void thatAnExplicitMergeIntoACreatedCurationCasMergesWhenAutoMergeIsDisabled()
            throws Exception
        {
            // E.g. a re-merge after the curation CAS was deleted in the meantime
            curationPrefs.setAutoMergeCurationSidebar(false);
            givenCuratableUsers(user("ann1"));
            givenTheCurationCasIsCreated();
            curationCasWithToken();

            readOrCreate(RECREATE);

            verify(curationMergeService).mergeCasses(eq(document), eq(CURATION_USER), any(),
                    eq(emptyMap()), eq(mergeStrategy), eq(layers), eq(true));
        }

        @Test
        void thatACurationCasCreatedByAnotherCuratorIsNotMergedInto() throws Exception
        {
            // Another curator started curation between the checks and taking the lock on the
            // curation CAS, so the read finds their curation CAS instead of creating one
            givenCuratableUsers(user("ann1"));
            var curationCas = curationCasWithToken();

            assertThat(readOrCreate(LOAD_ONLY)).isSameAs(curationCas);

            verify(documentService).readOrCreateAnnotationCas(eq(document), eq(CURATION_SET),
                    eq(NO_CAS_UPGRADE), eq(EXCLUSIVE_WRITE_ACCESS), any());
            verifyNoInteractions(curationMergeService);
            assertThat(curationCas.select(Token.class).asList()).hasSize(1);
            verify(curationDocumentService).markCurationInProgress(document);
            verify(documentService, never()).createOrGetAnnotationDocument(
                    any(SourceDocument.class), any(AnnotationSet.class));
        }

        @Test
        void thatAFinishedCurationIsOpenedWithoutBeingUpgradedOrWritten() throws Exception
        {
            // Viewing a finished curation must leave its stored version and backups alone
            givenCuratableUsers(user("ann1"));
            when(curationDocumentService.isCurationFinished(document)).thenReturn(true);
            when(curationDocumentService.existsCurationCas(document)).thenReturn(true);
            var curationCas = curationCasWithToken();

            assertThat(readOrCreate(LOAD_ONLY)).isSameAs(curationCas);

            verify(curationDocumentService, never()).upgradeCurationCas(any(), any());
            verify(curationDocumentService, never()).writeCurationCasSilently(any(), any(),
                    anyBoolean());
            verify(curationDocumentService, never()).writeCurationCas(any(), any(), anyBoolean());
            verify(documentService, never()).readAllCasesSharedNoUpgrade(any(SourceDocument.class),
                    anyCollection());
            verify(curationDocumentService).markCurationInProgress(document);
        }

        @Test
        void thatTheCreatedCurationCasIsRemovedWhenTheMergeFails() throws Exception
        {
            givenCuratableUsers(user("ann1"));
            givenTheCurationCasIsCreated();
            curationCasWithToken();
            doThrow(new IllegalStateException("boom")).when(curationMergeService).mergeCasses(any(),
                    any(), any(), any(), any(), any(), anyBoolean());

            assertThatExceptionOfType(IllegalStateException.class) //
                    .isThrownBy(() -> readOrCreate(LOAD_ONLY));

            verify(curationDocumentService).deleteCurationCas(document);
            verify(curationDocumentService, never()).markCurationInProgress(any());
        }

        @Test
        void thatAnExistingCurationCasIsNotRemovedWhenTheMergeFails() throws Exception
        {
            givenCuratableUsers(user("ann1"));
            curationCasWithToken();
            doThrow(new IllegalStateException("boom")).when(curationMergeService).mergeCasses(any(),
                    any(), any(), any(), any(), any(), anyBoolean());

            assertThatExceptionOfType(IllegalStateException.class) //
                    .isThrownBy(() -> readOrCreate(RECREATE));

            verify(curationDocumentService, never()).deleteCurationCas(any());
            verify(curationDocumentService, never()).markCurationInProgress(any());
        }

        @Test
        void thatACurationCasCreatedWithoutAnybodyToMergeFromIsRemoved() throws Exception
        {
            // The curation CAS existed during the checks, but was deleted (e.g. reset) before the
            // lock was taken
            when(curationDocumentService.isDocumentCuratable(document)).thenReturn(true);
            when(curationDocumentService.listCuratableUsers(document)).thenReturn(emptyList());
            when(curationDocumentService.existsCurationCas(document)).thenReturn(true);
            givenTheCurationCasIsCreated();

            assertThatExceptionOfType(DocumentNotCuratableException.class) //
                    .isThrownBy(() -> readOrCreate(LOAD_ONLY));

            verify(curationDocumentService).deleteCurationCas(document);
            verify(curationDocumentService, never()).markCurationInProgress(any());
            verifyNoInteractions(curationMergeService);
        }
    }

    @Nested
    @DisplayName("readOrCreateCurationCas with a data owner selection")
    class DataOwnerSelection
    {
        private CAS readOrCreate(CurationMergeMode aMergeMode, AnnotationSet... aDataOwners)
            throws Exception
        {
            return sut.readOrCreateCurationCas(document, CURATION_USER, layers, mergeStrategy,
                    aMergeMode, asList(aDataOwners));
        }

        @Test
        void thatOnlySelectedDataOwnersAreMerged() throws Exception
        {
            var ann1 = user("ann1");
            var ann2 = user("ann2");
            when(curationDocumentService.isDocumentCuratable(document)).thenReturn(true);
            when(curationDocumentService.listCuratableUsers(document))
                    .thenReturn(asList(ann1, ann2));
            when(curationDocumentService.existsCurationCas(document)).thenReturn(true);
            var curationCas = curationCasWithToken();
            Map<String, CAS> ann2Casses = Map.of("ann2", createCas());
            when(documentService.readAllCasesSharedNoUpgrade(document, asList(ann2)))
                    .thenReturn(ann2Casses);

            assertThat(readOrCreate(RECREATE, AnnotationSet.forUser("ann2"))).isSameAs(curationCas);

            verify(documentService, never()).readAllCasesSharedNoUpgrade(document,
                    asList(ann1, ann2));
            verify(curationMergeService).mergeCasses(eq(document), eq(CURATION_USER), any(),
                    eq(ann2Casses), eq(mergeStrategy), eq(layers), eq(true));
        }

        @Test
        @DisplayName("RECREATE with nobody selected must not discard the curator's work")
        void thatRecreateWithEmptySelectionIsRefused() throws Exception
        {
            when(curationDocumentService.isDocumentCuratable(document)).thenReturn(true);
            when(curationDocumentService.listCuratableUsers(document))
                    .thenReturn(asList(user("ann1")));
            when(curationDocumentService.existsCurationCas(document)).thenReturn(true);

            assertThatExceptionOfType(CurationNotPossibleException.class) //
                    .isThrownBy(() -> readOrCreate(RECREATE)) //
                    .withMessageContaining("none of the annotators to merge from");

            verify(curationDocumentService, never()).deleteCurationCas(any());
            verifyNoInteractions(curationMergeService);
        }

        @Test
        @DisplayName("FILL_ONLY with nobody to merge from must not report a merge")
        void thatFillOnlyWithEmptySelectionIsRefused() throws Exception
        {
            when(curationDocumentService.isDocumentCuratable(document)).thenReturn(true);
            when(curationDocumentService.listCuratableUsers(document))
                    .thenReturn(asList(user("ann1")));
            when(curationDocumentService.existsCurationCas(document)).thenReturn(true);

            // ann2 was selected in the dialog but is no longer curatable
            assertThatExceptionOfType(CurationNotPossibleException.class) //
                    .isThrownBy(() -> readOrCreate(FILL_ONLY, AnnotationSet.forUser("ann2"))) //
                    .withMessageContaining("none of the annotators to merge from");

            verify(curationDocumentService, never()).markCurationInProgress(any());
            verifyNoInteractions(curationMergeService);
        }

        @Test
        @DisplayName("An empty selection does not make the document uncuratable")
        void thatLoadOnlyWithEmptySelectionOpensTheCurationCas() throws Exception
        {
            when(curationDocumentService.isDocumentCuratable(document)).thenReturn(true);
            when(curationDocumentService.listCuratableUsers(document))
                    .thenReturn(asList(user("ann1")));
            when(curationDocumentService.existsCurationCas(document)).thenReturn(true);
            var curationCas = mock(CAS.class);
            when(curationDocumentService.readCurationCas(document)).thenReturn(curationCas);

            assertThat(readOrCreate(LOAD_ONLY)).isSameAs(curationCas);

            verify(curationDocumentService).markCurationInProgress(document);
        }
    }

    @Nested
    class IsOpenableForCuration
    {
        @Test
        void thatNonCuratableDocumentIsNotOpenable() throws Exception
        {
            // Everything else would make it openable, so only the state check can refuse
            when(curationDocumentService.listCuratableUsers(document))
                    .thenReturn(asList(user("ann1")));
            when(curationDocumentService.existsCurationCas(document)).thenReturn(true);
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
