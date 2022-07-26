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
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationContext;

import de.tudarmstadt.ukp.clarin.webanno.api.project.ProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.NamedEntityLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.QuickProjectInitializer;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.project.initializers.wikidatalinking.config.WikiDataLinkingProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.ui.kb.initializers.NamedEntityIdentifierFeatureInitializer;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link WikiDataLinkingProjectInitializersAutoConfiguration#entityLinkingProjectInitializer}.
 * </p>
 */
public class EntityLinkingProjectInitializer
    implements QuickProjectInitializer
{
    private final AnnotationSchemaService annotationService;
    private final ApplicationContext context;

    public EntityLinkingProjectInitializer(ApplicationContext aContext,
            AnnotationSchemaService aAnnotationService)
    {
        context = aContext;
        annotationService = aAnnotationService;
    }

    @Override
    public String getName()
    {
        return "Entity linking (Wikidata)";
    }

    @Override
    public boolean alreadyApplied(Project aProject)
    {
        return false;
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        List<Class<? extends ProjectInitializer>> dependencies = new ArrayList<>();
        dependencies.add(NamedEntityLayerInitializer.class);
        dependencies.add(NamedEntityIdentifierFeatureInitializer.class);
        dependencies.add(WikiDataKnowledgeBaseInitializer.class);

        if (context.getBeanNamesForType(
                NamedEntityIdentifierStringRecommenderInitializer.class).length > 0) {
            dependencies.add(NamedEntityIdentifierStringRecommenderInitializer.class);
        }

        return dependencies;
    }

    @Override
    public void configure(Project aProject) throws IOException
    {
        AnnotationLayer layer = annotationService.findLayer(aProject, NamedEntity.class.getName());
        AnnotationFeature valueFeature = annotationService.getFeature(NamedEntity._FeatName_value,
                layer);
        valueFeature.setEnabled(false);
        annotationService.createFeature(valueFeature);
    }
}
