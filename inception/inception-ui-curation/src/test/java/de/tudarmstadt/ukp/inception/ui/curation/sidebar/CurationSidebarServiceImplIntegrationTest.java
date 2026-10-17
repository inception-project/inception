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
package de.tudarmstadt.ukp.inception.ui.curation.sidebar;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.EXCLUSIVE_WRITE_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.FORCE_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.CHARACTERS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet.CURATION_SET;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.inception.curation.sidebar.CurationSidebarManagerPrefs.KEY_CURATION_SIDEBAR_MANAGER_PREFS;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.runAsync;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.NotEditableException;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.constraints.config.ConstraintsServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.config.CasDiffAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanLayerSupportImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.curation.SpanDiffSupport;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.curation.api.CurationSessionService;
import de.tudarmstadt.ukp.inception.curation.config.CurationDocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.curation.config.CurationServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeIncompleteStrategyFactory;
import de.tudarmstadt.ukp.inception.curation.service.CurationDocumentService;
import de.tudarmstadt.ukp.inception.curation.service.CurationMergeService;
import de.tudarmstadt.ukp.inception.curation.service.CurationService;
import de.tudarmstadt.ukp.inception.curation.service.DocumentNotCuratableException;
import de.tudarmstadt.ukp.inception.curation.sidebar.CurationSidebarManagerPrefs;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.config.DocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.event.DocumentStateChangedEvent;
import de.tudarmstadt.ukp.inception.editor.state.AnnotatorStateImpl;
import de.tudarmstadt.ukp.inception.log.config.EventLoggingAutoConfiguration;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.preferences.config.PreferencesServiceAutoConfig;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.LayerBehaviorRegistry;
import de.tudarmstadt.ukp.inception.schema.config.AnnotationSchemaServiceAutoConfiguration;

/**
 * Starting curation with real CAS storage, a real database and a real merge: whether the curation
 * CAS exists afterwards, what it contains and which state the document is stored in. Runs without a
 * test-managed transaction, so that the transaction of the service under test commits or rolls back
 * as it does in the application.
 */
@EnableAutoConfiguration
@DataJpaTest(showSql = false, //
        properties = { //
                "spring.main.banner-mode=off" })
@Transactional(propagation = Propagation.NEVER)
@EntityScan({ //
        "de.tudarmstadt.ukp.clarin.webanno.model", //
        "de.tudarmstadt.ukp.clarin.webanno.security.model", //
        "de.tudarmstadt.ukp.inception.preferences.model", //
        "de.tudarmstadt.ukp.inception.curation.model" })
@Import({ //
        EventLoggingAutoConfiguration.class, //
        ConstraintsServiceAutoConfiguration.class, //
        DocumentServiceAutoConfiguration.class, //
        ProjectServiceAutoConfiguration.class, //
        CasStorageServiceAutoConfiguration.class, //
        RepositoryAutoConfiguration.class, //
        AnnotationSchemaServiceAutoConfiguration.class, //
        SecurityAutoConfiguration.class, //
        PreferencesServiceAutoConfig.class, //
        CasDiffAutoConfiguration.class, //
        CurationServiceAutoConfiguration.class, //
        CurationDocumentServiceAutoConfiguration.class })
class CurationSidebarServiceImplIntegrationTest
{
    private static final String TEXT = "This is a test.";
    private static final String SPAN_LAYER = "custom.Span";

    static @TempDir Path tempFolder;

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry)
    {
        registry.add("repository.path", () -> tempFolder.toAbsolutePath().toString());
    }

    private @Autowired ProjectService projectService;
    private @Autowired DocumentService documentService;
    private @Autowired AnnotationSchemaService schemaService;
    private @Autowired UserDao userService;
    private @Autowired CurationService curationService;
    private @Autowired CurationSessionService curationSessionService;
    private @Autowired PreferencesService preferencesService;
    private @Autowired CurationSidebarService sut;
    private @Autowired FailingStateChange failingStateChange;

    // Only stubbed to let another curator start curation at a given point
    private @MockitoSpyBean CurationDocumentService curationDocumentService;

    private Project project;
    private SourceDocument document;
    private CasStorageSession casStorageSession;

    @BeforeEach
    void setup() throws Exception
    {
        // The database is not rolled back between the tests, so every test gets its own project
        project = new Project("project-" + System.nanoTime());
        projectService.createProject(project);

        schemaService.createOrUpdateLayer(AnnotationLayer.builder() //
                .withProject(project) //
                .withName(SPAN_LAYER) //
                .withUiName("Span") //
                .withType(SpanLayerSupport.TYPE) //
                .withAnchoringMode(CHARACTERS) //
                .build());

        document = documentService.createSourceDocument(new SourceDocument("doc", project, "text"));
        documentService.setSourceDocumentState(document, ANNOTATION_FINISHED);

        // Like a request: the lock on the curation CAS is held until the session is closed
        casStorageSession = CasStorageSession.open();
    }

    @AfterEach
    void tearDown()
    {
        failingStateChange.enabled = false;
        endRequest();
    }

    private void endRequest()
    {
        if (casStorageSession != null) {
            casStorageSession.close();
            casStorageSession = null;
        }
    }

    @Test
    void thatMergeIntoFinishedCurationIsRejected() throws Exception
    {
        givenAnnotatorFinished("ann1", "This");
        documentService.setSourceDocumentState(document, CURATION_FINISHED);
        var workflow = curationService.readOrCreateCurationWorkflow(project);

        assertThatExceptionOfType(NotEditableException.class) //
                .isThrownBy(() -> sut.merge(curationOf(document), workflow,
                        List.of(AnnotationSet.forUser("ann1")), true));

        endRequest();
        assertThat(curationCasExists()).isFalse();
        assertThat(storedState()).isEqualTo(CURATION_FINISHED);
    }

    @Test
    void thatNoInitialMergeIsPerformedForAnotherDataOwner() throws Exception
    {
        givenAutoMerge(true);
        givenAnnotatorFinished("ann1", "This");
        var state = curationOf(document);
        state.setUser(new User("anna"));

        assertThat(sut.performInitialMergeIfRequired(state)).isEmpty();

        endRequest();
        assertThat(curationCasExists()).isFalse();
        assertThat(storedState()).isEqualTo(ANNOTATION_FINISHED);
    }

    @Test
    void thatNoInitialMergeIsPerformedWhenNotRequired() throws Exception
    {
        // A finished curation whose curation CAS is missing must not get an empty or a merged
        // one, which would also rotate out the backups of the lost one
        givenAutoMerge(true);
        givenAnnotatorFinished("ann1", "This");
        documentService.setSourceDocumentState(document, CURATION_FINISHED);

        assertThat(sut.performInitialMergeIfRequired(curationOf(document))).isEmpty();

        endRequest();
        assertThat(curationCasExists()).isFalse();
        assertThat(storedState()).isEqualTo(CURATION_FINISHED);
    }

    @Test
    void thatStartingCurationWithoutAutoMergeCreatesTheCurationCasAndStartsCuration()
        throws Exception
    {
        givenAutoMerge(false);
        givenAnnotatorFinished("ann1", "This");

        assertThat(sut.performInitialMergeIfRequired(curationOf(document))).isEmpty();

        endRequest();
        assertThat(curationCasExists()).isTrue();
        assertThat(curatedSpans()).isEmpty();
        assertThat(storedState()).isEqualTo(CURATION_IN_PROGRESS);
    }

    @Test
    void thatNoCurationCasIsLeftBehindWhenStartingCurationWithoutAutoMergeFails() throws Exception
    {
        givenAutoMerge(false);
        failingStateChange.enabled = true;

        assertThatExceptionOfType(IllegalStateException.class) //
                .isThrownBy(() -> sut.performInitialMergeIfRequired(curationOf(document))) //
                .withMessage(FailingStateChange.MESSAGE);

        endRequest();
        // An existing curation CAS is never merged into, so one left behind here would make the
        // next opening skip the initial merge once auto-merge is enabled
        assertThat(curationCasExists()).isFalse();
        assertThat(storedState()).isEqualTo(ANNOTATION_FINISHED);
        assertThat(initialMergeRequired()) //
                .as("The next opening performs the initial merge") //
                .isTrue();
    }

    @Test
    void thatACurationCasCreatedByAnotherCuratorIsLeftAloneWithAutoMergeDisabled() throws Exception
    {
        givenAutoMerge(false);
        givenAnnotatorFinished("ann1", "This");
        givenAnotherCuratorStartsCurationAfterTheCheck("is");

        assertThat(sut.performInitialMergeIfRequired(curationOf(document))).isEmpty();

        endRequest();
        assertThat(curatedSpans()).containsExactly("is");
        assertThat(storedState()).isEqualTo(CURATION_IN_PROGRESS);
    }

    @Test
    void thatInitialMergeIsPerformedWhenRequired() throws Exception
    {
        givenAutoMerge(true);
        givenMergeStrategy(MergeIncompleteStrategyFactory.BEAN_NAME);
        givenAnnotatorFinished("ann1", "This");
        givenAnnotatorFinished("ann2", "test");
        // The first merge always uses all curatable annotators, not the curator's selection in
        // the curation sidebar
        curationSessionService.setSelectedDataOwners("curator", project.getId(),
                List.of(AnnotationSet.forUser("ann1"), AnnotationSet.forUser("ann2")),
                List.of(AnnotationSet.forUser("ann1")));

        var factory = sut.performInitialMergeIfRequired(curationOf(document));

        assertThat(factory).hasValueSatisfying(
                f -> assertThat(f.getId()).isEqualTo(MergeIncompleteStrategyFactory.BEAN_NAME));
        endRequest();
        // The curation CAS is created from the initial CAS, which does not know the project's
        // layers - merging into it only works because it is upgraded first
        assertThat(curatedSpans()).containsExactlyInAnyOrder("This", "test");
        assertThat(storedState()).isEqualTo(CURATION_IN_PROGRESS);
    }

    @Test
    void thatNoInitialMergeIsPerformedWhenAnotherCuratorCreatedTheCurationCas() throws Exception
    {
        givenAutoMerge(true);
        givenAnnotatorFinished("ann1", "This");
        givenAnotherCuratorStartsCurationAfterTheCheck("is");

        assertThat(sut.performInitialMergeIfRequired(curationOf(document))).isEmpty();

        endRequest();
        // Neither merged over nor deleted
        assertThat(curatedSpans()).containsExactly("is");
        assertThat(storedState()).isEqualTo(CURATION_IN_PROGRESS);
    }

    @Test
    void thatTheCurationCasIsDeletedWhenTheInitialMergeFails() throws Exception
    {
        givenAutoMerge(true);
        givenAnnotatorFinished("ann1", "This");
        // Fails after the merged curation CAS has been written
        failingStateChange.enabled = true;

        assertThatExceptionOfType(IllegalStateException.class) //
                .isThrownBy(() -> sut.performInitialMergeIfRequired(curationOf(document))) //
                .withMessage(FailingStateChange.MESSAGE);

        endRequest();
        assertThat(curationCasExists()).isFalse();
        assertThat(storedState()).isEqualTo(ANNOTATION_FINISHED);
        assertThat(initialMergeRequired()) //
                .as("The next opening performs the initial merge") //
                .isTrue();
    }

    @Test
    void thatTheCurationCasIsDeletedWhenThereIsNobodyToMergeFrom() throws Exception
    {
        // E.g. all annotators were removed from the project after the document became curatable
        givenAutoMerge(true);

        assertThatExceptionOfType(DocumentNotCuratableException.class) //
                .isThrownBy(() -> sut.performInitialMergeIfRequired(curationOf(document)));

        endRequest();
        // A curation CAS merged from nobody would look curated and never be merged again
        assertThat(curationCasExists()).isFalse();
        assertThat(storedState()).isEqualTo(ANNOTATION_FINISHED);
    }

    private AnnotatorState curationOf(SourceDocument aDocument)
    {
        var state = new AnnotatorStateImpl(Mode.CURATION);
        state.setProject(project);
        state.setDocument(aDocument, emptyList());
        state.setUser(new User(CURATION_USER));
        state.setAnnotationLayers(schemaService.listAnnotationLayer(project));
        return state;
    }

    private void givenAutoMerge(boolean aAutoMerge)
    {
        var prefs = new CurationSidebarManagerPrefs();
        prefs.setAutoMergeCurationSidebar(aAutoMerge);
        preferencesService.saveDefaultTraitsForProject(KEY_CURATION_SIDEBAR_MANAGER_PREFS, project,
                prefs);
    }

    private void givenMergeStrategy(String aMergeStrategy)
    {
        var workflow = curationService.readOrCreateCurationWorkflow(project);
        workflow.setMergeStrategy(aMergeStrategy);
        curationService.createOrUpdateCurationWorkflow(workflow);
    }

    private void givenAnnotatorFinished(String aUsername, String aCoveredText) throws Exception
    {
        if (!userService.exists(aUsername)) {
            userService.create(new User(aUsername));
        }

        var annDoc = documentService.createOrGetAnnotationDocument(document,
                AnnotationSet.forUser(aUsername));
        var cas = documentService.readAnnotationCas(annDoc, FORCE_CAS_UPGRADE,
                EXCLUSIVE_WRITE_ACCESS);
        addSpan(cas, aCoveredText);
        documentService.writeAnnotationCas(cas, annDoc);
        documentService.setAnnotationDocumentState(annDoc, FINISHED);
    }

    /**
     * Another curator starts curation between the check whether an initial merge is required and
     * taking the lock on the curation CAS - in their own request.
     */
    private void givenAnotherCuratorStartsCurationAfterTheCheck(String aCuratedText)
    {
        doAnswer(call -> {
            var required = call.callRealMethod();
            // The repository path is passed on in the MDC, like to any request
            var mdc = MDC.getCopyOfContextMap();
            runAsync(() -> {
                MDC.setContextMap(mdc);
                try (var session = CasStorageSession.open()) {
                    var cas = documentService.readOrCreateAnnotationCas(document, CURATION_SET,
                            FORCE_CAS_UPGRADE, EXCLUSIVE_WRITE_ACCESS);
                    addSpan(cas, aCuratedText);
                    documentService.writeAnnotationCas(cas,
                            documentService.createOrGetAnnotationDocument(document, CURATION_SET));
                    curationDocumentService.markCurationInProgress(document);
                }
                catch (Exception e) {
                    throw new IllegalStateException(e);
                }
                finally {
                    MDC.clear();
                }
            }).get();
            return required;
        }).when(curationDocumentService).isInitialMergeRequired(any());
    }

    private static void addSpan(CAS aCas, String aCoveredText)
    {
        var begin = TEXT.indexOf(aCoveredText);
        var type = aCas.getTypeSystem().getType(SPAN_LAYER);
        aCas.addFsToIndexes(aCas.createAnnotation(type, begin, begin + aCoveredText.length()));
    }

    private boolean curationCasExists() throws Exception
    {
        // Checked like the next request would see it
        try (var session = CasStorageSession.open()) {
            return curationDocumentService.existsCurationCas(document);
        }
    }

    private boolean initialMergeRequired() throws Exception
    {
        try (var session = CasStorageSession.open()) {
            return curationDocumentService.isInitialMergeRequired(document);
        }
    }

    private List<String> curatedSpans() throws Exception
    {
        try (var session = CasStorageSession.open()) {
            assertThat(curationDocumentService.existsCurationCas(document)).isTrue();
            var cas = documentService.readAnnotationCas(document, CURATION_SET);
            return cas.<AnnotationFS> getAnnotationIndex(cas.getTypeSystem().getType(SPAN_LAYER))
                    .stream() //
                    .map(AnnotationFS::getCoveredText) //
                    .toList();
        }
    }

    private SourceDocumentState storedState()
    {
        return documentService.getSourceDocument(project.getId(), document.getId()).getState();
    }

    /**
     * Makes moving a document into curation fail - state changes publish their event within the
     * transaction that makes them.
     */
    static class FailingStateChange
    {
        static final String MESSAGE = "State change failed";

        volatile boolean enabled;

        @EventListener
        void onDocumentStateChanged(DocumentStateChangedEvent aEvent)
        {
            if (enabled && aEvent.getNewState() == CURATION_IN_PROGRESS) {
                throw new IllegalStateException(MESSAGE);
            }
        }
    }

    @SpringBootConfiguration
    static class TestContext
    {
        @Bean
        DocumentImportExportService documentImportExportService() throws Exception
        {
            var importService = mock(DocumentImportExportService.class);
            // Supplies the initial CAS the annotator and curation CASes are created from
            when(importService.importCasFromFileNoChecks(any(), any(), any())).thenAnswer($ -> {
                var cas = CasCreationUtils.createCas(createTypeSystemDescription(), null, null,
                        null);
                cas.setDocumentText(TEXT);
                return cas;
            });
            return importService;
        }

        @Bean
        SpanLayerSupport spanLayerSupport(FeatureSupportRegistry aFeatureSupportRegistry,
                ApplicationEventPublisher aEventPublisher,
                LayerBehaviorRegistry aLayerBehaviorRegistry,
                ConstraintsService aConstraintsService)
        {
            return new SpanLayerSupportImpl(aFeatureSupportRegistry, aEventPublisher,
                    aLayerBehaviorRegistry, aConstraintsService);
        }

        @Bean
        SpanDiffSupport spanDiffSupport()
        {
            return new SpanDiffSupport();
        }

        @Bean
        FailingStateChange failingStateChange()
        {
            return new FailingStateChange();
        }

        @Bean
        CurationSidebarService curationSidebarService(DocumentService aDocumentService,
                CasStorageService aCasStorageService, CurationService aCurationService,
                CurationMergeService aCurationMergeService,
                CurationSessionService aCurationSessionService,
                CurationDocumentService aCurationDocumentService,
                PreferencesService aPreferencesService)
        {
            return new CurationSidebarServiceImpl(aDocumentService, aCasStorageService,
                    aCurationService, aCurationMergeService, aCurationSessionService,
                    aCurationDocumentService, aPreferencesService);
        }
    }
}
