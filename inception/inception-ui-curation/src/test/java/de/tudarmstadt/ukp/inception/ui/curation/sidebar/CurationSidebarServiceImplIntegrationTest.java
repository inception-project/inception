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
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static java.util.Collections.emptyList;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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
import de.tudarmstadt.ukp.inception.curation.service.CurationDocumentService;
import de.tudarmstadt.ukp.inception.curation.service.CurationMergeService;
import de.tudarmstadt.ukp.inception.curation.service.CurationService;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.config.DocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.editor.state.AnnotatorStateImpl;
import de.tudarmstadt.ukp.inception.log.config.EventLoggingAutoConfiguration;
import de.tudarmstadt.ukp.inception.preferences.config.PreferencesServiceAutoConfig;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.LayerBehaviorRegistry;
import de.tudarmstadt.ukp.inception.schema.config.AnnotationSchemaServiceAutoConfiguration;

/**
 * Merging in the curation sidebar with real CAS storage and a real database. Runs without a
 * test-managed transaction, so that the transaction of the service under test commits or rolls back
 * as it does in the application. Starting curation is tested in
 * {@code CurationEditingServiceImplIntegrationTest}.
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
    private @Autowired CurationSidebarService sut;
    private @Autowired CurationDocumentService curationDocumentService;

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

    private AnnotatorState curationOf(SourceDocument aDocument)
    {
        var state = new AnnotatorStateImpl(Mode.CURATION);
        state.setProject(project);
        state.setDocument(aDocument, emptyList());
        state.setUser(new User(CURATION_USER));
        state.setAnnotationLayers(schemaService.listAnnotationLayer(project));
        return state;
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

    private SourceDocumentState storedState()
    {
        return documentService.getSourceDocument(project.getId(), document.getId()).getState();
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
        CurationSidebarService curationSidebarService(DocumentService aDocumentService,
                CasStorageService aCasStorageService, CurationService aCurationService,
                CurationMergeService aCurationMergeService,
                CurationSessionService aCurationSessionService,
                CurationDocumentService aCurationDocumentService)
        {
            return new CurationSidebarServiceImpl(aDocumentService, aCasStorageService,
                    aCurationService, aCurationMergeService, aCurationSessionService,
                    aCurationDocumentService);
        }
    }
}
