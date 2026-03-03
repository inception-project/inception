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

import static java.util.Collections.emptyList;
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
import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureSupport;
import de.tudarmstadt.ukp.inception.project.api.FeatureInitializer;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.basic.config.InceptionBasicProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.wicket.resource.Strings;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link InceptionBasicProjectInitializersAutoConfiguration#commentFeatureInitializer}.
 * </p>
 */
@Order(10)
public class CommentFeatureInitializer
    implements FeatureInitializer
{
    private static final PackageResourceReference THUMBNAIL = new PackageResourceReference(
            MethodHandles.lookup().lookupClass(), "BasicSpanLayerInitializer.svg");

    public static final String BASIC_COMMENT_FEATURE_NAME = "comment";

    private final AnnotationSchemaService annotationSchemaService;
    private final StringFeatureSupport stringFeatureSupport;

    @Autowired
    public CommentFeatureInitializer(AnnotationSchemaService aAnnotationSchemaService,
            StringFeatureSupport aStringFeatureSupport)
    {
        annotationSchemaService = aAnnotationSchemaService;
        stringFeatureSupport = aStringFeatureSupport;
    }

    @Override
    public String getName()
    {
        return "Comment feature";
    }

    @Override
    public Optional<String> getDescription()
    {
        return Optional.of(Strings.getString("comment-feature.description"));
    }

    @Override
    public Optional<ResourceReference> getThumbnail()
    {
        return Optional.of(THUMBNAIL);
    }

    @Override
    public boolean alreadyApplied(AnnotationLayer aLayer)
    {
        if (aLayer.isBuiltIn()) {
            return true;
        }

        return annotationSchemaService.existsFeature(BASIC_COMMENT_FEATURE_NAME, aLayer);
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        return emptyList();
    }

    @Override
    public void configure(AnnotationLayer aLayer) throws IOException
    {
        var commentFeature = AnnotationFeature.builder() //
                .withName(BASIC_COMMENT_FEATURE_NAME) //
                .withUiName("Comment") //
                .withDescription("Leave comments here.") //
                .withType(TYPE_NAME_STRING) //
                .withLayer(aLayer) //
                .withCuratable(false) //
                .withVisible(false) //
                .withIncludeInHover(true) //
                .build();

        var traits = stringFeatureSupport.createDefaultTraits();
        traits.setMultipleRows(true);
        traits.setDynamicSize(true);
        stringFeatureSupport.writeTraits(commentFeature, traits);

        annotationSchemaService.createFeature(commentFeature);
    }
}
