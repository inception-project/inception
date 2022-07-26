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
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.OVERLAP_ONLY;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.RELATION_TYPE;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.project.ProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.config.ProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ProjectInitializersAutoConfiguration#dependencyLayerInitializer}.
 * </p>
 */
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
    public String getName()
    {
        return "Dependency parsing";
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        return asList(
                // Because locks to token boundaries
                TokenLayerInitializer.class,
                // Because attaches to POS annotations in the UI
                PartOfSpeechLayerInitializer.class,
                // Tagsets
                DependencyFlavorTagSetInitializer.class, DependencyTypeTagSetInitializer.class);
    }

    @Override
    public boolean alreadyApplied(Project aProject)
    {
        return annotationSchemaService.existsLayer(Dependency.class.getName(), aProject);
    }

    @Override
    public void configure(Project aProject) throws IOException
    {
        // Dependency Layer
        AnnotationLayer depLayer = new AnnotationLayer(Dependency.class.getName(), "Dependency",
                RELATION_TYPE, aProject, true, SINGLE_TOKEN, OVERLAP_ONLY);
        AnnotationLayer tokenLayer = annotationSchemaService.findLayer(aProject,
                Token.class.getName());
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

        TagSet depTagSet = annotationSchemaService
                .getTagSet(DependencyTypeTagSetInitializer.TAG_SET_NAME, aProject);

        annotationSchemaService.createOrUpdateLayer(depLayer);
        annotationSchemaService
                .createFeature(new AnnotationFeature(aProject, depLayer, "DependencyType",
                        "Relation", CAS.TYPE_NAME_STRING, "Dependency relation", depTagSet));

        TagSet flavorsTagset = annotationSchemaService
                .getTagSet(DependencyFlavorTagSetInitializer.TAG_SET_NAME, aProject);

        annotationSchemaService.createFeature(new AnnotationFeature(aProject, depLayer, "flavor",
                "Flavor", CAS.TYPE_NAME_STRING, "Dependency relation", flavorsTagset));
    }
}
