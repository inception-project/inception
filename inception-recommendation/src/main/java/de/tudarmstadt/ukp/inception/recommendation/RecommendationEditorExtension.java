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
import java.util.Optional;

import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.feedback.IFeedback;
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
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.event.AjaxRecommendationAcceptedEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.AjaxRecommendationRejectedEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommendationAcceptedEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommendationRejectedEvent;
import de.tudarmstadt.ukp.inception.recommendation.render.RecommendationRenderer;


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

        SourceDocument document = aState.getDocument();
        Predictions model =
                recommendationService.getPredictions(aState.getUser(), aState.getProject());
        Optional<AnnotationObject> prediction = model.getPredictionByVID(document, aVID);
        if (!prediction.isPresent()) {
            log.error("Could not find annotation in [{}] with id [{}]", document, aVID);
            aTarget.getPage().error("Could not find annotation");
            aTarget.addChildren(aTarget.getPage(), IFeedback.class);
            return;
        }

        // Obtain the predicted label
        String predictedValue = prediction.get().getLabel();
        
        Recommender recommender = recommendationService.getRecommender(aVID.getId());
        AnnotationLayer layer = annotationService.getLayer(aVID.getLayerId());

        // The feature of the predicted label
        AnnotationFeature feature = annotationService.getFeature(recommender.getFeature(), layer);
        SpanAdapter adapter = (SpanAdapter) annotationService.getAdapter(layer);

        // Get all annotations at this position
        Type type = CasUtil.getType(aJCas.getCas(), layer.getName());
        AnnotationFS annoFS = WebAnnoCasUtil.selectSingleFsAt(aJCas,
            type, aBegin, aEnd);
        int address;

        // Existing annotation at this position
        if (annoFS != null) {
            address = WebAnnoCasUtil.getAddr(annoFS);
        }
        else {
        // Create the annotation - this also takes care of attaching to an annotation if necessary
            address = adapter.add(aState, aJCas, aBegin, aEnd);
        }

        recommendationService
            .setFeatureValue(feature, predictedValue, adapter, aState, aJCas, address);

        // Send an event that the recommendation was accepted
        AnnotationFS fs = WebAnnoCasUtil.selectByAddr(aJCas, AnnotationFS.class, address);
        applicationEventPublisher.publishEvent(new RecommendationAcceptedEvent(this,
                document, aState.getUser().getUsername(), fs, feature, predictedValue));

        // Set selection to the accepted annotation
        aState.getSelection().selectSpan(new VID(address), aJCas, aBegin, aEnd);

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

        SourceDocument document = aState.getDocument();
        Optional<AnnotationObject> oPrediction = model.getPredictionByVID(document, aVID);
        if (!oPrediction.isPresent()) {
            log.error("Could not find annotation in [{}] with id [{}]", document, aVID);
            aTarget.getPage().error("Could not find annotation");
            aTarget.addChildren(aTarget.getPage(), IFeedback.class);
            return;
        }

        AnnotationObject prediction = oPrediction.get();
        String predictedValue = prediction.getLabel();
        String tokenText = aJCas.getDocumentText().substring(aBegin, aEnd);
        
        LearningRecord record = new LearningRecord();
        record.setUser(aState.getUser().getUsername());
        record.setSourceDocument(document);
        record.setUserAction(LearningRecordUserAction.REJECTED);
        record.setOffsetCharacterBegin(prediction.getOffset().getBeginCharacter());
        record.setOffsetCharacterEnd(prediction.getOffset().getEndCharacter());
        record.setOffsetTokenBegin(prediction.getOffset().getBeginToken());
        record.setOffsetTokenEnd(prediction.getOffset().getEndToken());
        record.setTokenText(tokenText);
        record.setAnnotation(predictedValue);
        record.setLayer(layer);
        record.setChangeLocation(LearningRecordChangeLocation.MAIN_EDITOR);
        record.setAnnotationFeature(feature);
        learningRecordService.create(record);
        aActionHandler.actionSelect(aTarget, aJCas);
        
        // Send an event that the recommendation was rejected
        aTarget.getPage().send(aTarget.getPage(), Broadcast.BREADTH, new
            AjaxRecommendationRejectedEvent(aTarget, aState, aVID));
        applicationEventPublisher.publishEvent(new RecommendationRejectedEvent(this,
                document, aState.getUser().getUsername(), aBegin, aEnd, tokenText,
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
