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
package de.tudarmstadt.ukp.inception.project.initializers.neannotation;

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

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.NamedEntityLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.QuickProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.io.jsoncas.UimaJsonCasFormatSupport;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializationRequest;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.wikidatalinking.config.WikiDataLinkingProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.wicket.resource.Strings;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link WikiDataLinkingProjectInitializersAutoConfiguration#entityAnnotationProjectInitializer}.
 * </p>
 */
@Order(4900)
public class EntityAnnotationProjectInitializer
    implements QuickProjectInitializer
{
    private static final PackageResourceReference THUMBNAIL = new PackageResourceReference(
            MethodHandles.lookup().lookupClass(), "EntityAnnotationProjectInitializer.svg");

    private final AnnotationSchemaService annotationService;
    private final ApplicationContext context;
    private final DocumentService documentService;
    private final UserDao userService;

    public EntityAnnotationProjectInitializer(ApplicationContext aContext,
            AnnotationSchemaService aAnnotationService, DocumentService aDocumentService,
            UserDao aUserService)
    {
        context = aContext;
        annotationService = aAnnotationService;
        documentService = aDocumentService;
        userService = aUserService;
    }

    @Override
    public String getName()
    {
        return "Entity annotation";
    }

    @Override
    public Optional<String> getDescription()
    {
        return Optional.of(Strings.getString("entity-annotation-project.description"));
    }

    @Override
    public Optional<ResourceReference> getThumbnail()
    {
        return Optional.of(THUMBNAIL);
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
        dependencies.add(NamedEntityLayerInitializer.class);

        if (isStringRecommenderAvailable()) {
            dependencies.add(NamedEntityStringRecommenderInitializer.class);
        }

        if (isSequenceClassifierRecommenderAvailable()) {
            dependencies.add(NamedEntitySequenceClassifierRecommenderInitializer.class);
        }

        return dependencies;
    }

    private boolean isSequenceClassifierRecommenderAvailable()
    {
        return context.getBeanNamesForType(
                NamedEntitySequenceClassifierRecommenderInitializer.class).length > 0;
    }

    private boolean isStringRecommenderAvailable()
    {
        return context
                .getBeanNamesForType(NamedEntityStringRecommenderInitializer.class).length > 0;
    }

    @Override
    public void configure(ProjectInitializationRequest aRequest) throws IOException
    {
        var project = aRequest.getProject();
        project.setName(userService.getCurrentUsername() + " - New entity annotation project");

        var layer = annotationService.findLayer(project, NamedEntity.class.getName());
        var valueFeature = annotationService.getFeature(NamedEntity._FeatName_identifier, layer);
        valueFeature.setEnabled(false);
        annotationService.createFeature(valueFeature);

        var description = //
                """
                This project comes pre-configured for **entity annotation**.

                To annotate an entity, mark the text with the mouse, then assign a category in annotation detail
                panel on the right.
                """;

        if (isSequenceClassifierRecommenderAvailable() || isStringRecommenderAvailable()) {
            description += //
                    """

                    The project includes recommenders that will learn from the annotations you make and suggest further
                    entities to annotate in the text. These suggestions will appear in gray. You can accept a
                    suggestion by a single click. A double-click will instead reject the suggestion.
                    """;
        }

        if (aRequest.isIncludeSampleData()) {
            importExampleDocument(project,
                    "foodista_blog_2019_08_13_northern-british-columbia_abbreviated.json");
            importExampleDocument(project,
                    "foodista_blog_2019_10_22_lewiston-clarkstons-new-wine-district_abbreviated.json");

            description += """

                           The project includes example documents.
                           Open the **Annotation** page from the left sidbar menu to dive right in.
                           """;
        }

        project.setDescription(description);
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
