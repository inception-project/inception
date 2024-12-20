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
package de.tudarmstadt.ukp.clarin.webanno.project.initializers;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.config.ProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.annotation.layer.LayerFactory;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializationRequest;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.wicket.resource.Strings;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ProjectInitializersAutoConfiguration#namedEntityLayerInitializer}.
 * </p>
 */
public class NamedEntityLayerInitializer
    implements LayerInitializer
{
    private static final PackageResourceReference THUMBNAIL = new PackageResourceReference(
            MethodHandles.lookup().lookupClass(), "NamedEntityLayerInitializer.svg");

    private final AnnotationSchemaService annotationSchemaService;

    @Autowired
    public NamedEntityLayerInitializer(AnnotationSchemaService aAnnotationSchemaService)
    {
        annotationSchemaService = aAnnotationSchemaService;
    }

    @Override
    public String getName()
    {
        return "Named entity tagging";
    }

    @Override
    public Optional<String> getDescription()
    {
        return Optional.of(Strings.getString("named-entity-layer.description"));
    }

    @Override
    public Optional<ResourceReference> getThumbnail()
    {
        return Optional.of(THUMBNAIL);
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        return asList(
                // Because locks to token boundaries
                TokenLayerInitializer.class,
                // Tagsets
                NamedEntityTagSetInitializer.class);
    }

    @Override
    public boolean alreadyApplied(Project aProject)
    {
        return annotationSchemaService.existsLayer(NamedEntity.class.getName(), aProject);
    }

    @Override
    public void configure(ProjectInitializationRequest aRequest) throws IOException
    {
        var project = aRequest.getProject();
        var nerTagSet = annotationSchemaService.getTagSet(NamedEntityTagSetInitializer.TAG_SET_NAME,
                project);

        var neLayer = LayerFactory.namedEntityLayer(project).build();

        annotationSchemaService.createOrUpdateLayer(neLayer);

        annotationSchemaService.createFeature(new AnnotationFeature(project, neLayer, "value",
                "value", CAS.TYPE_NAME_STRING, "Named entity type", nerTagSet));
    }
}
