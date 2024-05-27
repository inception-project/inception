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

import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CHAIN_TYPE;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.config.ProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.wicket.resource.Strings;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ProjectInitializersAutoConfiguration#coreferenceLayerInitializer}.
 * </p>
 */
public class CoreferenceLayerInitializer
    implements LayerInitializer
{
    private static final String COREFERENCE_LAYER_NAME = "de.tudarmstadt.ukp.dkpro.core.api.coref.type.Coreference";

    private static final PackageResourceReference THUMBNAIL = new PackageResourceReference(
            MethodHandles.lookup().lookupClass(), "CoreferenceLayerInitializer.svg");

    private final AnnotationSchemaService annotationSchemaService;

    @Autowired
    public CoreferenceLayerInitializer(AnnotationSchemaService aAnnotationSchemaService)
    {
        annotationSchemaService = aAnnotationSchemaService;
    }

    @Override
    public String getName()
    {
        return "Co-reference annotation";
    }

    @Override
    public Optional<String> getDescription()
    {
        return Optional.of(Strings.getString("coreference-layer.description"));
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
                CoreferenceTypeTagSetInitializer.class, CoreferenceRelationTagSetInitializer.class);
    }

    @Override
    public boolean alreadyApplied(Project aProject)
    {
        return annotationSchemaService.existsLayer(COREFERENCE_LAYER_NAME, aProject);
    }

    @Override
    public void configure(Project aProject) throws IOException
    {
        var corefTypeTagSet = annotationSchemaService
                .getTagSet(CoreferenceTypeTagSetInitializer.TAG_SET_NAME, aProject);
        var corefRelTagSet = annotationSchemaService
                .getTagSet(CoreferenceRelationTagSetInitializer.TAG_SET_NAME, aProject);

        var base = new AnnotationLayer(COREFERENCE_LAYER_NAME, "Coreference", CHAIN_TYPE, aProject,
                true, AnchoringMode.TOKENS, OverlapMode.ANY_OVERLAP);
        base.setCrossSentence(true);
        annotationSchemaService.createOrUpdateLayer(base);

        // FIXME: should probably be replaced by calling
        // annotationSchemaService.getAdapter(base)
        // .initializeLayerConfiguration(annotationSchemaService);

        annotationSchemaService.createFeature(new AnnotationFeature(aProject, base, "referenceType",
                "referenceType", CAS.TYPE_NAME_STRING, "Coreference type", corefTypeTagSet));
        annotationSchemaService.createFeature(
                new AnnotationFeature(aProject, base, "referenceRelation", "referenceRelation",
                        CAS.TYPE_NAME_STRING, "Coreference relation", corefRelTagSet));
    }
}
