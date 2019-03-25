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

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.OVERLAP_ONLY;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.JsonImportUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.DependencyFlavor;

@Component
public class DependencyLayerInitializer
    implements LayerInitializer
{
    private final AnnotationSchemaService annotationSchemaService;
    
    @Autowired
    public DependencyLayerInitializer(AnnotationSchemaService aAnnotationSchemaService)
    {
        annotationSchemaService = aAnnotationSchemaService;
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        return asList(
                // Because locks to token boundaries
                TokenLayerInitializer.class, 
                // Because attaches to POS annotations in the UI
                PartOfSpeechLayerInitializer.class);
    }

    @Override
    public void configure(Project aProject) throws IOException
    {
        TagSet depTagSet = JsonImportUtil.importTagSetFromJson(aProject,
                new ClassPathResource("/tagsets/mul-dep-ud.json").getInputStream(),
                annotationSchemaService);
        
        // Dependency Layer
        AnnotationLayer depLayer = new AnnotationLayer(Dependency.class.getName(), "Dependency",
                RELATION_TYPE, aProject, true, SINGLE_TOKEN, OVERLAP_ONLY);
        AnnotationLayer tokenLayer = annotationSchemaService.getLayer(Token.class.getName(),
                aProject);
        List<AnnotationFeature> tokenFeatures = annotationSchemaService
                .listAnnotationFeature(tokenLayer);
        AnnotationFeature tokenPosFeature = null;
        for (AnnotationFeature feature : tokenFeatures) {
            if (feature.getName().equals("pos")) {
                tokenPosFeature = feature;
                break;
            }
        }
        depLayer.setAttachType(tokenLayer);
        depLayer.setAttachFeature(tokenPosFeature);

        annotationSchemaService.createLayer(depLayer);
        annotationSchemaService
                .createFeature(new AnnotationFeature(aProject, depLayer, "DependencyType",
                        "Relation", CAS.TYPE_NAME_STRING, "Dependency relation", depTagSet));

        String[] flavors = { DependencyFlavor.BASIC, DependencyFlavor.ENHANCED };
        String[] flavorDesc = { DependencyFlavor.BASIC, DependencyFlavor.ENHANCED };
        TagSet flavorsTagset = annotationSchemaService.createTagSet("Dependency flavors",
                "Dependency flavors", "mul", flavors, flavorDesc, aProject);

        annotationSchemaService.createFeature(new AnnotationFeature(aProject, depLayer, "flavor",
                "Flavor", CAS.TYPE_NAME_STRING, "Dependency relation", flavorsTagset));
    }
}
