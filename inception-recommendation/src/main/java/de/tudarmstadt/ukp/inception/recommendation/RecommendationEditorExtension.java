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

import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.ACCEPTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.REJECTED;

import java.io.IOException;
import java.util.Optional;

import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.request.cycle.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
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
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.event.AjaxPredictionsSwitchedEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.AjaxRecommendationAcceptedEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.AjaxRecommendationRejectedEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommendationAcceptedEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommendationRejectedEvent;
import de.tudarmstadt.ukp.inception.recommendation.render.RecommendationRenderer;

/**
 * This component hooks into the annotation editor in order to:
 * 
 * <ul>
 * <li>Render annotation suggestions into the main editor area;</li>
 * <li>Intercept user actions on the annotation suggestions, in particular accepting or rejecting
 *     annotatons.</li>
 * </ul>
 */
@Component(RecommendationEditorExtension.BEAN_NAME)
public class RecommendationEditorExtension
    extends AnnotationEditorExtensionImplBase
    implements AnnotationEditorExtension
{
    public static final String BEAN_NAME = "recommendationEditorExtension";
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final AnnotationSchemaService annotationService;
    private final RecommendationService recommendationService;
    private final LearningRecordService learningRecordService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final FeatureSupportRegistry fsRegistry;
    private final DocumentService documentService;

    @Autowired
    public RecommendationEditorExtension(AnnotationSchemaService aAnnotationService,
            RecommendationService aRecommendationService,
            LearningRecordService aLearningRecordService,
            ApplicationEventPublisher aApplicationEventPublisher,
            FeatureSupportRegistry aFsRegistry, DocumentService aDocumentService)
    {
        annotationService = aAnnotationService;
        recommendationService = aRecommendationService;
        learningRecordService = aLearningRecordService;
        applicationEventPublisher = aApplicationEventPublisher;
        fsRegistry = aFsRegistry;
        documentService = aDocumentService;
    }

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
        Predictions model = recommendationService.getPredictions(aState.getUser(),
                aState.getProject());
        Optional<AnnotationSuggestion> prediction = model.getPredictionByVID(document, aVID);

        if (!prediction.isPresent()) {
            log.error("Could not find annotation in [{}] with id [{}]", document, aVID);
            aTarget.getPage().error("Could not find annotation");
            aTarget.addChildren(aTarget.getPage(), IFeedback.class);
            return;
        }

        AnnotationSuggestion ao = prediction.get();

        // Obtain the predicted label
        String predictedValue = ao.getLabel();
        
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

        adapter.setFeatureValue(aState, aJCas, address, feature, predictedValue);

        // Remove from view
        ao.setVisible(false);

        // Log the action to the learning record
        learningRecordService.logLearningRecord(document, aState.getUser().getUsername(),
                ao, layer, feature, ACCEPTED);
        
        // Send an event that the recommendation was accepted
        AnnotationFS fs = WebAnnoCasUtil.selectByAddr(aJCas, AnnotationFS.class, address);
        applicationEventPublisher.publishEvent(new RecommendationAcceptedEvent(this,
                document, aState.getUser().getUsername(), fs, feature, predictedValue));

        // Set selection to the accepted annotation
        aState.getSelection().selectSpan(new VID(address), aJCas, aBegin, aEnd);

        // ... select it and load it into the detail editor panel
        aActionHandler.actionSelect(aTarget, aJCas);            
        aActionHandler.actionCreateOrUpdate(aTarget, aJCas);

        aTarget.getPage().send(aTarget.getPage(), Broadcast.BREADTH,
                new AjaxRecommendationAcceptedEvent(aTarget, aState, aVID));
    }
    
    private void actionRejectRecommendation(AnnotationActionHandler aActionHandler,
            AnnotatorState aState, AjaxRequestTarget aTarget, JCas aJCas, VID aVID, int aBegin,
            int aEnd)
        throws AnnotationException
    {
        Predictions model = recommendationService.getPredictions(aState.getUser(),
                aState.getProject());
        
        SourceDocument document = aState.getDocument();
        Optional<AnnotationSuggestion> oPrediction = model.getPredictionByVID(document, aVID);
        if (!oPrediction.isPresent()) {
            log.error("Could not find annotation in [{}] with id [{}]", document, aVID);
            aTarget.getPage().error("Could not find annotation");
            aTarget.addChildren(aTarget.getPage(), IFeedback.class);
            return;
        }

        AnnotationSuggestion prediction = oPrediction.get();
        Recommender recommender = recommendationService.getRecommender(aVID.getId());
        AnnotationLayer layer = annotationService.getLayer(aVID.getLayerId());
        AnnotationFeature feature = annotationService.getFeature(recommender.getFeature(), layer);

        // Remove from view
        prediction.setVisible(false);

        // Log the action to the learning record
        learningRecordService.logLearningRecord(document, aState.getUser().getUsername(),
                prediction, layer, feature, REJECTED);

        // Send an UI event that the recommendation was rejected
        aTarget.getPage().send(aTarget.getPage(), Broadcast.BREADTH,
                new AjaxRecommendationRejectedEvent(aTarget, aState, aVID));
        
        // Send an application event that the recommendation was rejected
        applicationEventPublisher.publishEvent(
                new RecommendationRejectedEvent(this, document, aState.getUser().getUsername(),
                        aBegin, aEnd, prediction.getCoveredText(), feature, prediction.getLabel()));
    }
    
    @Override
    public void render(JCas jCas, AnnotatorState aState, VDocument aVDoc)
    {
        // We activate new suggestions during rendering. For one, we don't have a push mechanism
        // at the moment. For another, even if we had it, it would be quite annoying to the user
        // if the UI kept updating itself without any the user expecting an update. The user does
        // expect an update when she makes some interaction, so we piggy-back on this expectation.
        recommendationService.switchPredictions(aState.getUser(), aState.getProject());
        
        // Add the suggestions to the visual document
        RecommendationRenderer.render(aVDoc, aState, jCas, annotationService, recommendationService,
                learningRecordService, fsRegistry, documentService);
                
        // Notify other UI components on the page about the prediction switch such that they can
        // also update their state to remain in sync with the new predictions
        RequestCycle.get().find(AjaxRequestTarget.class)
                .ifPresent(_target -> _target.getPage().send(_target.getPage(), Broadcast.BREADTH,
                        new AjaxPredictionsSwitchedEvent(_target)));
    }
}
