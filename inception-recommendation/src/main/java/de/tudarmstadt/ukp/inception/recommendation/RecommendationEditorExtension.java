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

import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_TRANSIENT_ACCEPTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_TRANSIENT_REJECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation.MAIN_EDITOR;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType.ACCEPTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType.REJECTED;

import java.io.IOException;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
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
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.event.AjaxRecommendationAcceptedEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.AjaxRecommendationRejectedEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.PredictionsSwitchedEvent;
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
    public static final String BEAN_NAME = AnnotationSuggestion.EXTENSION_ID;
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final AnnotationSchemaService annotationService;
    private final RecommendationService recommendationService;
    private final LearningRecordService learningRecordService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final FeatureSupportRegistry fsRegistry;
    private final DocumentService documentService;
    private final UserDao userRegistry;

    @Autowired
    public RecommendationEditorExtension(AnnotationSchemaService aAnnotationService,
            RecommendationService aRecommendationService,
            LearningRecordService aLearningRecordService,
            ApplicationEventPublisher aApplicationEventPublisher,
            FeatureSupportRegistry aFsRegistry, DocumentService aDocumentService, 
            UserDao aUserRegistry)
    {
        annotationService = aAnnotationService;
        recommendationService = aRecommendationService;
        learningRecordService = aLearningRecordService;
        applicationEventPublisher = aApplicationEventPublisher;
        fsRegistry = aFsRegistry;
        documentService = aDocumentService;
        userRegistry = aUserRegistry;
    }

    @Override
    public String getBeanName()
    {
        return BEAN_NAME;
    }

    @Override
    public void handleAction(AnnotationActionHandler aActionHandler, AnnotatorState aState,
            AjaxRequestTarget aTarget, CAS aCas, VID aVID, String aAction, int aBegin, int aEnd)
        throws IOException, AnnotationException
    {
        // Create annotation
        if (SpanAnnotationResponse.is(aAction)) {
            actionAcceptRecommendation(aActionHandler, aState, aTarget, aCas, aVID, aBegin, aEnd);
        }
        // Reject annotation
        else if (DoActionResponse.is(aAction)) {
            actionRejectRecommendation(aActionHandler, aState, aTarget, aCas, aVID, aBegin, aEnd);
        }
    }
    
    /**
     * Accept a suggestion.
     * 
     * <ul>
     * <li>Creates a new annotation or updates an existing one with a new feature
     * value.</li>
     * <li>Marks the suggestions as hidden (not visible).</li>
     * <li>Logs the accepting to the learning log.</li>
     * <li>Sends events to the UI and application informing other components about the action.</li>
     * </ul>
     */
    private void actionAcceptRecommendation(AnnotationActionHandler aActionHandler,
            AnnotatorState aState, AjaxRequestTarget aTarget, CAS aCas, VID aVID, int aBegin,
            int aEnd)
        throws AnnotationException, IOException
    {
        SourceDocument document = aState.getDocument();
        Predictions predictions = recommendationService.getPredictions(aState.getUser(),
                aState.getProject());
        Optional<AnnotationSuggestion> prediction = predictions.getPredictionByVID(document, aVID);

        if (!prediction.isPresent()) {
            log.error("Could not find annotation in [{}] with id [{}]", document, aVID);
            aTarget.getPage().error("Could not find annotation");
            aTarget.addChildren(aTarget.getPage(), IFeedback.class);
            return;
        }

        AnnotationSuggestion suggestion = prediction.get();

        // Upsert an annotation based on the suggestion
        AnnotationLayer layer = annotationService.getLayer(suggestion.getLayerId());
        AnnotationFeature feature = annotationService.getFeature(suggestion.getFeature(), layer);
        int address = recommendationService.upsertFeature(annotationService, aState.getDocument(),
                aState.getUser().getUsername(), aCas, layer, feature, suggestion.getLabel(),
                suggestion.getBegin(), suggestion.getEnd());

        // Hide the suggestion. This is faster than having to recalculate the visibility status for
        // the entire document or even for the part visible on screen.
        suggestion.hide(FLAG_TRANSIENT_ACCEPTED);

        // Set selection to the accepted annotation and select it and load it into the detail editor
        // panel
        aState.getSelection().selectSpan(new VID(address), aCas, aBegin, aEnd);
        aActionHandler.actionSelect(aTarget, aCas);            
        aActionHandler.actionCreateOrUpdate(aTarget, aCas);

        // Log the action to the learning record
        learningRecordService.logRecord(document, aState.getUser().getUsername(),
                suggestion, layer, feature, ACCEPTED, MAIN_EDITOR);
        
        // Send an application event that the suggestion has been accepted
        AnnotationFS fs = WebAnnoCasUtil.selectByAddr(aCas, AnnotationFS.class, address);
        applicationEventPublisher.publishEvent(new RecommendationAcceptedEvent(this,
                document, aState.getUser().getUsername(), fs, feature, suggestion.getLabel()));

        // Send a UI event that the suggestion has been accepted
        aTarget.getPage().send(aTarget.getPage(), Broadcast.BREADTH,
                new AjaxRecommendationAcceptedEvent(aTarget, aState, aVID));
    }
    
    /**
     * Reject a suggestion.
     * 
     * <ul>
     * <li>Marks the suggestions as hidden (not visible).</li>
     * <li>Logs the rejection to the learning log.</li>
     * <li>Sends events to the UI and application informing other components about the action.</li>
     * </ul>
     */
    private void actionRejectRecommendation(AnnotationActionHandler aActionHandler,
            AnnotatorState aState, AjaxRequestTarget aTarget, CAS aCas, VID aVID, int aBegin,
            int aEnd)
        throws AnnotationException
    {
        Predictions predictions = recommendationService.getPredictions(aState.getUser(),
                aState.getProject());
        
        SourceDocument document = aState.getDocument();
        Optional<AnnotationSuggestion> oPrediction = predictions.getPredictionByVID(document, aVID);
        
        if (!oPrediction.isPresent()) {
            log.error("Could not find annotation in [{}] with id [{}]", document, aVID);
            aTarget.getPage().error("Could not find annotation");
            aTarget.addChildren(aTarget.getPage(), IFeedback.class);
            return;
        }

        AnnotationSuggestion suggestion = oPrediction.get();
        Recommender recommender = recommendationService.getRecommender(aVID.getId());
        AnnotationLayer layer = annotationService.getLayer(aVID.getLayerId());
        AnnotationFeature feature = recommender.getFeature();

        // Hide the suggestion. This is faster than having to recalculate the visibility status for
        // the entire document or even for the part visible on screen.
        suggestion.hide(FLAG_TRANSIENT_REJECTED);

        // Log the action to the learning record
        learningRecordService.logRecord(document, aState.getUser().getUsername(),
                suggestion, layer, feature, REJECTED, MAIN_EDITOR);

        // Trigger a re-rendering of the document
        aActionHandler.actionSelect(aTarget, aCas);
        
        // Send an application event that the suggestion has been rejected
        applicationEventPublisher.publishEvent(
                new RecommendationRejectedEvent(this, document, aState.getUser().getUsername(),
                        aBegin, aEnd, suggestion.getCoveredText(), feature, suggestion.getLabel()));

        // Send a UI event that the suggestion has been rejected
        aTarget.getPage().send(aTarget.getPage(), Broadcast.BREADTH,
                new AjaxRecommendationRejectedEvent(aTarget, aState, aVID));
    }
    
    @Override
    public void render(CAS aCas, AnnotatorState aState, VDocument aVDoc,
                       int aWindowBeginOffset, int aWindowEndOffset)
    {
        // do not show predictions during curation or when viewing others' work
        if (!aState.getMode().equals(Mode.ANNOTATION) || 
                !aState.getUser().equals(userRegistry.getCurrentUser())) {
            return;
        }
        
        // We activate new suggestions during rendering. For one, we don't have a push mechanism
        // at the moment. For another, even if we had it, it would be quite annoying to the user
        // if the UI kept updating itself without any the user expecting an update. The user does
        // expect an update when she makes some interaction, so we piggy-back on this expectation.
        boolean switched = recommendationService.switchPredictions(aState.getUser(),
                aState.getProject());

        // Notify other UI components on the page about the prediction switch such that they can
        // also update their state to remain in sync with the new predictions
        if (switched) {
            RequestCycle.get().find(AjaxRequestTarget.class)
                    .ifPresent(_target -> _target.getPage().send(_target.getPage(),
                            Broadcast.BREADTH,
                            new PredictionsSwitchedEvent(_target, aCas, aState, aVDoc)));
        }

        // Add the suggestions to the visual document
        RecommendationRenderer.render(aVDoc, aState, aCas, annotationService,
                recommendationService, learningRecordService, fsRegistry, documentService,
                aWindowBeginOffset, aWindowEndOffset);
    }
}
