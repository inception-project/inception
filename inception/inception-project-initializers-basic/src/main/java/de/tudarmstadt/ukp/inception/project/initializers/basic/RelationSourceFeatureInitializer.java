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

import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.PREFIX_SOURCE;
import static java.util.Collections.emptyList;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.project.api.FeatureInitializer;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.basic.config.InceptionBasicProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.wicket.resource.Strings;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link InceptionBasicProjectInitializersAutoConfiguration#relationSourceFeatureInitializer}.
 * </p>
 */
@Order(10)
public class RelationSourceFeatureInitializer
    implements FeatureInitializer
{
    private static final PackageResourceReference THUMBNAIL = new PackageResourceReference(
            MethodHandles.lookup().lookupClass(), "BasicSpanLayerInitializer.svg");

    private final AnnotationSchemaService annotationSchemaService;

    @Autowired
    public RelationSourceFeatureInitializer(AnnotationSchemaService aAnnotationSchemaService)
    {
        annotationSchemaService = aAnnotationSchemaService;
    }

    @Override
    public String getName()
    {
        return "Relation source feature";
    }

    @Override
    public Optional<String> getDescription()
    {
        return Optional.of(Strings.getString("source-feature.description"));
    }

    @Override
    public Optional<ResourceReference> getThumbnail()
    {
        return Optional.of(THUMBNAIL);
    }

    @Override
    public boolean alreadyApplied(AnnotationLayer aLayer)
    {
        if (!RelationLayerSupport.TYPE.equals(aLayer.getType())) {
            return false;
        }

        return annotationSchemaService.existsFeature(FEAT_REL_SOURCE, aLayer);
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        return emptyList();
    }

    @Override
    public void configure(AnnotationLayer aLayer) throws IOException
    {
        String attachTypeName;

        if (aLayer.getAttachType() != null && aLayer.getAttachFeature() != null
                && Token._TypeName.equals(aLayer.getAttachType().getName())
                && Token._FeatName_pos.equals(aLayer.getAttachFeature().getName())) {
            attachTypeName = POS._TypeName;
        }
        else {
            attachTypeName = aLayer.getAttachType() == null //
                    ? CAS.TYPE_NAME_ANNOTATION //
                    : aLayer.getAttachType().getName();
        }

        var sourceFeature = AnnotationFeature.builder() //
                .withLayer(aLayer) //
                .withType(PREFIX_SOURCE + attachTypeName) //
                .withName(FEAT_REL_SOURCE) //
                .withUiName("Source") //
                .withEnabled(true) //
                .build();

        annotationSchemaService.createFeature(sourceFeature);
    }
}
