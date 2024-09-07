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
package de.tudarmstadt.ukp.inception.ui.kb.initializers;

import static de.tudarmstadt.ukp.inception.ui.kb.feature.ConceptFeatureSupport.TYPE_ANY_OBJECT;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;

import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.LayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.NamedEntityLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.TokenLayerInitializer;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.wicket.resource.Strings;
import de.tudarmstadt.ukp.inception.ui.kb.config.KnowledgeBaseServiceUIAutoConfiguration;

/**
 * Adds the {@code identifier} feature provided since DKPro Core 1.9.0 as a concept feature.
 * <p>
 * This class is exposed as a Spring Component via
 * {@link KnowledgeBaseServiceUIAutoConfiguration#namedEntityIdentifierFeatureInitializer}.
 * </p>
 */
public class NamedEntityIdentifierFeatureInitializer
    implements LayerInitializer
{
    private static final PackageResourceReference THUMBNAIL = new PackageResourceReference(
            MethodHandles.lookup().lookupClass(), "NamedEntityIdentifierFeatureInitializer.svg");

    private final AnnotationSchemaService annotationSchemaService;

    @Autowired
    public NamedEntityIdentifierFeatureInitializer(AnnotationSchemaService aAnnotationSchemaService)
    {
        annotationSchemaService = aAnnotationSchemaService;
    }

    @Override
    public String getName()
    {
        return "Named entity linking";
    }

    @Override
    public Optional<String> getDescription()
    {
        return Optional.of(Strings.getString("entity-linking-layer.description"));
    }

    @Override
    public Optional<ResourceReference> getThumbnail()
    {
        return Optional.of(THUMBNAIL);
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        // Because locks to token boundaries
        return asList(TokenLayerInitializer.class, NamedEntityLayerInitializer.class);
    }

    @Override
    public boolean alreadyApplied(Project aProject)
    {
        if (!annotationSchemaService.existsLayer(NamedEntity._TypeName, aProject)) {
            return false;
        }

        var neLayer = annotationSchemaService.findLayer(aProject, NamedEntity.class.getName());
        return annotationSchemaService.existsFeature("identifier", neLayer);
    }

    @Override
    public void configure(Project aProject) throws IOException
    {
        var neLayer = annotationSchemaService.findLayer(aProject, NamedEntity.class.getName());

        annotationSchemaService.createFeature(new AnnotationFeature(aProject, neLayer, "identifier",
                "identifier", TYPE_ANY_OBJECT, "Linked entity", null));
    }
}
