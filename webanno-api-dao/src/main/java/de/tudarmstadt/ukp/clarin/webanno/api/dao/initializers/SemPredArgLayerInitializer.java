/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api.dao.initializers;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemArg;
import de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemArgLink;
import de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemPred;

@Component
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
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        // Because locks to token boundaries
        return asList(TokenLayerInitializer.class);
    }

    @Override
    public void configure(Project aProject) throws IOException
    {
        AnnotationLayer semArgLayer = new AnnotationLayer(SemArg.class.getName(), "SemArg",
                SPAN_TYPE, aProject, true);
        semArgLayer.setAllowStacking(true);
        semArgLayer.setCrossSentence(false);
        semArgLayer.setAnchoringMode(AnchoringMode.TOKENS);
        
        annotationSchemaService.createLayer(semArgLayer);
        
        
        AnnotationLayer semPredLayer = new AnnotationLayer(SemPred.class.getName(), "SemPred",
                SPAN_TYPE, aProject, true);
        semPredLayer.setAllowStacking(true);
        semPredLayer.setCrossSentence(false);
        semPredLayer.setAnchoringMode(AnchoringMode.TOKENS);
        
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
        
        annotationSchemaService.createLayer(semPredLayer);
    }
}
