/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation;

import java.io.IOException;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtensionImplBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.render.RecommendationRenderer;
import de.tudarmstadt.ukp.inception.recommendation.service.RecommendationService;

@Component
public class RecommendationEditorExtension
    extends AnnotationEditorExtensionImplBase
    implements AnnotationEditorExtension
{
    private Logger logger = LoggerFactory.getLogger(getClass());
    
    private @Resource AnnotationSchemaService annotationService;

    private @Resource RecommendationService recommendationService;

    public static final String BEAN_NAME = "recommendationEditorExtension";
    
    public RecommendationEditorExtension()
    {
        // TODO Auto-generated constructor stub
    }

    @Override
    public String getBeanName()
    {
        return BEAN_NAME;
    }

    @Override
    public void handleAction(AnnotationActionHandler aActionHandler, AnnotatorState aState,
            AjaxRequestTarget aTarget, JCas jCas, VID paramId, int aBegin, int aEnd)
        throws IOException, AnnotationException
    {
        Predictions model = 
                recommendationService.getPredictions(aState.getUser(), aState.getProject());
        
        AnnotationLayer layer = annotationService
                .getLayer(Long.valueOf(paramId.getLayerId()).longValue());
        SpanAdapter adapter = (SpanAdapter) annotationService.getAdapter(layer); 

        // Create the annotation - this also takes care of attaching to an
        // annotation if necessary
        int id = adapter.add(jCas, aBegin, aEnd); 
        for (AnnotationFeature feature : annotationService.listAnnotationFeature(layer)) {
            String predictedValue = model.getPrediction(paramId);
            if (StringUtils.isNotEmpty(predictedValue)) {
                adapter.setFeatureValue(feature, jCas, id, predictedValue);
            }
        }

        VID newVID = new VID(id);
        // Set selection to the accepted annotation
        aState.getSelection().selectSpan(newVID, jCas, aBegin, aEnd);

        // ... select it and load it into the detail editor panel
        aActionHandler.actionSelect(aTarget, jCas);
        
        aActionHandler.actionCreateOrUpdate(aTarget, jCas);
    }

    @Override
    public void render(JCas jCas, AnnotatorState aState, VDocument vdoc)
    {
        recommendationService.switchPredictions(aState.getUser(), aState.getProject());
        RecommendationRenderer.render(vdoc, aState, jCas, annotationService, recommendationService);
    }
}
