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
package de.tudarmstadt.ukp.inception.testscenarios.recommender;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.inception.support.uima.AnnotationBuilder.buildAnnotation;
import static de.tudarmstadt.ukp.inception.testscenarios.ScenarioDocuments.importDocument;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
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
import de.tudarmstadt.ukp.inception.project.initializers.basic.BasicSpanRecommenderInitializer;

/**
 * A project with an enabled recommender that produces suggestions immediately, for exercising the
 * recommendation accept/reject round-trip and the auto-accept-on-document-open path.
 */
@Order(9000)
public class RecommenderScenarioInitializer
    implements QuickProjectInitializer
{
    // The generic span recommender contributed by BasicSpanRecommenderInitializer targets the
    // custom.Span layer, so the training annotations have to be on that layer - not NamedEntity.
    private static final String TYPE_SPAN = "custom.Span";
    private static final String FEAT_LABEL = "label";

    private static final String TRAINING_DOCUMENT = "recommender-training.txt";
    private static final String TARGET_DOCUMENT = "recommender-target.txt";

    private final DocumentService documentService;
    private final ProjectService projectService;
    private final UserDao userService;

    public RecommenderScenarioInitializer(DocumentService aDocumentService,
            ProjectService aProjectService, UserDao aUserService)
    {
        documentService = aDocumentService;
        projectService = aProjectService;
        userService = aUserService;
    }

    @Override
    public String getName()
    {
        return "TEST: recommender, ready to suggest";
    }

    @Override
    public Optional<String> getDescription()
    {
        return Optional.of("Two documents and an enabled string-matching recommender, with the "
                + "first document pre-annotated so suggestions appear in the second.");
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
        return asList(BasicSpanRecommenderInitializer.class);
    }

    @Override
    public void configure(ProjectInitializationRequest aRequest) throws IOException
    {
        var project = aRequest.getProject();
        project.setName(userService.getCurrentUsername() + " - TEST recommender");
        project.setDescription("""
                Recommender test scenario.

                Two documents. `recommender-training.txt` is pre-annotated with custom.Span
                annotations for the current user; `recommender-target.txt` repeats the same
                entity strings so the string-matching recommender suggests them there.

                Use it to exercise:
                - accepting and rejecting a suggestion in the main editor
                - the same with the reference document sidebar open
                - auto-accept on document open (enable it on the recommender first)

                The current user is a manager and an annotator on this project.
                """);

        var currentUser = userService.getCurrentUser();
        projectService.assignRole(project, currentUser, MANAGER, ANNOTATOR);

        var trainingDocument = importDocument(documentService, project, getClass(),
                TRAINING_DOCUMENT);
        importDocument(documentService, project, getClass(), TARGET_DOCUMENT);

        annotate(trainingDocument, currentUser);
    }

    /**
     * Pre-annotate the training document so the string-matching recommender has something to learn
     * from. Deliberately left as {@code IN_PROGRESS} - the recommender trains on the annotations,
     * not on the document state, and leaving it open keeps the project usable for annotation.
     */
    private void annotate(SourceDocument aDocument, User aUser) throws IOException
    {
        var annotationDocument = documentService.createOrGetAnnotationDocument(aDocument, aUser);
        var cas = documentService.readAnnotationCas(annotationDocument);

        var spanType = cas.getTypeSystem().getType(TYPE_SPAN);

        addSpan(cas, spanType, "Angela Merkel", "PER");
        addSpan(cas, spanType, "Emmanuel Macron", "PER");
        addSpan(cas, spanType, "Berlin", "LOC");
        addSpan(cas, spanType, "Siemens", "ORG");

        documentService.writeAnnotationCas(cas, aDocument, aUser);
    }

    /**
     * Annotates <em>every</em> occurrence of the given text so the string-matching recommender has
     * as many training instances as the document offers. Texts that do not occur are skipped.
     */
    private void addSpan(CAS aCas, Type aType, String aCoveredText, String aValue)
    {
        var builder = buildAnnotation(aCas, aType);
        builder.withFeature(FEAT_LABEL, aValue);
        builder.onAll(aCoveredText).buildAllAndAddToIndexes();
    }
}
