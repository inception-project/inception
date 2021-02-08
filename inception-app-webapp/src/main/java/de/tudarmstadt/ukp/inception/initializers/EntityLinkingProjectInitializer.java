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
package de.tudarmstadt.ukp.inception.initializers;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.project.ProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.NamedEntityLayerInitializer;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.projectlist.QuickProjectInitializer;
import de.tudarmstadt.ukp.inception.ui.kb.initializers.NamedEntityIdentifierFeatureInitializer;

@Component
public class EntityLinkingProjectInitializer
    implements QuickProjectInitializer
{
    private final AnnotationSchemaService annotationService;

    public EntityLinkingProjectInitializer(AnnotationSchemaService aAnnotationService)
    {
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
        return asList(NamedEntityLayerInitializer.class, WikiDataKnowledgeBaseInitializer.class,
                NamedEntityIdentifierFeatureInitializer.class,
                NamedEntityIdentifierStringRecommenderInitializer.class);
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
