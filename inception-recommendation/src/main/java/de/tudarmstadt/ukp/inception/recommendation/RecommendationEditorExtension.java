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

import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtensionImplBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.DoActionResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.SpanAnnotationResponse;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.recommendation.event.AjaxRecommendationAcceptedEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.AjaxRecommendationRejectedEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommendationAcceptedEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommendationRejectedEvent;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.model.LearningRecordChangeLocation;
import de.tudarmstadt.ukp.inception.recommendation.model.LearningRecordUserAction;
import de.tudarmstadt.ukp.inception.recommendation.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.render.RecommendationRenderer;
import de.tudarmstadt.ukp.inception.recommendation.service.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.service.RecommendationService;


@Component
public class RecommendationEditorExtension
    extends AnnotationEditorExtensionImplBase
    implements AnnotationEditorExtension
{
    public static final String BEAN_NAME = "recommendationEditorExtension";
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private @Autowired AnnotationSchemaService annotationService;
    private @Autowired RecommendationService recommendationService;
    private @Autowired LearningRecordService learningRecordService;
    private @Autowired ApplicationEventPublisher applicationEventPublisher;
    private @Autowired FeatureSupportRegistry fsRegistry;

    @Override
    public String getBeanName()
    {
        return BEAN_NAME;
    }

    @Override
    public void handleAction(AnnotationActionHandler aActionHandler, AnnotatorState aState,
            AjaxRequestTarget aTarget, JCas aJCas, VID aVID, String aAction, int aBegin, int aEnd)
        throws IOException, AnnotationException
    {
        // Create annotation
        if (SpanAnnotationResponse.is(aAction)) {
            actionAcceptRecommendation(aActionHandler, aState, aTarget, aJCas, aVID, aBegin, aEnd);
        }
        // Reject annotation
        else if (DoActionResponse.is(aAction)) {
            actionRejectRecommendation(aActionHandler, aState, aTarget, aJCas, aVID, aBegin, aEnd);
        }
    }
    
    private void actionAcceptRecommendation(AnnotationActionHandler aActionHandler,
            AnnotatorState aState, AjaxRequestTarget aTarget, JCas aJCas, VID aVID, int aBegin,
            int aEnd) throws AnnotationException, IOException
    {
        // Obtain the predicted label
        Predictions model = 
                recommendationService.getPredictions(aState.getUser(), aState.getProject());
        
        AnnotationObject prediction = model.getPredictionByVID(aVID);
        String predictedValue = prediction.getAnnotation();
        
        // Create the annotation - this also takes care of attaching to an annotation if necessary
        Recommender recommender = recommendationService.getRecommender(aVID.getId());
        AnnotationLayer layer = annotationService.getLayer(aVID.getLayerId());
        AnnotationFeature feature = annotationService.getFeature(recommender.getFeature(), layer);
        SpanAdapter adapter = (SpanAdapter) annotationService.getAdapter(layer);
        int id = adapter.add(aState, aJCas, aBegin, aEnd);
        String fsId = fsRegistry.getFeatureSupport(feature).getId();
        if (fsId.equals("conceptFeatureSupport") || fsId.equals("propertyFeatureSupport")) {
            String uiName = fsRegistry.getFeatureSupport(feature)
                .renderFeatureValue(feature, predictedValue);
            KBHandle kbHandle = new KBHandle(predictedValue, uiName);
            adapter.setFeatureValue(aState, aJCas, id, feature, kbHandle);
        }
        else {
            adapter.setFeatureValue(aState, aJCas, id, feature, predictedValue);
        }

        // Send an event that the recommendation was accepted
        AnnotationFS fs = WebAnnoCasUtil.selectByAddr(aJCas, AnnotationFS.class, id);
        applicationEventPublisher.publishEvent(new RecommendationAcceptedEvent(this,
                aState.getDocument(), aState.getUser().getUsername(), fs, feature, predictedValue));

        // Set selection to the accepted annotation
        aState.getSelection().selectSpan(new VID(id), aJCas, aBegin, aEnd);

        // ... select it and load it into the detail editor panel
        aActionHandler.actionSelect(aTarget, aJCas);            
        aActionHandler.actionCreateOrUpdate(aTarget, aJCas);

        aTarget.getPage().send(aTarget.getPage(), Broadcast.BREADTH, new
            AjaxRecommendationAcceptedEvent(aTarget, aState, aVID));
    }
    
    private void actionRejectRecommendation(AnnotationActionHandler aActionHandler,
            AnnotatorState aState, AjaxRequestTarget aTarget, JCas aJCas, VID aVID, int aBegin,
            int aEnd)
        throws AnnotationException
    {
        Predictions model = 
                recommendationService.getPredictions(aState.getUser(), aState.getProject());
        
        Recommender recommender = recommendationService.getRecommender(aVID.getId());
        AnnotationLayer layer = annotationService.getLayer(aVID.getLayerId());
        AnnotationFeature feature = annotationService.getFeature(recommender.getFeature(), layer);
        
        AnnotationObject prediction = model.getPredictionByVID(aVID);
        String predictedValue = prediction.getAnnotation();
        String tokenText = aJCas.getDocumentText().substring(aBegin, aEnd);
        
        LearningRecord record = new LearningRecord();
        record.setUser(aState.getUser().getUsername());
        record.setSourceDocument(aState.getDocument());
        record.setUserAction(LearningRecordUserAction.REJECTED);
        record.setOffsetCharacterBegin(prediction.getOffset().getBeginCharacter());
        record.setOffsetCharacterEnd(prediction.getOffset().getEndCharacter());
        record.setOffsetTokenBegin(prediction.getOffset().getBeginToken());
        record.setOffsetTokenEnd(prediction.getOffset().getEndToken());
        record.setTokenText(tokenText);
        record.setAnnotation(predictedValue);
        record.setLayer(layer);
        record.setChangeLocation(LearningRecordChangeLocation.MAIN_EDITOR);
        learningRecordService.create(record);
        aActionHandler.actionSelect(aTarget, aJCas);
        
        // Send an event that the recommendation was rejected
        aTarget.getPage().send(aTarget.getPage(), Broadcast.BREADTH, new
            AjaxRecommendationRejectedEvent(aTarget, aState, aVID));
        applicationEventPublisher.publishEvent(new RecommendationRejectedEvent(this,
                aState.getDocument(), aState.getUser().getUsername(), aBegin, aEnd, tokenText,
                feature, predictedValue));
    }

    @Override
    public void render(JCas jCas, AnnotatorState aState, VDocument vdoc)
    {
        recommendationService.switchPredictions(aState.getUser(), aState.getProject());
        RecommendationRenderer.render(vdoc, aState, jCas, annotationService, recommendationService, 
                learningRecordService, fsRegistry);
    }
}
