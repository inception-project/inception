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
package de.tudarmstadt.ukp.inception.testscenarios.curation;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.inception.support.uima.AnnotationBuilder.buildAnnotation;
import static de.tudarmstadt.ukp.inception.testscenarios.ScenarioDocuments.importDocument;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.NamedEntityLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.QuickProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializationRequest;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;

/**
 * Builds a project that is ready for curation: one document, two annotators who have both marked
 * their annotation document {@code FINISHED}, and named-entity annotations that deliberately agree
 * on some units and disagree on others.
 * <p>
 * This is the scenario the curation pages need - the annotator panes have something to show, the
 * unit overview has a mix of states, and a re-merge has something to merge.
 * <p>
 * Subclasses vary what is layered on top; everything that makes the project <em>curatable</em>
 * lives here so the variants cannot drift apart on it.
 */
public abstract class CurationScenarioInitializer_ImplBase
    implements QuickProjectInitializer
{
    private static final String DOCUMENT = "curation-sample.txt";

    private static final String ANNOTATOR_1 = "test-annotator-1";
    private static final String ANNOTATOR_2 = "test-annotator-2";

    private static final String TYPE_NAMED_ENTITY = //
            "de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity";
    private static final String FEAT_VALUE = "value";

    private final DocumentService documentService;
    private final ProjectService projectService;
    private final UserDao userService;

    public CurationScenarioInitializer_ImplBase(DocumentService aDocumentService,
            ProjectService aProjectService, UserDao aUserService)
    {
        documentService = aDocumentService;
        projectService = aProjectService;
        userService = aUserService;
    }

    /**
     * @return the document service, for subclasses that need to adjust documents in
     *         {@link #finalizeDocument}.
     */
    protected DocumentService getDocumentService()
    {
        return documentService;
    }

    /**
     * @return the suffix appended to the project name, so the variants are distinguishable in a
     *         project listing.
     */
    protected abstract String getProjectNameSuffix();

    /**
     * @return the sample documents to import, in order. Every one of them is annotated by both
     *         annotators and moved to {@code ANNOTATION_FINISHED}, i.e. every one is curatable.
     *         Override to build a project with more than one curatable document.
     */
    protected List<String> listDocuments()
    {
        return asList(DOCUMENT);
    }

    /**
     * @return the class whose package holds the documents named by {@link #listDocuments()}.
     *         Defaults to this base class, which is where the inherited {@code curation-sample.txt}
     *         lives. A variant shipping its own documents overrides this to point at itself.
     */
    protected Class<?> getDocumentOwner()
    {
        return CurationScenarioInitializer_ImplBase.class;
    }

    /**
     * Hook to adjust a document after it has been annotated and made curatable. Runs once per
     * document, in {@link #listDocuments()} order.
     *
     * @param aDocument
     *            the document, currently in state {@code ANNOTATION_FINISHED}.
     * @param aIndex
     *            its zero-based position in {@link #listDocuments()}.
     */
    protected void finalizeDocument(SourceDocument aDocument, int aIndex)
    {
        // Nothing by default.
    }

    /**
     * Hook for whatever the variant adds on top of the curatable base - extra layers, preferences.
     * Runs after the document exists but before the annotations are written, so a layer enabled
     * here is in place by the time anything is annotated.
     *
     * @param aProject
     *            the project being initialized.
     */
    protected void configureVariant(Project aProject)
    {
        // Nothing by default.
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
        project.setName(userService.getCurrentUsername() + " - TEST " + getProjectNameSuffix());
        project.setDescription("""
                Curation test scenario.

                The document `curation-sample.txt` has been finished by **test-annotator-1** and
                **test-annotator-2**. Their named-entity annotations agree on some sentences and
                disagree on others, so the curation unit overview shows a mix of states and a
                re-merge has something to do.

                The current user is a curator and a manager on this project.
                """);

        // The curator has to be able to open the curation page, and to see the project at all.
        var currentUser = userService.getCurrentUser();
        projectService.assignRole(project, currentUser, MANAGER, CURATOR);

        // Remove annotator role granted by default during project creation.
        projectService.revokeRole(project, currentUser, ANNOTATOR);

        var documentNames = listDocuments();
        var documents = new ArrayList<SourceDocument>();
        for (var documentName : documentNames) {
            documents.add(
                    importDocument(documentService, project, getDocumentOwner(), documentName));
        }

        configureVariant(project);

        var annotator1 = createAnnotator(project, ANNOTATOR_1, "Test Annotator 1");
        var annotator2 = createAnnotator(project, ANNOTATOR_2, "Test Annotator 2");

        for (var i = 0; i < documents.size(); i++) {
            var document = documents.get(i);

            annotateAs(document, annotator1, 1);
            annotateAs(document, annotator2, 2);

            // The default curatable-documents strategy keys on the SOURCE document state, not on
            // the individual annotation document states (CurationDocumentServiceImpl
            // #listCuratableSourceDocuments_new). Normally the workload manager makes this
            // transition once every annotator has finished; since we set the annotation documents
            // directly, we have to move the source document along too or the document never shows
            // up for curation.
            documentService.setSourceDocumentState(document, ANNOTATION_FINISHED);

            finalizeDocument(document, i);
        }
    }

    private User createAnnotator(Project aProject, String aUsername, String aUiName)
    {
        var user = userService.get(aUsername);

        if (user == null) {
            user = User.builder() //
                    .withUsername(aUsername) //
                    .withUiName(aUiName) //
                    .withPassword(aUsername) //
                    .withEnabled(true) //
                    .withRoles(Role.ROLE_USER) //
                    .build();
            user = userService.create(user);
        }

        projectService.assignRole(aProject, user, ANNOTATOR);

        return user;
    }

    /**
     * Annotate every sentence of the document with named entities, varying the tags and the spans
     * per annotator so that the curation unit overview shows agreement on some units and
     * disagreement on others.
     */
    private void annotateAs(SourceDocument aDocument, User aUser, int aVariant) throws IOException
    {
        var annotationDocument = documentService.createOrGetAnnotationDocument(aDocument, aUser);
        var cas = documentService.readAnnotationCas(annotationDocument);

        var neType = cas.getTypeSystem().getType(TYPE_NAMED_ENTITY);

        // Agreed on by both annotators - these units come out green in the overview.
        addNamedEntity(cas, neType, "Angela Merkel", "PER");
        addNamedEntity(cas, neType, "Emmanuel Macron", "PER");
        addNamedEntity(cas, neType, "Berlin", "LOC");

        if (aVariant == 1) {
            // Same span, different label -> disagreement.
            addNamedEntity(cas, neType, "Siemens", "ORG");
            addNamedEntity(cas, neType, "Airbus", "ORG");
            // Only annotator 1 marks these at all -> incomplete.
            addNamedEntity(cas, neType, "Charles de Gaulle Airport", "LOC");
            addNamedEntity(cas, neType, "BBC", "ORG");
        }
        else {
            // Same span, different label -> disagreement.
            addNamedEntity(cas, neType, "Siemens", "LOC");
            addNamedEntity(cas, neType, "Airbus", "LOC");
            // Only annotator 2 marks these at all -> incomplete.
            addNamedEntity(cas, neType, "Charles de Gaulle", "PER");
            addNamedEntity(cas, neType, "Le Monde", "ORG");
        }

        documentService.writeAnnotationCas(cas, aDocument, aUser);
        documentService.setAnnotationDocumentState(annotationDocument, FINISHED);
    }

    /**
     * Annotates <em>every</em> occurrence of the given text so that documents which mention an
     * entity in several sentences yield several curation units. Texts that do not occur at all are
     * silently skipped - the two annotator variants deliberately use different fixture texts.
     */
    private void addNamedEntity(CAS aCas, Type aType, String aCoveredText, String aValue)
    {
        var builder = buildAnnotation(aCas, aType);
        builder.withFeature(FEAT_VALUE, aValue);
        builder.onAll(aCoveredText).buildAllAndAddToIndexes();
    }
}
