/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.recommendation;

import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.ANNOTATION;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_TRANSIENT_ACCEPTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_TRANSIENT_REJECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation.MAIN_EDITOR;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType.ACCEPTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType.REJECTED;
import static java.util.Collections.emptyList;
import static org.apache.wicket.event.Broadcast.BREADTH;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.request.cycle.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.AcceptActionResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.DoActionResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.RejectActionResponse;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.uima.ICasUtil;
import de.tudarmstadt.ukp.inception.diam.editor.actions.SelectAnnotationHandler;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorExtension;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorExtensionImplBase;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Preferences;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RelationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;
import de.tudarmstadt.ukp.inception.recommendation.config.RecommenderServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.event.AjaxRecommendationAcceptedEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.AjaxRecommendationRejectedEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.PredictionsSwitchedEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommendationAcceptedEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommendationRejectedEvent;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.selection.SelectionChangedEvent;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetailResult;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.adapter.AnnotationException;

/**
 * This component hooks into the annotation editor in order to:
 * 
 * <ul>
 * <li>Render annotation suggestions into the main editor area;</li>
 * <li>Intercept user actions on the annotation suggestions, in particular accepting or rejecting
 * annotations.</li>
 * </ul>
 * <p>
 * This class is exposed as a Spring Component via
 * {@link RecommenderServiceAutoConfiguration#recommendationEditorExtension}.
 * </p>
 */
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
    private final UserDao userRegistry;

    @Autowired
    public RecommendationEditorExtension(AnnotationSchemaService aAnnotationService,
            RecommendationService aRecommendationService,
            LearningRecordService aLearningRecordService,
            ApplicationEventPublisher aApplicationEventPublisher, UserDao aUserRegistry)
    {
        annotationService = aAnnotationService;
        recommendationService = aRecommendationService;
        learningRecordService = aLearningRecordService;
        applicationEventPublisher = aApplicationEventPublisher;
        userRegistry = aUserRegistry;
    }

    @Override
    public String getBeanName()
    {
        return BEAN_NAME;
    }

    @Override
    public void handleAction(AnnotationActionHandler aActionHandler, AnnotatorState aState,
            AjaxRequestTarget aTarget, CAS aCas, VID aVID, String aAction)
        throws IOException, AnnotationException
    {
        // only process actions relevant to recommendation
        if (!aVID.getExtensionId().equals(BEAN_NAME)) {
            return;
        }

        ((AnnotationPageBase) aTarget.getPage()).ensureIsEditable();

        // Create annotation
        if (SelectAnnotationHandler.COMMAND.equals(aAction) || AcceptActionResponse.is(aAction)) {
            Predictions predictions = recommendationService.getPredictions(aState.getUser(),
                    aState.getProject());
            SourceDocument document = aState.getDocument();
            VID recommendationVid = VID.parse(aVID.getExtensionPayload());
            Optional<AnnotationSuggestion> prediction = predictions //
                    .getPredictionByVID(document, recommendationVid);

            if (prediction.isEmpty()) {
                log.error("Could not find annotation in [{}] with id [{}]", document,
                        recommendationVid);
                aTarget.getPage().error("Could not find annotation");
                aTarget.addChildren(aTarget.getPage(), IFeedback.class);
                return;
            }

            if (prediction.map(p -> p instanceof SpanSuggestion).get()) {
                actionAcceptSpanRecommendation((SpanSuggestion) prediction.get(), document,
                        aActionHandler, aState, aTarget, aCas, aVID);
            }

            if (prediction.map(p -> p instanceof RelationSuggestion).get()) {
                actionAcceptRelationRecommendation((RelationSuggestion) prediction.get(), document,
                        aActionHandler, aState, aTarget, aCas, aVID);
            }
        }
        else if (DoActionResponse.is(aAction) || RejectActionResponse.is(aAction)) {
            actionRejectRecommendation(aActionHandler, aState, aTarget, aCas, aVID);
        }
    }

    /**
     * Accept a suggestion.
     * 
     * <ul>
     * <li>Creates a new annotation or updates an existing one with a new feature value.</li>
     * <li>Marks the suggestions as hidden (not visible).</li>
     * <li>Logs the accepting to the learning log.</li>
     * <li>Sends events to the UI and application informing other components about the action.</li>
     * </ul>
     */
    private void actionAcceptSpanRecommendation(SpanSuggestion suggestion, SourceDocument document,
            AnnotationActionHandler aActionHandler, AnnotatorState aState,
            AjaxRequestTarget aTarget, CAS aCas, VID aVID)
        throws AnnotationException, IOException
    {
        // Upsert an annotation based on the suggestion
        AnnotationLayer layer = annotationService.getLayer(suggestion.getLayerId());
        AnnotationFeature feature = annotationService.getFeature(suggestion.getFeature(), layer);
        int address = recommendationService.upsertSpanFeature(annotationService,
                aState.getDocument(), aState.getUser().getUsername(), aCas, layer, feature,
                suggestion.getLabel(), suggestion.getBegin(), suggestion.getEnd());

        // Set selection to the accepted annotation and select it and load it into the detail editor
        // panel
        aState.getSelection().selectSpan(new VID(address), aCas, suggestion.getBegin(),
                suggestion.getEnd());
        aActionHandler.actionSelect(aTarget);
        aActionHandler.actionCreateOrUpdate(aTarget, aCas);

        // Log the action to the learning record
        learningRecordService.logSpanRecord(document, aState.getUser().getUsername(), suggestion,
                layer, feature, ACCEPTED, MAIN_EDITOR);

        hideSuggestionAndPublishAceptedEvents(suggestion, aState, aTarget, aCas, aVID, address);
    }

    private void actionAcceptRelationRecommendation(RelationSuggestion suggestion,
            SourceDocument document, AnnotationActionHandler aActionHandler, AnnotatorState aState,
            AjaxRequestTarget aTarget, CAS aCas, VID aVID)
        throws AnnotationException, IOException
    {
        AnnotationLayer layer = annotationService.getLayer(suggestion.getLayerId());
        AnnotationFeature feature = annotationService.getFeature(suggestion.getFeature(), layer);

        int address = recommendationService.upsertRelationFeature(annotationService, document,
                aState.getUser().getUsername(), aCas, layer, feature, suggestion);

        AnnotationFS relation = ICasUtil.selectAnnotationByAddr(aCas, address);

        Type type = CasUtil.getType(aCas, layer.getName());

        Feature sourceFeature = type.getFeatureByBaseName(FEAT_REL_SOURCE);
        Feature targetFeature = type.getFeatureByBaseName(FEAT_REL_TARGET);

        AnnotationFS source = (AnnotationFS) relation.getFeatureValue(sourceFeature);
        AnnotationFS target = (AnnotationFS) relation.getFeatureValue(targetFeature);

        // Set selection to the accepted annotation and select it and load it into the detail editor
        // panel
        aState.getSelection().selectArc(new VID(address), source, target);

        aActionHandler.actionSelect(aTarget);
        aActionHandler.actionCreateOrUpdate(aTarget, aCas);

        // Log the action to the learning record
        learningRecordService.logRelationRecord(document, aState.getUser().getUsername(),
                suggestion, layer, feature, ACCEPTED, MAIN_EDITOR);

        hideSuggestionAndPublishAceptedEvents(suggestion, aState, aTarget, aCas, aVID, address);
    }

    private void hideSuggestionAndPublishAceptedEvents(AnnotationSuggestion aSuggestion,
            AnnotatorState aState, AjaxRequestTarget aTarget, CAS aCas, VID aSuggestionVID,
            int aNewAnnotationAddress)
    {
        AnnotationLayer layer = annotationService.getLayer(aSuggestion.getLayerId());
        AnnotationFeature feature = annotationService.getFeature(aSuggestion.getFeature(), layer);
        SourceDocument document = aState.getDocument();

        // Hide the suggestion. This is faster than having to recalculate the visibility status for
        // the entire document or even for the part visible on screen.
        aSuggestion.hide(FLAG_TRANSIENT_ACCEPTED);

        // Send an application event that the suggestion has been accepted
        AnnotationFS fs = ICasUtil.selectByAddr(aCas, AnnotationFS.class, aNewAnnotationAddress);
        applicationEventPublisher.publishEvent(new RecommendationAcceptedEvent(this, document,
                aState.getUser().getUsername(), fs, feature, aSuggestion.getLabel()));

        // Send a UI event that the suggestion has been accepted
        aTarget.getPage().send(aTarget.getPage(), Broadcast.BREADTH,
                new AjaxRecommendationAcceptedEvent(aTarget, aState, aSuggestionVID));
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
            AnnotatorState aState, AjaxRequestTarget aTarget, CAS aCas, VID aVID)

        throws AnnotationException, IOException
    {
        Predictions predictions = recommendationService.getPredictions(aState.getUser(),
                aState.getProject());

        SourceDocument document = aState.getDocument();
        VID recommendationVID = VID.parse(aVID.getExtensionPayload());
        Optional<AnnotationSuggestion> oPrediction = predictions.getPredictionByVID(document,
                recommendationVID);

        if (!oPrediction.isPresent()) {
            log.error("Could not find annotation in [{}] with id [{}]", document,
                    recommendationVID);
            aTarget.getPage().error("Could not find annotation");
            aTarget.addChildren(aTarget.getPage(), IFeedback.class);
            return;
        }

        AnnotationSuggestion suggestion = oPrediction.get();

        Recommender recommender = recommendationService.getRecommender(recommendationVID.getId());
        AnnotationLayer layer = annotationService.getLayer(recommendationVID.getLayerId());
        AnnotationFeature feature = recommender.getFeature();

        // Hide the suggestion. This is faster than having to recalculate the visibility status for
        // the entire document or even for the part visible on screen.
        suggestion.hide(FLAG_TRANSIENT_REJECTED);

        // Send a UI event that the suggestion has been rejected
        aTarget.getPage().send(aTarget.getPage(), Broadcast.BREADTH,
                new AjaxRecommendationRejectedEvent(aTarget, aState, aVID));

        if (suggestion instanceof SpanSuggestion) {
            SpanSuggestion spanSuggestion = (SpanSuggestion) suggestion;
            // Log the action to the learning record
            learningRecordService.logSpanRecord(document, aState.getUser().getUsername(),
                    spanSuggestion, layer, feature, REJECTED, MAIN_EDITOR);

            // Send an application event that the suggestion has been rejected
            applicationEventPublisher.publishEvent(
                    new RecommendationRejectedEvent(this, document, aState.getUser().getUsername(),
                            spanSuggestion.getBegin(), spanSuggestion.getEnd(),
                            spanSuggestion.getCoveredText(), feature, spanSuggestion.getLabel()));

        }
        else if (suggestion instanceof RelationSuggestion) {
            RelationSuggestion relationSuggestion = (RelationSuggestion) suggestion;
            // TODO: Log rejection
            // TODO: Publish rejection event
        }

        // Trigger a re-rendering of the document
        Page page = aTarget.getPage();
        page.send(page, Broadcast.BREADTH, new SelectionChangedEvent(aTarget));

        // Send a UI event that the suggestion has been rejected
        page.send(page, Broadcast.BREADTH,
                new AjaxRecommendationRejectedEvent(aTarget, aState, recommendationVID));
    }

    @Override
    public void renderRequested(AnnotatorState aState)
    {
        log.trace("renderRequested()");

        // do not show predictions during curation or when viewing others' work
        if (!aState.getMode().equals(ANNOTATION)
                || !aState.getUser().getUsername().equals(userRegistry.getCurrentUsername())) {
            return;
        }

        // We activate new suggestions during rendering. For one, we don't have a push mechanism
        // at the moment. For another, even if we had it, it would be quite annoying to the user
        // if the UI kept updating itself without any the user expecting an update. The user does
        // expect an update when she makes some interaction, so we piggy-back on this expectation.
        boolean switched = recommendationService.switchPredictions(aState.getUser(),
                aState.getProject());
        log.trace("switchPredictions() returned {}", switched);

        // Notify other UI components on the page about the prediction switch such that they can
        // also update their state to remain in sync with the new predictions
        if (switched) {
            RequestCycle.get().find(AjaxRequestTarget.class)
                    .ifPresent(_target -> _target.getPage().send(_target.getPage(), BREADTH,
                            new PredictionsSwitchedEvent(_target, aState)));
        }
    }

    @Override
    public List<VLazyDetailResult> renderLazyDetails(SourceDocument aDocument, User aUser, VID aVid,
            AnnotationFeature aFeature, String aQuery)
    {
        Predictions predictions = recommendationService.getPredictions(aUser,
                aDocument.getProject());

        if (predictions == null) {
            return emptyList();
        }

        Preferences pref = recommendationService.getPreferences(aUser, aDocument.getProject());

        VID vid = VID.parse(aVid.getExtensionPayload());
        Optional<AnnotationSuggestion> representative = predictions.getPredictionByVID(aDocument,
                vid);
        if (representative.isEmpty()) {
            return emptyList();
        }

        AnnotationSuggestion sao = representative.get();
        Optional<SuggestionGroup<AnnotationSuggestion>> group = predictions
                .getGroupedPredictions(AnnotationSuggestion.class, aDocument.getName(),
                        aFeature.getLayer(), sao.getWindowBegin(), sao.getWindowEnd())
                .stream().filter(g -> g.contains(representative.get())).findFirst();

        if (group.isEmpty()) {
            return emptyList();
        }

        List<AnnotationSuggestion> sortedByScore = group.get()
                .bestSuggestionsByFeatureAndLabel(pref, aFeature.getName(), aQuery);

        List<VLazyDetailResult> details = new ArrayList<>();
        for (AnnotationSuggestion ao : sortedByScore) {
            List<String> items = new ArrayList<>();
            if (ao.getScore() != -1) {
                items.add(String.format("Score: %.2f", ao.getScore()));
            }
            if (ao.getScoreExplanation().isPresent()) {
                items.add("Explanation: " + ao.getScoreExplanation().get());
            }
            if (pref.isShowAllPredictions() && !ao.isVisible()) {
                items.add("Hidden: " + ao.getReasonForHiding());
            }
            details.add(new VLazyDetailResult(ao.getRecommenderName(),
                    "\n" + String.join("\n", items)));
        }

        return details;
    }
}
