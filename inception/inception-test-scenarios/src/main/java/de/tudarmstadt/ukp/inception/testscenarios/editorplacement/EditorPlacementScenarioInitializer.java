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
package de.tudarmstadt.ukp.inception.testscenarios.editorplacement;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.inception.testscenarios.ScenarioDocuments.importDocument;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.NamedEntityLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.QuickProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializationRequest;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.testscenarios.config.InceptionTestScenariosAutoConfiguration;

/**
 * Builds a project with <b>three</b> plain documents and no annotations at all.
 * <p>
 * The point of this scenario is the document <em>count</em>. Editor placement - which editor a
 * document ends up in when it is opened - is only observable when there is more than one document
 * to switch between: with a single document, opening one is indistinguishable from opening it in
 * the wrong place. Two are needed to see a switch at all, and a third to tell "opens in the page's
 * own editor" apart from "opens in whichever editor was last touched" while the reference document
 * sidebar holds a second one.
 * <p>
 * Each document carries a distinctive marker word (<code>ALPHAMARKER</code> and friends) so a test
 * can identify which document an editor is showing from its text alone, without depending on a
 * title element that may or may not be present in a given editor.
 * <p>
 * Deliberately <em>not</em> curation-ready: no annotators, no finished annotation documents, no
 * source document state transitions. Placement is a question about the annotation page, and the
 * extra state would only add ways for the scenario to break.
 * <p>
 * <b>This class is exposed as a Spring Component via
 * {@link InceptionTestScenariosAutoConfiguration#editorPlacementScenarioInitializer}.</b>
 */
@Order(9000)
public class EditorPlacementScenarioInitializer
    implements QuickProjectInitializer
{
    private static final String[] DOCUMENTS = { "placement-alpha.txt", "placement-beta.txt",
            "placement-gamma.txt" };

    private final DocumentService documentService;
    private final ProjectService projectService;
    private final UserDao userService;

    public EditorPlacementScenarioInitializer(DocumentService aDocumentService,
            ProjectService aProjectService, UserDao aUserService)
    {
        documentService = aDocumentService;
        projectService = aProjectService;
        userService = aUserService;
    }

    @Override
    public String getName()
    {
        return "TEST: three documents, editor placement";
    }

    @Override
    public Optional<String> getDescription()
    {
        return Optional.of("Three unannotated documents. For exercising where a document opens - "
                + "the open-document dialog, the reference document sidebar, and deep links.");
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
        project.setName(userService.getCurrentUsername() + " - TEST placement");
        project.setDescription("""
                Editor placement test scenario.

                Three unannotated documents: `placement-alpha.txt`, `placement-beta.txt` and
                `placement-gamma.txt`. Each contains a distinctive marker word (`ALPHAMARKER`,
                `BETAMARKER`, `GAMMAMARKER`) so a test can tell which document an editor is
                showing from its text alone.

                The current user is a manager and an annotator on this project.
                """);

        // Manager to reach the project settings, annotator to open the annotation page.
        var currentUser = userService.getCurrentUser();
        projectService.assignRole(project, currentUser, MANAGER, ANNOTATOR);

        for (var documentName : DOCUMENTS) {
            importDocument(documentService, project, getClass(), documentName);
        }
    }

}
