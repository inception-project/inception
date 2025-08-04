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

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.OVERLAP_ONLY;
import static de.tudarmstadt.ukp.inception.project.initializers.phi.PhiSpanTagSetInitializer.PHI_SPAN_TAG_SET_NAME;
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
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.LayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.TokenLayerInitializer;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializationRequest;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.phi.config.InceptionPhiProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.wicket.resource.Strings;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link InceptionPhiProjectInitializersAutoConfiguration#phiSpanLayerInitializer}.
 * </p>
 */
public class PhiSpanLayerInitializer
    implements LayerInitializer
{
    private static final PackageResourceReference THUMBNAIL = new PackageResourceReference(
            MethodHandles.lookup().lookupClass(), "PhiSpanLayerInitializer.svg");

    public static final String PHI_SPAN_LAYER_NAME = "custom.PHI";
    public static final String PHI_SPAN_LABEL_FEATURE_NAME = "kind";

    private final AnnotationSchemaService annotationSchemaService;

    @Autowired
    public PhiSpanLayerInitializer(AnnotationSchemaService aAnnotationSchemaService)
    {
        annotationSchemaService = aAnnotationSchemaService;
    }

    @Override
    public String getName()
    {
        return "PHI span annotation";
    }

    @Override
    public Optional<String> getDescription()
    {
        return Optional.of(Strings.getString("phi-span-layer.description"));
    }

    @Override
    public Optional<ResourceReference> getThumbnail()
    {
        return Optional.of(THUMBNAIL);
    }

    @Override
    public boolean applyByDefault()
    {
        return false;
    }

    @Override
    public boolean alreadyApplied(Project aProject)
    {
        return annotationSchemaService.existsLayer(PHI_SPAN_LAYER_NAME, aProject);
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        return asList(
                // Because locks to token boundaries
                TokenLayerInitializer.class, //
                // Tagsets
                PhiSpanTagSetInitializer.class);
    }

    @Override
    public void configure(ProjectInitializationRequest aRequest) throws IOException
    {
        var spanLayer = AnnotationLayer.builder() //
                .withName(PHI_SPAN_LAYER_NAME) //
                .withUiName("PHI") //
                .withType(SpanLayerSupport.TYPE) //
                .withProject(aRequest.getProject()) //
                .withAnchoringMode(TOKENS) //
                .withOverlapMode(OVERLAP_ONLY) //
                .withCrossSentence(true) //
                .build();

        annotationSchemaService.createOrUpdateLayer(spanLayer);

        var spanTagSet = annotationSchemaService.getTagSet(PHI_SPAN_TAG_SET_NAME,
                aRequest.getProject());

        var labelFeature = AnnotationFeature.builder() //
                .withName(PHI_SPAN_LABEL_FEATURE_NAME) //
                .withUiName("Kind") //
                .withDescription("PHI kind") //
                .withType(TYPE_NAME_STRING) //
                .withLayer(spanLayer) //
                .withTagset(spanTagSet) //
                .build();

        annotationSchemaService.createFeature(labelFeature);
    }
}
