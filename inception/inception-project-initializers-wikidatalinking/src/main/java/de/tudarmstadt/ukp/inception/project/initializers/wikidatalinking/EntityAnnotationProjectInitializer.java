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
package de.tudarmstadt.ukp.inception.project.initializers.wikidatalinking;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.NamedEntityLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.QuickProjectInitializer;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
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

    public EntityAnnotationProjectInitializer(ApplicationContext aContext,
            AnnotationSchemaService aAnnotationService)
    {
        context = aContext;
        annotationService = aAnnotationService;
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

        if (context.getBeanNamesForType(NamedEntityStringRecommenderInitializer.class).length > 0) {
            dependencies.add(NamedEntityStringRecommenderInitializer.class);
        }

        if (context.getBeanNamesForType(
                NamedEntitySequenceClassifierRecommenderInitializer.class).length > 0) {
            dependencies.add(NamedEntitySequenceClassifierRecommenderInitializer.class);
        }

        return dependencies;
    }

    @Override
    public void configure(Project aProject) throws IOException
    {
        var layer = annotationService.findLayer(aProject, NamedEntity.class.getName());
        var valueFeature = annotationService.getFeature(NamedEntity._FeatName_value, layer);
        valueFeature.setEnabled(false);
        annotationService.createFeature(valueFeature);
    }
}
