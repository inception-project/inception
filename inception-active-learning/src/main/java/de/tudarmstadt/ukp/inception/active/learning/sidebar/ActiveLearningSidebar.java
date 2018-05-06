/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.active.learning.sidebar;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.JCasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.event.RenderAnnotationsEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VAnnotationMarker;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VMarker;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VTextMarker;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaChoiceRenderer;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.event.AjaxAfterAnnotationUpdateEvent;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.active.learning.ActiveLearningService;
import de.tudarmstadt.ukp.inception.active.learning.event.ActiveLearningRecommendationEvent;
import de.tudarmstadt.ukp.inception.active.learning.event.ActiveLearningSessionCompletedEvent;
import de.tudarmstadt.ukp.inception.active.learning.event.ActiveLearningSessionStartedEvent;
import de.tudarmstadt.ukp.inception.recommendation.RecommendationEditorExtension;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.event.AjaxRecommendationAcceptedEvent;
import de.tudarmstadt.ukp.inception.recommendation.event.AjaxRecommendationRejectedEvent;

public class ActiveLearningSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = -5312616540773904224L;
    
    private static final Logger LOG = LoggerFactory.getLogger(ActiveLearningSidebar.class);
    
    // Wicket component IDs used in the HTML file
    private static final String CID_MAIN_CONTAINER = "mainContainer";
    private static final String CID_HISTORY_LISTVIEW = "historyListview";
    private static final String CID_LEARNING_HISTORY_FORM = "learningHistoryForm";
    private static final String CID_REJECT_BUTTON = "rejectButton";
    private static final String CID_SKIP_BUTTON = "skipButton";
    private static final String CID_ACCEPT_BUTTON = "acceptButton";
    private static final String CID_RECOMMENDATION_COVERED_TEXT_LINK = "recommendationCoveredTextLink";
    private static final String CID_RECOMMENDED_DIFFERENCE = "recommendedDifference";
    private static final String CID_RECOMMENDED_CONFIDENCE = "recommendedConfidence";
    private static final String CID_RECOMMENDED_PREDITION = "recommendedPredition";
    private static final String CID_RECOMMENDATION_FORM = "recommendationForm";
    private static final String CID_LEARN_SKIPPED_ONES = "learnSkippedOnes";
    private static final String CID_ONLY_SKIPPED_RECOMMENDATION_LABEL = "onlySkippedRecommendationLabel";
    private static final String CID_LEARN_FROM_SKIPPED_RECOMMENDATION_FORM = "learnFromSkippedRecommendationForm";
    private static final String CID_NO_RECOMMENDATION_LABEL = "noRecommendationLabel";
    private static final String CID_LAYER_SELECTION_BUTTON = "layerSelectionButton";
    private static final String CID_SELECT_LAYER = "selectLayer";
    private static final String CID_SESSION_CONTROL_FORM = "sessionControlForm";
    private static final String CID_REMOVE_RECORD = "removeRecord";
    private static final String CID_USER_ACTION = "userAction";
    private static final String CID_RECOMMENDED_ANNOTATION = "recommendedAnnotation";
    private static final String CID_JUMP_TO_ANNOTATION = "jumpToAnnotation";
    
    private static final String ANNOTATION_MARKER = "VAnnotationMarker";
    private static final String TEXT_MARKER = "VTextMarker";
    
    private @SpringBean ActiveLearningService activeLearningService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean RecommendationService recommendationService;
    private @SpringBean LearningRecordService learningRecordService;
    private @SpringBean DocumentService documentService;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisherHolder;

    private IModel<AnnotationLayer> selectedLayer;
    private IModel<List<LearningRecord>> learningRecords;

    private final WebMarkupContainer mainContainer;

    private boolean sessionActive = false;
    private boolean hasUnseenRecommendation = false;
    private boolean hasSkippedRecommendation = false;
    
    private ActiveLearningRecommender activeLearningRecommender;
    private AnnotationObject currentRecommendation;
    private RecommendationDifference currentDifference;
    private AnnotationPage annotationPage;
    private Predictions predictionModel;
    private String vMarkerType = "";
    private VID highlightVID;
    private LearningRecord selectedRecord;
    private Date learnSkippedRecommendationTime;

    public ActiveLearningSidebar(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, JCasProvider aJCasProvider,
            AnnotationPage aAnnotationPage)
    {
        super(aId, aModel, aActionHandler, aJCasProvider, aAnnotationPage);

        annotationPage = aAnnotationPage;
        
        mainContainer = new WebMarkupContainer(CID_MAIN_CONTAINER);
        mainContainer.setOutputMarkupId(true);
        mainContainer.add(createSessionControlForm());
        mainContainer.add(createNoRecommendationLabel());
        mainContainer.add(createLearnFromSkippedRecommendationForm());
        mainContainer.add(createRecommendationOperationForm());
        mainContainer.add(createLearningHistory());
        add(mainContainer);
    }

    private Form<?> createSessionControlForm()
    {
        // Use the currently selected layer from the annotation detail editor panel as the
        // default choice in the active learning mode.
        List<AnnotationLayer> layersWithRecommenders = listLayersWithRecommenders();
        if (layersWithRecommenders.contains(getModelObject().getDefaultAnnotationLayer())) {
            selectedLayer = Model.of(getModelObject().getDefaultAnnotationLayer());
        }
        // If the currently selected layer has no recommenders, use the first one which has
        else if (!layersWithRecommenders.isEmpty()) {
            selectedLayer = Model.of(layersWithRecommenders.get(0));
        }
        // If there are no layers with recommenders, then choose nothing.
        else {
            // FIXME: in this case, we might display a nice message saying that none of the layers
            // have any recommenders configured.
            selectedLayer = Model.of();
        }
        
        Form<?> form = new Form<Void>(CID_SESSION_CONTROL_FORM);
        
        DropDownChoice<AnnotationLayer> layersDropdown = new DropDownChoice<>(CID_SELECT_LAYER);
        layersDropdown.setModel(selectedLayer);
        layersDropdown.setChoices(LambdaModel.of(this::listLayersWithRecommenders));
        layersDropdown.setChoiceRenderer(new LambdaChoiceRenderer<>(AnnotationLayer::getUiName));
        layersDropdown.add(LambdaBehavior.onConfigure(it -> it.setEnabled(!sessionActive)));
        layersDropdown.setOutputMarkupId(true);
        layersDropdown.setRequired(true);
        form.add(layersDropdown);
        
        LambdaAjaxButton<Void> startStopButton = new LambdaAjaxButton<>(
                CID_LAYER_SELECTION_BUTTON, this::actionStartStopTraining);
        startStopButton.setModel(LambdaModel.of(() -> sessionActive ? "Terminate" : "Start"));
        form.add(startStopButton);

        return form;
    }

    private List<AnnotationLayer> listLayersWithRecommenders()
    {
        return recommendationService
                .listLayersWithEnabledRecommenders(getModelObject().getProject());
    }
    
    private void actionStartStopTraining(AjaxRequestTarget target, Form<?> form)
        throws IOException
    {
        target.add(mainContainer);
        
        AnnotatorState annotatorState = getModelObject();
        annotatorState.setSelectedAnnotationLayer(selectedLayer.getObject());

        if (!sessionActive) {
            // Start new session
            sessionActive = true;
            learnSkippedRecommendationTime = null;
            
            activeLearningRecommender = new ActiveLearningRecommender(annotatorState,
                    selectedLayer.getObject());
            
            moveToNextRecommendation(target);
            
            applicationEventPublisherHolder.get().publishEvent(
                    new ActiveLearningSessionStartedEvent(this, annotatorState.getProject(),
                        annotatorState.getUser().getUsername()));
        }
        else {
            // Stop current session
            sessionActive = false;
            applicationEventPublisherHolder.get()
                    .publishEvent(new ActiveLearningSessionCompletedEvent(this,
                            annotatorState.getProject(), annotatorState.getUser().getUsername()));
        }
    }
    
    private void showAndHighlightRecommendationAndJumpToRecommendationLocation(
            AjaxRequestTarget aTarget)
    {
        if (currentDifference != null) {
            hasUnseenRecommendation = true;
            currentRecommendation = currentDifference.getRecommendation1();

            try {
                actionShowSelectedDocument(aTarget, documentService
                        .getSourceDocument(this.getModelObject().getProject(),
                            currentRecommendation.getDocumentName()),
                    currentRecommendation.getOffset().getBeginCharacter());
            }
            catch (IOException e) {
                LOG.error("Unable to switch to document : {} ", e.getMessage(), e);
                error("Unable to switch to document : " + e.getMessage());
                aTarget.addChildren(getPage(), IFeedback.class);
            }

            setShowingRecommendation();
            highlightCurrentRecommendation(aTarget);
        }
        else if (learnSkippedRecommendationTime == null) {
            hasUnseenRecommendation = false;
            hasSkippedRecommendation = activeLearningRecommender.hasRecommendationWhichIsSkipped(
                    learningRecordService, activeLearningService);
        }
        else {
            hasUnseenRecommendation = false;
            hasSkippedRecommendation = false;
        }
    }

    private void setShowingRecommendation()
    {
        AnnotatorState annotatorState = getModelObject();
        writeLearningRecordInDatabase(LearningRecordUserAction.SHOWN);
        
        applicationEventPublisherHolder.get().publishEvent(
                new ActiveLearningRecommendationEvent(this,
                        documentService.getSourceDocument(annotatorState.getProject(),
                                currentRecommendation.getDocumentName()),
                        currentRecommendation, annotatorState.getUser().getUsername(),
                        selectedLayer.getObject()));
    }

    private void highlightCurrentRecommendation(AjaxRequestTarget aTarget)
    {
        highlightRecommendation(aTarget, currentRecommendation.getOffset().getBeginCharacter(),
                currentRecommendation.getOffset().getEndCharacter(),
                currentRecommendation.getCoveredText(), currentRecommendation.getAnnotation());
    }
    
    private void highlightRecommendation(AjaxRequestTarget aTarget, int aBegin, int aEnd,
            String aText, String aRecommendation)
    {
        AnnotatorState annotatorState = ActiveLearningSidebar.this.getModelObject();
        predictionModel = recommendationService.getPredictions(annotatorState.getUser(),
                annotatorState.getProject());
        if (predictionModel != null) {
            Optional<AnnotationObject> aoForVID = predictionModel.getPrediction(aBegin, aEnd,
                    aRecommendation);
            if (aoForVID.isPresent()) {
                highlightVID = new VID(RecommendationEditorExtension.BEAN_NAME,
                        selectedLayer.getObject().getId(), (int) aoForVID.get().getRecommenderId(),
                        aoForVID.get().getId(), VID.NONE, VID.NONE);
                vMarkerType = ANNOTATION_MARKER;
            }
            else {
                error("Recommendation [" + aText + "] as [" + aRecommendation
                        + "] no longer exists");
                aTarget.addChildren(getPage(), IFeedback.class);
                
            }
        }
    }

    private Label createNoRecommendationLabel()
    {
        Label noRecommendation = new Label(CID_NO_RECOMMENDATION_LABEL,
            "There are no further suggestions.");
        noRecommendation.add(LambdaBehavior.onConfigure(component -> component
            .setVisible(sessionActive && !hasUnseenRecommendation && !hasSkippedRecommendation)));
        noRecommendation.setOutputMarkupPlaceholderTag(true);
        return noRecommendation;
    }

    private Form<?> createLearnFromSkippedRecommendationForm()
    {
        Form<?> learnFromSkippedRecommendationForm = new Form<Void>(
                CID_LEARN_FROM_SKIPPED_RECOMMENDATION_FORM);
        learnFromSkippedRecommendationForm.add(LambdaBehavior.onConfigure(component -> component
            .setVisible(sessionActive && !hasUnseenRecommendation && hasSkippedRecommendation)));
        learnFromSkippedRecommendationForm.setOutputMarkupPlaceholderTag(true);
        learnFromSkippedRecommendationForm.add(new Label(CID_ONLY_SKIPPED_RECOMMENDATION_LABEL, "There "
            + "are only skipped suggestions. Do you want to learn these again?"));
        learnFromSkippedRecommendationForm.add(new LambdaAjaxButton<>(CID_LEARN_SKIPPED_ONES,
            this::learnSkippedRecommendations));
        return learnFromSkippedRecommendationForm;
    }


    private void learnSkippedRecommendations(AjaxRequestTarget aTarget, Form<Void> aForm)
        throws IOException
    {
        learnSkippedRecommendationTime = new Date();
        
        moveToNextRecommendation(aTarget);
        
        aTarget.add(mainContainer);
    }

    private Form<?> createRecommendationOperationForm()
    {
        Form<?> recommendationForm = new Form<Void>(CID_RECOMMENDATION_FORM);
        recommendationForm.add(LambdaBehavior.onConfigure(component -> component.setVisible
            (sessionActive && hasUnseenRecommendation)));
        recommendationForm.setOutputMarkupPlaceholderTag(true);
        
        recommendationForm.add(createRecommendationCoveredTextLink());
        recommendationForm.add(new Label(CID_RECOMMENDED_PREDITION, LambdaModel.of(() -> 
                currentRecommendation != null ? currentRecommendation.getAnnotation() : null)));
        recommendationForm.add(new Label(CID_RECOMMENDED_CONFIDENCE, LambdaModel.of(() -> 
                currentRecommendation != null ? currentRecommendation.getConfidence() : 0.0)));
        recommendationForm.add(new Label(CID_RECOMMENDED_DIFFERENCE, LambdaModel.of(() -> 
                currentDifference != null ? currentDifference.getDifference() : 0.0)));
        
        recommendationForm.add(new LambdaAjaxLink(CID_ACCEPT_BUTTON, this::actionAccept));
        recommendationForm.add(new LambdaAjaxLink(CID_SKIP_BUTTON, this::actionSkip));
        recommendationForm.add(new LambdaAjaxLink(CID_REJECT_BUTTON, this::actionReject));
        
        return recommendationForm;
    }

    private LambdaAjaxLink createRecommendationCoveredTextLink()
    {
        LambdaAjaxLink link = new LambdaAjaxLink(CID_RECOMMENDATION_COVERED_TEXT_LINK,
                this::jumpToRecommendationLocationAndHighlightRecommendation);
//        link.setBody(LambdaModel.of(() -> Optional.ofNullable(currentRecommendation)
//                .map(it -> it.getCoveredText()).orElse("")));
        
        link.setEscapeModelStrings(false);
        link.setBody(LambdaModel.of(() -> {
            if (currentRecommendation == null) {
                return "";
            }
            else {
                try {
                    JCas jcas = getJCasProvider().get();
                    String text = jcas.getDocumentText();
                    int begin = currentRecommendation.getOffset().getBeginCharacter();
                    int end = currentRecommendation.getOffset().getEndCharacter();
                    int windowBegin = Math.max(begin - 25, 0);
                    int windowEnd = Math.min(end + 25, text.length());
                    
                    return 
                            Strings.escapeMarkup(text.substring(windowBegin, begin)) +
                            "<b>" + Strings.escapeMarkup(text.substring(begin, end)) + "</b>" +
                            Strings.escapeMarkup(text.substring(end, windowEnd));
                            
                }
                catch (Exception e) {
                    return "ERROR";
                }
            }
        }));
        return link;
    }

    private void jumpToRecommendationLocationAndHighlightRecommendation(AjaxRequestTarget aTarget)
        throws IOException
    {
        actionShowSelectedDocument(aTarget, documentService
                .getSourceDocument(this.getModelObject().getProject(),
                    currentRecommendation.getDocumentName()),
            currentRecommendation.getOffset().getBeginCharacter());
        highlightCurrentRecommendation(aTarget);
    }

    private void writeLearningRecordInDatabase(LearningRecordUserAction userAction)
    {
        AnnotatorState annotatorState = ActiveLearningSidebar.this.getModelObject();
        
        LearningRecord record = new LearningRecord();
        record.setUser(annotatorState.getUser().getUsername());
        record.setSourceDocument(annotatorState.getDocument());
        record.setTokenText(currentRecommendation.getCoveredText());
        record.setUserAction(userAction);
        record.setOffsetTokenBegin(currentRecommendation.getOffset().getBeginToken());
        record.setOffsetTokenEnd(currentRecommendation.getOffset().getEndToken());
        record.setOffsetCharacterBegin(currentRecommendation.getOffset().getBeginCharacter());
        record.setOffsetCharacterEnd(currentRecommendation.getOffset().getEndCharacter());
        record.setAnnotation(currentRecommendation.getAnnotation());
        record.setLayer(selectedLayer.getObject());
        record.setChangeLocation(LearningRecordChangeLocation.AL_SIDEBAR);

        learningRecordService.create(record);
    }

    private void actionAccept(AjaxRequestTarget aTarget) throws AnnotationException, IOException
    {
        aTarget.add(mainContainer);
        writeLearningRecordInDatabase(LearningRecordUserAction.ACCEPTED);
        
        // Create annotation from recommendation
        AnnotatorState annotatorState = ActiveLearningSidebar.this.getModelObject();
        JCas jCas = this.getJCasProvider().get();
        SpanAdapter adapter = (SpanAdapter) annotationService.getAdapter(selectedLayer.getObject());
        AnnotationObject acceptedRecommendation = currentRecommendation;
        int begin = acceptedRecommendation.getOffset().getBeginCharacter();
        int end = acceptedRecommendation.getOffset().getEndCharacter();
        int id = adapter.add(annotatorState, jCas, begin, end);
        for (AnnotationFeature feature : annotationService
                .listAnnotationFeature(selectedLayer.getObject())) {
            adapter.setFeatureValue(annotatorState, jCas, id, feature,
                    acceptedRecommendation.getAnnotation());
        }
        
        // Open accepted recommendation in the annotation detail editor panel
        VID vid = new VID(id);
        annotatorState.getSelection().selectSpan(vid, jCas, begin, end);
        AnnotationActionHandler aActionHandler = this.getActionHandler();
        aActionHandler.actionSelect(aTarget, jCas);
        
        // Save CAS
        aActionHandler.actionCreateOrUpdate(aTarget, jCas);
        
        moveToNextRecommendation(aTarget);
    }

    private void actionSkip(AjaxRequestTarget aTarget) throws IOException
    {
        aTarget.add(mainContainer);
        writeLearningRecordInDatabase(LearningRecordUserAction.SKIPPED);
        moveToNextRecommendation(aTarget);
    }

    private void actionReject(AjaxRequestTarget aTarget) throws IOException
    {
        aTarget.add(mainContainer);
        writeLearningRecordInDatabase(LearningRecordUserAction.REJECTED);
        moveToNextRecommendation(aTarget);
    }
    
    private void moveToNextRecommendation(AjaxRequestTarget aTarget)
    {
        annotationPage.actionRefreshDocument(aTarget);
        currentDifference = activeLearningRecommender
                .generateRecommendationWithLowestDifference(learningRecordService,
                        activeLearningService, learnSkippedRecommendationTime);
        showAndHighlightRecommendationAndJumpToRecommendationLocation(aTarget);
    }

    private Form<?> createLearningHistory()
    {
        Form<?> learningHistoryForm = new Form<Void>(CID_LEARNING_HISTORY_FORM)
        {
            private static final long serialVersionUID = -961690443085882064L;
        };
        learningHistoryForm.add(LambdaBehavior.onConfigure(component -> component
            .setVisible(sessionActive)));
        learningHistoryForm.setOutputMarkupPlaceholderTag(true);
        learningHistoryForm.setOutputMarkupId(true);

        learningHistoryForm.add(createLearningHistoryListView());
        return learningHistoryForm;
    }

    private ListView<LearningRecord> createLearningHistoryListView()
    {
        ListView<LearningRecord> learningHistory = new ListView<LearningRecord>(
                CID_HISTORY_LISTVIEW)
        {
            private static final long serialVersionUID = 5594228545985423567L;

            @Override
            protected void populateItem(ListItem<LearningRecord> item)
            {
                LearningRecord rec = item.getModelObject();
                
                LambdaAjaxLink textLink = new LambdaAjaxLink(CID_JUMP_TO_ANNOTATION,t -> 
                        jumpAndHighlightFromLearningHistory(t, item.getModelObject()));
                textLink.setBody(LambdaModel.of(rec::getTokenText));
                item.add(textLink);
                
                item.add(new Label(CID_RECOMMENDED_ANNOTATION, rec.getAnnotation()));
                item.add(new Label(CID_USER_ACTION, rec.getUserAction()));
                item.add(new LambdaAjaxLink(CID_REMOVE_RECORD, t -> 
                        actionRemoveHistoryItem(t, rec)));
            }
        };
        learningRecords = LambdaModel.of(this::listLearningRecords);
        learningHistory.setModel(learningRecords);
        return learningHistory;
    }

    private void jumpAndHighlightFromLearningHistory(AjaxRequestTarget aTarget,
            LearningRecord record)
        throws IOException
    {
        actionShowSelectedDocument(aTarget, record.getSourceDocument(),
            record.getOffsetCharacterBegin());

        if (record.getUserAction().equals(LearningRecordUserAction.REJECTED)) {
            highlightTextAndDisplayMessage(aTarget, record);
        }
        // if the suggestion still exists, highlight that suggestion.
        else if (activeLearningRecommender.checkRecommendationExist(activeLearningService,
                record)) {
            highlightRecommendation(aTarget, record.getOffsetCharacterBegin(),
                    record.getOffsetCharacterEnd(), record.getTokenText(), record.getAnnotation());
        }
        // if the suggestion doesn't exit -> if that suggestion is accepted and annotated,
        // highlight the annotation.
        // else, highlight the text.
        else if (!isAnnotatedInCas(record)) {
            highlightTextAndDisplayMessage(aTarget, record);
        }
    }

    private boolean isAnnotatedInCas(LearningRecord aRecord)
        throws IOException
    {
        JCas aJcas = this.getJCasProvider().get();
        Type type = CasUtil.getType(aJcas.getCas(), selectedLayer.getObject().getName());
        AnnotationFS annotationFS = WebAnnoCasUtil
            .selectSingleFsAt(aJcas, type, aRecord.getOffsetCharacterBegin(),
                aRecord.getOffsetCharacterEnd());
        if (annotationFS != null) {
            for (AnnotationFeature annotationFeature : annotationService
                .listAnnotationFeature(selectedLayer.getObject())) {
                String annotatedValue = WebAnnoCasUtil
                    .getFeature(annotationFS, annotationFeature.getName());
                if (annotatedValue.equals(aRecord.getAnnotation())) {
                    highlightVID = new VID(WebAnnoCasUtil.getAddr(annotationFS));
                    vMarkerType = ANNOTATION_MARKER;
                    return true;
                }
            }
        }
        return false;
    }

    private void highlightTextAndDisplayMessage(AjaxRequestTarget aTarget, LearningRecord aRecord)
    {
        selectedRecord = aRecord;
        vMarkerType = TEXT_MARKER;
        error("No annotation could be highlighted.");
        aTarget.addChildren(getPage(), IFeedback.class);
    }

    private List<LearningRecord> listLearningRecords()
    {
        AnnotatorState annotatorState = ActiveLearningSidebar.this.getModelObject();
        return learningRecordService.getAllRecordsByDocumentAndUserAndLayer(
                annotatorState.getDocument(), annotatorState.getUser().getUsername(),
                selectedLayer.getObject());
    }

    private void actionRemoveHistoryItem(AjaxRequestTarget aTarget,
            LearningRecord aRecord)
    {
        aTarget.add(mainContainer);
        annotationPage.actionRefreshDocument(aTarget);
        learningRecordService.delete(aRecord);
        learningRecords.detach();
    }

    @OnEvent
    public void afterAnnotationUpdateEvent(AjaxAfterAnnotationUpdateEvent aEvent)
    {
        AnnotatorState annotatorState = getModelObject();
        AnnotatorState eventState = aEvent.getAnnotatorState();

        //check active learning is active and same user and same document and same layer
        if (sessionActive && eventState.getUser().equals(annotatorState.getUser()) && eventState
            .getDocument().equals(annotatorState.getDocument()) && annotatorState
            .getSelectedAnnotationLayer().equals(selectedLayer.getObject())) {
            //check same document and same token
            if (annotatorState.getSelection().getBegin() == currentRecommendation.getOffset()
                .getBeginCharacter()
                && annotatorState.getSelection().getEnd() == currentRecommendation.getOffset()
                .getEndCharacter() && aEvent.getValue() != null) {
                moveToNextRecommendation(aEvent.getTarget());
            }
            aEvent.getTarget().add(mainContainer);
        }
    }

    @OnEvent
    public void onRecommendationRejectEvent(AjaxRecommendationRejectedEvent aEvent)
    {
        AnnotatorState annotatorState = getModelObject();
        AnnotatorState eventState = aEvent.getAnnotatorState();
        
        predictionModel = recommendationService.getPredictions(annotatorState.getUser(),
                annotatorState.getProject());
        
        if (sessionActive && eventState.getUser().equals(annotatorState.getUser())
                && eventState.getProject().equals(annotatorState.getProject())) {
            if (eventState.getDocument().equals(annotatorState.getDocument())
                    && aEvent.getVid().getLayerId() == selectedLayer.getObject().getId()
                    && predictionModel.getPredictionByVID(aEvent.getVid())
                            .equals(currentRecommendation)) {
                
                moveToNextRecommendation(aEvent.getTarget());
            }
            aEvent.getTarget().add(mainContainer);
        }
    }

    @OnEvent
    public void onRecommendationAcceptEvent(AjaxRecommendationAcceptedEvent aEvent)
    {
        AnnotatorState annotatorState = ActiveLearningSidebar.this.getModelObject();
        predictionModel = recommendationService.getPredictions(annotatorState.getUser(),
            annotatorState.getProject());
        AnnotatorState eventState = aEvent.getAnnotatorState();
        AnnotationObject acceptedRecommendation = predictionModel
                .getPredictionByVID(aEvent.getVid());

        LearningRecord record = new LearningRecord();
        record.setUser(eventState.getUser().getUsername());
        record.setSourceDocument(eventState.getDocument());
        record.setTokenText(acceptedRecommendation.getCoveredText());
        record.setUserAction(LearningRecordUserAction.ACCEPTED);
        record.setOffsetTokenBegin(acceptedRecommendation.getOffset().getBeginToken());
        record.setOffsetTokenEnd(acceptedRecommendation.getOffset().getEndToken());
        record.setOffsetCharacterBegin(acceptedRecommendation.getOffset().getBeginCharacter());
        record.setOffsetCharacterEnd(acceptedRecommendation.getOffset().getEndCharacter());
        record.setAnnotation(acceptedRecommendation.getAnnotation());
        record.setLayer(annotationService.getLayer(aEvent.getVid().getLayerId()));
        record.setChangeLocation(LearningRecordChangeLocation.MAIN_EDITOR);
        learningRecordService.create(record);

        if (sessionActive && currentRecommendation != null
                && eventState.getUser().equals(annotatorState.getUser())
                && eventState.getProject().equals(annotatorState.getProject())) {
            if (acceptedRecommendation.getOffset().equals(currentRecommendation.getOffset())) {
                moveToNextRecommendation(aEvent.getTarget());
            }
            aEvent.getTarget().add(mainContainer);
        }
    }

    @OnEvent
    public void onRenderAnnotations(RenderAnnotationsEvent aEvent)
    {
        if (vMarkerType.equals(ANNOTATION_MARKER)) {
            if (highlightVID != null) {
                aEvent.getVDocument().add(new VAnnotationMarker(VMarker.FOCUS, highlightVID));
            }
        }
        else if (vMarkerType.equals(TEXT_MARKER)) {
            if (selectedRecord != null) {
                AnnotatorState annotatorState = ActiveLearningSidebar.this.getModelObject();
                if (annotatorState.getWindowBeginOffset() <= selectedRecord
                    .getOffsetCharacterBegin()
                    && selectedRecord.getOffsetCharacterEnd() <= annotatorState
                    .getWindowEndOffset()) {
                    aEvent.getVDocument().add(new VTextMarker(VMarker.FOCUS,
                        selectedRecord.getOffsetCharacterBegin() - annotatorState
                            .getWindowBeginOffset(),
                        selectedRecord.getOffsetCharacterEnd() - annotatorState
                            .getWindowBeginOffset()));
                }
            }
        }
    }
}
