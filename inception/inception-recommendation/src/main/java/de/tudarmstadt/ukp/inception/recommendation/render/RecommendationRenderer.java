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

import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.ANNOTATION;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CHAIN_TYPE;

import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanAdapter;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.config.RecommenderServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.pipeline.RenderStep;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link RecommenderServiceAutoConfiguration#recommendationRenderer}.
 * </p>
 */
@Order(RenderStep.RENDER_SYNTHETIC_STRUCTURE)
public class RecommendationRenderer
    implements RenderStep
{
    public static final String ID = "RecommendationRenderer";

    private final AnnotationSchemaService annotationService;
    private final RecommendationSpanRenderer recommendationSpanRenderer;
    private final RecommendationRelationRenderer recommendationRelationRenderer;
    private final RecommendationService recommendationService;

    public RecommendationRenderer(AnnotationSchemaService aAnnotationService,
            RecommendationSpanRenderer aRecommendationSpanRenderer,
            RecommendationRelationRenderer aRecommendationRelationRenderer,
            RecommendationService aRecommendationService)
    {
        annotationService = aAnnotationService;
        recommendationSpanRenderer = aRecommendationSpanRenderer;
        recommendationRelationRenderer = aRecommendationRelationRenderer;
        recommendationService = aRecommendationService;
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public boolean accepts(RenderRequest aRequest)
    {
        AnnotatorState state = aRequest.getState();

        if (aRequest.getCas() == null) {
            return false;
        }

        // Do not show predictions on curation page
        if (state != null && state.getMode() != ANNOTATION) {
            return false;
        }

        return true;
    }

    @Override
    public void render(VDocument aVDoc, RenderRequest aRequest)
    {
        var cas = aRequest.getCas();

        if (cas == null || recommendationService == null) {
            return;
        }

        var predictions = recommendationService.getPredictions(aRequest.getSessionOwner(),
                aRequest.getProject());
        if (predictions == null) {
            return;
        }

        // Add the suggestions to the visual document
        for (var layer : aRequest.getVisibleLayers()) {
            if (Token.class.getName().equals(layer.getName())
                    || Sentence.class.getName().equals(layer.getName())
                    || CHAIN_TYPE.equals(layer.getType())
                    || !layer.isEnabled()) { /* Hide layer if not enabled */
                continue;
            }

            var adapter = annotationService.getAdapter(layer);
            if (adapter instanceof SpanAdapter) {
                recommendationSpanRenderer.render(aVDoc, aRequest, predictions,
                        (SpanAdapter) adapter);
            }

            if (adapter instanceof RelationAdapter) {
                recommendationRelationRenderer.render(aVDoc, aRequest, predictions,
                        (RelationAdapter) adapter);
            }
        }
    }
}
