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

import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.SPAN_TYPE;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.project.ProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.config.ProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemArg;
import de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemArgLink;
import de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemPred;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ProjectInitializersAutoConfiguration#semPredArgLayerInitializer}.
 * </p>
 */
public class SemPredArgLayerInitializer
    implements LayerInitializer
{
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
    public void configure(Project aProject) throws IOException
    {
        AnnotationLayer semArgLayer = new AnnotationLayer(SemArg.class.getName(), "SemArg",
                SPAN_TYPE, aProject, true, AnchoringMode.TOKENS, OverlapMode.ANY_OVERLAP);
        semArgLayer.setCrossSentence(false);

        annotationSchemaService.createOrUpdateLayer(semArgLayer);

        AnnotationLayer semPredLayer = new AnnotationLayer(SemPred.class.getName(), "SemPred",
                SPAN_TYPE, aProject, true, AnchoringMode.TOKENS, OverlapMode.ANY_OVERLAP);
        semPredLayer.setCrossSentence(false);

        annotationSchemaService.createFeature(new AnnotationFeature(aProject, semPredLayer,
                "category", "category", CAS.TYPE_NAME_STRING,
                "Category of the semantic predicate, e.g. the frame identifier.", null));

        AnnotationFeature semPredArgumentsFeature = new AnnotationFeature();
        semPredArgumentsFeature.setName("arguments");
        semPredArgumentsFeature.setUiName("arguments");
        semPredArgumentsFeature.setDescription("Arguments of the semantic predicate");
        semPredArgumentsFeature.setType(SemArg.class.getName());
        semPredArgumentsFeature.setProject(aProject);
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
