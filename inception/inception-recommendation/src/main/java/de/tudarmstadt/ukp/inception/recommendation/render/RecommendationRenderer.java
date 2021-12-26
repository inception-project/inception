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
package de.tudarmstadt.ukp.inception.recommendation.render;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CHAIN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.ANNOTATION;
import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.CURATION;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.RelationAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.IntermediateRenderStep;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.RenderRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.RenderStep;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.config.RecommenderProperties;
import de.tudarmstadt.ukp.inception.recommendation.config.RecommenderServiceAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link RecommenderServiceAutoConfiguration#recommendationRenderer}.
 * </p>
 */
@Order(RenderStep.RENDER_SYNTHETIC_STRUCTURE)
public class RecommendationRenderer
    implements IntermediateRenderStep
{
    public static final String ID = "RecommendationRenderer";
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AnnotationSchemaService annotationService;
    private final RecommendationService recommendationService;
    private final FeatureSupportRegistry fsRegistry;
    private final RecommenderProperties recommenderProperties;
    private final UserDao userRegistry;

    public RecommendationRenderer(AnnotationSchemaService aAnnotationService,
            RecommendationService aRecommendationService,
            LearningRecordService aLearningRecordService,
            ApplicationEventPublisher aApplicationEventPublisher,
            FeatureSupportRegistry aFsRegistry, RecommenderProperties aRecommenderProperties,
            UserDao aUserRegistry)
    {
        super();
        annotationService = aAnnotationService;
        recommendationService = aRecommendationService;
        fsRegistry = aFsRegistry;
        recommenderProperties = aRecommenderProperties;
        userRegistry = aUserRegistry;
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public boolean accepts(RenderRequest aRequest)
    {
        // do not show predictions during curation or when viewing others' work
        if (!aRequest.getState().getMode().equals(ANNOTATION) || !aRequest.getAnnotationUser()
                .getUsername().equals(userRegistry.getCurrentUsername())) {
            return false;
        }

        if (aRequest.getCas() == null) {
            return false;
        }

        return true;
    }

    @Override
    public VDocument render(VDocument aVDoc, RenderRequest aRequest)
    {
        // Add the suggestions to the visual document
        for (AnnotationLayer layer : aRequest.getState().getAnnotationLayers()) {
            if (layer.getName().equals(Token.class.getName())
                    || layer.getName().equals(Sentence.class.getName())
                    || (layer.getType().equals(CHAIN_TYPE)
                            && CURATION == aRequest.getState().getMode())
                    || !layer.isEnabled()) { /* Hide layer if not enabled */
                continue;
            }

            TypeAdapter adapter = annotationService.getAdapter(layer);
            RecommendationTypeRenderer renderer = getRenderer(adapter);
            if (renderer != null) {
                renderer.render(aVDoc, aRequest);
            }
        }

        return aVDoc;
    }

    /**
     * Helper method to fetch a renderer for a given type. This is indented to be a temporary
     * solution. The final solution should be able to return renderers specific to a certain
     * visualization - one of which would be brat.
     */
    private RecommendationTypeRenderer getRenderer(TypeAdapter aTypeAdapter)
    {
        if (aTypeAdapter instanceof SpanAdapter) {
            return new RecommendationSpanRenderer((SpanAdapter) aTypeAdapter, recommendationService,
                    annotationService, fsRegistry, recommenderProperties);
        }

        if (aTypeAdapter instanceof RelationAdapter) {
            return new RecommendationRelationRenderer((RelationAdapter) aTypeAdapter,
                    recommendationService, annotationService, fsRegistry);
        }

        return null;
    }
}
