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
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
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
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.config.ProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializationRequest;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.wicket.resource.Strings;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ProjectInitializersAutoConfiguration#morphologicalFeaturesLayerInitializer}.
 * </p>
 */
public class MorphologicalFeaturesLayerInitializer
    implements LayerInitializer
{
    private static final PackageResourceReference THUMBNAIL = new PackageResourceReference(
            MethodHandles.lookup().lookupClass(), "MorphologicalFeaturesLayerInitializer.svg");

    private final AnnotationSchemaService annotationSchemaService;

    @Autowired
    public MorphologicalFeaturesLayerInitializer(AnnotationSchemaService aAnnotationSchemaService)
    {
        annotationSchemaService = aAnnotationSchemaService;
    }

    @Override
    public String getName()
    {
        return "Morphological analysis";
    }

    @Override
    public Optional<String> getDescription()
    {
        return Optional.of(Strings.getString("morpholological-features-layer.description"));
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
        return asList(TokenLayerInitializer.class);
    }

    @Override
    public boolean alreadyApplied(Project aProject)
    {
        return annotationSchemaService.existsLayer(MorphologicalFeatures.class.getName(), aProject);
    }

    @Override
    public void configure(ProjectInitializationRequest aRequest) throws IOException
    {
        var project = aRequest.getProject();
        var tokenLayer = annotationSchemaService.findLayer(project, Token.class.getName());

        var tokenMorphFeature = new AnnotationFeature(project, tokenLayer, "morph", "morph",
                MorphologicalFeatures.class.getName());
        annotationSchemaService.createFeature(tokenMorphFeature);

        var morphLayer = new AnnotationLayer(MorphologicalFeatures.class.getName(),
                "Morphological features", SpanLayerSupport.TYPE, project, true, SINGLE_TOKEN,
                NO_OVERLAP);
        morphLayer.setAttachType(tokenLayer);
        morphLayer.setAttachFeature(tokenMorphFeature);
        annotationSchemaService.createOrUpdateLayer(morphLayer);

        var valueFeature = new AnnotationFeature();
        valueFeature.setDescription("Morphological features");
        valueFeature.setName("value");
        valueFeature.setType(CAS.TYPE_NAME_STRING);
        valueFeature.setProject(project);
        valueFeature.setUiName("Features");
        valueFeature.setLayer(morphLayer);
        valueFeature.setIncludeInHover(true);
        valueFeature.setVisible(false);
        annotationSchemaService.createFeature(valueFeature);
    }
}
