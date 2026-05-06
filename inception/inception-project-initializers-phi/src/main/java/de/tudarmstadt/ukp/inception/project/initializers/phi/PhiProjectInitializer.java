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
package de.tudarmstadt.ukp.inception.project.initializers.phi;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationEditorManagerPrefs.KEY_ANNOTATION_EDITOR_MANAGER_PREFS;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.uima.UIMAException;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratTokenWrappingAnnotationEditorFactory;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.QuickProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.io.jsoncas.UimaJsonCasFormatSupport;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializationRequest;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.phi.config.InceptionPhiProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.inception.support.wicket.resource.Strings;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link InceptionPhiProjectInitializersAutoConfiguration#phiProjectInitializer}.
 * </p>
 */
@Order(5500)
public class PhiProjectInitializer
    implements QuickProjectInitializer
{
    private static final PackageResourceReference THUMBNAIL = new PackageResourceReference(
            MethodHandles.lookup().lookupClass(), "PhiProjectInitializer.svg");

    private final ApplicationContext context;

    private final DocumentService documentService;
    private final UserDao userService;
    private final PreferencesService preferencesService;

    public PhiProjectInitializer(ApplicationContext aContext, DocumentService aDocumentService,
            UserDao aUserService, PreferencesService aPreferencesService)
    {
        context = aContext;
        documentService = aDocumentService;
        userService = aUserService;
        preferencesService = aPreferencesService;
    }

    @Override
    public String getName()
    {
        return "PHI annotation";
    }

    @Override
    public Optional<String> getDescription()
    {
        return Optional.of("Annotate personal health information (PHI).");
    }

    @Override
    public Optional<ResourceReference> getThumbnail()
    {
        return Optional.of(THUMBNAIL);
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
        var dependencies = new ArrayList<Class<? extends ProjectInitializer>>();
        dependencies.add(PhiSpanLayerInitializer.class);

        if (context.getBeanNamesForType(
                PhiSpanStringMatchingRecommenderInitializer.class).length > 0) {
            dependencies.add(PhiSpanStringMatchingRecommenderInitializer.class);
        }

        if (context.getBeanNamesForType(PhiSpanOpenNlpNerRecommenderInitializer.class).length > 0) {
            dependencies.add(PhiSpanOpenNlpNerRecommenderInitializer.class);
        }

        return dependencies;
    }

    @Override
    public void configure(ProjectInitializationRequest aRequest) throws IOException
    {
        var project = aRequest.getProject();
        project.setName(userService.getCurrentUsername() + " - New PHI annotation project");

        var description = //
                """
                        This project comes pre-configured for **Personal Health Information (PHI)**.

                        To annotate a PHI information, mark the text with the mouse, then assign a category in annotation detail
                        panel on the right.

                        The tagset used in this project template was derived from the PHI tagset used by the GeMTeX project.
                        It was originally published as part of the [`GraSCCo_PHI` dataset](https://zenodo.org/records/11502329).
                        """;

        if (isStringRecommenderAvailable()) {
            description += //
                    """

                            The project includes recommenders that will learn from the annotations you make and suggest further
                            entities to annotate in the text. These suggestions will appear in gray. You can accept a
                            suggestion by a single click. A double-click will instead reject the suggestion.
                            """;
        }

        if (aRequest.isIncludeSampleData()) {
            importExampleDocument(project, "Albers.txt_phi.json");
            importExampleDocument(project, "Amanda_Alzheimer.txt_phi.json");
            importExampleDocument(project, "Baastrup.txt_phi.json");

            description += Strings.getString("phi-span-layer.example-data");
            ;
        }

        project.setDescription(description);

        var bratEditorFactory = context.getBean(BratTokenWrappingAnnotationEditorFactory.class);
        if (bratEditorFactory != null) {
            var editorState = preferencesService
                    .loadDefaultTraitsForProject(KEY_ANNOTATION_EDITOR_MANAGER_PREFS, project);
            editorState.setDefaultEditor(bratEditorFactory.getBeanName());
            preferencesService.saveDefaultTraitsForProject(KEY_ANNOTATION_EDITOR_MANAGER_PREFS,
                    project, editorState);
        }
    }

    private boolean isStringRecommenderAvailable()
    {
        return context
                .getBeanNamesForType(PhiSpanStringMatchingRecommenderInitializer.class).length > 0;
    }

    private void importExampleDocument(Project aProject, String docName) throws IOException
    {
        var doc = SourceDocument.builder() //
                .withProject(aProject) //
                .withName(docName) //
                .withFormat(UimaJsonCasFormatSupport.ID) //
                .build();
        try (var is = getClass().getResourceAsStream("data/" + docName)) {
            documentService.uploadSourceDocument(is, doc);
        }
        catch (UIMAException e) {
            throw new IOException(e);
        }
    }
}
