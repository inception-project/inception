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
package de.tudarmstadt.ukp.inception.project.initializers.basic;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.OVERLAP_ONLY;
import static de.tudarmstadt.ukp.inception.project.initializers.basic.BasicRelationTagSetInitializer.BASIC_RELATION_TAG_SET_NAME;
import static de.tudarmstadt.ukp.inception.project.initializers.basic.BasicSpanLayerInitializer.BASIC_SPAN_LAYER_NAME;
import static java.util.Arrays.asList;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;

import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.LayerInitializer;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.basic.config.InceptionBasicProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.wicket.resource.Strings;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link InceptionBasicProjectInitializersAutoConfiguration#basicRelationLayerInitializer}.
 * </p>
 */
@Order(20)
public class BasicRelationLayerInitializer
    implements LayerInitializer
{
    private static final PackageResourceReference THUMBNAIL = new PackageResourceReference(
            MethodHandles.lookup().lookupClass(), "BasicRelationLayerInitializer.svg");

    public static final String BASIC_RELATION_LAYER_NAME = "custom.Relation";
    public static final String BASIC_RELATION_LABEL_FEATURE_NAME = "label";

    private final AnnotationSchemaService annotationSchemaService;

    @Autowired
    public BasicRelationLayerInitializer(AnnotationSchemaService aAnnotationSchemaService)
    {
        annotationSchemaService = aAnnotationSchemaService;
    }

    @Override
    public String getName()
    {
        return "Generic relation annotation";
    }

    @Override
    public Optional<String> getDescription()
    {
        return Optional.of(Strings.getString("basic-relation-layer.description"));
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
        return annotationSchemaService.existsLayer(BASIC_RELATION_LAYER_NAME, aProject);
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        return asList( //
                BasicSpanLayerInitializer.class,
                // Tagsets
                BasicRelationTagSetInitializer.class);
    }

    @Override
    public void configure(Project aProject) throws IOException
    {
        var spanLayer = annotationSchemaService.findLayer(aProject, BASIC_SPAN_LAYER_NAME);

        var relationLayer = AnnotationLayer.builder() //
                .withName(BASIC_RELATION_LAYER_NAME) //
                .withUiName("Relation") //
                .withType(RelationLayerSupport.TYPE) //
                .withProject(aProject) //
                .withAttachType(spanLayer).withAnchoringMode(TOKENS) //
                .withOverlapMode(OVERLAP_ONLY) //
                .withCrossSentence(true) //
                .build();

        annotationSchemaService.createOrUpdateLayer(relationLayer);

        annotationSchemaService.getAdapter(relationLayer)
                .initializeLayerConfiguration(annotationSchemaService);

        var relationTagSet = annotationSchemaService.getTagSet(BASIC_RELATION_TAG_SET_NAME,
                aProject);

        var labelFeature = AnnotationFeature.builder() //
                .withName(BASIC_RELATION_LABEL_FEATURE_NAME) //
                .withUiName("Label") //
                .withDescription("Relation label") //
                .withType(TYPE_NAME_STRING) //
                .withLayer(relationLayer) //
                .withTagset(relationTagSet) //
                .build();

        annotationSchemaService.createFeature(labelFeature);
    }
}
