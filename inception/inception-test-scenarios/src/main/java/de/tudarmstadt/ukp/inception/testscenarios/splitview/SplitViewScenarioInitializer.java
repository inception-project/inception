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
package de.tudarmstadt.ukp.inception.testscenarios.splitview;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.inception.project.initializers.basic.BasicRelationLayerInitializer.BASIC_RELATION_LABEL_FEATURE_NAME;
import static de.tudarmstadt.ukp.inception.project.initializers.basic.BasicRelationLayerInitializer.BASIC_RELATION_LAYER_NAME;
import static de.tudarmstadt.ukp.inception.project.initializers.basic.BasicSpanLayerInitializer.BASIC_SPAN_LABEL_FEATURE_NAME;
import static de.tudarmstadt.ukp.inception.project.initializers.basic.BasicSpanLayerInitializer.BASIC_SPAN_LAYER_NAME;
import static de.tudarmstadt.ukp.inception.support.uima.AnnotationBuilder.buildAnnotation;
import static de.tudarmstadt.ukp.inception.testscenarios.ScenarioDocuments.importDocument;
import static de.tudarmstadt.ukp.inception.testscenarios.accounts.ScenarioAccount.ANNOTATOR_1;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
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
 * Builds a project for the split view in which the panes can hold <b>different data owners</b>.
 * <p>
 * <ul>
 * <li><code>splitview-alpha.txt</code> has been annotated by a second annotator,
 * <code>scenario-annotator-1</code>, so a manager can open that annotator's work in one pane and
 * their own in another. The annotator can also log in: the project creator is always a curator as
 * well, which changes what the finish button does, so a plain annotator is needed to see the
 * annotator's version.</li>
 * <li><code>splitview-beta.txt</code> has been annotated by the current user with two spans that
 * are connected by <b>two identical relations</b>. Those are drawn at the same height when the
 * "collapse arcs" preference is on and stacked when it is off, which makes the preference visible
 * in the rendered SVG.</li>
 * </ul>
 * <p>
 * The current user is manager, curator and annotator, so they may both view the other annotator's
 * work and finish or lock their own documents.
 * <p>
 * <b>This class is exposed as a Spring Component via
 * {@link InceptionTestScenariosAutoConfiguration#splitViewScenarioInitializer}.</b>
 */
@Order(9000)
public class SplitViewScenarioInitializer
    implements QuickProjectInitializer
{
    private static final String DOC_ALPHA = "splitview-alpha.txt";
    private static final String DOC_BETA = "splitview-beta.txt";

    private final DocumentService documentService;
    private final ProjectService projectService;
    private final UserDao userService;
    private final ScenarioAccountService scenarioAccountService;

    public SplitViewScenarioInitializer(DocumentService aDocumentService,
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
        return "TEST: split view, other annotator and relations";
    }

    @Override
    public Optional<String> getDescription()
    {
        return Optional.of("Two documents: one annotated by another annotator, one by you with "
                + "two identical relations. For panes with different data owners and the "
                + "collapse-arcs preference.");
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
        return asList(BasicRelationLayerInitializer.class);
    }

    @Override
    public void configure(ProjectInitializationRequest aRequest) throws IOException
    {
        var project = aRequest.getProject();
        project.setName(userService.getCurrentUsername() + " - TEST split view");
        project.setDescription("""
                Split view test scenario.

                `splitview-alpha.txt` (marker `ALPHAMARKER`) is annotated by `scenario-annotator-1`
                (password `admin`).
                `splitview-beta.txt` (marker `BETAMARKER`) is annotated by you: two spans joined
                by two identical relations, to make the "collapse arcs" preference visible.

                The current user is a manager, curator and annotator on this project.
                """);

        var currentUser = userService.getCurrentUser();
        projectService.assignRole(project, currentUser, MANAGER, CURATOR, ANNOTATOR);

        var alpha = importDocument(documentService, project, getClass(), DOC_ALPHA);
        var beta = importDocument(documentService, project, getClass(), DOC_BETA);

        var otherAnnotator = scenarioAccountService.join(project, ANNOTATOR_1);

        annotateOtherAnnotator(alpha, otherAnnotator);
        annotateWithDuplicateRelations(beta, currentUser);
    }

    private void annotateOtherAnnotator(SourceDocument aDocument, User aUser) throws IOException
    {
        var annotationDocument = documentService.createOrGetAnnotationDocument(aDocument, aUser);
        var cas = documentService.readAnnotationCas(annotationDocument);

        addSpan(cas, "Angela Merkel", "BOB");
        addSpan(cas, "Paris", "BOB");

        documentService.writeAnnotationCas(cas, aDocument, aUser);
    }

    private void annotateWithDuplicateRelations(SourceDocument aDocument, User aUser)
        throws IOException
    {
        var annotationDocument = documentService.createOrGetAnnotationDocument(aDocument, aUser);
        var cas = documentService.readAnnotationCas(annotationDocument);

        var governor = addSpan(cas, "Angela Merkel", "PER");
        var dependent = addSpan(cas, "Emmanuel Macron", "PER");

        // Same type, same endpoints, same label - only then does the brat editor bundle them.
        addRelation(cas, governor, dependent, "meets");
        addRelation(cas, governor, dependent, "meets");

        documentService.writeAnnotationCas(cas, aDocument, aUser);
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
}
