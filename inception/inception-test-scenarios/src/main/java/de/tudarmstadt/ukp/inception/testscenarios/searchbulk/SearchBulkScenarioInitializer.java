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
package de.tudarmstadt.ukp.inception.testscenarios.searchbulk;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.inception.project.initializers.basic.BasicRelationLayerInitializer.BASIC_RELATION_LABEL_FEATURE_NAME;
import static de.tudarmstadt.ukp.inception.project.initializers.basic.BasicRelationLayerInitializer.BASIC_RELATION_LAYER_NAME;
import static de.tudarmstadt.ukp.inception.project.initializers.basic.BasicSpanLayerInitializer.BASIC_SPAN_LABEL_FEATURE_NAME;
import static de.tudarmstadt.ukp.inception.project.initializers.basic.BasicSpanLayerInitializer.BASIC_SPAN_LAYER_NAME;
import static de.tudarmstadt.ukp.inception.support.uima.AnnotationBuilder.buildAnnotation;
import static de.tudarmstadt.ukp.inception.testscenarios.ScenarioDocuments.importDocument;
import static de.tudarmstadt.ukp.inception.testscenarios.accounts.ScenarioAccount.ANNOTATOR_1;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.util.CasUtil.getAnnotationType;
import static org.apache.uima.fit.util.CasUtil.getType;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.CoreferenceLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.QuickProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializationRequest;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.project.initializers.basic.BasicRelationLayerInitializer;
import de.tudarmstadt.ukp.inception.testscenarios.accounts.ScenarioAccountService;
import de.tudarmstadt.ukp.inception.testscenarios.config.InceptionTestScenariosAutoConfiguration;

/**
 * Builds a project for the bulk actions of the search sidebar. Every document contains the word
 * <code>Paris</code>, so a search for it has hits in all three.
 * <p>
 * <ul>
 * <li><code>searchbulk-alpha.txt</code> has been annotated only by
 * <code>scenario-annotator-1</code> ("Paris" as <code>LOC</code>). Opened as that annotator's
 * version, it is a read-only pane with a span that can be selected as the template.</li>
 * <li><code>searchbulk-beta.txt</code> has been annotated by the current user: two <code>PER</code>
 * spans joined by a relation, and a coreference chain span on "Beta" - selections that must not
 * enable the bulk actions.</li>
 * <li><code>searchbulk-gamma.txt</code> is <b>finished</b> for the current user, so bulk actions
 * have to skip it.</li>
 * </ul>
 * <p>
 * The current user is manager, curator and annotator.
 * <p>
 * <b>This class is exposed as a Spring Component via
 * {@link InceptionTestScenariosAutoConfiguration#searchBulkScenarioInitializer}.</b>
 */
@Order(9000)
public class SearchBulkScenarioInitializer
    implements QuickProjectInitializer
{
    private static final String DOC_ALPHA = "searchbulk-alpha.txt";
    private static final String DOC_BETA = "searchbulk-beta.txt";
    private static final String DOC_GAMMA = "searchbulk-gamma.txt";

    private static final String COREFERENCE_LAYER_NAME = "de.tudarmstadt.ukp.dkpro.core.api.coref.type.Coreference";

    private final DocumentService documentService;
    private final ProjectService projectService;
    private final UserDao userService;
    private final ScenarioAccountService scenarioAccountService;

    public SearchBulkScenarioInitializer(DocumentService aDocumentService,
            ProjectService aProjectService, UserDao aUserService,
            ScenarioAccountService aScenarioAccountService)
    {
        documentService = aDocumentService;
        projectService = aProjectService;
        userService = aUserService;
        scenarioAccountService = aScenarioAccountService;
    }

    @Override
    public String getName()
    {
        return "TEST: search bulk actions";
    }

    @Override
    public Optional<String> getDescription()
    {
        return Optional.of("Three documents containing \"Paris\": one annotated by another "
                + "annotator, one by you with a relation and a chain, one finished by you.");
    }

    @Override
    public boolean hasExamples()
    {
        return true;
    }

    @Override
    public boolean alreadyApplied(Project aProject)
    {
        return false;
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        return asList(BasicRelationLayerInitializer.class, CoreferenceLayerInitializer.class);
    }

    @Override
    public void configure(ProjectInitializationRequest aRequest) throws IOException
    {
        var project = aRequest.getProject();
        project.setName(userService.getCurrentUsername() + " - TEST search bulk");
        project.setDescription("""
                Search bulk actions test scenario. Search for `Paris`.

                `searchbulk-alpha.txt` (marker `ALPHAMARKER`) is annotated by
                `scenario-annotator-1` (password `admin`).
                `searchbulk-beta.txt` (marker `BETAMARKER`) is annotated by you: two spans joined
                by a relation, and a coreference chain span.
                `searchbulk-gamma.txt` (marker `GAMMAMARKER`) is finished for you.

                The current user is a manager, curator and annotator on this project.
                """);

        var currentUser = userService.getCurrentUser();
        projectService.assignRole(project, currentUser, MANAGER, CURATOR, ANNOTATOR);

        var alpha = importDocument(documentService, project, getClass(), DOC_ALPHA);
        var beta = importDocument(documentService, project, getClass(), DOC_BETA);
        var gamma = importDocument(documentService, project, getClass(), DOC_GAMMA);

        var otherAnnotator = scenarioAccountService.join(project, ANNOTATOR_1);

        annotateTemplate(alpha, otherAnnotator);
        annotateNonSpanSelections(beta, currentUser);
        finish(gamma, currentUser);
    }

    private void annotateTemplate(SourceDocument aDocument, User aUser) throws IOException
    {
        var annotationDocument = documentService.createOrGetAnnotationDocument(aDocument, aUser);
        var cas = documentService.readAnnotationCas(annotationDocument);

        addSpan(cas, "Paris", "LOC");

        documentService.writeAnnotationCas(cas, aDocument, aUser);
    }

    private void annotateNonSpanSelections(SourceDocument aDocument, User aUser) throws IOException
    {
        var annotationDocument = documentService.createOrGetAnnotationDocument(aDocument, aUser);
        var cas = documentService.readAnnotationCas(annotationDocument);

        var governor = addSpan(cas, "Angela Merkel", "PER");
        var dependent = addSpan(cas, "Emmanuel Macron", "PER");
        addRelation(cas, governor, dependent, "meets");
        addChain(cas, "Beta");

        documentService.writeAnnotationCas(cas, aDocument, aUser);
    }

    private void finish(SourceDocument aDocument, User aUser) throws IOException
    {
        var annotationDocument = documentService.createOrGetAnnotationDocument(aDocument, aUser);
        var cas = documentService.readAnnotationCas(annotationDocument);
        documentService.writeAnnotationCas(cas, aDocument, aUser);
        documentService.setAnnotationDocumentState(annotationDocument, FINISHED);
    }

    private AnnotationFS addSpan(CAS aCas, String aCoveredText, String aLabel)
    {
        var builder = buildAnnotation(aCas, BASIC_SPAN_LAYER_NAME);
        builder.withFeature(BASIC_SPAN_LABEL_FEATURE_NAME, aLabel);
        return builder.on(aCoveredText).buildAndAddToIndexes();
    }

    private void addRelation(CAS aCas, AnnotationFS aGovernor, AnnotationFS aDependent,
            String aLabel)
    {
        var builder = buildAnnotation(aCas, BASIC_RELATION_LAYER_NAME);
        builder.withFeature("Governor", aGovernor);
        builder.withFeature("Dependent", aDependent);
        builder.withFeature(BASIC_RELATION_LABEL_FEATURE_NAME, aLabel);
        builder.at(aDependent).buildAndAddToIndexes();
    }

    private void addChain(CAS aCas, String aCoveredText)
    {
        var begin = aCas.getDocumentText().indexOf(aCoveredText);
        var linkType = getAnnotationType(aCas, COREFERENCE_LAYER_NAME + "Link");
        var link = aCas.createAnnotation(linkType, begin, begin + aCoveredText.length());
        aCas.addFsToIndexes(link);

        var chainType = getType(aCas, COREFERENCE_LAYER_NAME + "Chain");
        var chain = aCas.createFS(chainType);
        chain.setFeatureValue(chainType.getFeatureByBaseName("first"), link);
        aCas.addFsToIndexes(chain);
    }
}
