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
package de.tudarmstadt.ukp.inception.testscenarios.urlfragment;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IGNORE;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.inception.testscenarios.ScenarioDocuments.importDocument;
import static de.tudarmstadt.ukp.inception.testscenarios.accounts.ScenarioAccount.ANNOTATOR_1;
import static de.tudarmstadt.ukp.inception.testscenarios.accounts.ScenarioAccount.ANNOTATOR_2;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.FeatureStructure;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.NamedEntityLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.QuickProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializationRequest;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.testscenarios.accounts.ScenarioAccountService;
import de.tudarmstadt.ukp.inception.testscenarios.config.InceptionTestScenariosAutoConfiguration;

/**
 * Builds a project for exercising the parts of URL-fragment reconciliation that the
 * editor-placement scenario cannot reach.
 * <p>
 * Three things this scenario supplies that {@code EditorPlacementScenarioInitializer} deliberately
 * does not:
 * <ul>
 * <li><b>Long documents.</b> 40 lines each, and every line names its own index
 * (<code>LONGMARKER unit 07 of 40</code>). Each line holds two sentences, so unit <i>n</i> starts
 * at sentence 2<i>n</i>-1. A <code>f=</code> focus move is therefore readable from the rendered
 * text alone - with the 5-line placement documents the whole document fits on one screen and a
 * focus move is invisible.</li>
 * <li><b>A second data owner with real annotations.</b> Needed to exercise <code>u=</code> slots: a
 * fragment can then put two editors on the <em>same</em> document with <em>different</em>
 * owners.</li>
 * <li><b>A document a user may not open.</b> {@code fragment-blocked.txt} is set to {@code IGNORE}
 * for {@code scenario-annotator-1}. That is the one per-document denial in
 * {@code DocumentAccessImpl#assertCanViewAnnotationDocument}.</li>
 * </ul>
 * <p>
 * ⚠️ <b>The denial case must be exercised as {@code scenario-annotator-1}, not as the user who
 * created the project.</b> Managers and curators return early from the access check, so for them
 * the blocked document simply opens and the case passes without testing anything. The creating user
 * is always a manager - see the comment in {@link #configure}.
 * <p>
 * <b>This class is exposed as a Spring Component via
 * {@link InceptionTestScenariosAutoConfiguration#urlFragmentScenarioInitializer}.</b>
 */
@Order(9000)
public class UrlFragmentScenarioInitializer
    implements QuickProjectInitializer
{
    private static final String DOC_LONG = "fragment-long.txt";
    private static final String DOC_SECOND = "fragment-second.txt";
    private static final String DOC_BLOCKED = "fragment-blocked.txt";

    private static final String TYPE_RESUMPTION_LOCATION = //
            "de.tudarmstadt.ukp.inception.annotation.type.ResumptionLocation";
    private static final String FEAT_OFFSET = "offset";

    private final DocumentService documentService;
    private final ProjectService projectService;
    private final UserDao userService;
    private final ScenarioAccountService scenarioAccountService;

    public UrlFragmentScenarioInitializer(DocumentService aDocumentService,
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
        return "TEST: URL fragment reconciliation";
    }

    @Override
    public Optional<String> getDescription()
    {
        return Optional.of("Long documents, a second data owner, and a blocked document - "
                + "for exercising the u=, f= and access-denied paths of the URL fragment.");
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
        return asList(NamedEntityLayerInitializer.class);
    }

    @Override
    public void configure(ProjectInitializationRequest aRequest) throws IOException
    {
        var project = aRequest.getProject();
        project.setName(userService.getCurrentUsername() + " - TEST url fragment");
        project.setDescription("""
                URL fragment reconciliation test scenario.

                Three 40-line documents. Every line names its own index, e.g.
                `LONGMARKER unit 07 of 40`, so a `f=` focus move can be read off the
                rendered text. Each line holds two sentences, so unit n starts at
                sentence 2n-1.

                * `fragment-long.txt` (LONGMARKER) and `fragment-second.txt` (SECONDMARKER)
                  are openable by the session owner and by **scenario-annotator-2**, so `u=` can
                  put two editors on the same document with different data owners.
                * `fragment-blocked.txt` (BLOCKEDMARKER) is set to `IGNORE` for
                  **scenario-annotator-1** and cannot be opened by them.
                * `fragment-long.txt` has a resumption point at `unit 25` (sentence 49) for the
                  project creator and at `unit 15` (sentence 29) for **scenario-annotator-2**, so
                  opening it without a focus shows where "resume" lands.

                Scenario accounts in this project, password `admin` for both:

                * `scenario-annotator-2` - a second data owner, for `u=` slots.
                * `scenario-annotator-1` - a plain annotator with no other role. **Log in as
                  this user to test the access-denied case.** The project creator is a
                  manager, and managers bypass the per-document access check, so the
                  blocked document just opens for them.
                """);

        var currentUser = userService.getCurrentUser();

        // The creating user keeps the manager role the project-creation dialog gave them - it is
        // what lets them reach the project settings at all.
        //
        // ⚠️ The access-denied case is therefore NOT testable as the creating user. Managers and
        // curators return early from DocumentAccessImpl#assertCanViewAnnotationDocument, so for
        // them the blocked document simply opens. Stripping the roles does not help either - the
        // project would be left without a manager.
        //
        // Hence the plain annotator below, who holds no other role. Log in as
        // scenario-annotator-1 to exercise the denial.
        projectService.assignRole(project, currentUser, ANNOTATOR);

        var longDoc = importDocument(documentService, project, getClass(), DOC_LONG);
        var secondDoc = importDocument(documentService, project, getClass(), DOC_SECOND);
        var blockedDoc = importDocument(documentService, project, getClass(), DOC_BLOCKED);

        // A second data owner who also has data on the long document, so the fragment can ask for
        // the same document under two different owners.
        var otherOwner = scenarioAccountService.join(project, ANNOTATOR_2);
        documentService.createOrGetAnnotationDocument(longDoc, otherOwner);

        // Resumption points somewhere recognisable, so that "opened at the resumption point" can
        // be told apart from "opened at the top". Different ones per data owner: only the session
        // owner's own data resumes, so opening scenario-annotator-2's data must NOT land on theirs.
        setResumptionLocation(longDoc, currentUser, "LONGMARKER unit 25 of 40");
        setResumptionLocation(longDoc, otherOwner, "LONGMARKER unit 15 of 40");

        // A plain annotator with no other role, for the access-denied case. See the comment on
        // the role assignment above for why the creating user cannot serve here.
        var plainAnnotator = scenarioAccountService.join(project, ANNOTATOR_1);

        // Blocked for the plain annotator: IGNORE is what takes a document out of an annotator's
        // workload, and it is what the per-document access check denies.
        var blockedAnnDoc = documentService.createOrGetAnnotationDocument(blockedDoc,
                plainAnnotator);
        documentService.setAnnotationDocumentState(blockedAnnDoc, IGNORE);

        // ... while the other two documents stay open to them, so a fragment can ask for one
        // allowed and one denied document in the same request.
        documentService.createOrGetAnnotationDocument(longDoc, plainAnnotator);
        documentService.createOrGetAnnotationDocument(secondDoc, plainAnnotator);
    }

    /**
     * Store a resumption location at the start of the given text, as the span/relation adapters do
     * when an annotation is created. Written with plain UIMA by type name rather than via
     * {@code TypeAdapter_ImplBase#setResumptionLocation}, which would pull in a dependency on
     * {@code inception-api-annotation} just for this.
     */
    private void setResumptionLocation(SourceDocument aDocument, User aUser, String aAnchorText)
        throws IOException
    {
        var annotationDocument = documentService.createOrGetAnnotationDocument(aDocument, aUser);
        var cas = documentService.readAnnotationCas(annotationDocument);

        var type = cas.getTypeSystem().getType(TYPE_RESUMPTION_LOCATION);
        if (type == null) {
            throw new IOException("Type system has no [" + TYPE_RESUMPTION_LOCATION + "]");
        }

        var offset = cas.getDocumentText().indexOf(aAnchorText);
        if (offset < 0) {
            throw new IOException(
                    "Anchor [" + aAnchorText + "] not found in [" + aDocument.getName() + "]");
        }

        var feature = type.getFeatureByBaseName(FEAT_OFFSET);
        var existing = cas.getIndexRepository().getAllIndexedFS(type);
        if (existing.hasNext()) {
            existing.next().setIntValue(feature, offset);
        }
        else {
            FeatureStructure location = cas.createFS(type);
            location.setIntValue(feature, offset);
            cas.addFsToIndexes(location);
        }

        documentService.writeAnnotationCas(cas, aDocument, aUser);
    }
}
