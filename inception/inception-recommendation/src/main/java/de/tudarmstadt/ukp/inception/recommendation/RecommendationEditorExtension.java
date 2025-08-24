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

import static de.tudarmstadt.ukp.clarin.webanno.model.LinkMode.WITH_ROLE;
import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.ANNOTATION;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation.MAIN_EDITOR;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.wicket.event.Broadcast.BREADTH;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.inception.diam.editor.actions.ScrollToHandler;
import de.tudarmstadt.ukp.inception.diam.editor.actions.SelectAnnotationHandler;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorExtension;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorExtensionImplBase;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.recommendation.actionbar.RecommenderActionBarPanel;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.event.AjaxRecommendationAcceptedEvent;
import de.tudarmstadt.ukp.inception.recommendation.api.event.AjaxRecommendationRejectedEvent;
import de.tudarmstadt.ukp.inception.recommendation.api.event.PredictionsSwitchedEvent;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RelationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.config.RecommenderServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.selection.SelectionChangedEvent;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetail;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetailGroup;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;

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
    private final ApplicationEventPublisher applicationEventPublisher;
    private final UserDao userService;
    private final FeatureSupportRegistry featureSupportRegistry;

    public RecommendationEditorExtension(AnnotationSchemaService aAnnotationService,
            RecommendationService aRecommendationService,
            ApplicationEventPublisher aApplicationEventPublisher, UserDao aUserRegistry,
            FeatureSupportRegistry aFeatureSupportRegistry)
    {
        annotationService = aAnnotationService;
        recommendationService = aRecommendationService;
        applicationEventPublisher = aApplicationEventPublisher;
        userService = aUserRegistry;
        featureSupportRegistry = aFeatureSupportRegistry;
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

        // Create annotation
        if (SelectAnnotationHandler.COMMAND.equals(aAction) || AcceptActionResponse.is(aAction)) {
            ((AnnotationPageBase) aTarget.getPage()).ensureIsEditable();

            var recommendationVid = VID.parse(aVID.getExtensionPayload());
            var predictions = recommendationService.getPredictions(aState.getUser(),
                    aState.getProject());
            var prediction = predictions.getPredictionByVID(aState.getDocument(),
                    recommendationVid);
            var document = aState.getDocument();

            if (prediction.isEmpty()) {
                log.error("Could not find annotation in [{}] with id [{}]", document,
                        recommendationVid);
                aTarget.getPage().error("Could not find annotation");
                aTarget.addChildren(aTarget.getPage(), IFeedback.class);
                return;
            }

            actionAcceptPrediction(aActionHandler, aState, aTarget, aCas, aVID, predictions,
                    prediction.get(), document);
        }
        else if (DoActionResponse.is(aAction) || RejectActionResponse.is(aAction)) {
            ((AnnotationPageBase) aTarget.getPage()).ensureIsEditable();

            actionRejectRecommendation(aActionHandler, aState, aTarget, aCas, aVID);
        }
        else if (ScrollToHandler.COMMAND.equals(aAction)) {
            var recommendationVid = VID.parse(aVID.getExtensionPayload());
            var predictions = recommendationService.getPredictions(aState.getUser(),
                    aState.getProject());
            var prediction = predictions.getPredictionByVID(aState.getDocument(),
                    recommendationVid);
            var page = (AnnotationPageBase) aTarget.getPage();
            if (prediction.map(p -> p instanceof SpanSuggestion).orElse(false)) {
                var suggestion = (SpanSuggestion) prediction.get();
                page.getAnnotationActionHandler().actionJump(aTarget, suggestion.getBegin(),
                        suggestion.getEnd());
            }
            if (prediction.map(p -> p instanceof RelationSuggestion).orElse(false)) {
                var suggestion = (RelationSuggestion) prediction.get();
                var position = suggestion.getPosition();
                page.getAnnotationActionHandler().actionJump(aTarget, position.getSourceBegin(),
                        position.getSourceEnd());
            }
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
    private void actionAcceptPrediction(AnnotationActionHandler aActionHandler,
            AnnotatorState aState, AjaxRequestTarget aTarget, CAS aCas, VID aVID,
            Predictions aPredictions, AnnotationSuggestion aSuggestion, SourceDocument document)
        throws AnnotationException, IOException
    {
        var page = (AnnotationPage) aTarget.getPage();
        var dataOwner = aState.getUser().getUsername();
        var sessionOwner = userService.getCurrentUsername();
        var layer = annotationService.getLayer(aSuggestion.getLayerId());
        var adapter = annotationService.getAdapter(layer);

        var annotation = (Annotation) recommendationService.acceptSuggestion(sessionOwner, document,
                dataOwner, aCas, aPredictions, aSuggestion, MAIN_EDITOR);

        page.writeEditorCas(aCas);

        // Set selection to the accepted annotation and select it and load it into the detail editor
        aState.getSelection().set(adapter.select(VID.of(annotation), annotation));
        page.getAnnotationActionHandler().actionSelect(aTarget);

        // Send a UI event that the suggestion has been accepted
        page.send(page, BREADTH, new AjaxRecommendationAcceptedEvent(aTarget, aState, aVID));
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
        var sessionOwner = userService.getCurrentUser();
        var predictions = recommendationService.getPredictions(sessionOwner, aState.getProject());

        var document = aState.getDocument();
        var recommendationVID = VID.parse(aVID.getExtensionPayload());
        var maybeSuggestion = predictions.getPredictionByVID(document, recommendationVID);

        if (!maybeSuggestion.isPresent()) {
            log.error("Could not find annotation in [{}] with id [{}]", document,
                    recommendationVID);
            aTarget.getPage().error("Could not find annotation");
            aTarget.addChildren(aTarget.getPage(), IFeedback.class);
            return;
        }

        recommendationService.rejectSuggestion(sessionOwner.getUsername(), document,
                aState.getUser().getUsername(), maybeSuggestion.get(), MAIN_EDITOR);

        // Send a UI event that the suggestion has been rejected
        aTarget.getPage().send(aTarget.getPage(), BREADTH,
                new AjaxRecommendationRejectedEvent(aTarget, aState, aVID));

        // Trigger a re-rendering of the document
        var page = aTarget.getPage();
        page.send(page, BREADTH, new SelectionChangedEvent(aTarget));
    }

    @Override
    public void renderRequested(AjaxRequestTarget aTarget, AnnotatorState aState)
    {
        log.trace("renderRequested()");

        // do not show predictions during curation or when viewing others' work
        var sessionOwner = userService.getCurrentUsername();
        if (aState.getMode() != ANNOTATION) {
            return;
        }

        // We activate new suggestions during rendering. For one, we don't have a push mechanism
        // at the moment. For another, even if we had it, it would be quite annoying to the user
        // if the UI kept updating itself without any the user expecting an update. The user does
        // expect an update when she makes some interaction, so we piggy-back on this expectation.
        var switched = recommendationService.switchPredictions(sessionOwner, aState.getProject());
        log.trace("switchPredictions() returned {}", switched);

        if (!switched) {
            return;
        }

        // Notify other UI components on the page about the prediction switch such that they can
        // also update their state to remain in sync with the new predictions
        applicationEventPublisher.publishEvent(
                new PredictionsSwitchedEvent(this, sessionOwner, aState.getDocument()));

        aTarget.appendJavaScript("document.body.classList.remove('"
                + RecommenderActionBarPanel.STATE_PREDICTIONS_AVAILABLE + "')");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> V getFeatureValue(SourceDocument aDocument, User aUser, CAS aCas, VID aVid,
            AnnotationFeature aFeature)
    {
        var predictions = recommendationService.getPredictions(aUser, aDocument.getProject());

        if (predictions == null) {
            return null;
        }

        var vid = VID.parse(aVid.getExtensionPayload());
        var ao = predictions.getPredictionByVID(aDocument, vid);
        if (ao.isEmpty() || !ao.get().getFeature().equals(aFeature.getName())) {
            return null;
        }

        return (V) featureSupportRegistry.findExtension(aFeature)
                .map(ext -> ext.wrapFeatureValue(aFeature, aCas, ao.get().getLabel())).orElse(null);
    }

    @Override
    public List<VLazyDetailGroup> lookupLazyDetails(SourceDocument aDocument, User aDataOwner,
            CAS aCas, VID aVid, AnnotationLayer aLayer)
    {
        var sessionOwner = userService.getCurrentUser();

        var predictions = recommendationService.getPredictions(sessionOwner,
                aDocument.getProject());

        if (predictions == null) {
            return emptyList();
        }

        var detailGroups = new ArrayList<VLazyDetailGroup>();
        for (var aFeature : annotationService.listAnnotationFeature(aLayer)) {
            if (aFeature.getLinkMode() == WITH_ROLE) {
                continue;
            }

            var vid = VID.parse(aVid.getExtensionPayload());
            var representative = predictions.getPredictionByVID(aDocument, vid);
            if (representative.isEmpty()
                    || !representative.get().getFeature().equals(aFeature.getName())) {
                continue;
            }

            var sao = representative.get();
            var group = predictions
                    .getGroupedPredictions(AnnotationSuggestion.class, aDocument,
                            aFeature.getLayer(), sao.getWindowBegin(), sao.getWindowEnd())
                    .stream() //
                    .filter(g -> g.contains(representative.get())) //
                    .findFirst();

            if (group.isEmpty()) {
                continue;
            }

            var pref = recommendationService.getPreferences(sessionOwner, aDocument.getProject());
            var label = defaultIfBlank(sao.getLabel(), null);
            var sortedByScore = group.get().bestSuggestionsByFeatureAndLabel(pref,
                    aFeature.getName(), label);

            var value = getFeatureValue(aDocument, sessionOwner, aCas, aVid, aFeature);
            featureSupportRegistry.findExtension(aFeature).orElseThrow()
                    .lookupLazyDetails(aFeature, value).forEach(detailGroups::add);

            for (var ao : sortedByScore) {
                var detailGroup = new VLazyDetailGroup(ao.getRecommenderName());
                // detailGroup.addDetail(new VLazyDetail("Age", String.valueOf(ao.getAge())));
                if (ao.getScore() > 0.0d) {
                    detailGroup.addDetail(
                            new VLazyDetail("Score", String.format("%.2f", ao.getScore())));
                }
                if (ao.getScoreExplanation().isPresent()) {
                    detailGroup.addDetail(
                            new VLazyDetail("Score explanation", ao.getScoreExplanation().get()));
                }
                if (ao.getCorrectionExplanation().isPresent()) {
                    detailGroup.addDetail(new VLazyDetail("Correction explanation",
                            ao.getCorrectionExplanation().get()));
                }
                if (pref.isShowAllPredictions() && !ao.isVisible()) {
                    detailGroup.addDetail(new VLazyDetail("Hidden", ao.getReasonForHiding()));
                }
                detailGroups.add(detailGroup);
            }
        }

        return detailGroups;
    }
}
