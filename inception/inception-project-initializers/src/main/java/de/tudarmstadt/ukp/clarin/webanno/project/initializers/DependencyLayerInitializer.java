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

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.OVERLAP_ONLY;
import static java.util.Arrays.asList;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;

import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.config.ProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializationRequest;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.wicket.resource.Strings;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ProjectInitializersAutoConfiguration#dependencyLayerInitializer}.
 * </p>
 */
public class DependencyLayerInitializer
    implements LayerInitializer
{
    private final AnnotationSchemaService annotationSchemaService;

    private static final PackageResourceReference THUMBNAIL = new PackageResourceReference(
            MethodHandles.lookup().lookupClass(), "DependencyLayerInitializer.svg");

    @Autowired
    public DependencyLayerInitializer(AnnotationSchemaService aAnnotationSchemaService)
    {
        annotationSchemaService = aAnnotationSchemaService;
    }

    @Override
    public String getName()
    {
        return "Dependency parsing";
    }

    @Override
    public Optional<String> getDescription()
    {
        return Optional.of(Strings.getString("dependency-layer.description"));
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
                // Because attaches to POS annotations in the UI
                PartOfSpeechLayerInitializer.class,
                // Tagsets
                DependencyFlavorTagSetInitializer.class, DependencyTypeTagSetInitializer.class);
    }

    @Override
    public boolean alreadyApplied(Project aProject)
    {
        return annotationSchemaService.existsLayer(Dependency.class.getName(), aProject);
    }

    @Override
    public void configure(ProjectInitializationRequest aRequest) throws IOException
    {
        var project = aRequest.getProject();
        // Dependency Layer
        var depLayer = new AnnotationLayer(Dependency.class.getName(), "Dependency",
                RelationLayerSupport.TYPE, project, true, SINGLE_TOKEN, OVERLAP_ONLY);
        var tokenLayer = annotationSchemaService.findLayer(project, Token.class.getName());
        var tokenFeatures = annotationSchemaService.listAnnotationFeature(tokenLayer);
        AnnotationFeature tokenPosFeature = null;
        for (var feature : tokenFeatures) {
            if (feature.getName().equals("pos")) {
                tokenPosFeature = feature;
                break;
            }
        }
        depLayer.setAttachType(tokenLayer);
        depLayer.setAttachFeature(tokenPosFeature);
        annotationSchemaService.createOrUpdateLayer(depLayer);

        annotationSchemaService.getAdapter(depLayer)
                .initializeLayerConfiguration(annotationSchemaService);

        var depTagSet = annotationSchemaService
                .getTagSet(DependencyTypeTagSetInitializer.TAG_SET_NAME, project);

        annotationSchemaService.createFeature(new AnnotationFeature(project, depLayer,
                "DependencyType", "Relation", TYPE_NAME_STRING, "Dependency relation", depTagSet));

        var flavorsTagset = annotationSchemaService
                .getTagSet(DependencyFlavorTagSetInitializer.TAG_SET_NAME, project);

        annotationSchemaService.createFeature(new AnnotationFeature(project, depLayer, "flavor",
                "Flavor", TYPE_NAME_STRING, "Dependency relation", flavorsTagset));
    }
}
