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

import static java.util.Arrays.asList;

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

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.keybindings.KeyBinding;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.NamedEntityLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.QuickProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.text.LineOrientedTextFormatSupport;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureTraits;
import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureTraits.EditorType;
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
    private final StringFeatureSupport stringFeatureSupport;

    public EntityAnnotationProjectInitializer(ApplicationContext aContext,
            AnnotationSchemaService aAnnotationService, DocumentService aDocumentService,
            UserDao aUserService, StringFeatureSupport aStringFeatureSupport)
    {
        context = aContext;
        annotationService = aAnnotationService;
        documentService = aDocumentService;
        userService = aUserService;
        stringFeatureSupport = aStringFeatureSupport;
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
        dependencies.add(NamedEntityLayerInitializer.class);
        dependencies.add(NamedEntitySampleDataTagSetInitializer.class);

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

        var identifierFeature = annotationService.getFeature(NamedEntity._FeatName_identifier,
                layer);
        identifierFeature.setEnabled(false);
        annotationService.createFeature(identifierFeature);

        var valueFeature = annotationService.getFeature(NamedEntity._FeatName_value, layer);
        valueFeature.setEnabled(true);

        if (aRequest.isIncludeSampleData()) {
            var tagset = annotationService
                    .getTagSet(NamedEntitySampleDataTagSetInitializer.TAG_SET_NAME, project);
            valueFeature.setTagset(tagset);

            var valueFeatureTraits = new StringFeatureTraits();
            valueFeatureTraits.setEditorType(EditorType.RADIOGROUP);
            valueFeatureTraits.setKeyBindings(asList( //
                    new KeyBinding("1", "Date"), //
                    new KeyBinding("2", "Event"), //
                    new KeyBinding("3", "Location"), //
                    new KeyBinding("4", "Organization"), //
                    new KeyBinding("5", "Person"), //
                    new KeyBinding("6", "Product"), //
                    new KeyBinding("7", "Product Category"), //
                    new KeyBinding("8", "Route")));
            stringFeatureSupport.writeTraits(valueFeature, valueFeatureTraits);
        }

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
            importExampleDocument(project, UimaJsonCasFormatSupport.ID,
                    "foodista_blog_2019_08_13_northern-british-columbia_abbreviated.json");
            importExampleDocument(project, UimaJsonCasFormatSupport.ID,
                    "foodista_blog_2019_10_22_lewiston-clarkstons-new-wine-district_abbreviated.json");
            importExampleDocument(project, LineOrientedTextFormatSupport.ID,
                    "foodista_blog_2024_09_18_the-schoolhouse-district-of-downtown-woodinville-washington.txt");

            description += """

                    The project includes example documents.
                    Open the **Annotation** page from the left sidbar menu to dive right in.
                    Two of the three documents come pre-annotated.
                    """;
        }

        project.setDescription(description);
    }

    private void importExampleDocument(Project aProject, String aFormat, String aDocName)
        throws IOException
    {
        var doc = SourceDocument.builder() //
                .withProject(aProject) //
                .withName(aDocName) //
                .withFormat(aFormat) //
                .build();

        try (var is = getClass().getResourceAsStream("data/" + aDocName)) {
            documentService.uploadSourceDocument(is, doc);
        }
        catch (UIMAException e) {
            throw new IOException(e);
        }
    }
}
