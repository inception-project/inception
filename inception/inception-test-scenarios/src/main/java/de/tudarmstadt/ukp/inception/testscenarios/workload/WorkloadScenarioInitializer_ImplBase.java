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
package de.tudarmstadt.ukp.inception.testscenarios.workload;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.inception.support.uima.AnnotationBuilder.buildAnnotation;
import static de.tudarmstadt.ukp.inception.testscenarios.ScenarioDocuments.importDocument;
import static de.tudarmstadt.ukp.inception.testscenarios.accounts.ScenarioAccount.ANNOTATOR_1;
import static de.tudarmstadt.ukp.inception.testscenarios.accounts.ScenarioAccount.MANAGER_ANNOTATOR;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;

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
import de.tudarmstadt.ukp.inception.testscenarios.curation.CurationScenarioInitializer_ImplBase;
import de.tudarmstadt.ukp.inception.testscenarios.recommender.RecommenderScenarioInitializer;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

/**
 * A project under a given workload manager, with one annotator part-way through the first of two
 * documents. The variants differ only in the workload manager, so the same interaction can be run
 * against each - e.g. the annotator's own action bar (finish, reset) or requesting a second editor
 * through the URL fragment, which only the matrix workload manager permits.
 */
public abstract class WorkloadScenarioInitializer_ImplBase
    implements QuickProjectInitializer
{
    private static final String DOCUMENT = "curation-sample.txt";
    private static final String SECOND_DOCUMENT = "recommender-target.txt";

    private static final String TYPE_NAMED_ENTITY = //
            "de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity";
    private static final String FEAT_VALUE = "value";

    private final DocumentService documentService;
    private final ProjectService projectService;
    private final UserDao userService;
    private final ScenarioAccountService scenarioAccountService;
    private final WorkloadManagementService workloadManagementService;

    public WorkloadScenarioInitializer_ImplBase(DocumentService aDocumentService,
            ProjectService aProjectService, UserDao aUserService,
            ScenarioAccountService aScenarioAccountService,
            WorkloadManagementService aWorkloadManagementService)
    {
        documentService = aDocumentService;
        projectService = aProjectService;
        userService = aUserService;
        scenarioAccountService = aScenarioAccountService;
        workloadManagementService = aWorkloadManagementService;
    }

    /**
     * @return the id of the workload manager extension the project uses.
     */
    protected abstract String getWorkloadManager();

    @Override
    public Optional<String> getDescription()
    {
        return Optional
                .of("Workload manager [" + getWorkloadManager() + "]; " + ANNOTATOR_1.getUsername()
                        + " has started annotating the first of two documents.");
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
        project.setName(
                userService.getCurrentUsername() + " - TEST " + getWorkloadManager() + " workload");
        project.setDescription("""
                Workload test scenario.

                The project uses the %s workload manager, with document reset allowed.
                **scenario-annotator-1** (password `admin`) has annotated some named entities in
                `curation-sample.txt`; the annotation document is in progress.
                `recommender-target.txt` has not been started.

                The current user is a manager and a curator on this project, not an annotator.
                **scenario-manager-annotator** (password `admin`) is a manager and an annotator
                but not a curator, and has not started any document.
                """.formatted(getWorkloadManager()));

        var currentUser = userService.getCurrentUser();
        projectService.assignRole(project, currentUser, MANAGER, CURATOR);
        projectService.revokeRole(project, currentUser, ANNOTATOR);

        var manager = workloadManagementService.loadOrCreateWorkloadManagerConfiguration(project);
        manager.setType(getWorkloadManager());
        workloadManagementService.saveConfiguration(manager);

        var document = importDocument(documentService, project,
                CurationScenarioInitializer_ImplBase.class, DOCUMENT);
        importDocument(documentService, project, RecommenderScenarioInitializer.class,
                SECOND_DOCUMENT);

        var annotator = scenarioAccountService.join(project, ANNOTATOR_1);
        annotate(document, annotator);

        scenarioAccountService.join(project, MANAGER_ANNOTATOR);
    }

    private void annotate(SourceDocument aDocument, User aUser) throws IOException
    {
        var annotationDocument = documentService.createOrGetAnnotationDocument(aDocument, aUser);
        var cas = documentService.readAnnotationCas(annotationDocument);

        var neType = cas.getTypeSystem().getType(TYPE_NAMED_ENTITY);
        addNamedEntity(cas, neType, "Siemens", "ORG");
        addNamedEntity(cas, neType, "Airbus", "ORG");
        addNamedEntity(cas, neType, "Berlin", "LOC");

        documentService.writeAnnotationCas(cas, aDocument, aUser);
        documentService.setAnnotationDocumentState(annotationDocument, IN_PROGRESS);
    }

    private void addNamedEntity(CAS aCas, Type aType, String aCoveredText, String aValue)
    {
        var builder = buildAnnotation(aCas, aType);
        builder.withFeature(FEAT_VALUE, aValue);
        builder.onAll(aCoveredText).buildAllAndAddToIndexes();
    }
}
