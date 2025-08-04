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

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.ANY_OVERLAP;
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
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.config.ProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemArg;
import de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemArgLink;
import de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemPred;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializationRequest;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.wicket.resource.Strings;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ProjectInitializersAutoConfiguration#semPredArgLayerInitializer}.
 * </p>
 */
public class SemPredArgLayerInitializer
    implements LayerInitializer
{
    private static final PackageResourceReference THUMBNAIL = new PackageResourceReference(
            MethodHandles.lookup().lookupClass(), "SemPredArgLayerInitializer.svg");

    private final AnnotationSchemaService annotationSchemaService;

    @Autowired
    public SemPredArgLayerInitializer(AnnotationSchemaService aAnnotationSchemaService)
    {
        annotationSchemaService = aAnnotationSchemaService;
    }

    @Override
    public String getName()
    {
        return "Predicate argument structure";
    }

    @Override
    public Optional<String> getDescription()
    {
        return Optional.of(Strings.getString("predicate-argument-layer.description"));
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
        return annotationSchemaService.existsLayer(SemPred.class.getName(), aProject)
                || annotationSchemaService.existsLayer(SemArg.class.getName(), aProject);
    }

    @Override
    public void configure(ProjectInitializationRequest aRequest) throws IOException
    {
        var project = aRequest.getProject();
        var semArgLayer = new AnnotationLayer(SemArg.class.getName(), "SemArg",
                SpanLayerSupport.TYPE, project, true, TOKENS, ANY_OVERLAP);
        semArgLayer.setCrossSentence(false);

        annotationSchemaService.createOrUpdateLayer(semArgLayer);

        var semPredLayer = new AnnotationLayer(SemPred.class.getName(), "SemPred",
                SpanLayerSupport.TYPE, project, true, TOKENS, ANY_OVERLAP);
        semPredLayer.setCrossSentence(false);

        annotationSchemaService.createOrUpdateLayer(semPredLayer);

        annotationSchemaService.createFeature(new AnnotationFeature(project, semPredLayer,
                "category", "category", TYPE_NAME_STRING,
                "Category of the semantic predicate, e.g. the frame identifier.", null));

        var semPredArgumentsFeature = new AnnotationFeature();
        semPredArgumentsFeature.setName("arguments");
        semPredArgumentsFeature.setUiName("arguments");
        semPredArgumentsFeature.setDescription("Arguments of the semantic predicate");
        semPredArgumentsFeature.setType(SemArg.class.getName());
        semPredArgumentsFeature.setProject(project);
        semPredArgumentsFeature.setTagset(null);
        semPredArgumentsFeature.setMode(MultiValueMode.ARRAY);
        semPredArgumentsFeature.setLinkMode(LinkMode.WITH_ROLE);
        semPredArgumentsFeature.setLinkTypeName(SemArgLink.class.getName());
        semPredArgumentsFeature.setLinkTypeRoleFeatureName("role");
        semPredArgumentsFeature.setLinkTypeTargetFeatureName("target");
        semPredArgumentsFeature.setLayer(semPredLayer);
        annotationSchemaService.createFeature(semPredArgumentsFeature);

        annotationSchemaService.createOrUpdateLayer(semPredLayer);
    }
}
