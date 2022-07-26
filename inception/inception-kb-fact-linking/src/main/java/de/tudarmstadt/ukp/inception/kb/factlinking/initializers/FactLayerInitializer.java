/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
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
package de.tudarmstadt.ukp.inception.kb.factlinking.initializers;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.SPAN_TYPE;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.project.ProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.LayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.TokenLayerInitializer;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.kb.factlinking.config.FactLinkingAutoConfiguration;
import de.tudarmstadt.ukp.inception.kb.factlinking.feature.FactLinkingConstants;
import de.tudarmstadt.ukp.inception.kb.factlinking.feature.PropertyFeatureSupport;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link FactLinkingAutoConfiguration#factLayerInitializer}.
 * </p>
 */
@Deprecated
public class FactLayerInitializer
    implements LayerInitializer
{
    private final AnnotationSchemaService annotationSchemaService;

    @Autowired
    public FactLayerInitializer(AnnotationSchemaService aAnnotationSchemaService)
    {
        annotationSchemaService = aAnnotationSchemaService;
    }

    @Override
    public String getName()
    {
        return "Fact annotation";
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
        return annotationSchemaService.existsLayer(FactLinkingConstants.FACT_LAYER, aProject);
    }

    @Override
    public void configure(Project aProject) throws IOException
    {
        AnnotationLayer factLayer = new AnnotationLayer(FactLinkingConstants.FACT_LAYER, "Fact",
                SPAN_TYPE, aProject, false, TOKENS, OverlapMode.NO_OVERLAP);
        factLayer.setCrossSentence(false);

        annotationSchemaService.createFeature(
                new AnnotationFeature(aProject, factLayer, "predicate", "1) Predicate",
                        PropertyFeatureSupport.PREFIX, "Predicate of a fact", null));

        AnnotationFeature subjectFeature = createLinkedFeature("subject", "2) Subject",
                "The subject of a fact.", FactLinkingConstants.SUBJECT_LINK, factLayer, aProject);
        annotationSchemaService.createFeature(subjectFeature);
        annotationSchemaService.createOrUpdateLayer(factLayer);

        AnnotationFeature objectFeature = createLinkedFeature("object", "3) Object",
                "The object of a fact.", FactLinkingConstants.OBJECT_LINK, factLayer, aProject);
        annotationSchemaService.createFeature(objectFeature);
        annotationSchemaService.createOrUpdateLayer(factLayer);

        AnnotationFeature qualifierFeature = createLinkedFeature("qualifiers", "4) Qualifiers",
                "The qualifier of a fact.", FactLinkingConstants.QUALIFIER_LINK, factLayer,
                aProject);
        annotationSchemaService.createFeature(qualifierFeature);
        annotationSchemaService.createOrUpdateLayer(factLayer);
    }

    private AnnotationFeature createLinkedFeature(String featureName, String featureUiName,
            String description, String linkedTypeName, AnnotationLayer aAnnotationLayer,
            Project aProject)
    {
        AnnotationFeature linkedFeature = new AnnotationFeature();
        linkedFeature.setName(featureName);
        linkedFeature.setUiName(featureUiName);
        linkedFeature.setDescription(description);
        linkedFeature.setType(NamedEntity.class.getName());
        linkedFeature.setProject(aProject);
        linkedFeature.setTagset(null);
        linkedFeature.setMode(MultiValueMode.ARRAY);
        linkedFeature.setLinkMode(LinkMode.WITH_ROLE);
        linkedFeature.setLinkTypeName(linkedTypeName);
        linkedFeature.setLinkTypeRoleFeatureName("role");
        linkedFeature.setLinkTypeTargetFeatureName("target");
        linkedFeature.setLayer(aAnnotationLayer);
        return linkedFeature;
    }
}
